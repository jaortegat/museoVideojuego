package com.example.museovideojuego.repository;

import com.example.museovideojuego.model.ConsoleVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConsoleVoteRepository extends JpaRepository<ConsoleVote, Long> {
    Optional<ConsoleVote> findByConsoleName(String consoleName);
}
