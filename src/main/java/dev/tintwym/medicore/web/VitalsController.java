package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.service.PushNotificationService;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vitals")
public class VitalsController {
  private final VitalReadingRepository vitals;
  private final PatientRepository patients;
  private final UserAccountRepository users;
  private final MessageRepository messages;
  private final AuditEventRepository audits;
  private final PushNotificationService push;

  public VitalsController(
      VitalReadingRepository vitals,
      PatientRepository patients,
      UserAccountRepository users,
      MessageRepository messages,
      AuditEventRepository audits,
      PushNotificationService push) {
    this.vitals = vitals;
    this.patients = patients;
    this.users = users;
    this.messages = messages;
    this.audits = audits;
    this.push = push;
  }

  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) String patientId) {
    String id = AuthSupport.patientScopeOr(patientId);
    return vitals.findByPatient_IdOrderByTimestampAsc(id).stream().map(this::map).toList();
  }

  @PostMapping
  public Map<String, Object> ingest(@RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    String scopedPatientId = AuthSupport.patientScopeOrNull();
    if (scopedPatientId == null) scopedPatientId = String.valueOf(body.getOrDefault("patientId", ""));
    if (scopedPatientId.isBlank()) throw new ApiException(400, "patientId required");
    final String patientId = scopedPatientId;
    Patient patient = patients.findById(patientId).orElseThrow(() -> new ApiException(404, "Patient not found"));

    int hr = toInt(body.get("heartRate"));
    int sys = toInt(body.get("bloodPressureSys"));
    int dia = toInt(body.get("bloodPressureDia"));
    int spo2 = toInt(body.get("spO2"));
    double temp = toDouble(body.get("temperature"));
    int rr = body.get("respRate") == null ? 16 : toInt(body.get("respRate"));
    boolean abnormal = hr > 100 || hr < 55 || sys > 140 || dia > 90 || spo2 < 94 || temp > 100.4;

    VitalReading row = new VitalReading();
    row.setId("v-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    row.setPatient(patient);
    row.setHeartRate(hr);
    row.setBloodPressureSys(sys);
    row.setBloodPressureDia(dia);
    row.setSpO2(spo2);
    row.setTemperature(temp);
    row.setRespRate(rr);
    row.setNotes(body.get("notes") == null ? null : String.valueOf(body.get("notes")));
    row.setRecordedBy(body.get("recordedBy") == null ? auth.getName() : String.valueOf(body.get("recordedBy")));
    row.setAbnormal(abnormal);
    row.setTimestamp(body.get("timestamp") == null ? Instant.now() : Instant.parse(String.valueOf(body.get("timestamp"))));
    vitals.save(row);

    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("UPDATE_VITALS");
    e.setResource("vitals/" + row.getId());
    e.setPatientId(patientId);
    audits.save(e);

    if (abnormal) {
      final int fHr = hr;
      final int fSys = sys;
      final int fDia = dia;
      final int fSpo2 = spo2;
      users.findFirstByRole(UserRole.doctor).ifPresent(doctor -> {
        Message msg = new Message();
        msg.setId("msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        msg.setSenderId(auth.getId());
        msg.setSenderName("Medicore Monitor");
        msg.setSenderRole("system");
        msg.setRecipientId(doctor.getId());
        msg.setRecipientName(doctor.getName());
        msg.setSubject("Abnormal home vitals alert");
        msg.setBody("Patient " + patientId + " logged abnormal vitals (HR " + fHr + ", BP " + fSys + "/" + fDia + ", SpO2 " + fSpo2 + ").");
        msg.setUrgent(true);
        messages.save(msg);
      });
      users.findFirstByPatientId(patientId).ifPresent(patientUser ->
          push.notifyUser(
              patientUser.getId(),
              "Abnormal vitals recorded",
              "HR " + fHr + " · BP " + fSys + "/" + fDia + " · SpO₂ " + fSpo2 + "%. Open Monitor to review.",
              Map.of(
                  "type", "vitals",
                  "screen", "Monitor",
                  "vitalId", row.getId())));
    }
    return map(row);
  }

  private Map<String, Object> map(VitalReading v) {
    Map<String, Object> m = new LinkedHashMap<>();
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

  private static int toInt(Object o) {
    if (o instanceof Number n) return n.intValue();
    return Integer.parseInt(String.valueOf(o));
  }

  private static double toDouble(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    return Double.parseDouble(String.valueOf(o));
  }
}
