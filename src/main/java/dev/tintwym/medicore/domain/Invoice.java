package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class Invoice {
  @Id
  private String id;

  @Column(nullable = false, unique = true)
  private String invoiceNumber;

  private String patientId;
  private String patientName;
  private String patientMrn;
  private String appointmentId;
  private String date;
  private String dueDate;

  @Column(columnDefinition = "TEXT")
  private String itemsJson;

  private double subtotal;
  private double insuranceAdjustment;
  private double insuranceCovered;
  private double patientResponsibility;
  private double amountPaid;

  @Enumerated(EnumType.STRING)
  private InvoiceStatus status = InvoiceStatus.pending;

  @Column(columnDefinition = "TEXT")
  private String claimJson;

  private String receiptId;
  private String stripeSessionId;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() { updatedAt = Instant.now(); }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getInvoiceNumber() { return invoiceNumber; }
  public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }
  public String getPatientName() { return patientName; }
  public void setPatientName(String patientName) { this.patientName = patientName; }
  public String getPatientMrn() { return patientMrn; }
  public void setPatientMrn(String patientMrn) { this.patientMrn = patientMrn; }
  public String getAppointmentId() { return appointmentId; }
  public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public String getDueDate() { return dueDate; }
  public void setDueDate(String dueDate) { this.dueDate = dueDate; }
  public String getItemsJson() { return itemsJson; }
  public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
  public double getSubtotal() { return subtotal; }
  public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
  public double getInsuranceAdjustment() { return insuranceAdjustment; }
  public void setInsuranceAdjustment(double insuranceAdjustment) { this.insuranceAdjustment = insuranceAdjustment; }
  public double getInsuranceCovered() { return insuranceCovered; }
  public void setInsuranceCovered(double insuranceCovered) { this.insuranceCovered = insuranceCovered; }
  public double getPatientResponsibility() { return patientResponsibility; }
  public void setPatientResponsibility(double patientResponsibility) { this.patientResponsibility = patientResponsibility; }
  public double getAmountPaid() { return amountPaid; }
  public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
  public InvoiceStatus getStatus() { return status; }
  public void setStatus(InvoiceStatus status) { this.status = status; }
  public String getClaimJson() { return claimJson; }
  public void setClaimJson(String claimJson) { this.claimJson = claimJson; }
  public String getReceiptId() { return receiptId; }
  public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
  public String getStripeSessionId() { return stripeSessionId; }
  public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
