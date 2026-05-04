package app.stadoor.common.repository;

import app.stadoor.common.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByTokenAndIsActiveTrue(String token);

    boolean existsByToken(String token);
}
