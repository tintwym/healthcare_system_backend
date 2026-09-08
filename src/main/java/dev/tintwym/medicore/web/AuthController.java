package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.UserAccount;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.UserAccountRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.security.JwtService;
import dev.tintwym.medicore.domain.AuditEvent;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final UserAccountRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwtService;
  private final AuditEventRepository audits;

  public AuthController(
      UserAccountRepository users,
      PasswordEncoder encoder,
      JwtService jwtService,
      AuditEventRepository audits) {
    this.users = users;
    this.encoder = encoder;
    this.jwtService = jwtService;
    this.audits = audits;
  }

  @PostMapping("/login")
  public Map<String, Object> login(@RequestBody Map<String, String> body) {
    String email = body.getOrDefault("email", "").trim().toLowerCase();
    String password = body.getOrDefault("password", "");
    if (email.isBlank() || password.isBlank()) {
      throw new ApiException(400, "Email and password required");
    }
    UserAccount user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ApiException(401, "Invalid email or password."));
    if (!encoder.matches(password, user.getPasswordHash())) {
      throw new ApiException(401, "Invalid email or password.");
    }
    AuthUser authUser = new AuthUser(
        user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getPatientId(), user.getPasswordHash());
    String token = jwtService.createToken(authUser);
    audit(user, "LOGIN", "auth", null);
    Map<String, Object> userMap = new java.util.LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("email", user.getEmail());
    userMap.put("name", user.getName());
    userMap.put("role", user.getRole().name());
    userMap.put("patientId", user.getPatientId());
    userMap.put("department", user.getDepartment());
    userMap.put("avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
    return Map.of("token", token, "user", userMap);
  }

  @PostMapping("/logout")
  public Map<String, Boolean> logout() {
    AuthUser user = AuthSupport.requireUser();
    auditUser(user, "LOGOUT", "auth");
    return Map.of("ok", true);
  }

  @GetMapping("/me")
  public Map<String, Object> me() {
    AuthUser auth = AuthSupport.requireUser();
    UserAccount user = users.findById(auth.getId()).orElseThrow(() -> new ApiException(404, "User not found"));
    Map<String, Object> userMap = new java.util.LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("email", user.getEmail());
    userMap.put("name", user.getName());
    userMap.put("role", user.getRole().name());
    userMap.put("patientId", user.getPatientId());
    userMap.put("department", user.getDepartment());
    userMap.put("avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
    return userMap;
  }

  private void audit(UserAccount user, String action, String resource, String details) {
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction(action);
    e.setResource(resource);
    e.setPatientId(user.getPatientId());
    e.setDetails(details);
    audits.save(e);
  }

  private void auditUser(AuthUser user, String action, String resource) {
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction(action);
    e.setResource(resource);
    e.setPatientId(user.getPatientId());
    audits.save(e);
  }
}
