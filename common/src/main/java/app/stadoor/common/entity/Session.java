package app.stadoor.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;

    @Column(unique = true, nullable = false)
    private String subdomain;

    @Column(name = "assigned_port", nullable = false)
    private int assignedPort;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @PrePersist
    public void prePersist() {
        if (connectedAt == null) connectedAt = LocalDateTime.now();
        isActive = true;
    }
}
