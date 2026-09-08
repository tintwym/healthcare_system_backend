package dev.tintwym.medicore.security;

import dev.tintwym.medicore.domain.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUser implements UserDetails {
  private final String id;
  private final String email;
  private final String name;
  private final UserRole role;
  private final String patientId;
  private final String passwordHash;

  public AuthUser(String id, String email, String name, UserRole role, String patientId, String passwordHash) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.role = role;
    this.patientId = patientId;
    this.passwordHash = passwordHash;
  }

  public String getId() { return id; }
  public String getName() { return name; }
  public UserRole getRole() { return role; }
  public String getPatientId() { return patientId; }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()));
  }

  @Override
  public String getPassword() { return passwordHash; }

  @Override
  public String getUsername() { return email; }

  @Override
  public boolean isAccountNonExpired() { return true; }

  @Override
  public boolean isAccountNonLocked() { return true; }

  @Override
  public boolean isCredentialsNonExpired() { return true; }

  @Override
  public boolean isEnabled() { return true; }
}
