package com.example.museovideojuego.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "console_votes")
public class ConsoleVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String consoleName;

    @Column(nullable = false)
    private long votes = 0;

    public ConsoleVote() {}

    public ConsoleVote(String consoleName) {
        this.consoleName = consoleName;
        this.votes = 0;
    }

    public Long getId() { return id; }
    public String getConsoleName() { return consoleName; }
    public void setConsoleName(String consoleName) { this.consoleName = consoleName; }
    public long getVotes() { return votes; }
    public void setVotes(long votes) { this.votes = votes; }
    public void increment() { this.votes++; }

    @Override
    public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ConsoleVote)) return false; ConsoleVote that = (ConsoleVote) o; return Objects.equals(id, that.id); }
    @Override
    public int hashCode() { return Objects.hash(id); }
}
