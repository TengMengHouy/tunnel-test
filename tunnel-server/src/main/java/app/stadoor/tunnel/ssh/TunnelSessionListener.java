package app.stadoor.tunnel.ssh;

import app.stadoor.tunnel.TunnelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelSessionListener implements SessionListener {

    private final TunnelManager tunnelManager;

    @Override
    public void sessionCreated(Session session) {
        log.debug("SSH session created: remote={}", session.getRemoteAddress());
    }

    @Override
    public void sessionClosed(Session session) {
        long sessionId = System.identityHashCode(session);
        log.info("SSH session closed: id={}", sessionId);
        tunnelManager.releaseSession(sessionId);
    }

    @Override
    public void sessionException(Session session, Throwable t) {
        log.warn("SSH session exception: id={} error={}",
                System.identityHashCode(session), t.getMessage());
    }
}
