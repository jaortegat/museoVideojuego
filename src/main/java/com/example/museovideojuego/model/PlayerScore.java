package com.example.museovideojuego.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player_scores")
public class PlayerScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String videogame;

    @Column
    private Integer score;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public PlayerScore() {}

    public PlayerScore(String playerName, String videogame, Integer score) {
        this.playerName = playerName;
        this.videogame = videogame;
        this.score = score;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public String getVideogame() { return videogame; }
    public Integer getScore() { return score; }
    public Instant getCreatedAt() { return createdAt; }
}
