package app.stadoor.tunnel.ssh;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.forward.PortForwardingEventListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Configures and starts the Apache MINA SSHD server on port 2222.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SshServerConfig {

    @Value("${stadoor.ssh.port:2222}")
    private int sshPort;

    @Value("${stadoor.ssh.hostkey:hostkey.ser}")
    private String hostKeyPath;

    private final TokenPasswordAuthenticator tokenPasswordAuthenticator;
    private final TunnelSessionListener tunnelSessionListener;
    private final TunnelPortForwardingListener tunnelPortForwardingListener;

    private SshServer sshd;

    @PostConstruct
    public void startSshServer() throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setHost("0.0.0.0");
        sshd.setPort(sshPort);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(hostKeyPath)));
        sshd.setPasswordAuthenticator(tokenPasswordAuthenticator);
        sshd.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);

        // Allow gateway-ports style forwarding
        sshd.getProperties().put("GatewayPorts", "true");

        // Register session lifecycle listener
        sshd.addSessionListener(tunnelSessionListener);

        // Register port-forwarding listener
        sshd.addPortForwardingEventListener(tunnelPortForwardingListener);

        sshd.start();
        log.info("MINA SSHD started on port {}", sshPort);
    }

    @PreDestroy
    public void stopSshServer() throws IOException {
        if (sshd != null && sshd.isStarted()) {
            sshd.stop();
            log.info("MINA SSHD stopped");
        }
    }
}
