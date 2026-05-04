package app.stadoor.tunnel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "app.stadoor.common.entity")
@EnableJpaRepositories(basePackages = "app.stadoor.common.repository")
public class TunnelServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TunnelServerApplication.class, args);
    }
}
