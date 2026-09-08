package dev.tintwym.medicore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tintwym.medicore.config.PushProperties;
import dev.tintwym.medicore.domain.DevicePlatform;
import dev.tintwym.medicore.domain.DeviceRegistration;
import dev.tintwym.medicore.repo.DeviceRegistrationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {
  private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
  private static final String EXPO_URL = "https://exp.host/--/api/v2/push/send";

  private final DeviceRegistrationRepository devices;
  private final ObjectMapper mapper;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private PushService webPush;

  public PushNotificationService(
      DeviceRegistrationRepository devices, PushProperties pushProperties, ObjectMapper mapper) {
    this.devices = devices;
    this.mapper = mapper;
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
    try {
      if (pushProperties.getVapidPublicKey() != null
          && !pushProperties.getVapidPublicKey().isBlank()
          && pushProperties.getVapidPrivateKey() != null
          && !pushProperties.getVapidPrivateKey().isBlank()) {
        webPush =
            new PushService(
                pushProperties.getVapidPublicKey(),
                pushProperties.getVapidPrivateKey(),
                pushProperties.getVapidSubject());
      }
    } catch (Exception e) {
      log.warn("Web Push VAPID init failed: {}", e.getMessage());
    }
  }

  public void notifyUser(String userId, String title, String body, Map<String, String> data) {
    if (userId == null || userId.isBlank()) return;
    List<DeviceRegistration> regs = devices.findByUserId(userId);
    if (regs.isEmpty()) return;
    List<DeviceRegistration> expo = new ArrayList<>();
    List<DeviceRegistration> web = new ArrayList<>();
    for (DeviceRegistration r : regs) {
      if (r.getPlatform() == DevicePlatform.EXPO) expo.add(r);
      else if (r.getPlatform() == DevicePlatform.WEB) web.add(r);
    }
    if (!expo.isEmpty()) sendExpo(expo, title, body, data);
    if (!web.isEmpty()) sendWeb(web, title, body, data);
  }

  private void sendExpo(
      List<DeviceRegistration> regs, String title, String body, Map<String, String> data) {
    try {
      List<Map<String, Object>> messages = new ArrayList<>();
      for (DeviceRegistration r : regs) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("to", r.getToken());
        msg.put("sound", "default");
        msg.put("title", title);
        msg.put("body", body);
        msg.put("data", data == null ? Map.of() : data);
        msg.put("priority", "high");
        messages.add(msg);
      }
      String json = mapper.writeValueAsString(messages.size() == 1 ? messages.get(0) : messages);
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(EXPO_URL))
              .timeout(Duration.ofSeconds(12))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .header("Accept-Encoding", "gzip, deflate")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();
      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 400) {
        log.warn("Expo push HTTP {}: {}", res.statusCode(), res.body());
        return;
      }
      JsonNode root = mapper.readTree(res.body());
      JsonNode dataNode = root.has("data") ? root.get("data") : root;
      if (dataNode.isArray()) {
        int i = 0;
        for (JsonNode ticket : dataNode) {
          if (i >= regs.size()) break;
          String status = ticket.path("status").asText("");
          String err = ticket.path("details").path("error").asText(ticket.path("message").asText(""));
          if ("error".equalsIgnoreCase(status)
              && (err.contains("DeviceNotRegistered") || err.contains("InvalidCredentials"))) {
            devices.delete(regs.get(i));
          }
          i++;
        }
      } else if (dataNode.isObject()) {
        String status = dataNode.path("status").asText("");
        String err =
            dataNode.path("details").path("error").asText(dataNode.path("message").asText(""));
        if ("error".equalsIgnoreCase(status)
            && (err.contains("DeviceNotRegistered") || err.contains("InvalidCredentials"))) {
          devices.delete(regs.get(0));
        }
      }
    } catch (Exception e) {
      log.warn("Expo push failed: {}", e.getMessage());
    }
  }

  private void sendWeb(
      List<DeviceRegistration> regs, String title, String body, Map<String, String> data) {
    if (webPush == null) {
      log.debug("Web Push not configured; skipping {} subscriptions", regs.size());
      return;
    }
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("title", title);
      payload.put("body", body);
      payload.put("data", data == null ? Map.of() : data);
      String payloadJson = mapper.writeValueAsString(payload);
      for (DeviceRegistration r : regs) {
        try {
          JsonNode sub = mapper.readTree(r.getToken());
          String endpoint = sub.path("endpoint").asText();
          JsonNode keys = sub.path("keys");
          Subscription subscription =
              new Subscription(
                  endpoint,
                  new Subscription.Keys(keys.path("p256dh").asText(), keys.path("auth").asText()));
          Notification notification = new Notification(subscription, payloadJson);
          var response = webPush.send(notification);
          int code = response.getStatusLine().getStatusCode();
          if (code == 404 || code == 410) {
            devices.delete(r);
          } else if (code >= 400) {
            log.warn("Web push status {} for {}", code, endpoint);
          }
        } catch (Exception ex) {
          log.warn("Web push to device {} failed: {}", r.getId(), ex.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Web push batch failed: {}", e.getMessage());
    }
  }
}
