package com.dnd.simulator.repository;

import com.dnd.simulator.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
  Optional<AppUser> findByEmail(String email);
}
