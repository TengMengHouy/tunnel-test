package app.stadoor.tunnel.ssh;

import app.stadoor.common.entity.Token;
import app.stadoor.common.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /** Session attribute key to store the validated Token entity. */
    public static final String TOKEN_ATTR = "stadoor.token";

    private final TokenRepository tokenRepository;

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        return tokenRepository.findByTokenAndIsActiveTrue(username)
                .map(token -> {
                    // Store the token entity so the port-forwarding listener can use it
                    session.setAttribute(
                            org.apache.sshd.common.AttributeRepository.AttributeKey.ofType(Token.class, TOKEN_ATTR),
                            token
                    );
                    log.info("Authenticated token '{}' (name: {}) from {}",
                            username, token.getName(), session.getClientAddress());
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Authentication failed for token '{}' from {}", username, session.getClientAddress());
                    return false;
                });
    }
}
