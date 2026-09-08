package dev.tintwym.medicore.security;

import dev.tintwym.medicore.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${medicore.jwt.secret}") String secret,
      @Value("${medicore.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String createToken(AuthUser user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getId())
        .claim("email", user.getUsername())
        .claim("role", user.getRole().name())
        .claim("name", user.getName())
        .claim("patientId", user.getPatientId())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public AuthUser parse(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    String role = claims.get("role", String.class);
    return new AuthUser(
        claims.getSubject(),
        claims.get("email", String.class),
        claims.get("name", String.class),
        UserRole.valueOf(role),
        claims.get("patientId", String.class),
        "");
  }
}
