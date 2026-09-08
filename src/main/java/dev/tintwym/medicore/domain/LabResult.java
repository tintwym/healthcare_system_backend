package dev.tintwym.medicore.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "lab_results")
public class LabResult {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  private String testName;
  private String category;
  private String date;
  private String value;
  private String referenceRange;
  private String status = "normal";
  private String notes;
  private String orderedBy;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Patient getPatient() { return patient; }
  public void setPatient(Patient patient) { this.patient = patient; }
  public String getTestName() { return testName; }
  public void setTestName(String testName) { this.testName = testName; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }
  public String getReferenceRange() { return referenceRange; }
  public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public String getOrderedBy() { return orderedBy; }
  public void setOrderedBy(String orderedBy) { this.orderedBy = orderedBy; }
}
