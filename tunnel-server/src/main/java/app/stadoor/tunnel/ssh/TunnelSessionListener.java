package app.stadoor.tunnel.ssh;

import app.stadoor.tunnel.TunnelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.springframework.stereotype.Component;

/**
 * Listens for SSH session lifecycle events to clean up tunnels on disconnect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelSessionListener implements SessionListener {

    private final TunnelManager tunnelManager;

    @Override
    public void sessionCreated(Session session) {
        log.debug("SSH session created: id={} remote={}", session.getId(), session.getClientAddress());
    }

    @Override
    public void sessionClosed(Session session) {
        log.info("SSH session closed: id={}", session.getId());
        tunnelManager.releaseSession(session.getId());
    }

    @Override
    public void sessionException(Session session, Throwable t) {
        log.warn("SSH session exception: id={} error={}", session.getId(), t.getMessage());
    }
}
