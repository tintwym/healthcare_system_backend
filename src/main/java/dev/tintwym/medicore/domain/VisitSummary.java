package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visit_summaries")
public class VisitSummary {
  @Id
  private String id;

  @Column(nullable = false)
  private String patientId;

  private String appointmentId;

  @Column(nullable = false)
  private String date;

  @Column(nullable = false)
  private String author;

  private String authorRole = "Attending";

  @Column(nullable = false)
  private String title;

  @Column(length = 4000)
  private String summaryBody;

  @Column(length = 2000)
  private String instructions;

  @Column(length = 2000)
  private String warningSigns;

  /** draft | finalized */
  private String status = "finalized";

  private Instant createdAt = Instant.now();

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getAppointmentId() { return appointmentId; }
  public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
  public String getAuthorRole() { return authorRole; }
  public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSummaryBody() { return summaryBody; }
  public void setSummaryBody(String summaryBody) { this.summaryBody = summaryBody; }
  public String getInstructions() { return instructions; }
  public void setInstructions(String instructions) { this.instructions = instructions; }
  public String getWarningSigns() { return warningSigns; }
  public void setWarningSigns(String warningSigns) { this.warningSigns = warningSigns; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
