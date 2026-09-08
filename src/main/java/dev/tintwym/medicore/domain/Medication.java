package dev.tintwym.medicore.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "medications")
public class Medication {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  private String name;
  private String dosage;
  private String frequency;
  private String route = "Oral";
  private String startDate;
  private String endDate;
  private String prescribedBy;
  private String status = "active";

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Patient getPatient() { return patient; }
  public void setPatient(Patient patient) { this.patient = patient; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDosage() { return dosage; }
  public void setDosage(String dosage) { this.dosage = dosage; }
  public String getFrequency() { return frequency; }
  public void setFrequency(String frequency) { this.frequency = frequency; }
  public String getRoute() { return route; }
  public void setRoute(String route) { this.route = route; }
  public String getStartDate() { return startDate; }
  public void setStartDate(String startDate) { this.startDate = startDate; }
  public String getEndDate() { return endDate; }
  public void setEndDate(String endDate) { this.endDate = endDate; }
  public String getPrescribedBy() { return prescribedBy; }
  public void setPrescribedBy(String prescribedBy) { this.prescribedBy = prescribedBy; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
