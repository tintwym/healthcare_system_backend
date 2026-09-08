package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.VitalReading;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalReadingRepository extends JpaRepository<VitalReading, String> {
  List<VitalReading> findByPatient_IdOrderByTimestampAsc(String patientId);
}
