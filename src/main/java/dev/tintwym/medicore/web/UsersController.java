package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.UserAccount;
import dev.tintwym.medicore.domain.UserRole;
import dev.tintwym.medicore.repo.UserAccountRepository;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UsersController {
  private final UserAccountRepository users;

  public UsersController(UserAccountRepository users) {
    this.users = users;
  }

  /** Staff directory + messaging recipients (excludes password hashes). */
  @GetMapping
  public List<Map<String, Object>> list(@RequestParam(required = false) String role) {
    AuthSupport.requireUser();
    List<UserAccount> rows = users.findAll();
    if (role != null && !role.isBlank()) {
      String want = role.trim().toLowerCase(Locale.ROOT);
      rows = rows.stream().filter(u -> u.getRole().name().equalsIgnoreCase(want)).toList();
    }
    return rows.stream().map(UsersController::mapUser).toList();
  }

  static Map<String, Object> mapUser(UserAccount u) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", u.getId());
    m.put("name", u.getName());
    m.put("email", u.getEmail());
    m.put("role", u.getRole().name());
    m.put("department", u.getDepartment() == null ? "" : u.getDepartment());
    m.put("avatarUrl", u.getAvatarUrl() == null ? "" : u.getAvatarUrl());
    m.put("patientId", u.getPatientId());
    m.put("licenseNumber", u.getLicenseNumber());
    m.put("mfaEnabled", u.isMfaEnabled());
    return m;
  }
}
