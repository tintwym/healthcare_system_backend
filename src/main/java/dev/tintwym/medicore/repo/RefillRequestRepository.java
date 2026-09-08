package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.RefillRequest;
import dev.tintwym.medicore.domain.RefillStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefillRequestRepository extends JpaRepository<RefillRequest, String> {
  List<RefillRequest> findByPatientIdOrderByCreatedAtDesc(String patientId);
  List<RefillRequest> findByStatusInOrderByCreatedAtAsc(Collection<RefillStatus> statuses);
  List<RefillRequest> findAllByOrderByCreatedAtDesc();
}
