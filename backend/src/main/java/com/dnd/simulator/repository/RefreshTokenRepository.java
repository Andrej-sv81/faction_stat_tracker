package com.dnd.simulator.repository;

import com.dnd.simulator.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByFamilyId(String familyId);

  List<RefreshToken> findByUser_IdAndRevokedAtIsNull(String userId);
}
