package dev.tintwym.medicore.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads a local {@code .env} file into the Spring Environment (does not override
 * variables already set in the OS / Render / Docker).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Path file = resolveEnvFile();
    if (file == null) return;

    Map<String, Object> map = new LinkedHashMap<>();
    try {
      List<String> lines = Files.readAllLines(file);
      for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int eq = trimmed.indexOf('=');
        if (eq <= 0) continue;
        String key = trimmed.substring(0, eq).trim();
        String value = trimmed.substring(eq + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'"))) {
          value = value.substring(1, value.length() - 1);
        }
        if (environment.getProperty(key) == null && System.getenv(key) == null) {
          map.put(key, value);
        }
      }
    } catch (IOException ignored) {
      return;
    }
    if (!map.isEmpty()) {
      environment.getPropertySources().addFirst(new MapPropertySource("dotenvFile", map));
    }
  }

  private static Path resolveEnvFile() {
    Path cwd = Path.of(".env");
    if (Files.isRegularFile(cwd)) return cwd;
    Path nested = Path.of("backend", ".env");
    if (Files.isRegularFile(nested)) return nested;
    return null;
  }
}
