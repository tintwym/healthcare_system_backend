package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.domain.UserRole;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {
  private final AuditEventRepository audits;

  public AuditController(AuditEventRepository audits) {
    this.audits = audits;
  }

  @GetMapping("/events")
  public List<Map<String, Object>> list() {
    AuthUser user = AuthSupport.requireUser();
    if (user.getRole() == UserRole.patient) {
      throw new ApiException(403, "Staff only");
    }
    return audits.findAllByOrderByTimestampDesc().stream().map(AuditController::map).toList();
  }

  private static Map<String, Object> map(AuditEvent e) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", e.getId());
    m.put("timestamp", e.getTimestamp().toString());
    m.put("userId", e.getUserId());
    m.put("userName", e.getUserName());
    m.put("userRole", e.getUserRole());
    m.put("action", e.getAction());
    m.put("resource", e.getResource());
    m.put("patientId", e.getPatientId());
    m.put("details", e.getDetails());
    m.put("ipHash", e.getIpHash());
    return m;
  }
}
