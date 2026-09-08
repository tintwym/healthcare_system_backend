package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refill_requests")
public class RefillRequest {
  @Id
  private String id;

  private String patientId;
  private String medicationName;
  private String notes;

  @Enumerated(EnumType.STRING)
  private RefillStatus status = RefillStatus.pending;

  private String prescriptionId;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() { updatedAt = Instant.now(); }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public RefillStatus getStatus() { return status; }
  public void setStatus(RefillStatus status) { this.status = status; }
  public String getPrescriptionId() { return prescriptionId; }
  public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
