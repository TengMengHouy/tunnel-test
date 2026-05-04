package app.stadoor.tunnel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;

/**
 * Explicit JPA configuration for the tunnel-server module.
 *
 * Spring Boot 4.0 removed @EntityScan from spring-boot-autoconfigure.
 * Instead we use @EnableJpaRepositories (Spring Data JPA, stable) and
 * configure entity package scanning via a LocalContainerEntityManagerFactoryBeanCustomizer.
 */
@Configuration
@EnableJpaRepositories(basePackages = "app.stadoor.common.repository")
public class JpaConfig {

    /**
     * Tells Hibernate to scan the common module's entity package.
     * This replaces the old @EntityScan annotation.
     */
    @Bean
    public org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
            entityPackageCustomizer() {
        return hibernateProperties ->
                hibernateProperties.put(
                        org.hibernate.cfg.AvailableSettings.LOADED_CLASSES,
                        java.util.List.of(
                                app.stadoor.common.entity.Token.class,
                                app.stadoor.common.entity.Session.class
                        )
                );
    }
}
