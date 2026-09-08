package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
  List<Invoice> findByPatientIdOrderByDateDesc(String patientId);
}
