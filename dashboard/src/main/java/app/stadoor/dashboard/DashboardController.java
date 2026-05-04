package app.stadoor.dashboard;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Proxies all calls to tunnel-server's REST API so the dashboard frontend
 * only needs to talk to a single origin (no CORS issues).
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final RestClient restClient;

    public DashboardController(
            @Value("${stadoor.tunnel-server.url:http://localhost:8080}") String tunnelServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(tunnelServerUrl)
                .build();
    }

    @GetMapping("/tunnels")
    public ResponseEntity<List<Object>> listTunnels() {
        List<Object> body = restClient.get()
                .uri("/api/tunnels")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.ok(body);
    }

    @GetMapping("/tokens")
    public ResponseEntity<List<Object>> listTokens() {
        List<Object> body = restClient.get()
                .uri("/api/tokens")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.ok(body);
    }

    @PostMapping("/tokens/generate")
    public ResponseEntity<Object> generateToken(@RequestBody(required = false) Map<String, String> req) {
        Object body = restClient.post()
                .uri("/api/tokens/generate")
                .body(req)
                .retrieve()
                .body(Object.class);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/tokens/{token}")
    public ResponseEntity<Void> revokeToken(@PathVariable String token) {
        restClient.delete()
                .uri("/api/tokens/{token}", token)
                .retrieve()
                .toBodilessEntity();
        return ResponseEntity.noContent().build();
    }
}
