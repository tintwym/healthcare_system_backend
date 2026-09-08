package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "device_registrations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "token"}))
public class DeviceRegistration {
  @Id
  private String id;

  @Column(nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DevicePlatform platform;

  /** Expo push token, or serialized Web PushSubscription JSON. */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String token;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public DevicePlatform getPlatform() { return platform; }
  public void setPlatform(DevicePlatform platform) { this.platform = platform; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
