package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.service.PushNotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/visit-summaries")
public class VisitSummariesController {
  private final VisitSummaryRepository summaries;
  private final PatientRepository patients;
  private final UserAccountRepository users;
  private final AuditEventRepository audits;
  private final PushNotificationService push;

  public VisitSummariesController(
      VisitSummaryRepository summaries,
      PatientRepository patients,
      UserAccountRepository users,
      AuditEventRepository audits,
      PushNotificationService push) {
    this.summaries = summaries;
    this.patients = patients;
    this.users = users;
    this.audits = audits;
    this.push = push;
  }

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) String patientId) {
    String scoped = AuthSupport.patientScopeOrNull();
    List<VisitSummary> rows;
    boolean patientCaller = scoped != null;
    if (scoped != null) {
      rows = summaries.findByPatientIdOrderByDateDesc(scoped);
    } else if (patientId != null && !patientId.isBlank()) {
      rows = summaries.findByPatientIdOrderByDateDesc(patientId);
    } else {
      AuthUser user = AuthSupport.requireUser();
      if (user.getRole() == UserRole.patient) throw new ApiException(403, "Forbidden");
      rows = summaries.findAllByOrderByDateDesc();
    }
    return rows.stream()
        .filter(row -> !patientCaller || "finalized".equalsIgnoreCase(row.getStatus()))
        .map(VisitSummariesController::map)
        .toList();
  }

  @PostMapping
  public Map<String, Object> create(@RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    if (auth.getRole() == UserRole.patient) throw new ApiException(403, "Staff only");

    String patientId = String.valueOf(body.getOrDefault("patientId", ""));
    if (patientId.isBlank()) throw new ApiException(400, "patientId required");
    if (!patients.existsById(patientId)) throw new ApiException(404, "Patient not found");

    String title = String.valueOf(body.getOrDefault("title", "After-visit summary"));
    String summaryBody = body.get("summaryBody") == null ? "" : String.valueOf(body.get("summaryBody"));
    String instructions = body.get("instructions") == null ? "" : String.valueOf(body.get("instructions"));
    String warningSigns = body.get("warningSigns") == null ? "" : String.valueOf(body.get("warningSigns"));
    String appointmentId =
        body.get("appointmentId") == null ? null : String.valueOf(body.get("appointmentId"));
    String date =
        body.get("date") == null
            ? LocalDate.now(java.time.ZoneId.of("Asia/Yangon")).toString()
            : String.valueOf(body.get("date"));
    String status = String.valueOf(body.getOrDefault("status", "finalized"));

    VisitSummary row = new VisitSummary();
    row.setId("avs-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    row.setPatientId(patientId);
    row.setAppointmentId(appointmentId);
    row.setDate(date);
    row.setAuthor(auth.getName());
    row.setAuthorRole(auth.getRole().name());
    row.setTitle(title);
    row.setSummaryBody(summaryBody);
    row.setInstructions(instructions);
    row.setWarningSigns(warningSigns);
    row.setStatus(status);
    row.setCreatedAt(Instant.now());
    summaries.save(row);

    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("CREATE_VISIT_SUMMARY");
    e.setResource("visit-summary/" + row.getId());
    e.setPatientId(patientId);
    e.setDetails(title);
    audits.save(e);

    if ("finalized".equalsIgnoreCase(status)) {
      users.findFirstByPatientId(patientId).ifPresent(u -> {
        try {
          push.notifyUser(
              u.getId(),
              "After-visit summary ready",
              title,
              Map.of("type", "visit_summary", "screen", "Home", "summaryId", row.getId()));
        } catch (Exception ignored) {
          /* push optional */
        }
      });
    }

    return map(row);
  }

  private static Map<String, Object> map(VisitSummary row) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", row.getId());
    m.put("patientId", row.getPatientId());
    m.put("appointmentId", row.getAppointmentId());
    m.put("date", row.getDate());
    m.put("author", row.getAuthor());
    m.put("authorRole", row.getAuthorRole());
    m.put("title", row.getTitle());
    m.put("summaryBody", row.getSummaryBody());
    m.put("instructions", row.getInstructions());
    m.put("warningSigns", row.getWarningSigns());
    m.put("status", row.getStatus());
    m.put("createdAt", row.getCreatedAt() == null ? null : row.getCreatedAt().toString());
    return m;
  }
}
