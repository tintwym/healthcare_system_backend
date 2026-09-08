package dev.tintwym.medicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medicore.push")
public class PushProperties {
  private String vapidPublicKey = "";
  private String vapidPrivateKey = "";
  private String vapidSubject = "mailto:care@medicore.local";

  public String getVapidPublicKey() { return vapidPublicKey; }
  public void setVapidPublicKey(String vapidPublicKey) { this.vapidPublicKey = vapidPublicKey; }
  public String getVapidPrivateKey() { return vapidPrivateKey; }
  public void setVapidPrivateKey(String vapidPrivateKey) { this.vapidPrivateKey = vapidPrivateKey; }
  public String getVapidSubject() { return vapidSubject; }
  public void setVapidSubject(String vapidSubject) { this.vapidSubject = vapidSubject; }
}
