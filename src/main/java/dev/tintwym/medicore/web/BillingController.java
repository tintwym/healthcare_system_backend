package dev.tintwym.medicore.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.domain.Invoice;
import dev.tintwym.medicore.domain.InvoiceStatus;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.InvoiceRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
public class BillingController {
  private final InvoiceRepository invoices;
  private final AuditEventRepository audits;
  private final ObjectMapper mapper;

  public BillingController(InvoiceRepository invoices, AuditEventRepository audits, ObjectMapper mapper) {
    this.invoices = invoices;
    this.audits = audits;
    this.mapper = mapper;
  }

  @GetMapping("/invoices")
  public List<Map<String, Object>> list(@RequestParam(required = false) String patientId) {
    String scoped = AuthSupport.patientScopeOrNull();
    List<Invoice> rows = scoped != null
        ? invoices.findByPatientIdOrderByDateDesc(scoped)
        : (patientId != null ? invoices.findByPatientIdOrderByDateDesc(patientId) : invoices.findAll());
    return rows.stream().map(this::mapInvoice).toList();
  }

  @PostMapping("/invoices/{id}/pay")
  public Map<String, Object> pay(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    Invoice inv = invoices.findById(id).orElseThrow(() -> new ApiException(404, "Invoice not found"));
    String scoped = AuthSupport.patientScopeOrNull();
    if (scoped != null && !scoped.equals(inv.getPatientId())) throw new ApiException(403, "Forbidden");
    String receipt = "RCPT-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    inv.setAmountPaid(inv.getPatientResponsibility());
    inv.setStatus(InvoiceStatus.paid);
    inv.setReceiptId(receipt);
    invoices.save(inv);
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction("PAY_BILL");
    e.setResource("invoice/" + id);
    e.setPatientId(inv.getPatientId());
    e.setDetails(receipt);
    audits.save(e);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("invoice", mapInvoice(inv));
    out.put("receiptId", receipt);
    out.put("method", body != null ? body.getOrDefault("method", "card") : "card");
    return out;
  }

  @PostMapping("/invoices/{id}/checkout")
  public Map<String, Object> checkout(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
    // Demo mode: same as pay when Stripe is not configured
    Map<String, Object> paid = pay(id, body);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("mode", "demo");
    out.put("invoice", paid.get("invoice"));
    out.put("receiptId", paid.get("receiptId"));
    return out;
  }

  private Map<String, Object> mapInvoice(Invoice inv) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", inv.getId());
    m.put("invoiceNumber", inv.getInvoiceNumber());
    m.put("patientId", inv.getPatientId());
    m.put("patientName", inv.getPatientName());
    m.put("patientMrn", inv.getPatientMrn());
    m.put("appointmentId", inv.getAppointmentId());
    m.put("date", inv.getDate());
    m.put("dueDate", inv.getDueDate());
    try {
      m.put("items", mapper.readValue(inv.getItemsJson() == null ? "[]" : inv.getItemsJson(), new TypeReference<List<Object>>() {}));
    } catch (Exception e) {
      m.put("items", List.of());
    }
    m.put("subtotal", inv.getSubtotal());
    m.put("insuranceAdjustment", inv.getInsuranceAdjustment());
    m.put("insuranceCovered", inv.getInsuranceCovered());
    m.put("patientResponsibility", inv.getPatientResponsibility());
    m.put("amountPaid", inv.getAmountPaid());
    m.put("status", inv.getStatus().name());
    try {
      m.put("insuranceClaim", inv.getClaimJson() == null ? null : mapper.readValue(inv.getClaimJson(), Object.class));
    } catch (Exception e) {
      m.put("insuranceClaim", null);
    }
    m.put("receiptId", inv.getReceiptId());
    return m;
  }
}
