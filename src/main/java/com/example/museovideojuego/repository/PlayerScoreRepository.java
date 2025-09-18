package com.example.museovideojuego.repository;

import com.example.museovideojuego.model.PlayerScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerScoreRepository extends JpaRepository<PlayerScore, Long> {
    List<PlayerScore> findByVideogameOrderByScoreDesc(String videogame);
}
