package dev.tintwym.medicore.repo;

import dev.tintwym.medicore.domain.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {
  List<Message> findBySenderIdOrRecipientIdOrderByTimestampDesc(String senderId, String recipientId);
}
