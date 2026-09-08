package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.Appointment;
import dev.tintwym.medicore.domain.AppointmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
  List<Appointment> findByPatientIdOrderByDateAscTimeAsc(String patientId);
  List<Appointment> findByDoctorIdAndDateAndStatusNot(String doctorId, String date, AppointmentStatus status);
  List<Appointment> findByDoctorIdAndDateAndTimeAndStatusNotAndIdNot(
      String doctorId, String date, String time, AppointmentStatus status, String id);
  List<Appointment> findByDateAndStatusAndReminderSentAtIsNull(String date, AppointmentStatus status);
  List<Appointment> findByDateInAndStatusAndReminderSentAtIsNull(List<String> dates, AppointmentStatus status);
  List<Appointment> findAllByOrderByDateAscTimeAsc();
}
