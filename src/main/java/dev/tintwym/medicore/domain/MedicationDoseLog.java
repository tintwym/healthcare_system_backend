package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "medication_dose_logs")
public class MedicationDoseLog {
  @Id
  private String id;

  @Column(nullable = false)
  private String patientId;

  @Column(nullable = false)
  private String medicationId;

  @Column(nullable = false)
  private String medicationName;

  /** taken | skipped */
  @Column(nullable = false)
  private String status;

  private String notes;

  @Column(nullable = false)
  private Instant loggedAt = Instant.now();

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getMedicationId() { return medicationId; }
  public void setMedicationId(String medicationId) { this.medicationId = medicationId; }
  public String getMedicationName() { return medicationName; }
  public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getLoggedAt() { return loggedAt; }
  public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }
}
