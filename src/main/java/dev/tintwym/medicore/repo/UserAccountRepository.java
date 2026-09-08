package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);
  Optional<UserAccount> findFirstByRole(dev.tintwym.medicore.domain.UserRole role);
  Optional<UserAccount> findFirstByPatientId(String patientId);
}
