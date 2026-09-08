package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private Instant timestamp = Instant.now();
  private String userId;
  private String userName;
  private String userRole;
  private String action;
  private String resource;
  private String patientId;
  private String details;
  private String ipHash = "local";

  public String getId() { return id; }
  public Instant getTimestamp() { return timestamp; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getUserRole() { return userRole; }
  public void setUserRole(String userRole) { this.userRole = userRole; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getResource() { return resource; }
  public void setResource(String resource) { this.resource = resource; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public String getIpHash() { return ipHash; }
  public void setIpHash(String ipHash) { this.ipHash = ipHash; }
}
