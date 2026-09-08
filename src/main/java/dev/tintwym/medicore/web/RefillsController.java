package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refills")
public class RefillsController {
  private final RefillRequestRepository refills;
  private final PatientRepository patients;
  private final UserAccountRepository users;
  private final MessageRepository messages;
  private final AuditEventRepository audits;

  public RefillsController(
      RefillRequestRepository refills,
      PatientRepository patients,
      UserAccountRepository users,
      MessageRepository messages,
      AuditEventRepository audits) {
    this.refills = refills;
    this.patients = patients;
    this.users = users;
    this.messages = messages;
    this.audits = audits;
  }

  @GetMapping
  public List<RefillRequest> list() {
    String scoped = AuthSupport.patientScopeOrNull();
    if (scoped != null) return refills.findByPatientIdOrderByCreatedAtDesc(scoped);
    return refills.findAllByOrderByCreatedAtDesc();
  }

  @GetMapping("/pharmacy/queue")
  @Transactional(readOnly = true)
  public List<Map<String, Object>> pharmacyQueue() {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() == UserRole.patient) throw new ApiException(403, "Forbidden");
    return refills.findByStatusInOrderByCreatedAtAsc(List.of(RefillStatus.pending))
        .stream()
        .map(r -> {
          Patient p = patients.findById(r.getPatientId()).orElse(null);
          if (p != null) p.getAllergies().size();
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", r.getId());
          m.put("medicationName", r.getMedicationName());
          m.put("notes", r.getNotes());
          m.put("status", r.getStatus().name());
          m.put("prescriptionId", r.getPrescriptionId());
          m.put("createdAt", r.getCreatedAt().toString());
          m.put("patientId", r.getPatientId());
          m.put("patientName", p == null ? "" : p.getFirstName() + " " + p.getLastName());
          m.put("patientMrn", p == null ? "" : p.getMrn());
          m.put("allergies", p == null ? List.of() : p.getAllergies());
          return m;
        })
        .toList();
  }

  @PostMapping
  public RefillRequest create(@RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    String patientId = AuthSupport.patientScopeOrNull();
    if (patientId == null) patientId = String.valueOf(body.getOrDefault("patientId", ""));
    if (patientId.isBlank()) throw new ApiException(400, "patientId required");
    String medicationName = String.valueOf(body.getOrDefault("medicationName", ""));
    if (medicationName.isBlank()) throw new ApiException(400, "medicationName required");
    String notes = body.get("notes") == null ? null : String.valueOf(body.get("notes"));
    String prescriptionId = "RX-REFILL-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();

    RefillRequest row = new RefillRequest();
    row.setId("ref-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    row.setPatientId(patientId);
    row.setMedicationName(medicationName);
    row.setNotes(notes);
    row.setStatus(RefillStatus.pending);
    row.setPrescriptionId(prescriptionId);
    refills.save(row);

    users.findFirstByRole(UserRole.pharmacist).ifPresent(pharmacist -> {
      Message msg = new Message();
      msg.setId("msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
      msg.setSenderId(auth.getId());
      msg.setSenderName(auth.getName());
      msg.setSenderRole(auth.getRole().name());
      msg.setRecipientId(pharmacist.getId());
      msg.setRecipientName(pharmacist.getName());
      msg.setSubject("Refill request: " + medicationName);
      msg.setBody(notes != null ? notes : "Please refill " + medicationName + " for a 30-day supply. Queue id " + row.getId());
      messages.save(msg);
    });

    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("REQUEST_REFILL");
    e.setResource("refill/" + row.getId());
    e.setPatientId(patientId);
    e.setDetails(medicationName);
    audits.save(e);
    return row;
  }

  @PatchMapping("/{id}")
  public RefillRequest update(@PathVariable String id, @RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() == UserRole.patient) throw new ApiException(403, "Forbidden");
    RefillRequest row = refills.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    if (body.get("status") != null) {
      row.setStatus(RefillStatus.valueOf(String.valueOf(body.get("status"))));
    }
    return refills.save(row);
  }
}
