package app.stadoor.tunnel.ssh;

import app.stadoor.common.dto.TunnelInfoDto;
import app.stadoor.common.entity.Token;
import app.stadoor.tunnel.PortPool;
import app.stadoor.tunnel.TunnelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Listens for SSH remote port-forwarding events.
 *
 * <p>When a client connects with {@code -R 0:localhost:PORT}, MINA SSHD binds
 * a port on the server. This listener fires once binding succeeds and:
 * <ol>
 *   <li>Acquires a port from the PortPool (or uses the OS-assigned one as fallback)</li>
 *   <li>Registers the tunnel in TunnelManager</li>
 *   <li>Sends an SSH debug message to the client with the public URL</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelPortForwardingListener implements PortForwardingEventListener {

    private final TunnelManager tunnelManager;
    private final PortPool portPool;

    private static final AttributeRepository.AttributeKey<Token> TOKEN_KEY =
            AttributeRepository.AttributeKey.ofType(Token.class, TokenPasswordAuthenticator.TOKEN_ATTR);

    @Override
    public void establishedExplicitTunnel(
            Session session,
            SshdSocketAddress local,
            SshdSocketAddress remote,
            boolean localForwarding,
            SshdSocketAddress boundAddress,
            Throwable reason) throws IOException {

        // Only handle remote-to-local forwards (client's -R flag), skip errors
        if (localForwarding || reason != null) return;

        Token token = session.getAttribute(TOKEN_KEY);
        if (token == null) {
            log.error("No token found in session attributes — rejecting tunnel");
            return;
        }

        int boundPort = boundAddress.getPort();
        log.debug("Remote forward established on port {} for token '{}'", boundPort, token.getName());

        TunnelInfoDto info = tunnelManager.registerTunnel(session.getId(), token, boundPort);

        // Notify the client via SSH debug message (visible with -v, or always with display=true)
        String message = "\r\n" +
                "╔══════════════════════════════════════════════════╗\r\n" +
                "║  Stadoor Tunnel Active                           ║\r\n" +
                "║  URL  : " + padRight(info.getUrl(), 40) + "  ║\r\n" +
                "║  Port : " + padRight(String.valueOf(boundPort), 40) + "  ║\r\n" +
                "╚══════════════════════════════════════════════════╝\r\n";

        try {
            session.sendDebugMessage(true, message, "en");
        } catch (Exception e) {
            log.warn("Could not send debug message to client: {}", e.getMessage());
        }
    }

    @Override
    public void tearingDownExplicitTunnel(
            Session session,
            SshdSocketAddress address,
            boolean localForwarding,
            SshdSocketAddress remoteAddress) throws IOException {
        // Cleanup is handled by TunnelSessionListener.sessionClosed()
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }
}
