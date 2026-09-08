package dev.tintwym.medicore.service;

import dev.tintwym.medicore.domain.Appointment;
import dev.tintwym.medicore.domain.AppointmentStatus;
import dev.tintwym.medicore.domain.UserAccount;
import dev.tintwym.medicore.repo.AppointmentRepository;
import dev.tintwym.medicore.repo.UserAccountRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppointmentReminderJob {
  private static final Logger log = LoggerFactory.getLogger(AppointmentReminderJob.class);
  private static final ZoneId YANGON = ZoneId.of("Asia/Yangon");

  private final AppointmentRepository appointments;
  private final UserAccountRepository users;
  private final PushNotificationService push;

  public AppointmentReminderJob(
      AppointmentRepository appointments,
      UserAccountRepository users,
      PushNotificationService push) {
    this.appointments = appointments;
    this.users = users;
    this.push = push;
  }

  @Scheduled(fixedDelayString = "900000", initialDelayString = "30000")
  public void sendReminders() {
    LocalDate today = LocalDate.now(YANGON);
    LocalDate tomorrow = today.plusDays(1);
    List<String> dates = List.of(today.toString(), tomorrow.toString());
    List<Appointment> due =
        appointments.findByDateInAndStatusAndReminderSentAtIsNull(
            dates, AppointmentStatus.scheduled);
    for (Appointment a : due) {
      UserAccount patientUser = users.findFirstByPatientId(a.getPatientId()).orElse(null);
      if (patientUser == null) continue;
      boolean isToday = today.toString().equals(a.getDate());
      String title = isToday ? "Visit today — check in" : "Visit tomorrow";
      String body =
          (a.getType() == null ? "Appointment" : a.getType())
              + " with "
              + (a.getDoctorName() == null ? "your care team" : a.getDoctorName())
              + " at "
              + a.getTime();
      push.notifyUser(
          patientUser.getId(),
          title,
          body,
          Map.of(
              "type", "appointment",
              "screen", "Visits",
              "appointmentId", a.getId()));
      a.setReminderSentAt(Instant.now());
      appointments.save(a);
      log.info("Sent appointment reminder for {}", a.getId());
    }
  }
}
