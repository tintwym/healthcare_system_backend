package dev.tintwym.medicore.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {
  @Id
  private String id;

  private String senderId;
  private String senderName;
  private String senderRole;
  private String recipientId;
  private String recipientName;
  private String subject;

  @Column(columnDefinition = "TEXT")
  private String body;

  private boolean readFlag;
  private boolean isUrgent;
  private Instant timestamp = Instant.now();

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getSenderId() { return senderId; }
  public void setSenderId(String senderId) { this.senderId = senderId; }
  public String getSenderName() { return senderName; }
  public void setSenderName(String senderName) { this.senderName = senderName; }
  public String getSenderRole() { return senderRole; }
  public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
  public String getRecipientId() { return recipientId; }
  public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
  public String getRecipientName() { return recipientName; }
  public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getBody() { return body; }
  public void setBody(String body) { this.body = body; }
  public boolean isReadFlag() { return readFlag; }
  public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
  public boolean isUrgent() { return isUrgent; }
  public void setUrgent(boolean urgent) { isUrgent = urgent; }
  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
