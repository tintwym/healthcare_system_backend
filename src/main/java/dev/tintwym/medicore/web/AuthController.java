package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.Patient;
import dev.tintwym.medicore.domain.UserAccount;
import dev.tintwym.medicore.domain.UserRole;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.PatientRepository;
import dev.tintwym.medicore.repo.UserAccountRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.security.JwtService;
import dev.tintwym.medicore.domain.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final UserAccountRepository users;
  private final PatientRepository patients;
  private final PasswordEncoder encoder;
  private final JwtService jwtService;
  private final AuditEventRepository audits;

  public AuthController(
      UserAccountRepository users,
      PatientRepository patients,
      PasswordEncoder encoder,
      JwtService jwtService,
      AuditEventRepository audits) {
    this.users = users;
    this.patients = patients;
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
    return Map.of("token", token, "user", toUserMap(user));
  }

  @PostMapping("/register")
  public Map<String, Object> register(@RequestBody Map<String, String> body) {
    String email = body.getOrDefault("email", "").trim().toLowerCase();
    String password = body.getOrDefault("password", "");
    String firstName = body.getOrDefault("firstName", "").trim();
    String lastName = body.getOrDefault("lastName", "").trim();
    String phone = body.getOrDefault("phone", "").trim();
    if (email.isBlank() || password.isBlank() || firstName.isBlank() || lastName.isBlank()) {
      throw new ApiException(400, "First name, last name, email, and password are required");
    }
    if (password.length() < 8) {
      throw new ApiException(400, "Password must be at least 8 characters");
    }
    if (users.findByEmailIgnoreCase(email).isPresent()) {
      throw new ApiException(409, "An account with this email already exists");
    }

    String patientId = "pat-" + UUID.randomUUID().toString().substring(0, 8);
    String userId = "u-" + UUID.randomUUID().toString().substring(0, 8);
    String mrn = generateUniqueMrn();
    String displayName = firstName + " " + lastName;

    Patient patient = new Patient();
    patient.setId(patientId);
    patient.setMrn(mrn);
    patient.setFirstName(firstName);
    patient.setLastName(lastName);
    patient.setEmail(email);
    patient.setPhone(phone);
    patient.setDob("");
    patient.setAge(0);
    patient.setGender("Other");
    patient.setBloodType("O+");
    patient.setAddress("");
    patient.setEmergencyName("");
    patient.setEmergencyRelation("");
    patient.setEmergencyPhone("");
    patient.setPrimaryDoctor("");
    patient.setDepartment("Outpatient Care");
    patient.setRoom("");
    patient.setBed("");
    patient.setAdmissionStatus("Outpatient");
    patient.setAdmissionDate(Instant.now().toString());
    patient.setInsuranceProvider("");
    patient.setInsurancePolicy("");
    patient.setInsuranceGroup("");
    patient.setInsuranceVerified(false);
    patient.setInsuranceCopay(35);
    patient.setFhirId("FHIR-" + patientId.toUpperCase());
    patients.save(patient);

    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setName(displayName);
    user.setEmail(email);
    user.setPasswordHash(encoder.encode(password));
    user.setRole(UserRole.patient);
    user.setDepartment("Outpatient Care");
    user.setAvatarUrl("");
    user.setMfaEnabled(false);
    user.setPatientId(patientId);
    users.save(user);

    AuthUser authUser = new AuthUser(
        user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getPatientId(), user.getPasswordHash());
    String token = jwtService.createToken(authUser);
    audit(user, "REGISTER", "auth", "Patient self-registration");
    return Map.of("token", token, "user", toUserMap(user));
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
    return toUserMap(user);
  }

  private Map<String, Object> toUserMap(UserAccount user) {
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

  private String generateUniqueMrn() {
    for (int i = 0; i < 20; i++) {
      String mrn = "MRN-" + ThreadLocalRandom.current().nextInt(100000, 999999);
      if (patients.findAll().stream().noneMatch(p -> mrn.equals(p.getMrn()))) {
        return mrn;
      }
    }
    return "MRN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
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
