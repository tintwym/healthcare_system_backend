package dev.tintwym.medicore.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render / Heroku style {@code DATABASE_URL=postgres://user:pass@host/db} → JDBC properties.
 * Leaves existing {@code jdbc:postgresql://...} values unchanged.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String raw = firstNonBlank(
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("SPRING_DATASOURCE_URL"));
    if (raw == null) return;

    String trimmed = raw.trim();
    if (trimmed.startsWith("jdbc:")) return;
    if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) return;

    try {
      String normalized = trimmed.replaceFirst("^postgres(ql)?://", "http://");
      URI uri = URI.create(normalized);
      String user = null;
      String pass = null;
      if (uri.getUserInfo() != null) {
        String[] parts = uri.getUserInfo().split(":", 2);
        user = parts[0];
        pass = parts.length > 1 ? parts[1] : "";
      }
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String path = uri.getPath() == null || uri.getPath().isBlank() ? "/medicore" : uri.getPath();
      String query = uri.getQuery();

      StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
          .append(uri.getHost())
          .append(':')
          .append(port)
          .append(path);
      if (query != null && !query.isBlank()) {
        jdbcUrl.append('?').append(query);
        if (!query.contains("sslmode=")) jdbcUrl.append("&sslmode=require");
      } else {
        jdbcUrl.append("?sslmode=require");
      }

      Map<String, Object> map = new HashMap<>();
      map.put("DATABASE_URL", jdbcUrl.toString());
      map.put("spring.datasource.url", jdbcUrl.toString());
      if (user != null) {
        map.put("DATABASE_USERNAME", user);
        map.put("spring.datasource.username", user);
      }
      if (pass != null) {
        map.put("DATABASE_PASSWORD", pass);
        map.put("spring.datasource.password", pass);
      }
      environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", map));
    } catch (Exception ignored) {
      /* keep original env; startup will surface a clear datasource error */
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return null;
  }
}
