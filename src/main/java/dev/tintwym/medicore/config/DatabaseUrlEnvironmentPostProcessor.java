package dev.tintwym.medicore.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Normalizes {@code DATABASE_URL} for Spring Boot:
 * <ul>
 *   <li>Render/Heroku {@code postgres(ql)://} → JDBC + username/password</li>
 *   <li>Existing {@code jdbc:postgresql://} → wires {@code spring.datasource.*}</li>
 *   <li>Blank env vars (common when Render keys exist but values were never pasted) are ignored</li>
 * </ul>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
  private static final String LOCAL_DEFAULT =
      "jdbc:postgresql://127.0.0.1:5433/medicore";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String raw = firstNonBlank(
        environment.getProperty("DATABASE_URL"),
        environment.getProperty("SPRING_DATASOURCE_URL"),
        System.getenv("DATABASE_URL"),
        System.getenv("SPRING_DATASOURCE_URL"));

    boolean onRender = "true".equalsIgnoreCase(System.getenv("RENDER"));
    boolean onRailway = System.getenv("RAILWAY_ENVIRONMENT") != null
        || System.getenv("RAILWAY_SERVICE_NAME") != null;

    if (raw == null) {
      if (onRender || onRailway) {
        throw missingDatabaseUrl(onRender ? "Render" : "Railway");
      }
      return;
    }

    String trimmed = raw.trim();
    try {
      if (trimmed.startsWith("jdbc:")) {
        applyJdbc(environment, trimmed, null, null);
        return;
      }
      if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
        if (onRender || onRailway) {
          throw new IllegalStateException(
              "DATABASE_URL must be jdbc:postgresql://... or postgres(ql)://... (got: "
                  + trimmed.substring(0, Math.min(32, trimmed.length()))
                  + "…)");
        }
        return;
      }

      String normalized = trimmed.replaceFirst("^postgres(ql)?://", "http://");
      URI uri = URI.create(normalized);
      String user = null;
      String pass = null;
      if (uri.getUserInfo() != null) {
        String[] parts = uri.getUserInfo().split(":", 2);
        user = decode(parts[0]);
        pass = parts.length > 1 ? decode(parts[1]) : "";
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new IllegalStateException("DATABASE_URL has no host");
      }
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String path = uri.getPath() == null || uri.getPath().isBlank() ? "/neondb" : uri.getPath();
      String query = uri.getQuery();

      StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
          .append(uri.getHost())
          .append(':')
          .append(port)
          .append(path);
      if (query != null && !query.isBlank()) {
        jdbcUrl.append('?').append(query);
        if (!query.contains("sslmode=")) {
          jdbcUrl.append("&sslmode=require");
        }
      } else {
        jdbcUrl.append("?sslmode=require");
      }

      applyJdbc(environment, jdbcUrl.toString(), user, pass);
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Failed to parse DATABASE_URL. Use JDBC form: "
              + "jdbc:postgresql://HOST/DB?sslmode=require with DATABASE_USERNAME / DATABASE_PASSWORD. "
              + "Cause: "
              + ex.getMessage(),
          ex);
    }
  }

  private static void applyJdbc(
      ConfigurableEnvironment environment, String jdbcUrl, String user, String pass) {
    boolean onRender = "true".equalsIgnoreCase(System.getenv("RENDER"));
    boolean onRailway = System.getenv("RAILWAY_ENVIRONMENT") != null
        || System.getenv("RAILWAY_SERVICE_NAME") != null;
    boolean onCloud = onRender || onRailway;

    if (jdbcUrl == null || jdbcUrl.isBlank()
        || (onCloud && jdbcUrl.equals(LOCAL_DEFAULT))) {
      throw missingDatabaseUrl(onRender ? "Render" : onRailway ? "Railway" : "cloud");
    }

    String username = firstNonBlank(
        user,
        environment.getProperty("DATABASE_USERNAME"),
        environment.getProperty("SPRING_DATASOURCE_USERNAME"),
        System.getenv("DATABASE_USERNAME"));
    String password = firstNonBlank(
        pass,
        environment.getProperty("DATABASE_PASSWORD"),
        environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
        System.getenv("DATABASE_PASSWORD"));

    if (onCloud && (username == null || password == null)) {
      throw new IllegalStateException(
          "DATABASE_USERNAME and DATABASE_PASSWORD are required on "
              + (onRender ? "Render" : "Railway")
              + " (or embed user:password in a postgres:// DATABASE_URL).");
    }

    Map<String, Object> map = new HashMap<>();
    map.put("DATABASE_URL", jdbcUrl);
    map.put("spring.datasource.url", jdbcUrl);
    if (username != null) {
      map.put("DATABASE_USERNAME", username);
      map.put("spring.datasource.username", username);
    }
    if (password != null) {
      map.put("DATABASE_PASSWORD", password);
      map.put("spring.datasource.password", password);
    }

    environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", map));
  }

  private static IllegalStateException missingDatabaseUrl(String platform) {
    return new IllegalStateException(
        "DATABASE_URL is missing or empty on "
            + platform
            + ". Set these environment variables on the service (not only locally):\n"
            + "  DATABASE_URL=jdbc:postgresql://ep-xxxx-pooler.region.aws.neon.tech/neondb?sslmode=require\n"
            + "  DATABASE_USERNAME=neondb_owner\n"
            + "  DATABASE_PASSWORD=<neon-password>\n"
            + "On Render: Dashboard → your service → Environment → add/save → Manual Deploy.");
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private static String firstNonBlank(String... values) {
    if (values == null) return null;
    for (String value : values) {
      if (value != null && !value.isBlank()) return value.trim();
    }
    return null;
  }
}
