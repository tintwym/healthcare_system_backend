package dev.tintwym.medicore.config;

import dev.tintwym.medicore.domain.*;
import dev.tintwym.medicore.repo.*;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
  @Bean
  CommandLineRunner seed(
      UserAccountRepository users,
      PatientRepository patients,
      AppointmentRepository appointments,
      InvoiceRepository invoices,
      MessageRepository messages,
      RefillRequestRepository refills,
      VisitSummaryRepository visitSummaries,
      MedicationDoseLogRepository doseLogs,
      PasswordEncoder encoder) {
    return args -> {
      if (users.count() == 0) {
        seedFresh(
            users, patients, appointments, invoices, messages, refills, encoder);
      }
      seedCareExtras(patients, visitSummaries, doseLogs);
    };
  }

  private static void seedCareExtras(
      PatientRepository patients,
      VisitSummaryRepository visitSummaries,
      MedicationDoseLogRepository doseLogs) {
    if (!patients.existsById("pat-001")) return;

    if (visitSummaries.count() == 0) {
      VisitSummary avs = new VisitSummary();
      avs.setId("avs-demo-001");
      avs.setPatientId("pat-001");
      avs.setAppointmentId(null);
      avs.setDate("2026-09-01");
      avs.setAuthor("Dr. Aye Myat Thu, MD");
      avs.setAuthorRole("doctor");
      avs.setTitle("Cardiology follow-up summary");
      avs.setSummaryBody(
          "Stable hypertension and mild asthma. Blood pressure improving on Lisinopril. "
              + "No chest pain at rest. Continue current regimen and home BP log.");
      avs.setInstructions(
          "Take Lisinopril 10 mg every morning. Log BP twice daily. Echo in 4 weeks. "
              + "Use Albuterol inhaler as needed before exercise.");
      avs.setWarningSigns(
          "Return or message care team if chest pain, severe shortness of breath, "
              + "or systolic BP over 150 with headache.");
      avs.setStatus("finalized");
      avs.setCreatedAt(Instant.parse("2026-09-01T10:30:00Z"));
      visitSummaries.save(avs);
    }

    if (doseLogs.count() == 0) {
      MedicationDoseLog d1 = new MedicationDoseLog();
      d1.setId("dose-demo-001");
      d1.setPatientId("pat-001");
      d1.setMedicationId("med-1");
      d1.setMedicationName("Lisinopril");
      d1.setStatus("taken");
      d1.setNotes("Morning dose");
      d1.setLoggedAt(Instant.parse("2026-09-08T01:10:00Z"));
      doseLogs.save(d1);

      MedicationDoseLog d2 = new MedicationDoseLog();
      d2.setId("dose-demo-002");
      d2.setPatientId("pat-001");
      d2.setMedicationId("med-3");
      d2.setMedicationName("Atorvastatin");
      d2.setStatus("taken");
      d2.setLoggedAt(Instant.parse("2026-09-07T15:40:00Z"));
      doseLogs.save(d2);
    }
  }

  private static void seedFresh(
      UserAccountRepository users,
      PatientRepository patients,
      AppointmentRepository appointments,
      InvoiceRepository invoices,
      MessageRepository messages,
      RefillRequestRepository refills,
      PasswordEncoder encoder) {
      String patientPassword = encoder.encode("patient123");
      String staffPassword = encoder.encode("staff123");

      // --- Staff (minimal demo set) ---
      seedUser(users, "u-1", "Dr. Aye Myat Thu, MD", "aye.myatthu@medicore.mm", UserRole.doctor, "Cardiology", staffPassword, null, "MMC-C-92841");
      seedUser(users, "u-2", "Dr. Kyaw Zin Oo, MD", "kyaw.zinoo@medicore.mm", UserRole.doctor, "Internal Medicine", staffPassword, null, "MMC-IM-84729");
      seedUser(users, "u-3", "Daw Hnin Wai, RN", "hnin.wai@medicore.mm", UserRole.nurse, "Cardiology Ward", staffPassword, null, null);
      seedUser(users, "u-4", "U Min Thu, MHA", "min.thu@medicore.mm", UserRole.admin, "Hospital Administration", staffPassword, null, null);
      seedUser(users, "u-5", "Ko Aung Ko, CPB", "aung.ko@medicore.mm", UserRole.billing, "Revenue Cycle Management", staffPassword, null, null);
      seedUser(users, "u-7", "Daw Khin Sandar, PharmD", "khin.sandar@medicore.mm", UserRole.pharmacist, "Pharmacy", staffPassword, null, "MMP-RPH-44102");

      // --- Patients ---
      Patient thiri = buildThiri();
      addMed(thiri, "med-1", "Lisinopril", "10 mg", "Once daily", "Oral", "2025-01-15", "Dr. Aye Myat Thu", "active");
      addMed(thiri, "med-2", "Albuterol Inhaler", "90 mcg", "As needed", "Inhalation", "2024-06-01", "Dr. Kyaw Zin Oo", "active");
      addMed(thiri, "med-3", "Atorvastatin", "20 mg", "Once daily at bedtime", "Oral", "2025-03-01", "Dr. Aye Myat Thu", "active");
      addVital(thiri, "v-101", "2026-09-02T08:00:00Z", 74, 124, 82, 98, 98.6, 16, "Nurse Daw Hnin Wai", false, null);
      addVital(thiri, "v-102", "2026-09-03T08:15:00Z", 78, 128, 84, 97, 98.4, 17, "Nurse Daw Hnin Wai", false, "Mild morning elevation");
      addVital(thiri, "v-103", "2026-09-04T20:00:00Z", 88, 138, 90, 96, 99.1, 18, "Thiri Su Pyae (home)", true, "Home BP elevated after cardio rehab");
      addVital(thiri, "v-104", "2026-09-05T07:45:00Z", 72, 122, 80, 98, 98.2, 15, "Nurse Daw Hnin Wai", false, null);
      addVital(thiri, "v-105", "2026-09-06T08:00:00Z", 76, 126, 83, 98, 98.5, 16, "Ko Naing Lin, RN", false, null);
      addLab(thiri, "lab-1", "BMP / Creatinine", "Biochemistry", "2026-09-02", "1.1 mg/dL", "0.6–1.2", "normal", null, "Dr. Aye Myat Thu");
      addLab(thiri, "lab-2", "Troponin I", "Cardiology", "2026-09-01", "0.02 ng/mL", "<0.04", "normal", "Ruled out ACS", "Dr. Aye Myat Thu");
      addLab(thiri, "lab-3", "Lipid Panel / LDL", "Biochemistry", "2026-08-28", "118 mg/dL", "<100 optimal", "flagged", "Lifestyle counseling reinforced", "Dr. Aye Myat Thu");
      addLab(thiri, "lab-4", "CBC / Hemoglobin", "Hematology", "2026-08-28", "13.2 g/dL", "12.0–15.5", "normal", null, "Dr. Aye Myat Thu");
      addNote(
          thiri, "note-1", "2026-09-01", "Dr. Aye Myat Thu", "Attending", "Cardiology follow-up",
          "Reports mild exertional dyspnea after rehab sessions. No chest pain at rest. Sleeping through the night.",
          "BP 128/82, HR 78, SpO2 99%. S1/S2 normal, lungs clear. Telemetry without sustained arrhythmia.",
          "Stable stage 1 hypertension with mild asthma. Improving on current regimen.",
          "Continue Lisinopril 10mg. Home BP log twice daily. Echo in 4 weeks. Return precautions reviewed.");
      addNote(
          thiri, "note-2", "2026-09-04", "Daw Hnin Wai, RN", "Nurse", "Home monitoring review",
          "Patient messaged elevated evening BP after exercise. Mild headache, resolved with rest.",
          "Remote vitals: HR 88, BP 138/90, SpO2 96%. Alert flagged in patient app.",
          "Transient post-exertion hypertension; no end-organ symptoms.",
          "Advised cool-down protocol, hydrate, recheck in 30 minutes. Escalate if systolic >150.");
      patients.save(thiri);
      seedUser(users, "u-6", "Thiri Su Pyae (Patient)", "thiri.supyae@gmail.com", UserRole.patient, "Outpatient Care", patientPassword, "pat-001", null);

      // --- Appointments (pat-001 only) ---
      appointments.save(apt("apt-001", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-09-15", "10:30 AM", 30, "Follow-up", AppointmentStatus.scheduled, "routine",
          "Routine cardiovascular checkup and medication review.", "Clinic A", null));
      appointments.save(apt("apt-002", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-2", "Dr. Kyaw Zin Oo, MD",
          "Internal Medicine", "2026-09-22", "2:00 PM", 45, "General Checkup", AppointmentStatus.scheduled, "routine",
          "Asthma follow-up and inhaler technique review.", "Clinic B", null));
      appointments.save(apt("apt-003", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-08-20", "9:00 AM", 30, "Follow-up", AppointmentStatus.completed, "routine",
          "BP trend review after Lisinopril titration.", "Clinic A", "BP improved; continue current dose."));
      // --- Invoices (pat-001 only) ---
      invoices.save(invoice("inv-001", "INV-2026-0912", "pat-001", "Thiri Su Pyae", "MRN-782104", "apt-001",
          "2026-09-02", "2026-09-30",
          "[{\"id\":\"ii-1\",\"code\":\"99213\",\"description\":\"Office visit\",\"category\":\"Consultation\",\"quantity\":1,\"unitPrice\":185,\"total\":185},{\"id\":\"ii-2\",\"code\":\"93000\",\"description\":\"ECG\",\"category\":\"Diagnostics\",\"quantity\":1,\"unitPrice\":95,\"total\":95}]",
          280, 40, 180, 60, 0, InvoiceStatus.pending,
          "{\"claimId\":\"CLM-99421\",\"payerName\":\"AIA Myanmar\",\"submittedDate\":\"2026-09-02\",\"status\":\"In Review\"}", null));
      invoices.save(invoice("inv-002", "INV-2026-0820", "pat-001", "Thiri Su Pyae", "MRN-782104", "apt-003",
          "2026-08-20", "2026-09-10",
          "[{\"id\":\"ii-3\",\"code\":\"80053\",\"description\":\"CMP\",\"category\":\"Laboratory\",\"quantity\":1,\"unitPrice\":120,\"total\":120}]",
          120, 20, 70, 30, 30, InvoiceStatus.paid, null, "RCPT-SEED001"));
      // --- Messages (pat-001 thread) ---
      messages.save(msg("msg-001", "u-1", "Dr. Aye Myat Thu", "doctor", "u-6", "Thiri Su Pyae",
          "Follow-up after vitals review",
          "Thiri, your recent readings show mild elevation overnight. Please continue Lisinopril and log home BP twice daily before our Sept 15 visit.",
          false, false, "2026-09-04T15:20:00Z"));
      messages.save(msg("msg-002", "u-6", "Thiri Su Pyae", "patient", "u-1", "Dr. Aye Myat Thu",
          "Question about inhaler",
          "Should I increase albuterol use before cardio rehab sessions?",
          true, false, "2026-09-03T11:05:00Z"));
      messages.save(msg("msg-003", "u-1", "Dr. Aye Myat Thu", "doctor", "u-6", "Thiri Su Pyae",
          "Re: Question about inhaler",
          "No — keep rescue use as needed only. If you need it more than twice weekly, message me and we will adjust controller therapy.",
          false, false, "2026-09-03T16:40:00Z"));
      messages.save(msg("msg-004", "u-7", "Daw Khin Sandar", "pharmacist", "u-6", "Thiri Su Pyae",
          "Refill approved",
          "Your Atorvastatin refill was approved and will be ready for pickup tomorrow after 2 PM.",
          false, false, "2026-09-06T14:30:00Z"));

      // --- Refills (pat-001 only) ---
      refills.save(refill("ref-001", "pat-001", "Lisinopril", "Need 90-day supply before travel", RefillStatus.pending, "med-1", "2026-09-06T08:00:00Z"));
      refills.save(refill("ref-002", "pat-001", "Atorvastatin", "Running low — 5 tablets left", RefillStatus.approved, "med-3", "2026-09-05T16:20:00Z"));

      System.out.println(
          "Seeded Medicore demo data: 1 patient (pat-001), staff accounts, appointments/invoices/messages/refills. "
              + "Patient: thiri.supyae@gmail.com / patient123 · Staff: */staff123");
  }

  private static Patient buildThiri() {
    return buildPatient(
        "pat-001", "MRN-782104", "Thiri Su", "Pyae", "1989-11-23", 36, "Female", "O+",
        "+95 9 420 100 201", "thiri.supyae@gmail.com",
        "No. 42, Inya Road, Kamayut Township, Yangon",
        "Aung Ko Phyo", "Brother", "+95 9 420 100 202",
        List.of("Penicillin", "Sulfa Drugs"),
        List.of("Stage 1 Hypertension", "Mild Asthma"),
        "Dr. Aye Myat Thu, MD", "Cardiology",
        "Room 304", "Bed A", "Inpatient", "2026-09-01T14:30:00Z",
        "AIA Myanmar Comprehensive", "AIA-MM-994821", "YANGON-CARE", "FHIR-PAT-001-MM", 35);
  }

  private static Patient buildPatient(
      String id, String mrn, String first, String last, String dob, int age, String gender, String blood,
      String phone, String email, String address,
      String emergencyName, String emergencyRelation, String emergencyPhone,
      List<String> allergies, List<String> conditions,
      String primaryDoctor, String department,
      String room, String bed, String admissionStatus, String admissionDate,
      String insuranceProvider, String insurancePolicy, String insuranceGroup, String fhirId, double copay) {
    Patient p = new Patient();
    p.setId(id);
    p.setMrn(mrn);
    p.setFirstName(first);
    p.setLastName(last);
    p.setDob(dob);
    p.setAge(age);
    p.setGender(gender);
    p.setBloodType(blood);
    p.setPhone(phone);
    p.setEmail(email);
    p.setAddress(address);
    p.setEmergencyName(emergencyName);
    p.setEmergencyRelation(emergencyRelation);
    p.setEmergencyPhone(emergencyPhone);
    p.setAllergies(allergies);
    p.setChronicConditions(conditions);
    p.setPrimaryDoctor(primaryDoctor);
    p.setDepartment(department);
    p.setRoom(room);
    p.setBed(bed);
    p.setAdmissionStatus(admissionStatus);
    p.setAdmissionDate(admissionDate);
    p.setInsuranceProvider(insuranceProvider);
    p.setInsurancePolicy(insurancePolicy);
    p.setInsuranceGroup(insuranceGroup);
    p.setInsuranceCopay(copay);
    p.setFhirId(fhirId);
    return p;
  }

  private static void addMed(
      Patient patient, String id, String name, String dosage, String frequency, String route,
      String startDate, String prescribedBy, String status) {
    Medication m = new Medication();
    m.setId(id);
    m.setPatient(patient);
    m.setName(name);
    m.setDosage(dosage);
    m.setFrequency(frequency);
    m.setRoute(route);
    m.setStartDate(startDate);
    m.setPrescribedBy(prescribedBy);
    m.setStatus(status);
    patient.getMedications().add(m);
  }

  private static void addVital(
      Patient patient, String id, String timestamp, int hr, int sys, int dia, int spo2,
      double temp, int resp, String recordedBy, boolean abnormal, String notes) {
    VitalReading v = new VitalReading();
    v.setId(id);
    v.setPatient(patient);
    v.setTimestamp(Instant.parse(timestamp));
    v.setHeartRate(hr);
    v.setBloodPressureSys(sys);
    v.setBloodPressureDia(dia);
    v.setSpO2(spo2);
    v.setTemperature(temp);
    v.setRespRate(resp);
    v.setRecordedBy(recordedBy);
    v.setAbnormal(abnormal);
    v.setNotes(notes);
    patient.getVitals().add(v);
  }

  private static void addLab(
      Patient patient, String id, String testName, String category, String date, String value,
      String referenceRange, String status, String notes, String orderedBy) {
    LabResult lab = new LabResult();
    lab.setId(id);
    lab.setPatient(patient);
    lab.setTestName(testName);
    lab.setCategory(category);
    lab.setDate(date);
    lab.setValue(value);
    lab.setReferenceRange(referenceRange);
    lab.setStatus(status);
    lab.setNotes(notes);
    lab.setOrderedBy(orderedBy);
    patient.getLabResults().add(lab);
  }

  private static void addNote(
      Patient patient, String id, String date, String author, String authorRole, String title,
      String subjective, String objective, String assessment, String plan) {
    ClinicalNote note = new ClinicalNote();
    note.setId(id);
    note.setPatient(patient);
    note.setDate(date);
    note.setAuthor(author);
    note.setAuthorRole(authorRole);
    note.setTitle(title);
    note.setSoapSubjective(subjective);
    note.setSoapObjective(objective);
    note.setSoapAssessment(assessment);
    note.setSoapPlan(plan);
    patient.getClinicalNotes().add(note);
  }

  private static Appointment apt(
      String id, String patientId, String patientName, String mrn, String doctorId, String doctorName,
      String department, String date, String time, int duration, String type, AppointmentStatus status,
      String priority, String reason, String room, String notes) {
    Appointment a = new Appointment();
    a.setId(id);
    a.setPatientId(patientId);
    a.setPatientName(patientName);
    a.setPatientMrn(mrn);
    a.setDoctorId(doctorId);
    a.setDoctorName(doctorName);
    a.setDepartment(department);
    a.setDate(date);
    a.setTime(time);
    a.setDurationMinutes(duration);
    a.setType(type);
    a.setStatus(status);
    a.setPriority(priority);
    a.setReason(reason);
    a.setRoom(room);
    a.setNotes(notes);
    return a;
  }

  private static Invoice invoice(
      String id, String number, String patientId, String patientName, String mrn, String appointmentId,
      String date, String dueDate, String itemsJson, double subtotal, double adj, double covered,
      double responsibility, double paid, InvoiceStatus status, String claimJson, String receiptId) {
    Invoice inv = new Invoice();
    inv.setId(id);
    inv.setInvoiceNumber(number);
    inv.setPatientId(patientId);
    inv.setPatientName(patientName);
    inv.setPatientMrn(mrn);
    inv.setAppointmentId(appointmentId);
    inv.setDate(date);
    inv.setDueDate(dueDate);
    inv.setItemsJson(itemsJson);
    inv.setSubtotal(subtotal);
    inv.setInsuranceAdjustment(adj);
    inv.setInsuranceCovered(covered);
    inv.setPatientResponsibility(responsibility);
    inv.setAmountPaid(paid);
    inv.setStatus(status);
    inv.setClaimJson(claimJson);
    inv.setReceiptId(receiptId);
    return inv;
  }

  private static Message msg(
      String id, String senderId, String senderName, String senderRole, String recipientId,
      String recipientName, String subject, String body, boolean read, boolean urgent, String timestamp) {
    Message m = new Message();
    m.setId(id);
    m.setSenderId(senderId);
    m.setSenderName(senderName);
    m.setSenderRole(senderRole);
    m.setRecipientId(recipientId);
    m.setRecipientName(recipientName);
    m.setSubject(subject);
    m.setBody(body);
    m.setReadFlag(read);
    m.setUrgent(urgent);
    m.setTimestamp(Instant.parse(timestamp));
    return m;
  }

  private static RefillRequest refill(
      String id, String patientId, String medName, String notes, RefillStatus status,
      String prescriptionId, String createdAt) {
    RefillRequest r = new RefillRequest();
    r.setId(id);
    r.setPatientId(patientId);
    r.setMedicationName(medName);
    r.setNotes(notes);
    r.setStatus(status);
    r.setPrescriptionId(prescriptionId);
    r.setCreatedAt(Instant.parse(createdAt));
    return r;
  }

  private static void seedUser(
      UserAccountRepository users,
      String id,
      String name,
      String email,
      UserRole role,
      String department,
      String passwordHash,
      String patientId,
      String license) {
    UserAccount u = new UserAccount();
    u.setId(id);
    u.setName(name);
    u.setEmail(email);
    u.setRole(role);
    u.setDepartment(department);
    u.setPasswordHash(passwordHash);
    u.setPatientId(patientId);
    u.setLicenseNumber(license);
    u.setMfaEnabled(true);
    users.save(u);
  }
}
