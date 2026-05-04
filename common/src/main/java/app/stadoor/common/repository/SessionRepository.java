package app.stadoor.common.repository;

import app.stadoor.common.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findBySubdomainAndIsActiveTrue(String subdomain);

    List<Session> findAllByIsActiveTrue();

    boolean existsBySubdomainAndIsActiveTrue(String subdomain);
}
