package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
public class Patient {
  @Id
  private String id;

  @Column(nullable = false, unique = true)
  private String mrn;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  private String dob;
  private int age;
  private String gender;
  private String bloodType;
  private String phone;
  private String email;
  private String address;
  private String emergencyName;
  private String emergencyRelation;
  private String emergencyPhone;

  @ElementCollection
  @CollectionTable(name = "patient_allergies", joinColumns = @JoinColumn(name = "patient_id"))
  @Column(name = "allergy")
  private List<String> allergies = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "patient_conditions", joinColumns = @JoinColumn(name = "patient_id"))
  @Column(name = "condition_name")
  private List<String> chronicConditions = new ArrayList<>();

  private String primaryDoctor;
  private String department;
  private String room;
  private String bed;
  private String admissionStatus;
  private String admissionDate;
  private String insuranceProvider;
  private String insurancePolicy;
  private String insuranceGroup;
  private boolean insuranceVerified = true;
  private double insuranceCopay = 35;
  private String fhirId;
  private String paymentMethodPref = "hsa";

  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Medication> medications = new ArrayList<>();

  @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("timestamp ASC")
  private List<VitalReading> vitals = new ArrayList<>();

  @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("date DESC")
  private List<LabResult> labResults = new ArrayList<>();

  @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("date DESC")
  private List<ClinicalNote> clinicalNotes = new ArrayList<>();

  @PreUpdate
  void touch() { updatedAt = Instant.now(); }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getMrn() { return mrn; }
  public void setMrn(String mrn) { this.mrn = mrn; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getDob() { return dob; }
  public void setDob(String dob) { this.dob = dob; }
  public int getAge() { return age; }
  public void setAge(int age) { this.age = age; }
  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }
  public String getBloodType() { return bloodType; }
  public void setBloodType(String bloodType) { this.bloodType = bloodType; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getEmergencyName() { return emergencyName; }
  public void setEmergencyName(String emergencyName) { this.emergencyName = emergencyName; }
  public String getEmergencyRelation() { return emergencyRelation; }
  public void setEmergencyRelation(String emergencyRelation) { this.emergencyRelation = emergencyRelation; }
  public String getEmergencyPhone() { return emergencyPhone; }
  public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
  public List<String> getAllergies() { return allergies; }
  public void setAllergies(List<String> allergies) { this.allergies = allergies; }
  public List<String> getChronicConditions() { return chronicConditions; }
  public void setChronicConditions(List<String> chronicConditions) { this.chronicConditions = chronicConditions; }
  public String getPrimaryDoctor() { return primaryDoctor; }
  public void setPrimaryDoctor(String primaryDoctor) { this.primaryDoctor = primaryDoctor; }
  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }
  public String getRoom() { return room; }
  public void setRoom(String room) { this.room = room; }
  public String getBed() { return bed; }
  public void setBed(String bed) { this.bed = bed; }
  public String getAdmissionStatus() { return admissionStatus; }
  public void setAdmissionStatus(String admissionStatus) { this.admissionStatus = admissionStatus; }
  public String getAdmissionDate() { return admissionDate; }
  public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }
  public String getInsuranceProvider() { return insuranceProvider; }
  public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }
  public String getInsurancePolicy() { return insurancePolicy; }
  public void setInsurancePolicy(String insurancePolicy) { this.insurancePolicy = insurancePolicy; }
  public String getInsuranceGroup() { return insuranceGroup; }
  public void setInsuranceGroup(String insuranceGroup) { this.insuranceGroup = insuranceGroup; }
  public boolean isInsuranceVerified() { return insuranceVerified; }
  public void setInsuranceVerified(boolean insuranceVerified) { this.insuranceVerified = insuranceVerified; }
  public double getInsuranceCopay() { return insuranceCopay; }
  public void setInsuranceCopay(double insuranceCopay) { this.insuranceCopay = insuranceCopay; }
  public String getFhirId() { return fhirId; }
  public void setFhirId(String fhirId) { this.fhirId = fhirId; }
  public String getPaymentMethodPref() { return paymentMethodPref; }
  public void setPaymentMethodPref(String paymentMethodPref) { this.paymentMethodPref = paymentMethodPref; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<Medication> getMedications() { return medications; }
  public List<VitalReading> getVitals() { return vitals; }
  public List<LabResult> getLabResults() { return labResults; }
  public List<ClinicalNote> getClinicalNotes() { return clinicalNotes; }
}
