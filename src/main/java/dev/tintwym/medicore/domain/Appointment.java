package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "appointments")
public class Appointment {
  @Id
  private String id;

  @Column(nullable = false)
  private String patientId;

  private String patientName;
  private String patientMrn;
  private String doctorId;
  private String doctorName;
  private String department;
  private String date;
  private String time;
  private int durationMinutes = 30;
  private String type = "Follow-up";

  @Enumerated(EnumType.STRING)
  private AppointmentStatus status = AppointmentStatus.scheduled;

  private String priority = "routine";
  private String reason;
  private String room = "Outpatient Clinic";
  private String notes;
  private boolean automatedBillingTriggered;
  private Instant reminderSentAt;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() { updatedAt = Instant.now(); }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getPatientName() { return patientName; }
  public void setPatientName(String patientName) { this.patientName = patientName; }
  public String getPatientMrn() { return patientMrn; }
  public void setPatientMrn(String patientMrn) { this.patientMrn = patientMrn; }
  public String getDoctorId() { return doctorId; }
  public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
  public String getDoctorName() { return doctorName; }
  public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public String getTime() { return time; }
  public void setTime(String time) { this.time = time; }
  public int getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public AppointmentStatus getStatus() { return status; }
  public void setStatus(AppointmentStatus status) { this.status = status; }
  public String getPriority() { return priority; }
  public void setPriority(String priority) { this.priority = priority; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getRoom() { return room; }
  public void setRoom(String room) { this.room = room; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getReminderSentAt() { return reminderSentAt; }
  public void setReminderSentAt(Instant reminderSentAt) { this.reminderSentAt = reminderSentAt; }
  public boolean isAutomatedBillingTriggered() { return automatedBillingTriggered; }
  public void setAutomatedBillingTriggered(boolean automatedBillingTriggered) {
    this.automatedBillingTriggered = automatedBillingTriggered;
  }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
