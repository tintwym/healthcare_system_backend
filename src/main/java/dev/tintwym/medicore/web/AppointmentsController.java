package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments")
public class AppointmentsController {
  private static final List<String> SLOT_TIMES = List.of(
      "9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM",
      "1:00 PM", "1:30 PM", "2:00 PM", "2:30 PM", "3:00 PM", "3:30 PM", "4:00 PM");

  /** Normalize "09:00 AM" / "9:00 am" → canonical "9:00 AM" for conflict checks. */
  static String normalizeTime(String raw) {
    if (raw == null) return "";
    String t = raw.trim().replaceAll("\\s+", " ");
    String upper = t.toUpperCase(Locale.ROOT);
    for (String slot : SLOT_TIMES) {
      if (slot.equalsIgnoreCase(upper)) return slot;
      // strip leading zero on hour: 09:00 AM → 9:00 AM
      String noLead = upper.replaceFirst("^0(\\d:)", "$1");
      if (slot.equalsIgnoreCase(noLead)) return slot;
    }
    return upper.replaceFirst("^0(\\d:)", "$1");
  }

  private final AppointmentRepository appointments;
  private final PatientRepository patients;
  private final UserAccountRepository users;
  private final AuditEventRepository audits;

  public AppointmentsController(
      AppointmentRepository appointments,
      PatientRepository patients,
      UserAccountRepository users,
      AuditEventRepository audits) {
    this.appointments = appointments;
    this.patients = patients;
    this.users = users;
    this.audits = audits;
  }

  @GetMapping("/slots")
  public Map<String, Object> slots(@RequestParam String date, @RequestParam(defaultValue = "u-1") String doctorId) {
    var taken = appointments.findByDoctorIdAndDateAndStatusNot(doctorId, date, AppointmentStatus.cancelled);
    Set<String> takenTimes = new HashSet<>();
    taken.forEach(a -> takenTimes.add(normalizeTime(a.getTime())));
    List<Map<String, Object>> slots = SLOT_TIMES.stream()
        .map(t -> Map.<String, Object>of("time", t, "available", !takenTimes.contains(t)))
        .toList();
    return Map.of("doctorId", doctorId, "date", date, "slots", slots);
  }

