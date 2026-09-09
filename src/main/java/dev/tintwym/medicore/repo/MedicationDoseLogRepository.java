package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.MedicationDoseLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationDoseLogRepository extends JpaRepository<MedicationDoseLog, String> {
  List<MedicationDoseLog> findByPatientIdOrderByLoggedAtDesc(String patientId);

  List<MedicationDoseLog> findAllByOrderByLoggedAtDesc();

  List<MedicationDoseLog> findByPatientIdAndMedicationIdAndLoggedAtGreaterThanEqualOrderByLoggedAtDesc(
      String patientId, String medicationId, Instant loggedAt);
}
