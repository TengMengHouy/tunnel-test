package app.stadoor.tunnel.ssh;

import app.stadoor.common.entity.Token;
import app.stadoor.tunnel.TunnelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelPortForwardingListener implements PortForwardingEventListener {

    private final TunnelManager tunnelManager;

    @Override
    public void establishedExplicitTunnel(
            Session session,
            SshdSocketAddress local,
            SshdSocketAddress remote,
            boolean localForwarding,
            SshdSocketAddress boundAddress,
            Throwable reason) throws IOException {

        if (localForwarding || reason != null) return;

        // Retrieve the token stored during authentication using the shared AttributeKey
        Token token = session.getAttribute(TokenPasswordAuthenticator.TOKEN_KEY);
        if (token == null) {
            log.error("No token found in session attributes — rejecting tunnel");
            return;
        }

        int boundPort = boundAddress.getPort();
        log.debug("Remote forward established on port {} for token '{}'", boundPort, token.getName());

        // Use System.identityHashCode as a stable long ID for this session object
        long sessionId = System.identityHashCode(session);

        var info = tunnelManager.registerTunnel(sessionId, token, boundPort);

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
        // Cleanup handled by TunnelSessionListener.sessionClosed()
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }
}
