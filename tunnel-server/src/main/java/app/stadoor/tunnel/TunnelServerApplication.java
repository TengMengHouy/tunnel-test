package app.stadoor.tunnel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "app.stadoor")
public class TunnelServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TunnelServerApplication.class, args);
    }
}
