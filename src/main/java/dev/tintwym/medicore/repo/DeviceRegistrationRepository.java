package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.DeviceRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, String> {
  List<DeviceRegistration> findByUserId(String userId);

  Optional<DeviceRegistration> findByUserIdAndToken(String userId, String token);

  void deleteByUserIdAndToken(String userId, String token);

  void deleteByToken(String token);
}
