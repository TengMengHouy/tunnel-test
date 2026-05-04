package app.stadoor.tunnel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe port pool for managing SSH remote-forward port assignments.
 * Ports are sourced from the configured range (default 10000–20000).
 */
@Slf4j
@Component
public class PortPool {

    private final ConcurrentSkipListSet<Integer> available = new ConcurrentSkipListSet<>();

    public PortPool() {
        for (int port = 10000; port <= 20000; port++) {
            available.add(port);
        }
        log.info("PortPool initialized with {} ports (10000–20000)", available.size());
    }

    /**
     * Acquires the next available port from the pool.
     *
     * @return Optional containing the port, or empty if none available
     */
    public Optional<Integer> acquirePort() {
        Integer port = available.pollFirst();
        if (port != null) {
            log.debug("Acquired port {}", port);
        } else {
            log.warn("No ports available in pool");
        }
        return Optional.ofNullable(port);
    }

    /**
     * Returns a port back to the pool after a tunnel disconnects.
     */
    public void releasePort(int port) {
        available.add(port);
        log.debug("Released port {}", port);
    }

    public int availableCount() {
        return available.size();
    }
}
