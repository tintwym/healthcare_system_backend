package dev.tintwym.medicore.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tintwym.medicore.config.PushProperties;
import dev.tintwym.medicore.domain.DevicePlatform;
import dev.tintwym.medicore.domain.DeviceRegistration;
import dev.tintwym.medicore.repo.DeviceRegistrationRepository;
import dev.tintwym.medicore.security.ApiException;
import dev.tintwym.medicore.security.AuthSupport;
import dev.tintwym.medicore.security.AuthUser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class DevicesController {
  private final DeviceRegistrationRepository devices;
  private final PushProperties pushProperties;
  private final ObjectMapper mapper;

  public DevicesController(
      DeviceRegistrationRepository devices, PushProperties pushProperties, ObjectMapper mapper) {
    this.devices = devices;
    this.pushProperties = pushProperties;
    this.mapper = mapper;
  }

  @GetMapping("/push/vapid-public-key")
  public Map<String, String> vapidPublicKey() {
    String key = pushProperties.getVapidPublicKey();
    if (key == null || key.isBlank()) throw new ApiException(503, "Web Push not configured");
    return Map.of("publicKey", key);
  }

  @PostMapping("/devices/register")
  public Map<String, Object> register(@RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    String platformRaw = String.valueOf(body.getOrDefault("platform", "")).trim().toUpperCase();
    Object tokenObj = body.get("token");
    if (platformRaw.isBlank() || tokenObj == null) {
      throw new ApiException(400, "platform and token required");
    }
    DevicePlatform platform;
    try {
      platform = DevicePlatform.valueOf(platformRaw);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "platform must be EXPO or WEB");
    }
    String token = serializeToken(tokenObj);
    if (token.isBlank()) throw new ApiException(400, "token required");

    DeviceRegistration row =
        devices
            .findByUserIdAndToken(user.getId(), token)
            .orElseGet(
                () -> {
                  DeviceRegistration d = new DeviceRegistration();
                  d.setId("dev-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                  d.setUserId(user.getId());
                  d.setToken(token);
                  d.setCreatedAt(Instant.now());
                  return d;
                });
    row.setPlatform(platform);
    row.setUpdatedAt(Instant.now());
    devices.save(row);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", row.getId());
    out.put("platform", row.getPlatform().name());
    out.put("ok", true);
    return out;
  }

  @DeleteMapping("/devices")
  public Map<String, Object> unregister(@RequestBody Map<String, Object> body) {
    AuthUser user = AuthSupport.requireUser();
    Object tokenObj = body.get("token");
    if (tokenObj == null) throw new ApiException(400, "token required");
    devices.deleteByUserIdAndToken(user.getId(), serializeToken(tokenObj));
    return Map.of("ok", true);
  }

  private String serializeToken(Object tokenObj) {
    if (tokenObj instanceof String s) return s.trim();
    try {
      return mapper.writeValueAsString(tokenObj);
    } catch (Exception e) {
      throw new ApiException(400, "invalid token");
    }
  }
}
