package app.stadoor.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for the common module.
 *
 * @AutoConfigurationPackage registers app.stadoor.common as a base package
 * for Spring Boot's JPA entity scanner — this is the correct Spring Boot 3+/4+
 * way to expose entities from a shared library module.
 */
@AutoConfiguration
@AutoConfigurationPackage
@EnableJpaRepositories(basePackages = "app.stadoor.common.repository")
public class CommonAutoConfiguration {
}