  @GetMapping("/staff/all")
  public List<Appointment> staffAll() {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() == UserRole.patient) throw new ApiException(403, "Forbidden");
    return appointments.findAllByOrderByDateAscTimeAsc();
  }

  @PostMapping("/reminders/run")
  public Map<String, Object> reminders() {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() != UserRole.admin) throw new ApiException(403, "Forbidden");
    String date = LocalDate.now().plusDays(1).toString();
    var due = appointments.findByDateAndStatusAndReminderSentAtIsNull(date, AppointmentStatus.scheduled);
    due.forEach(a -> {
      System.out.println("[appointments] 24h reminder → " + a.getPatientName() + " on " + a.getDate() + " " + a.getTime());
      a.setReminderSentAt(Instant.now());
      appointments.save(a);
    });
    return Map.of("sent", due.size());
  }

  @GetMapping
  public List<Appointment> list(@RequestParam(required = false) String patientId) {
    String scoped = AuthSupport.patientScopeOrNull();
    if (scoped != null) return appointments.findByPatientIdOrderByDateAscTimeAsc(scoped);
    if (patientId != null) return appointments.findByPatientIdOrderByDateAscTimeAsc(patientId);
    return appointments.findAllByOrderByDateAscTimeAsc();
  }

  @PostMapping
  public Appointment create(@RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    String patientId = AuthSupport.patientScopeOrNull();
    if (patientId == null) patientId = String.valueOf(body.getOrDefault("patientId", ""));
    if (patientId.isBlank()) throw new ApiException(400, "patientId required");
    Patient patient = patients.findById(patientId).orElseThrow(() -> new ApiException(404, "Patient not found"));
    String doctorId = String.valueOf(body.getOrDefault("doctorId", "u-1"));
    UserAccount doctor = users.findById(doctorId).orElseThrow(() -> new ApiException(400, "Invalid doctor"));
    if (doctor.getRole() != UserRole.doctor) throw new ApiException(400, "Invalid doctor");
    String date = String.valueOf(body.getOrDefault("date", ""));
    String time = normalizeTime(String.valueOf(body.getOrDefault("time", "")));
    if (date.isBlank() || time.isBlank()) throw new ApiException(400, "date and time required");
    var conflict = appointments.findByDoctorIdAndDateAndStatusNot(doctorId, date, AppointmentStatus.cancelled)
        .stream().filter(a -> normalizeTime(a.getTime()).equals(time)).findFirst();
    if (conflict.isPresent()) throw new ApiException(409, "Slot unavailable");

    Appointment apt = new Appointment();
    apt.setId("apt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    apt.setPatientId(patientId);
    apt.setPatientName(patient.getFirstName() + " " + patient.getLastName());
    apt.setPatientMrn(patient.getMrn());
    apt.setDoctorId(doctorId);
    apt.setDoctorName(doctor.getName());
    apt.setDepartment(String.valueOf(body.getOrDefault("department", doctor.getDepartment())));
    apt.setDate(date);
    apt.setTime(time);
    apt.setReason(String.valueOf(body.getOrDefault("reason", "Follow-up visit")));
    apt.setType(String.valueOf(body.getOrDefault("type", "Follow-up")));
    appointments.save(apt);

    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("CREATE_APPOINTMENT");
    e.setResource("appointment/" + apt.getId());
    e.setPatientId(patientId);
    e.setDetails(date + " " + time);
    audits.save(e);
    System.out.println("[appointments] confirmation email → " + patient.getEmail() + ": " + date + " " + time);
    return apt;
  }

  @PatchMapping("/{id}")
  public Appointment update(@PathVariable String id, @RequestBody Map<String, Object> body) {
    AuthUser auth = AuthSupport.requireUser();
    Appointment existing = appointments.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    String scoped = AuthSupport.patientScopeOrNull();
    if (scoped != null && !scoped.equals(existing.getPatientId())) throw new ApiException(403, "Forbidden");

    if (body.get("date") != null) existing.setDate(String.valueOf(body.get("date")));
    if (body.get("time") != null) existing.setTime(normalizeTime(String.valueOf(body.get("time"))));
    if (body.get("reason") != null) existing.setReason(String.valueOf(body.get("reason")));
    if (body.get("status") != null) existing.setStatus(AppointmentStatus.valueOf(String.valueOf(body.get("status"))));

    if (body.get("date") != null || body.get("time") != null) {
      String want = normalizeTime(existing.getTime());
      var conflicts = appointments.findByDoctorIdAndDateAndStatusNot(
              existing.getDoctorId(), existing.getDate(), AppointmentStatus.cancelled)
          .stream()
          .filter(a -> !a.getId().equals(existing.getId()))
          .filter(a -> normalizeTime(a.getTime()).equals(want))
          .toList();
      if (!conflicts.isEmpty()) throw new ApiException(409, "Slot unavailable");
    }
    appointments.save(existing);
    AuditEvent e = new AuditEvent();
    e.setUserId(auth.getId());
    e.setUserName(auth.getName());
    e.setUserRole(auth.getRole().name());
    e.setAction("UPDATE_APPOINTMENT");
    e.setResource("appointment/" + existing.getId());
    e.setPatientId(existing.getPatientId());
    audits.save(e);
    return existing;
  }

  @PostMapping("/{id}/check-in")
  public Appointment checkIn(@PathVariable String id) {
    Appointment existing = appointments.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    String scoped = AuthSupport.patientScopeOrNull();
    if (scoped != null && !scoped.equals(existing.getPatientId())) throw new ApiException(403, "Forbidden");
    existing.setStatus(AppointmentStatus.checked_in);
    return appointments.save(existing);
  }
}
