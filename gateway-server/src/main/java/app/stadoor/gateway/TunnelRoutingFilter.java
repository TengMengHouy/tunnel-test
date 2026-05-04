package app.stadoor.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * GlobalFilter that dynamically routes requests to the correct local port
 * based on the subdomain extracted from the Host header.
 *
 * <p>Flow:
 * <ol>
 *   <li>Extract subdomain from Host header (e.g. "abc123" from "abc123.stadoor.app")</li>
 *   <li>Call tunnel-server to look up the assigned port</li>
 *   <li>Rewrite the request URI to http://localhost:{port}</li>
 *   <li>Return 404 if subdomain is not found</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelRoutingFilter implements GlobalFilter, Ordered {

    private static final String BASE_DOMAIN = "stadoor.app";

    private final TunnelClient tunnelClient;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String host = exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
        String subdomain = extractSubdomain(host);

        if (subdomain == null) {
            return notFound(exchange, "Invalid host header");
        }

        return tunnelClient.getTunnel(subdomain)
                .flatMap(info -> {
                    URI targetUri = URI.create("http://localhost:" + info.getAssignedPort());
                    log.debug("Routing {}.{} → {}", subdomain, BASE_DOMAIN, targetUri);

                    ServerWebExchange mutated = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .uri(URI.create(targetUri + exchange.getRequest().getURI().getRawPath()))
                                    .build())
                            .build();

                    mutated.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);
                    return chain.filter(mutated);
                })
                .switchIfEmpty(Mono.defer(() -> notFound(exchange,
                        "Tunnel not found: " + subdomain + "." + BASE_DOMAIN)));
    }

    /**
     * Extracts the subdomain from a host string like "abc123.stadoor.app" or "abc123.stadoor.app:80".
     */
    private String extractSubdomain(String host) {
        if (host == null || host.isBlank()) return null;
        // Strip port if present
        String hostOnly = host.contains(":") ? host.substring(0, host.lastIndexOf(':')) : host;
        if (!hostOnly.endsWith("." + BASE_DOMAIN)) return null;
        String sub = hostOnly.substring(0, hostOnly.length() - BASE_DOMAIN.length() - 1);
        return sub.isBlank() ? null : sub;
    }

    private Mono<Void> notFound(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
