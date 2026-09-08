package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fhir_bundle_logs")
public class FhirBundleLog {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String direction;
  private String resourceType;

  @Column(columnDefinition = "TEXT")
  private String payload;

  private Instant createdAt = Instant.now();

  public String getId() { return id; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public String getResourceType() { return resourceType; }
  public void setResourceType(String resourceType) { this.resourceType = resourceType; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public Instant getCreatedAt() { return createdAt; }
}
