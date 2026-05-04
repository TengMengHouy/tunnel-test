package app.stadoor.tunnel;

import app.stadoor.common.dto.TunnelInfoDto;
import app.stadoor.common.entity.Session;
import app.stadoor.common.entity.Token;
import app.stadoor.common.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central in-memory registry of active SSH tunnels.
 * Persists sessions to the database and maintains a fast lookup map.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunnelManager {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUBDOMAIN_LENGTH = 8;
    private static final String BASE_DOMAIN = "stadoor.app";

    private final SessionRepository sessionRepository;
    private final PortPool portPool;

    // subdomain → TunnelInfoDto
    private final ConcurrentHashMap<String, TunnelInfoDto> activeTunnels = new ConcurrentHashMap<>();
    // MINA session id → subdomain (for disconnect cleanup)
    private final ConcurrentHashMap<Long, String> sessionIdToSubdomain = new ConcurrentHashMap<>();

    /**
     * Registers a new tunnel after the SSH remote-forward is confirmed.
     *
     * @param sessionId  MINA ServerSession id
     * @param token      authenticated token entity
     * @param assignedPort port that MINA SSHD bound
     * @return TunnelInfoDto with the assigned subdomain and URL
     */
    public TunnelInfoDto registerTunnel(long sessionId, Token token, int assignedPort) {
        String subdomain = generateUniqueSubdomain();
        String url = "https://" + subdomain + "." + BASE_DOMAIN;

        // Persist to DB
        Session session = Session.builder()
                .token(token)
                .subdomain(subdomain)
                .assignedPort(assignedPort)
                .connectedAt(LocalDateTime.now())
                .isActive(true)
                .build();
        sessionRepository.save(session);

        TunnelInfoDto dto = TunnelInfoDto.builder()
                .subdomain(subdomain)
                .url(url)
                .assignedPort(assignedPort)
                .tokenName(token.getName())
                .connectedAt(session.getConnectedAt())
                .build();

        activeTunnels.put(subdomain, dto);
        sessionIdToSubdomain.put(sessionId, subdomain);

        log.info("Tunnel registered: {} → localhost:{} (token: {})", url, assignedPort, token.getName());
        return dto;
    }

    /**
     * Releases a tunnel when the SSH session disconnects.
     */
    public void releaseSession(long sessionId) {
        String subdomain = sessionIdToSubdomain.remove(sessionId);
        if (subdomain == null) return;

        TunnelInfoDto dto = activeTunnels.remove(subdomain);
        if (dto != null) {
            portPool.releasePort(dto.getAssignedPort());
        }

        // Mark inactive in DB
        sessionRepository.findBySubdomainAndIsActiveTrue(subdomain).ifPresent(s -> {
            s.setActive(false);
            s.setDisconnectedAt(LocalDateTime.now());
            sessionRepository.save(s);
        });

        log.info("Tunnel released: {}.{}", subdomain, BASE_DOMAIN);
    }

    public List<TunnelInfoDto> getActiveTunnels() {
        return new ArrayList<>(activeTunnels.values());
    }

    public Optional<TunnelInfoDto> getTunnelBySubdomain(String subdomain) {
        return Optional.ofNullable(activeTunnels.get(subdomain));
    }

    public boolean hasActiveSession(long sessionId) {
        return sessionIdToSubdomain.containsKey(sessionId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String generateUniqueSubdomain() {
        SecureRandom random = new SecureRandom();
        String subdomain;
        int attempts = 0;
        do {
            subdomain = randomString(random, SUBDOMAIN_LENGTH);
            attempts++;
            if (attempts > 100) throw new IllegalStateException("Could not generate unique subdomain after 100 attempts");
        } while (activeTunnels.containsKey(subdomain));
        return subdomain;
    }

    private static String randomString(SecureRandom random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
