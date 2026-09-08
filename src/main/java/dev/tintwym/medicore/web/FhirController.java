package dev.tintwym.medicore.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.domain.FhirBundleLog;
import dev.tintwym.medicore.domain.Patient;
import dev.tintwym.medicore.domain.UserRole;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.FhirBundleLogRepository;
import dev.tintwym.medicore.repo.PatientRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir")
public class FhirController {
  private final PatientRepository patients;
  private final FhirBundleLogRepository logs;
  private final AuditEventRepository audits;
  private final ObjectMapper mapper;

  public FhirController(
      PatientRepository patients,
      FhirBundleLogRepository logs,
      AuditEventRepository audits,
      ObjectMapper mapper) {
    this.patients = patients;
    this.logs = logs;
    this.audits = audits;
    this.mapper = mapper;
  }

  @GetMapping("/Patient/{id}")
  public Map<String, Object> patient(@PathVariable String id) throws Exception {
    Patient patient = patients.findById(id).orElseThrow(() -> new ApiException(404, "not-found"));
    Map<String, Object> bundle = new LinkedHashMap<>();
    bundle.put("resourceType", "Patient");
    bundle.put("id", patient.getFhirId() != null ? patient.getFhirId() : patient.getId());
    bundle.put("identifier", List.of(entry("system", "urn:medicore:mrn", "value", nz(patient.getMrn()))));
    bundle.put("name", List.of(Map.of(
        "family", nz(patient.getLastName()),
        "given", List.of(nz(patient.getFirstName())))));
    bundle.put("gender", patient.getGender() == null ? null : patient.getGender().toLowerCase());
    bundle.put("birthDate", patient.getDob());
    List<Map<String, String>> telecom = new ArrayList<>();
    if (patient.getPhone() != null) telecom.add(entry("system", "phone", "value", patient.getPhone()));
    if (patient.getEmail() != null) telecom.add(entry("system", "email", "value", patient.getEmail()));
    bundle.put("telecom", telecom);
    if (patient.getAddress() != null) {
      bundle.put("address", List.of(Map.of("text", patient.getAddress())));
    } else {
      bundle.put("address", List.of());
    }

    FhirBundleLog log = new FhirBundleLog();
    log.setDirection("export");
    log.setResourceType("Patient");
    log.setPayload(mapper.writeValueAsString(bundle));
    logs.save(log);
    return bundle;
  }

  @PostMapping("/Bundle")
  public Map<String, Object> importBundle(@RequestBody Map<String, Object> payload) throws Exception {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() != UserRole.admin && user.getRole() != UserRole.doctor) {
      throw new ApiException(403, "Forbidden");
    }
    FhirBundleLog log = new FhirBundleLog();
    log.setDirection("import");
    log.setResourceType(String.valueOf(payload.getOrDefault("resourceType", "Bundle")));
    log.setPayload(mapper.writeValueAsString(payload));
    logs.save(log);
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction("EXPORT_FHIR");
    e.setResource("fhir/" + log.getId());
    audits.save(e);
    return Map.of(
        "resourceType", "OperationOutcome",
        "id", log.getId(),
        "issue", List.of(Map.of("severity", "information", "code", "informational", "diagnostics", "Bundle accepted")));
  }

  @GetMapping("/logs")
  public List<FhirBundleLog> listLogs() {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() != UserRole.admin && user.getRole() != UserRole.doctor) {
      throw new ApiException(403, "Forbidden");
    }
    return logs.findTop50ByOrderByCreatedAtDesc();
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  private static Map<String, String> entry(String k1, String v1, String k2, String v2) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put(k1, v1);
    m.put(k2, v2);
    return m;
  }
}
