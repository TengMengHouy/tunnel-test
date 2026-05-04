package app.stadoor.tunnel.api;

import app.stadoor.common.dto.TokenDto;
import app.stadoor.common.dto.TunnelInfoDto;
import app.stadoor.common.entity.Token;
import app.stadoor.common.repository.TokenRepository;
import app.stadoor.tunnel.TunnelManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TunnelController {

    private final TunnelManager tunnelManager;
    private final TokenRepository tokenRepository;

    // ─── Tunnel endpoints ─────────────────────────────────────────────────────

    @GetMapping("/api/tunnels")
    public List<TunnelInfoDto> listTunnels() {
        return tunnelManager.getActiveTunnels();
    }

    @GetMapping("/api/tunnels/{subdomain}")
    public ResponseEntity<TunnelInfoDto> getTunnel(@PathVariable String subdomain) {
        return tunnelManager.getTunnelBySubdomain(subdomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Token endpoints ──────────────────────────────────────────────────────

    @PostMapping("/api/tokens/generate")
    public ResponseEntity<TokenDto> generateToken(@RequestBody(required = false) Map<String, String> body) {
        String name = (body != null && body.containsKey("name"))
                ? body.get("name")
                : "token-" + System.currentTimeMillis();

        String rawToken = UUID.randomUUID().toString().replace("-", "");

        Token token = Token.builder()
                .token(rawToken)
                .name(name)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        tokenRepository.save(token);

        TokenDto dto = TokenDto.builder()
                .id(token.getId().toString())
                .token(rawToken)
                .name(name)
                .createdAt(token.getCreatedAt())
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/api/tokens/{token}")
    public ResponseEntity<Void> revokeToken(@PathVariable String token) {
        return tokenRepository.findByTokenAndIsActiveTrue(token)
                .map(t -> {
                    t.setActive(false);
                    tokenRepository.save(t);
                    return ResponseEntity.<Void>noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/tokens")
    public List<TokenDto> listTokens() {
        return tokenRepository.findAll().stream()
                .map(t -> TokenDto.builder()
                        .id(t.getId().toString())
                        .token(t.getToken())
                        .name(t.getName())
                        .createdAt(t.getCreatedAt())
                        .isActive(t.isActive())
                        .build())
                .toList();
    }
}
