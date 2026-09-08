package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vital_readings")
public class VitalReading {
  @Id
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  private Instant timestamp = Instant.now();
  private int heartRate;
  private int bloodPressureSys;
  private int bloodPressureDia;
  private int spO2;
  private double temperature;
  private int respRate;
  private String notes;
  private String recordedBy;
  private boolean isAbnormal;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Patient getPatient() { return patient; }
  public void setPatient(Patient patient) { this.patient = patient; }
  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
  public int getHeartRate() { return heartRate; }
  public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
  public int getBloodPressureSys() { return bloodPressureSys; }
  public void setBloodPressureSys(int bloodPressureSys) { this.bloodPressureSys = bloodPressureSys; }
  public int getBloodPressureDia() { return bloodPressureDia; }
  public void setBloodPressureDia(int bloodPressureDia) { this.bloodPressureDia = bloodPressureDia; }
  public int getSpO2() { return spO2; }
  public void setSpO2(int spO2) { this.spO2 = spO2; }
  public double getTemperature() { return temperature; }
  public void setTemperature(double temperature) { this.temperature = temperature; }
  public int getRespRate() { return respRate; }
  public void setRespRate(int respRate) { this.respRate = respRate; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public String getRecordedBy() { return recordedBy; }
  public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
  public boolean isAbnormal() { return isAbnormal; }
  public void setAbnormal(boolean abnormal) { isAbnormal = abnormal; }
}
