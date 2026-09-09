package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/med-doses")
public class MedDosesController {
  /** Hospital timezone for “dose already logged today” checks. */
  private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Yangon");

  private final MedicationDoseLogRepository doses;
  private final PatientRepository patients;
  private final AuditEventRepository audits;

  public MedDosesController(
      MedicationDoseLogRepository doses,
      PatientRepository patients,
      AuditEventRepository audits) {
    this.doses = doses;
    this.patients = patients;
    this.audits = audits;
  }

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) String patientId) {
    AuthUser user = AuthSupport.requireUser();
    String scoped = AuthSupport.patientScopeOrNull();
    List<MedicationDoseLog> rows;
    if (scoped != null) {
      rows = doses.findByPatientIdOrderByLoggedAtDesc(scoped);
    } else if (patientId != null && !patientId.isBlank()) {
      rows = doses.findByPatientIdOrderByLoggedAtDesc(patientId);
    } else if (user.getRole() == UserRole.patient) {
      throw new ApiException(403, "Forbidden");
    } else {
      rows = doses.findAllByOrderByLoggedAtDesc();
    }
    return rows.stream().map(MedDosesController::map).toList();
  }

  @PostMapping
  @Transactional
  public Map<String, Object> log(@RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    String patientId = AuthSupport.patientScopeOrNull();
    if (patientId == null) {
      if (auth.getRole() == UserRole.patient) {
        throw new ApiException(403, "Forbidden");
      }
      patientId = String.valueOf(body.getOrDefault("patientId", ""));
    }
    if (patientId == null || patientId.isBlank()) throw new ApiException(400, "patientId required");

    String medicationId = String.valueOf(body.getOrDefault("medicationId", ""));
    if (medicationId.isBlank()) throw new ApiException(400, "medicationId required");

    Patient patient =
        patients.findById(patientId).orElseThrow(() -> new ApiException(404, "Patient not found"));
    patient.getMedications().size();
    Medication med =
        patient.getMedications().stream()
            .filter(m -> medicationId.equals(m.getId()))
            .findFirst()
            .orElseThrow(() -> new ApiException(404, "Medication not found"));

    String status = String.valueOf(body.getOrDefault("status", "taken")).toLowerCase(Locale.ROOT);
    if (!status.equals("taken") && !status.equals("skipped")) {
      throw new ApiException(400, "status must be taken or skipped");
    }
    String notes = body.get("notes") == null ? null : String.valueOf(body.get("notes"));

    Instant startOfDay = LocalDate.now(HOSPITAL_ZONE).atStartOfDay(HOSPITAL_ZONE).toInstant();
    if (!doses
        .findByPatientIdAndMedicationIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(
            patientId, med.getId(), startOfDay)
        .isEmpty()) {
      throw new ApiException(409, "Dose already logged today for this medication");
    }

    MedicationDoseLog row = new MedicationDoseLog();
    row.setId("dose-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    row.setPatientId(patientId);
    row.setMedicationId(med.getId());
    row.setMedicationName(med.getName());
    row.setStatus(status);
    row.setNotes(notes);
    row.setLoggedAt(Instant.now());
    doses.save(row);

    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("LOG_MED_DOSE");
    e.setResource("med-dose/" + row.getId());
    e.setPatientId(patientId);
    e.setDetails(med.getName() + " → " + status);
    audits.save(e);

    return map(row);
  }

  private static Map<String, Object> map(MedicationDoseLog row) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", row.getId());
    m.put("patientId", row.getPatientId());
    m.put("medicationId", row.getMedicationId());
    m.put("medicationName", row.getMedicationName());
    m.put("status", row.getStatus());
    m.put("notes", row.getNotes());
    m.put("loggedAt", row.getLoggedAt().toString());
    return m;
  }
}
