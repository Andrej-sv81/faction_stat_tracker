package com.dnd.simulator.repository;

import com.dnd.simulator.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
  List<PasswordResetToken> findByUser_IdAndConsumedAtIsNull(String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select token from PasswordResetToken token "
          + "where token.tokenHash = :tokenHash")
  Optional<PasswordResetToken> findByTokenHashForUpdate(
      @Param("tokenHash") String tokenHash);
}
