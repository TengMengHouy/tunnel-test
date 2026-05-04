package app.stadoor.tunnel;

import org.springframework.context.annotation.Configuration;

/**
 * JPA is fully configured by CommonAutoConfiguration in the common module.
 * That class uses @AutoConfigurationPackage + @EnableJpaRepositories and is
 * registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * so Spring Boot picks it up automatically.
 *
 * Nothing extra needed here.
 */
@Configuration
public class JpaConfig {
}

