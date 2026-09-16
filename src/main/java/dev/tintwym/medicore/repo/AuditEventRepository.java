package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
  List<AuditEvent> findAllByOrderByTimestampDesc();
}
