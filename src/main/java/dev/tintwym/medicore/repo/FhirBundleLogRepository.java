package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.FhirBundleLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FhirBundleLogRepository extends JpaRepository<FhirBundleLog, String> {
  List<FhirBundleLog> findTop50ByOrderByCreatedAtDesc();
}
