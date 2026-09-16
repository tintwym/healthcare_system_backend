package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.service.AiAssistService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {
  private final AiAssistService ai;
  private final AuditEventRepository audits;

  public AiController(AiAssistService ai, AuditEventRepository audits) {
    this.ai = ai;
    this.audits = audits;
  }

  @GetMapping("/status")
  public Map<String, Object> status() {
    return ai.status();
  }

  @PostMapping("/assist")
  public Map<String, Object> assist(@RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    Map<String, Object> result = ai.assist(body);

    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction("AI_ASSIST");
    e.setResource("ai/assist");
    Object patientId = result.get("patientId");
    if (patientId != null) e.setPatientId(String.valueOf(patientId));
    e.setDetails(
        String.valueOf(result.getOrDefault("intent", "chat"))
            + " via "
            + result.getOrDefault("provider", "unknown"));
    audits.save(e);

    return result;
  }
}
