package dev.tintwym.medicore.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "clinical_notes")
public class ClinicalNote {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  private String date;
  private String author;
  private String authorRole;
  private String title;

  @Column(length = 2000)
  private String soapSubjective;

  @Column(length = 2000)
  private String soapObjective;

  @Column(length = 2000)
  private String soapAssessment;

  @Column(length = 2000)
  private String soapPlan;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Patient getPatient() { return patient; }
  public void setPatient(Patient patient) { this.patient = patient; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
  public String getAuthorRole() { return authorRole; }
  public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSoapSubjective() { return soapSubjective; }
  public void setSoapSubjective(String soapSubjective) { this.soapSubjective = soapSubjective; }
  public String getSoapObjective() { return soapObjective; }
  public void setSoapObjective(String soapObjective) { this.soapObjective = soapObjective; }
  public String getSoapAssessment() { return soapAssessment; }
  public void setSoapAssessment(String soapAssessment) { this.soapAssessment = soapAssessment; }
  public String getSoapPlan() { return soapPlan; }
  public void setSoapPlan(String soapPlan) { this.soapPlan = soapPlan; }
}
