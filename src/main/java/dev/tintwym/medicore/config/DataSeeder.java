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
      PasswordEncoder encoder) {
    return args -> {
      if (users.count() > 0) return;

      String patientPassword = encoder.encode("patient123");
      String staffPassword = encoder.encode("staff123");

      // --- Staff ---
      seedUser(users, "u-1", "Dr. Aye Myat Thu, MD", "aye.myatthu@medicore.mm", UserRole.doctor, "Cardiology", staffPassword, null, "MMC-C-92841");
      seedUser(users, "u-2", "Dr. Kyaw Zin Oo, MD", "kyaw.zinoo@medicore.mm", UserRole.doctor, "Internal Medicine", staffPassword, null, "MMC-IM-84729");
      seedUser(users, "u-3", "Daw Hnin Wai, RN", "hnin.wai@medicore.mm", UserRole.nurse, "Cardiology Ward", staffPassword, null, null);
      seedUser(users, "u-4", "U Min Thu, MHA", "min.thu@medicore.mm", UserRole.admin, "Hospital Administration", staffPassword, null, null);
      seedUser(users, "u-5", "Ko Aung Ko, CPB", "aung.ko@medicore.mm", UserRole.billing, "Revenue Cycle Management", staffPassword, null, null);
      seedUser(users, "u-7", "Daw Khin Sandar, PharmD", "khin.sandar@medicore.mm", UserRole.pharmacist, "Pharmacy", staffPassword, null, "MMP-RPH-44102");
      seedUser(users, "u-8", "Dr. Su Su Hlaing, MD", "susu.hlaing@medicore.mm", UserRole.doctor, "Endocrinology", staffPassword, null, "MMC-EN-55102");
      seedUser(users, "u-9", "Dr. Ye Min Htet, MD", "yemin.htet@medicore.mm", UserRole.doctor, "Orthopedics", staffPassword, null, "MMC-OR-66218");
      seedUser(users, "u-10", "Ko Naing Lin, RN", "naing.lin@medicore.mm", UserRole.nurse, "Emergency Department", staffPassword, null, null);
      seedUser(users, "u-11", "Ma Ei Phyo, CPhT", "ei.phyo@medicore.mm", UserRole.pharmacist, "Pharmacy", staffPassword, null, "MMP-CPHT-19044");

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

      Patient marcus = buildPatient(
          "pat-002", "MRN-882301", "Kyaw Min", "Thu", "1978-04-12", 48, "Male", "A+",
          "+95 9 420 100 301", "kyaw.minthu@email.com",
          "No. 18, 18th Street, Latha Township, Yangon",
          "Su Mon Aye", "Spouse", "+95 9 420 100 302",
          List.of("Latex"),
          List.of("Type 2 Diabetes", "Hyperlipidemia"),
          "Dr. Su Su Hlaing, MD", "Endocrinology",
          null, null, "Outpatient", null,
          "CB Life Health Plus", "CBL-441902", "YGN-ENDO", "FHIR-PAT-002-MM", 40);
      addMed(marcus, "med-4", "Metformin", "1000 mg", "Twice daily with meals", "Oral", "2023-08-10", "Dr. Su Su Hlaing", "active");
      addMed(marcus, "med-5", "Empagliflozin", "10 mg", "Once daily", "Oral", "2025-02-18", "Dr. Su Su Hlaing", "active");
      addMed(marcus, "med-6", "Rosuvastatin", "10 mg", "Once daily", "Oral", "2024-11-01", "Dr. Su Su Hlaing", "active");
      addVital(marcus, "v-201", "2026-09-01T09:00:00Z", 70, 118, 76, 99, 98.3, 14, "Ko Naing Lin, RN", false, null);
      addVital(marcus, "v-202", "2026-09-05T09:30:00Z", 74, 122, 78, 98, 98.6, 15, "Daw Hnin Wai, RN", false, "Fasting visit");
      patients.save(marcus);
      seedUser(users, "u-12", "Kyaw Min Thu (Patient)", "kyaw.minthu@email.com", UserRole.patient, "Outpatient Care", patientPassword, "pat-002", null);

      Patient aisha = buildPatient(
          "pat-003", "MRN-903512", "May Zin", "Htet", "1992-07-30", 34, "Female", "B+",
          "+95 9 420 100 401", "may.zinhtet@email.com",
          "No. 27, Strand Road, Pazundaung Township, Yangon",
          "Daw Aye Aye Myint", "Mother", "+95 9 420 100 402",
          List.of("NSAIDs", "Shellfish"),
          List.of("Migraine without aura"),
          "Dr. Kyaw Zin Oo, MD", "Internal Medicine",
          "Room 212", "Bed B", "Inpatient", "2026-09-05T09:10:00Z",
          "Prudential Myanmar Health", "PRU-229184", "YGN-IM", "FHIR-PAT-003-MM", 25);
      addMed(aisha, "med-7", "Sumatriptan", "50 mg", "As needed for migraine", "Oral", "2024-01-20", "Dr. Kyaw Zin Oo", "active");
      addMed(aisha, "med-8", "Propranolol", "40 mg", "Twice daily", "Oral", "2025-06-12", "Dr. Kyaw Zin Oo", "active");
      addVital(aisha, "v-301", "2026-09-05T10:00:00Z", 92, 110, 70, 99, 99.4, 18, "Ko Naing Lin, RN", true, "Post-admit headache");
      addVital(aisha, "v-302", "2026-09-06T06:00:00Z", 78, 108, 68, 99, 98.7, 15, "Daw Hnin Wai, RN", false, null);
      addVital(aisha, "v-303", "2026-09-07T06:00:00Z", 76, 112, 72, 98, 98.5, 14, "Daw Hnin Wai, RN", false, "Ready for discharge planning");
      patients.save(aisha);
      seedUser(users, "u-13", "May Zin Htet (Patient)", "may.zinhtet@email.com", UserRole.patient, "Inpatient Care", patientPassword, "pat-003", null);

      Patient diego = buildPatient(
          "pat-004", "MRN-714880", "Aung Myo", "Win", "1965-12-02", 60, "Male", "O-",
          "+95 9 420 100 501", "aung.myowin@email.com",
          "No. 55, 84th Street, Chan Aye Thar Zan Township, Mandalay",
          "Ma Hla Hla Win", "Daughter", "+95 9 420 100 502",
          List.of("Codeine"),
          List.of("Osteoarthritis of right knee", "CKD Stage 2"),
          "Dr. Ye Min Htet, MD", "Orthopedics",
          null, null, "Outpatient", null,
          "Self-Pay Package / Grand Guardian", "GG-883201", "MDY-ORTHO", "FHIR-PAT-004-MM", 50);
      addMed(diego, "med-9", "Acetaminophen", "650 mg", "Every 6 hours as needed", "Oral", "2025-09-01", "Dr. Ye Min Htet", "active");
      addMed(diego, "med-10", "Meloxicam", "7.5 mg", "Once daily", "Oral", "2026-01-15", "Dr. Ye Min Htet", "active");
      addMed(diego, "med-11", "Lisinopril", "5 mg", "Once daily", "Oral", "2022-04-01", "Dr. Aye Myat Thu", "active");
      addVital(diego, "v-401", "2026-08-28T14:00:00Z", 68, 130, 78, 97, 98.1, 14, "Ko Naing Lin, RN", false, "Pre-op screening");
      addVital(diego, "v-402", "2026-09-04T11:20:00Z", 72, 128, 80, 98, 98.4, 15, "Daw Hnin Wai, RN", false, null);
      patients.save(diego);
      seedUser(users, "u-14", "Aung Myo Win (Patient)", "aung.myowin@email.com", UserRole.patient, "Outpatient Care", patientPassword, "pat-004", null);

      Patient helen = buildPatient(
          "pat-005", "MRN-550219", "Khin Mar", "Lar", "1958-03-19", 68, "Female", "AB+",
          "+95 9 420 100 601", "khin.marlar@email.com",
          "No. 120, Pyay Road, Mayangone Township, Yangon",
          "Ko Zin Min", "Son", "+95 9 420 100 602",
          List.of("Iodine contrast"),
          List.of("Atrial fibrillation", "Heart failure with preserved EF"),
          "Dr. Aye Myat Thu, MD", "Cardiology",
          "Room 318", "Bed A", "Inpatient", "2026-09-03T18:40:00Z",
          "SSB + Top-up Coverage", "SSB-102948", "SSB-TOPUP", "FHIR-PAT-005-MM", 20);
      addMed(helen, "med-12", "Apixaban", "5 mg", "Twice daily", "Oral", "2024-05-01", "Dr. Aye Myat Thu", "active");
      addMed(helen, "med-13", "Furosemide", "20 mg", "Once daily", "Oral", "2025-11-12", "Dr. Aye Myat Thu", "active");
      addMed(helen, "med-14", "Metoprolol Succinate", "50 mg", "Once daily", "Oral", "2023-09-08", "Dr. Aye Myat Thu", "active");
      addVital(helen, "v-501", "2026-09-03T19:00:00Z", 110, 142, 88, 94, 98.9, 22, "Ko Naing Lin, RN", true, "ED arrival — irregular rhythm");
      addVital(helen, "v-502", "2026-09-04T06:00:00Z", 88, 128, 80, 96, 98.4, 18, "Daw Hnin Wai, RN", false, "Rate controlled");
      addVital(helen, "v-503", "2026-09-06T06:00:00Z", 82, 124, 76, 97, 98.2, 16, "Daw Hnin Wai, RN", false, null);
      patients.save(helen);
      seedUser(users, "u-15", "Daw Khin Mar Lar (Patient)", "khin.marlar@email.com", UserRole.patient, "Inpatient Care", patientPassword, "pat-005", null);

      // --- Appointments ---
      appointments.save(apt("apt-001", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-09-15", "10:30 AM", 30, "Follow-up", AppointmentStatus.scheduled, "routine",
          "Routine cardiovascular checkup and medication review.", "Clinic A", null));
      appointments.save(apt("apt-002", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-2", "Dr. Kyaw Zin Oo, MD",
          "Internal Medicine", "2026-09-22", "2:00 PM", 45, "General Checkup", AppointmentStatus.scheduled, "routine",
          "Asthma follow-up and inhaler technique review.", "Clinic B", null));
      appointments.save(apt("apt-003", "pat-001", "Thiri Su Pyae", "MRN-782104", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-08-20", "9:00 AM", 30, "Follow-up", AppointmentStatus.completed, "routine",
          "BP trend review after Lisinopril titration.", "Clinic A", "BP improved; continue current dose."));
      appointments.save(apt("apt-004", "pat-002", "Kyaw Min Thu", "MRN-882301", "u-8", "Dr. Su Su Hlaing, MD",
          "Endocrinology", "2026-09-12", "11:00 AM", 40, "Diabetes follow-up", AppointmentStatus.scheduled, "routine",
          "A1C review and SGLT2 tolerance check.", "Endo Suite 2", null));
      appointments.save(apt("apt-005", "pat-002", "Kyaw Min Thu", "MRN-882301", "u-8", "Dr. Su Su Hlaing, MD",
          "Endocrinology", "2026-08-01", "10:00 AM", 40, "New patient", AppointmentStatus.completed, "routine",
          "Initial diabetes consult.", "Endo Suite 2", "Started empagliflozin."));
      appointments.save(apt("apt-006", "pat-003", "May Zin Htet", "MRN-903512", "u-2", "Dr. Kyaw Zin Oo, MD",
          "Internal Medicine", "2026-09-08", "8:30 AM", 30, "Inpatient round", AppointmentStatus.in_progress, "urgent",
          "Migraine admission — neurology consult pending.", "Room 212", "MRI scheduled AM."));
      appointments.save(apt("apt-007", "pat-004", "Aung Myo Win", "MRN-714880", "u-9", "Dr. Ye Min Htet, MD",
          "Orthopedics", "2026-09-18", "1:15 PM", 60, "Pre-op", AppointmentStatus.scheduled, "high",
          "Right knee arthroplasty pre-op clearance.", "Ortho Clinic 1", null));
      appointments.save(apt("apt-008", "pat-004", "Aung Myo Win", "MRN-714880", "u-9", "Dr. Ye Min Htet, MD",
          "Orthopedics", "2026-09-02", "3:00 PM", 30, "Follow-up", AppointmentStatus.cancelled, "routine",
          "Imaging review — patient rescheduled.", "Ortho Clinic 1", "Cancelled by patient."));
      appointments.save(apt("apt-009", "pat-005", "Daw Khin Mar Lar", "MRN-550219", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-09-07", "9:45 AM", 45, "Inpatient consult", AppointmentStatus.checked_in, "urgent",
          "AFib rate control and diuresis plan.", "Room 318", null));
      appointments.save(apt("apt-010", "pat-005", "Daw Khin Mar Lar", "MRN-550219", "u-1", "Dr. Aye Myat Thu, MD",
          "Cardiology", "2026-07-14", "2:30 PM", 30, "Follow-up", AppointmentStatus.completed, "routine",
          "Outpatient AFib check prior to admission.", "Clinic A", "INR not required on apixaban."));

      // --- Invoices ---
      invoices.save(invoice("inv-001", "INV-2026-0912", "pat-001", "Thiri Su Pyae", "MRN-782104", "apt-001",
          "2026-09-02", "2026-09-30",
          "[{\"id\":\"ii-1\",\"code\":\"99213\",\"description\":\"Office visit\",\"category\":\"Consultation\",\"quantity\":1,\"unitPrice\":185,\"total\":185},{\"id\":\"ii-2\",\"code\":\"93000\",\"description\":\"ECG\",\"category\":\"Diagnostics\",\"quantity\":1,\"unitPrice\":95,\"total\":95}]",
          280, 40, 180, 60, 0, InvoiceStatus.pending,
          "{\"claimId\":\"CLM-99421\",\"payerName\":\"AIA Myanmar\",\"submittedDate\":\"2026-09-02\",\"status\":\"In Review\"}", null));
      invoices.save(invoice("inv-002", "INV-2026-0820", "pat-001", "Thiri Su Pyae", "MRN-782104", "apt-003",
          "2026-08-20", "2026-09-10",
          "[{\"id\":\"ii-3\",\"code\":\"80053\",\"description\":\"CMP\",\"category\":\"Laboratory\",\"quantity\":1,\"unitPrice\":120,\"total\":120}]",
          120, 20, 70, 30, 30, InvoiceStatus.paid, null, "RCPT-SEED001"));
      invoices.save(invoice("inv-003", "INV-2026-0905", "pat-002", "Kyaw Min Thu", "MRN-882301", "apt-005",
          "2026-08-01", "2026-08-31",
          "[{\"id\":\"ii-4\",\"code\":\"99204\",\"description\":\"New patient endocrinology\",\"category\":\"Consultation\",\"quantity\":1,\"unitPrice\":320,\"total\":320},{\"id\":\"ii-5\",\"code\":\"83036\",\"description\":\"HbA1c\",\"category\":\"Laboratory\",\"quantity\":1,\"unitPrice\":65,\"total\":65}]",
          385, 45, 280, 60, 0, InvoiceStatus.insurance_processing,
          "{\"claimId\":\"CLM-10082\",\"payerName\":\"CB Life\",\"submittedDate\":\"2026-08-02\",\"status\":\"Pending\"}", null));
      invoices.save(invoice("inv-004", "INV-2026-0908", "pat-003", "May Zin Htet", "MRN-903512", "apt-006",
          "2026-09-05", "2026-09-20",
          "[{\"id\":\"ii-6\",\"code\":\"99223\",\"description\":\"Initial hospital care\",\"category\":\"Inpatient\",\"quantity\":1,\"unitPrice\":450,\"total\":450},{\"id\":\"ii-7\",\"code\":\"70553\",\"description\":\"Brain MRI w/wo contrast\",\"category\":\"Imaging\",\"quantity\":1,\"unitPrice\":1200,\"total\":1200}]",
          1650, 200, 1200, 250, 0, InvoiceStatus.pending,
          "{\"claimId\":\"CLM-11044\",\"payerName\":\"Prudential Myanmar\",\"submittedDate\":\"2026-09-06\",\"status\":\"Submitted\"}", null));
      invoices.save(invoice("inv-005", "INV-2026-0815", "pat-004", "Aung Myo Win", "MRN-714880", null,
          "2026-08-15", "2026-09-01",
          "[{\"id\":\"ii-8\",\"code\":\"73560\",\"description\":\"Knee X-ray 3 views\",\"category\":\"Imaging\",\"quantity\":1,\"unitPrice\":180,\"total\":180}]",
          180, 30, 100, 50, 0, InvoiceStatus.overdue, null, null));
      invoices.save(invoice("inv-006", "INV-2026-0903", "pat-005", "Daw Khin Mar Lar", "MRN-550219", "apt-009",
          "2026-09-03", "2026-09-25",
          "[{\"id\":\"ii-9\",\"code\":\"99285\",\"description\":\"ED visit high complexity\",\"category\":\"Emergency\",\"quantity\":1,\"unitPrice\":980,\"total\":980},{\"id\":\"ii-10\",\"code\":\"93010\",\"description\":\"ECG interpretation\",\"category\":\"Diagnostics\",\"quantity\":1,\"unitPrice\":55,\"total\":55}]",
          1035, 150, 750, 135, 0, InvoiceStatus.insurance_processing,
          "{\"claimId\":\"CLM-12001\",\"payerName\":\"SSB + Top-up\",\"submittedDate\":\"2026-09-04\",\"status\":\"In Review\"}", null));
      invoices.save(invoice("inv-007", "INV-2026-0702", "pat-005", "Daw Khin Mar Lar", "MRN-550219", "apt-010",
          "2026-07-14", "2026-08-01",
          "[{\"id\":\"ii-11\",\"code\":\"99214\",\"description\":\"Established patient visit\",\"category\":\"Consultation\",\"quantity\":1,\"unitPrice\":210,\"total\":210}]",
          210, 30, 160, 20, 20, InvoiceStatus.paid, null, "RCPT-SEED007"));

      // --- Messages ---
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
      messages.save(msg("msg-004", "u-8", "Dr. Su Su Hlaing", "doctor", "u-12", "Kyaw Min Thu",
          "Lab results ready",
          "Kyaw Min Thu, your A1C improved to 7.1%. Continue Metformin and Empagliflozin. Bring your glucose log to the Sept 12 visit.",
          false, false, "2026-09-05T13:10:00Z"));
      messages.save(msg("msg-005", "u-12", "Kyaw Min Thu", "patient", "u-8", "Dr. Su Su Hlaing",
          "Pharmacy delay",
          "The pharmacy said Empagliflozin is on backorder. Can we switch temporarily?",
          false, true, "2026-09-06T09:22:00Z"));
      messages.save(msg("msg-006", "u-2", "Dr. Kyaw Zin Oo", "doctor", "u-13", "May Zin Htet",
          "Admission update",
          "May Zin Htet, MRI is scheduled for this morning. We will keep migraine protocol in place and reassess after imaging.",
          false, true, "2026-09-06T07:05:00Z"));
      messages.save(msg("msg-007", "u-9", "Dr. Ye Min Htet", "doctor", "u-14", "Aung Myo Win",
          "Pre-op instructions",
          "Please stop Meloxicam 7 days before surgery and arrange a ride home after the Sept 18 pre-op visit.",
          false, false, "2026-09-04T18:00:00Z"));
      messages.save(msg("msg-008", "u-3", "Daw Hnin Wai", "nurse", "u-15", "Daw Khin Mar Lar",
          "Daily weight reminder",
          "Daw Khin Mar Lar — please record morning weight before breakfast. Call the floor if you gain more than 2 lbs in a day.",
          true, false, "2026-09-05T12:00:00Z"));
      messages.save(msg("msg-009", "u-7", "Daw Khin Sandar", "pharmacist", "u-6", "Thiri Su Pyae",
          "Refill approved",
          "Your Atorvastatin refill was approved and will be ready for pickup tomorrow after 2 PM.",
          false, false, "2026-09-06T14:30:00Z"));
      messages.save(msg("msg-010", "u-5", "Ko Aung Ko", "billing", "u-14", "Aung Myo Win",
          "Overdue balance notice",
          "Invoice INV-2026-0815 has an unpaid patient responsibility of MMK 50,000. You can pay in the portal anytime.",
          false, false, "2026-09-05T10:00:00Z"));

      // --- Refills ---
      refills.save(refill("ref-001", "pat-001", "Lisinopril", "Need 90-day supply before travel", RefillStatus.pending, "med-1", "2026-09-06T08:00:00Z"));
      refills.save(refill("ref-002", "pat-001", "Atorvastatin", "Running low — 5 tablets left", RefillStatus.approved, "med-3", "2026-09-05T16:20:00Z"));
      refills.save(refill("ref-003", "pat-002", "Metformin", "Standard 90-day refill", RefillStatus.pending, "med-4", "2026-09-07T09:10:00Z"));
      refills.save(refill("ref-004", "pat-002", "Empagliflozin", "Backorder — patient requesting alternate", RefillStatus.pending, "med-5", "2026-09-06T09:30:00Z"));
      refills.save(refill("ref-005", "pat-004", "Meloxicam", "Hold pending pre-op instructions", RefillStatus.rejected, "med-10", "2026-09-04T12:00:00Z"));
      refills.save(refill("ref-006", "pat-005", "Furosemide", "Inpatient bridge to outpatient fill", RefillStatus.dispensed, "med-13", "2026-09-04T08:00:00Z"));
      refills.save(refill("ref-007", "pat-003", "Sumatriptan", "Discharge supply", RefillStatus.pending, "med-7", "2026-09-07T07:45:00Z"));

      System.out.println(
          "Seeded Medicore demo data: 5 patients, 10 staff, appointments/invoices/messages/refills. "
              + "Patients: */patient123 · Staff: */staff123 (e.g. thiri.supyae@gmail.com, khin.sandar@medicore.mm)");
    };
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
