package app.stadoor.gateway;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive client that calls tunnel-server's REST API to look up a tunnel by subdomain.
 */
@Slf4j
@Component
public class TunnelClient {

    private final WebClient webClient;

    public TunnelClient(@Value("${stadoor.tunnel-server.url:http://localhost:8080}") String tunnelServerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(tunnelServerUrl)
                .build();
    }

    /**
     * Returns tunnel info for the given subdomain, or empty if not found.
     */
    public Mono<TunnelInfo> getTunnel(String subdomain) {
        return webClient.get()
                .uri("/api/tunnels/{subdomain}", subdomain)
                .retrieve()
                .bodyToMono(TunnelInfo.class)
                .onErrorResume(e -> {
                    log.debug("Tunnel not found for subdomain '{}': {}", subdomain, e.getMessage());
                    return Mono.empty();
                });
    }

    @Data
    public static class TunnelInfo {
        private String subdomain;
        private String url;
        private int assignedPort;
        private String tokenName;
    }
}
