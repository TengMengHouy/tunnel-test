package app.stadoor.tunnel.ssh;

import app.stadoor.common.entity.Token;
import app.stadoor.common.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.stereotype.Component;

/**
 * Authenticates SSH clients using the token as the username.
 * Password field is completely ignored — token is the only credential.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenPasswordAuthenticator implements PasswordAuthenticator {

    /**
     * Shared AttributeKey instance — MINA SSHD uses object identity for key matching,
     * so both the authenticator and the port-forwarding listener must reference this same instance.
     */
    public static final AttributeRepository.AttributeKey<Token> TOKEN_KEY =
            new AttributeRepository.AttributeKey<>();

    private final TokenRepository tokenRepository;

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        return tokenRepository.findByTokenAndIsActiveTrue(username)
                .map(token -> {
                    session.setAttribute(TOKEN_KEY, token);
                    log.info("Authenticated token '{}' (name: {}) from {}",
                            username, token.getName(), session.getRemoteAddress());
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Authentication failed for token '{}' from {}",
                            username, session.getRemoteAddress());
                    return false;
                });
    }
}
