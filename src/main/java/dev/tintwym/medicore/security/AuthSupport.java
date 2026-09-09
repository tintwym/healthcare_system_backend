package dev.tintwym.medicore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthSupport {
  private AuthSupport() {}

  public static AuthUser requireUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
      throw new ApiException(401, "Unauthorized");
    }
    return user;
  }

  public static String patientScopeOrNull() {
    AuthUser user = requireUser();
    if (user.getRole().name().equals("patient")) {
      String patientId = user.getPatientId();
      if (patientId == null || patientId.isBlank()) {
        throw new ApiException(403, "Patient account is not linked");
      }
      return patientId;
    }
    return null;
  }

  public static String patientScopeOr(String requestedPatientId) {
    String scoped = patientScopeOrNull();
    if (scoped != null) return scoped;
    if (requestedPatientId == null || requestedPatientId.isBlank()) {
      throw new ApiException(400, "patientId required for staff");
    }
    return requestedPatientId;
  }
}
