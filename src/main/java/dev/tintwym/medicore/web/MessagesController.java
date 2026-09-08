package dev.tintwym.medicore.web;

import dev.tintwym.medicore.domain.AuditEvent;
import dev.tintwym.medicore.domain.Message;
import dev.tintwym.medicore.domain.UserAccount;
import dev.tintwym.medicore.repo.AuditEventRepository;
import dev.tintwym.medicore.repo.MessageRepository;
import dev.tintwym.medicore.repo.UserAccountRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import dev.tintwym.medicore.service.PushNotificationService;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class MessagesController {
  private final MessageRepository messages;
  private final UserAccountRepository users;
  private final AuditEventRepository audits;
  private final PushNotificationService push;

  public MessagesController(
      MessageRepository messages,
      UserAccountRepository users,
      AuditEventRepository audits,
      PushNotificationService push) {
    this.messages = messages;
    this.users = users;
    this.audits = audits;
    this.push = push;
  }

  @GetMapping
  public List<Map<String, Object>> list() {
    AuthUser user = AuthSupport.requireUser();
    return messages.findBySenderIdOrRecipientIdOrderByTimestampDesc(user.getId(), user.getId())
        .stream().map(this::map).toList();
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable String id) {
    AuthUser user = AuthSupport.requireUser();
    Message msg = messages.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    if (!msg.getSenderId().equals(user.getId()) && !msg.getRecipientId().equals(user.getId())) {
      throw new ApiException(403, "Forbidden");
    }
    return map(msg);
  }

  @PostMapping
  public Map<String, Object> send(@RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    String recipientId = String.valueOf(body.getOrDefault("recipientId", ""));
    String subject = String.valueOf(body.getOrDefault("subject", "Care message"));
    String bodyText = String.valueOf(body.getOrDefault("body", ""));
    if (recipientId.isBlank() || bodyText.isBlank()) throw new ApiException(400, "recipientId and body required");
    UserAccount recipient = users.findById(recipientId).orElseThrow(() -> new ApiException(404, "Recipient not found"));
    Message msg = new Message();
    msg.setId("msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    msg.setSenderId(user.getId());
    msg.setSenderName(user.getName());
    msg.setSenderRole(user.getRole().name());
    msg.setRecipientId(recipientId);
    msg.setRecipientName(recipient.getName());
    msg.setSubject(subject);
    msg.setBody(bodyText);
    msg.setUrgent(Boolean.TRUE.equals(body.get("isUrgent")));
    msg.setReadFlag(false);
    msg.setTimestamp(Instant.now());
    messages.save(msg);
    AuditEvent e = new AuditEvent();
    e.setUserId(user.getId());
    e.setUserName(user.getName());
    e.setUserRole(user.getRole().name());
    e.setAction("SEND_MESSAGE");
    e.setResource("message/" + msg.getId());
    e.setPatientId(user.getPatientId());
    audits.save(e);
    push.notifyUser(
        recipientId,
        msg.isUrgent() ? "Urgent care message" : "New care message",
        preview(user.getName(), subject, bodyText),
        Map.of(
            "type", "message",
            "screen", "Chat",
            "messageId", msg.getId()));
    return map(msg);
  }

  @PostMapping("/{id}/read")
  public Map<String, Object> read(@PathVariable String id) {
    AuthUser user = AuthSupport.requireUser();
    Message msg = messages.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    if (!msg.getRecipientId().equals(user.getId())) throw new ApiException(403, "Forbidden");
    msg.setReadFlag(true);
    messages.save(msg);
    return map(msg);
  }

  @PostMapping("/{id}/reply")
  public Map<String, Object> reply(@PathVariable String id, @RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    Message original = messages.findById(id).orElseThrow(() -> new ApiException(404, "Not found"));
    if (!original.getSenderId().equals(user.getId()) && !original.getRecipientId().equals(user.getId())) {
      throw new ApiException(403, "Forbidden");
    }
    String text = String.valueOf(body.getOrDefault("body", ""));
    if (text.isBlank()) throw new ApiException(400, "body required");
    String recipientId = original.getSenderId().equals(user.getId()) ? original.getRecipientId() : original.getSenderId();
    UserAccount recipient = users.findById(recipientId).orElseThrow(() -> new ApiException(404, "Recipient not found"));
    Message msg = new Message();
    msg.setId("msg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    msg.setSenderId(user.getId());
    msg.setSenderName(user.getName());
    msg.setSenderRole(user.getRole().name());
    msg.setRecipientId(recipientId);
    msg.setRecipientName(recipient.getName());
    String subj = original.getSubject().startsWith("Re:") ? original.getSubject() : "Re: " + original.getSubject();
    msg.setSubject(subj);
    msg.setBody(text);
    msg.setTimestamp(Instant.now());
    messages.save(msg);
    push.notifyUser(
        recipientId,
        "Care message reply",
        preview(user.getName(), subj, text),
        Map.of(
            "type", "message",
            "screen", "Chat",
            "messageId", msg.getId()));
    return map(msg);
  }

  private static String preview(String from, String subject, String body) {
    String snippet = body.length() > 120 ? body.substring(0, 117) + "…" : body;
    return from + " · " + subject + " — " + snippet;
  }

  private Map<String, Object> map(Message m) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", m.getId());
    out.put("senderId", m.getSenderId());
    out.put("senderName", m.getSenderName());
    out.put("senderRole", m.getSenderRole());
    out.put("recipientId", m.getRecipientId());
    out.put("recipientName", m.getRecipientName());
    out.put("subject", m.getSubject());
    out.put("body", m.getBody());
    out.put("read", m.isReadFlag());
    out.put("isUrgent", m.isUrgent());
    out.put("timestamp", m.getTimestamp().toString());
    return out;
  }
}
