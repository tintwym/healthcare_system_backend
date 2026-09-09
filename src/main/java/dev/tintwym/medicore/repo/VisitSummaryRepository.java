package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.VisitSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitSummaryRepository extends JpaRepository<VisitSummary, String> {
  List<VisitSummary> findByPatientIdOrderByDateDesc(String patientId);

  List<VisitSummary> findAllByOrderByDateDesc();
}
