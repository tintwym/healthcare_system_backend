package dev.tintwym.medicore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medicore")
public class MedicoreProperties {
  private String corsOrigins = "http://127.0.0.1:3000,http://localhost:3000";
  private String appUrl = "http://127.0.0.1:3000";

  public String getCorsOrigins() {
    return corsOrigins;
  }

  public void setCorsOrigins(String corsOrigins) {
    this.corsOrigins = corsOrigins;
  }

  public String getAppUrl() {
    return appUrl;
  }

  public void setAppUrl(String appUrl) {
    this.appUrl = appUrl;
  }
}
