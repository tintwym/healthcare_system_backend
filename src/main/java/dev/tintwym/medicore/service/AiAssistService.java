package dev.tintwym.medicore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.tintwym.medicore.config.AiProperties;
import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.InvoiceRepository;
import dev.tintwym.medicore.repo.PatientRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistService {
  private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);
  private static final int MAX_MESSAGE_CHARS = 4000;
  private static final Set<String> INTENTS =
      Set.of("chat", "explain_vitals", "draft_summary", "draft_message", "billing_help");

  private static final String STRUCTURED_SUMMARY_HINT =
      " When drafting a summary, reply ONLY with this exact structure (no preamble):\n"
          + "COURSE:\n<hospital course paragraph(s)>\n\n"
          + "INSTRUCTIONS:\n<follow-up / home instructions>\n\n"
          + "WARNINGS:\n- <warning 1>\n- <warning 2>\n- <warning 3>\n";

  private static final String MESSAGE_ONLY_HINT =
      " When drafting a message, reply with ONLY the message body the user can paste/send. No quotes, no markdown headings, no disclaimer footer.";

  private final AiProperties ai;
  private final PatientRepository patients;
  private final InvoiceRepository invoices;
  private final ObjectMapper mapper;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

  public AiAssistService(
      AiProperties ai, PatientRepository patients, InvoiceRepository invoices, ObjectMapper mapper) {
    this.ai = ai;
    this.patients = patients;
    this.invoices = invoices;
    this.mapper = mapper;
  }

  public Map<String, Object> status() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("enabled", ai.isEnabled());
    m.put("configured", ai.isConfigured());
    m.put("provider", ai.isConfigured() ? "gemini" : "fallback");
    m.put("model", ai.isConfigured() ? ai.getModel() : "medicore-rules");
    m.put(
        "disclaimer",
        "Advisory only — not a diagnosis or substitute for clinical judgment or emergency care.");
    return m;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> assist(Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    String message = String.valueOf(body.getOrDefault("message", "")).trim();
    if (message.isBlank()) throw new ApiException(400, "message required");
    if (message.length() > MAX_MESSAGE_CHARS) {
      throw new ApiException(400, "message too long (max " + MAX_MESSAGE_CHARS + " chars)");
    }

    String intent = normalizeIntent(String.valueOf(body.getOrDefault("intent", "chat")));
    String patientId = resolvePatientId(user, body.get("patientId"));
    String chartContext = patientId == null ? "" : buildPatientContext(patientId, user);
    String extra =
        body.get("context") == null ? "" : String.valueOf(body.get("context")).trim();
    if (extra.length() > 6000) extra = extra.substring(0, 6000);

    String system = systemPrompt(user, intent);
    String userPrompt = buildUserPrompt(intent, message, chartContext, extra);

    String reply;
    String provider;
    if (ai.isConfigured()) {
      try {
        reply = callGemini(system, userPrompt);
        provider = "gemini";
      } catch (Exception ex) {
        log.warn("Gemini call failed, using fallback: {}", ex.getMessage());
        reply = fallbackReply(intent, message, chartContext, user);
        provider = "fallback";
      }
    } else {
      reply = fallbackReply(intent, message, chartContext, user);
      provider = "fallback";
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("reply", reply);
    out.put("intent", intent);
    out.put("provider", provider);
    out.put("patientId", patientId);
    out.put(
        "disclaimer",
        "Advisory only — not a diagnosis. For emergencies call local emergency services.");
    return out;
  }

  private String normalizeIntent(String raw) {
    String intent = raw == null ? "chat" : raw.trim().toLowerCase(Locale.ROOT);
    if (!INTENTS.contains(intent)) intent = "chat";
    return intent;
  }

  private String resolvePatientId(AuthUser user, Object requested) {
    if (user.getRole() == UserRole.patient) {
      String scoped = user.getPatientId();
      if (scoped == null || scoped.isBlank()) {
        throw new ApiException(403, "Patient account is not linked");
      }
      return scoped;
    }
    if (requested == null) return null;
    String id = String.valueOf(requested).trim();
    return id.isBlank() || "null".equalsIgnoreCase(id) ? null : id;
  }

  private String systemPrompt(AuthUser user, String intent) {
    boolean patient = user.getRole() == UserRole.patient;
    String roleLine =
        patient
            ? "You assist a patient using Medicore Care. Use plain, calm language. Never diagnose or prescribe. Suggest contacting their care team for clinical decisions. Encourage emergency care for chest pain, severe SOB, stroke signs, or uncontrolled bleeding."
            : "You assist licensed hospital staff on Medicore Healthcare OS. Draft concise clinical language. Flag uncertainty. Never invent labs, vitals, meds, or orders that are not in the provided chart context. Staff must review before charting.";

    String intentLine =
        switch (intent) {
          case "explain_vitals" ->
              "Focus on explaining recent vitals vs typical adult ranges (temps are °F) and when to escalate.";
          case "draft_summary" ->
              "Draft an after-visit / discharge style summary." + STRUCTURED_SUMMARY_HINT;
          case "draft_message" ->
              "Draft a short secure message the user can send (or a staff reply)." + MESSAGE_ONLY_HINT;
          case "billing_help" ->
              "Explain invoices, copays, and next payment steps using only provided billing context. Do not invent charges.";
          default -> "Answer helpfully within Medicore care workflows (meds, visits, vitals, messages, billing).";
        };

    return roleLine
        + " "
        + intentLine
        + " If chart context is missing, say what you need. Reply in the user's language when clear (English or Myanmar). Keep replies under 350 words unless drafting a summary.";
  }

  private String buildUserPrompt(String intent, String message, String chart, String extra) {
    StringBuilder sb = new StringBuilder();
    sb.append("Intent: ").append(intent).append("\n");
    sb.append("User request:\n").append(message).append("\n");
    if (!chart.isBlank()) {
      sb.append("\n--- Chart context (authoritative; do not invent beyond this) ---\n");
      sb.append(chart).append("\n");
    }
    if (!extra.isBlank()) {
      sb.append("\n--- Extra context ---\n").append(extra).append("\n");
    }
    return sb.toString();
  }

  private String buildPatientContext(String patientId, AuthUser user) {
    Patient p =
        patients.findById(patientId).orElseThrow(() -> new ApiException(404, "Patient not found"));
    // Touch collections inside transaction
    p.getAllergies().size();
    p.getChronicConditions().size();
    p.getMedications().size();
    p.getVitals().size();
    p.getLabResults().size();
    p.getClinicalNotes().size();

    boolean patientCaller = user.getRole() == UserRole.patient;
    StringBuilder sb = new StringBuilder();
    sb.append("Patient: ")
        .append(p.getFirstName())
        .append(' ')
        .append(p.getLastName())
        .append(" (MRN ")
        .append(patientCaller ? "****" : p.getMrn())
        .append(")\n");
    sb.append("Age/sex: ").append(p.getAge()).append(" / ").append(p.getGender()).append('\n');
    sb.append("Allergies: ").append(joinOrNone(p.getAllergies())).append('\n');
    sb.append("Conditions: ").append(joinOrNone(p.getChronicConditions())).append('\n');
    sb.append("Primary doctor: ").append(nullToDash(p.getPrimaryDoctor())).append('\n');

    List<Medication> meds =
        p.getMedications().stream()
            .filter(m -> m.getStatus() == null || "active".equalsIgnoreCase(m.getStatus()))
            .limit(12)
            .toList();
    sb.append("Active meds:\n");
    if (meds.isEmpty()) sb.append("- none listed\n");
    else {
      for (Medication m : meds) {
        sb.append("- ")
            .append(m.getName())
            .append(' ')
            .append(nullToDash(m.getDosage()))
            .append(' ')
            .append(nullToDash(m.getFrequency()))
            .append('\n');
      }
    }

    List<VitalReading> vitals =
        p.getVitals().stream()
            .sorted(Comparator.comparing(VitalReading::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .limit(5)
            .toList();
    sb.append("Recent vitals (newest first):\n");
    if (vitals.isEmpty()) sb.append("- none\n");
    else {
      for (VitalReading v : vitals) {
        sb.append("- ")
            .append(v.getTimestamp())
            .append(": BP ")
            .append(v.getBloodPressureSys())
            .append('/')
            .append(v.getBloodPressureDia())
            .append(", HR ")
            .append(v.getHeartRate())
            .append(", SpO2 ")
            .append(v.getSpO2())
            .append("%, Temp ")
            .append(v.getTemperature())
            .append("°F, RR ")
            .append(v.getRespRate())
            .append(v.isAbnormal() ? " [FLAGGED]" : "")
            .append('\n');
      }
    }

    if (!patientCaller) {
      List<LabResult> labs =
          p.getLabResults().stream().limit(6).toList();
      sb.append("Recent labs:\n");
      if (labs.isEmpty()) sb.append("- none\n");
      else {
        for (LabResult lab : labs) {
          sb.append("- ")
              .append(lab.getDate())
              .append(' ')
              .append(lab.getTestName())
              .append(": ")
              .append(lab.getValue())
              .append(" (ref ")
              .append(nullToDash(lab.getReferenceRange()))
              .append(", ")
              .append(nullToDash(lab.getStatus()))
              .append(")\n");
        }
      }
      List<ClinicalNote> notes = p.getClinicalNotes().stream().limit(3).toList();
      sb.append("Recent notes:\n");
      if (notes.isEmpty()) sb.append("- none\n");
      else {
        for (ClinicalNote n : notes) {
          String soap =
              "S: "
                  + nullToDash(n.getSoapSubjective())
                  + " O: "
                  + nullToDash(n.getSoapObjective())
                  + " A: "
                  + nullToDash(n.getSoapAssessment())
                  + " P: "
                  + nullToDash(n.getSoapPlan());
          sb.append("- ")
              .append(n.getDate())
              .append(' ')
              .append(nullToDash(n.getAuthor()))
              .append(" — ")
              .append(nullToDash(n.getTitle()))
              .append(": ")
              .append(trim(soap, 400))
              .append('\n');
        }
      }
    }

    List<Invoice> bills = invoices.findByPatientIdOrderByDateDesc(patientId);
    sb.append("Billing (recent):\n");
    if (bills.isEmpty()) sb.append("- no invoices\n");
    else {
      for (Invoice inv : bills.stream().limit(5).toList()) {
        sb.append("- ")
            .append(inv.getDate())
            .append(' ')
            .append(nullToDash(inv.getInvoiceNumber()))
            .append(": subtotal=")
            .append(inv.getSubtotal())
            .append(" status=")
            .append(inv.getStatus())
            .append(" patientShare=")
            .append(inv.getPatientResponsibility())
            .append(" paid=")
            .append(inv.getAmountPaid())
            .append('\n');
      }
    }

    return sb.toString();
  }

  private String callGemini(String system, String userPrompt) throws Exception {
    String model = ai.getModel() == null || ai.getModel().isBlank() ? "gemini-2.0-flash" : ai.getModel();
    String url =
        "https://generativelanguage.googleapis.com/v1beta/models/"
            + model
            + ":generateContent";

    ObjectNode root = mapper.createObjectNode();
    ObjectNode systemInstruction = root.putObject("systemInstruction");
    ArrayNode sysParts = systemInstruction.putArray("parts");
    sysParts.addObject().put("text", system);

    ArrayNode contents = root.putArray("contents");
    ObjectNode userTurn = contents.addObject();
    userTurn.put("role", "user");
    ArrayNode parts = userTurn.putArray("parts");
    parts.addObject().put("text", userPrompt);

    ObjectNode generationConfig = root.putObject("generationConfig");
    generationConfig.put("temperature", 0.4);
    generationConfig.put("maxOutputTokens", 1024);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", ai.getGeminiApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
            .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new IllegalStateException("Gemini HTTP " + response.statusCode() + ": " + trim(response.body(), 200));
    }

    JsonNode tree = mapper.readTree(response.body());
    JsonNode textNode =
        tree.path("candidates").path(0).path("content").path("parts").path(0).path("text");
    if (textNode.isMissingNode() || textNode.asText().isBlank()) {
      throw new IllegalStateException("Empty Gemini response");
    }
    return textNode.asText().trim();
  }

  private String fallbackReply(String intent, String message, String chart, AuthUser user) {
    boolean patient = user.getRole() == UserRole.patient;
    String lower = message.toLowerCase(Locale.ROOT);

    if ("explain_vitals".equals(intent) || lower.contains("vital") || lower.contains("blood pressure") || lower.contains("heart rate")) {
      String flagged = chart.contains("[FLAGGED]") ? " At least one recent reading was flagged as abnormal — share that with your care team." : "";
      if (chart.contains("Recent vitals")) {
        return "Here is a plain-language reading of your recent vitals from the chart context."
            + flagged
            + "\n\nTypical adult resting ranges (guidance only):\n"
            + "- Blood pressure: roughly under 120/80 is ideal; 130–139 / 80–89 is elevated stage; ≥180/120 needs urgent care.\n"
            + "- Heart rate: often ~60–100 bpm at rest.\n"
            + "- SpO₂: usually ≥95% on room air for many adults.\n"
            + "- Temperature: ~97.8–99.1°F (about 36.6–37.3°C).\n\n"
            + "This is not a diagnosis. If you have chest pain, severe shortness of breath, fainting, or stroke symptoms, seek emergency care now.\n\n"
            + "(AI provider key not configured — using Medicore rule-based assistant.)";
      }
      return "I do not see recent vitals in context yet. Log a reading in Monitor (or ask staff to record one), then ask again.\n\n(AI provider key not configured — using Medicore rule-based assistant.)";
    }

    if ("draft_summary".equals(intent)) {
      if (patient) {
        return "Visit summaries are drafted by your care team. You can ask them for your after-visit summary in Messages.";
      }
      String course =
          chart.isBlank()
              ? "Summarize the encounter, key findings, and response to treatment from the chart. Review and edit before finalizing."
              : "Patient course reviewed from chart context. Condition monitored with serial vitals and labs; prepare clinician-edited hospital course before finalizing.";
      return "COURSE:\n"
          + course
          + "\n\nINSTRUCTIONS:\n"
          + "- Continue active medications as listed unless changed by the care team.\n"
          + "- Follow up with primary doctor within 7–14 days.\n"
          + "- Rest, hydrate, and track symptoms at home.\n\n"
          + "WARNINGS:\n"
          + "- Chest pain, severe shortness of breath, or stroke signs\n"
          + "- Fever >100.4°F lasting more than 24 hours\n"
          + "- Uncontrolled bleeding, severe headache, or confusion\n";
    }

    if ("draft_message".equals(intent)) {
      return patient
          ? "Hi care team — I have a question about my recent care and would appreciate guidance on next steps. Thank you."
          : "Thanks for reaching out. I reviewed your note and will follow up with guidance shortly. Please seek emergency care for severe or worsening symptoms.";
    }

    if ("billing_help".equals(intent) || lower.contains("bill") || lower.contains("invoice") || lower.contains("copay")) {
      if (chart.contains("Billing (recent)")) {
        return "I can help explain statements using your recent invoices in context: amounts, status, and patient share. "
            + "Open Billing in the app to pay an open balance. For insurance disputes, contact the billing desk with your MRN and invoice date.\n\n"
            + "(AI provider key not configured — using Medicore rule-based assistant.)";
      }
      return "No invoice context loaded yet. Open Billing, then ask again about a specific statement.";
    }

    return "I am Medicore Assist"
        + (patient ? " for patients" : " for care teams")
        + ". I can help explain vitals, draft care-team messages, clarify billing language"
        + (patient ? "" : ", and draft after-visit summaries")
        + ". Ask a specific question, or tap a quick action.\n\n"
        + "Reminder: advisory only — not emergency care or a medical diagnosis.\n\n"
        + "(Set GEMINI_API_KEY on the API server for full generative replies.)";
  }

  private static String joinOrNone(List<String> items) {
    if (items == null || items.isEmpty()) return "none listed";
    return items.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
  }

  private static String nullToDash(String s) {
    return s == null || s.isBlank() ? "—" : s;
  }

  private static String trim(String s, int max) {
    if (s == null) return "";
    String t = s.trim();
    return t.length() <= max ? t : t.substring(0, max) + "…";
  }
}
