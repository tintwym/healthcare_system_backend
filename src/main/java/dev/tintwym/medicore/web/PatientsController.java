package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.Medication;
import dev.tintwym.medicore.domain.Patient;
import dev.tintwym.medicore.domain.VitalReading;
import dev.tintwym.medicore.domain.LabResult;
import dev.tintwym.medicore.domain.ClinicalNote;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.PatientRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.domain.UserRole;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/patients")
public class PatientsController {
  private final PatientRepository patients;
  private final AuditEventRepository audits;

  public PatientsController(PatientRepository patients, AuditEventRepository audits) {
    this.patients = patients;
    this.audits = audits;
  }

  @GetMapping("/me")
  @Transactional(readOnly = true)
  public Map<String, Object> me(@RequestParam(required = false) String patientId) {
    String id = AuthSupport.patientScopeOr(patientId);
    Patient p = patients.findById(id).orElseThrow(() -> new ApiException(404, "Patient not found"));
    hydrate(p);
    return mapPatient(p);
  }

  @PatchMapping("/me")
  @Transactional
  public Map<String, Object> update(@RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() != UserRole.patient || user.getPatientId() == null) {
      throw new ApiException(403, "Patients only");
    }
    Patient p = patients.findById(user.getPatientId()).orElseThrow(() -> new ApiException(404, "Patient not found"));
    hydrate(p);
    if (body.get("phone") instanceof String phone) p.setPhone(phone);
    if (body.get("paymentMethodPref") instanceof String pref) p.setPaymentMethodPref(pref);
    Object ec = body.get("emergencyContact");
    if (ec instanceof Map<?, ?> map) {
      if (map.get("name") instanceof String n) p.setEmergencyName(n);
      if (map.get("relationship") instanceof String r) p.setEmergencyRelation(r);
      if (map.get("phone") instanceof String ph) p.setEmergencyPhone(ph);
    }
    patients.save(p);
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction("UPDATE_PROFILE");
    e.setResource("patient/" + p.getId());
    e.setPatientId(p.getId());
    audits.save(e);
    return mapPatient(p);
  }

  private static void hydrate(Patient p) {
    p.getAllergies().size();
    p.getChronicConditions().size();
    p.getMedications().size();
    p.getVitals().size();
    p.getLabResults().size();
    p.getClinicalNotes().size();
  }

  static Map<String, Object> mapPatient(Patient p) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", p.getId());
    m.put("mrn", p.getMrn());
    m.put("firstName", p.getFirstName());
    m.put("lastName", p.getLastName());
    m.put("dob", p.getDob());
    m.put("age", p.getAge());
    m.put("gender", p.getGender());
    m.put("bloodType", p.getBloodType());
    m.put("phone", p.getPhone());
    m.put("email", p.getEmail());
    m.put("address", p.getAddress());
    m.put("emergencyContact", Map.of(
        "name", nullToEmpty(p.getEmergencyName()),
        "relationship", nullToEmpty(p.getEmergencyRelation()),
        "phone", nullToEmpty(p.getEmergencyPhone())));
    m.put("allergies", p.getAllergies());
    m.put("chronicConditions", p.getChronicConditions());
    m.put("primaryDoctor", p.getPrimaryDoctor());
    m.put("department", p.getDepartment());
    m.put("room", p.getRoom());
    m.put("bed", p.getBed());
    m.put("admissionStatus", p.getAdmissionStatus());
    m.put("admissionDate", p.getAdmissionDate());
    m.put("insurance", Map.of(
        "provider", nullToEmpty(p.getInsuranceProvider()),
        "policyNumber", nullToEmpty(p.getInsurancePolicy()),
        "groupNumber", nullToEmpty(p.getInsuranceGroup()),
        "verified", p.isInsuranceVerified(),
        "copay", p.getInsuranceCopay()));
    m.put("fhirId", p.getFhirId());
    m.put("paymentMethodPref", p.getPaymentMethodPref());
    m.put("medications", p.getMedications().stream().map(PatientsController::mapMed).toList());
    m.put("vitals", p.getVitals().stream().map(PatientsController::mapVital).toList());
    m.put("labResults", p.getLabResults().stream().map(PatientsController::mapLab).toList());
    m.put("clinicalNotes", p.getClinicalNotes().stream().map(PatientsController::mapNote).toList());
    return m;
  }

  private static Map<String, Object> mapMed(Medication med) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", med.getId());
    m.put("name", med.getName());
    m.put("dosage", med.getDosage());
    m.put("frequency", med.getFrequency());
    m.put("route", med.getRoute());
    m.put("startDate", med.getStartDate());
    m.put("endDate", med.getEndDate());
    m.put("prescribedBy", med.getPrescribedBy());
    m.put("status", med.getStatus());
    return m;
  }

  private static Map<String, Object> mapVital(VitalReading v) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", v.getId());
    m.put("timestamp", v.getTimestamp().toString());
    m.put("heartRate", v.getHeartRate());
    m.put("bloodPressureSys", v.getBloodPressureSys());
    m.put("bloodPressureDia", v.getBloodPressureDia());
    m.put("spO2", v.getSpO2());
    m.put("temperature", v.getTemperature());
    m.put("respRate", v.getRespRate());
    m.put("notes", v.getNotes());
    m.put("recordedBy", v.getRecordedBy());
    m.put("isAbnormal", v.isAbnormal());
    return m;
  }

  private static Map<String, Object> mapLab(LabResult lab) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", lab.getId());
    m.put("testName", lab.getTestName());
    m.put("category", lab.getCategory());
    m.put("date", lab.getDate());
    m.put("value", lab.getValue());
    m.put("referenceRange", lab.getReferenceRange());
    m.put("status", lab.getStatus());
    m.put("notes", lab.getNotes());
    m.put("orderedBy", lab.getOrderedBy());
    return m;
  }

  private static Map<String, Object> mapNote(ClinicalNote note) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", note.getId());
    m.put("date", note.getDate());
    m.put("author", note.getAuthor());
    m.put("authorRole", note.getAuthorRole());
    m.put("title", note.getTitle());
    m.put("soapSubjective", note.getSoapSubjective());
    m.put("soapObjective", note.getSoapObjective());
    m.put("soapAssessment", note.getSoapAssessment());
    m.put("soapPlan", note.getSoapPlan());
    return m;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
