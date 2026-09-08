package dev.tintwym.medicore;

import dev.tintwym.medicore.config.MedicoreProperties;
import dev.tintwym.medicore.config.PushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({PushProperties.class, MedicoreProperties.class})
public class MedicoreBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(MedicoreBackendApplication.class, args);
  }
}
