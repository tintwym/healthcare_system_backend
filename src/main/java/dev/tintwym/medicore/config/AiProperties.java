package dev.tintwym.medicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medicore.ai")
public class AiProperties {
  /** Gemini API key (server-side only). When blank, rule-based fallback replies are used. */
  private String geminiApiKey = "";
  private String model = "gemini-2.0-flash";
  private boolean enabled = true;

  public String getGeminiApiKey() {
    return geminiApiKey;
  }

  public void setGeminiApiKey(String geminiApiKey) {
    this.geminiApiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isConfigured() {
    return enabled && geminiApiKey != null && !geminiApiKey.isBlank();
  }
}
