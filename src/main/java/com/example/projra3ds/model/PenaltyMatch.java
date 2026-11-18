package com.example.projra3ds.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "penalty_matches")
public class PenaltyMatch {
    public static final int MAX_TITLE_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "user_goals", nullable = false)
    private int userGoals;

    @Column(name = "cpu_goals", nullable = false)
    private int cpuGoals;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_side", nullable = false, length = 10)
    private MatchSide winnerSide;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<MatchTeam> teams = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<MatchAttempt> attempts = new ArrayList<>();

    protected PenaltyMatch() {
    }

    public PenaltyMatch(int userGoals, int cpuGoals, MatchSide winnerSide) {
        this.userGoals = userGoals;
        this.cpuGoals = cpuGoals;
        this.winnerSide = winnerSide;
    }

    @PrePersist
    private void setPlayedAt() {
        if (playedAt == null) {
            playedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getUserGoals() {
        return userGoals;
    }

    public int getCpuGoals() {
        return cpuGoals;
    }

    public MatchSide getWinnerSide() {
        return winnerSide;
    }

    public List<MatchTeam> getTeams() {
        return teams;
    }

    public List<MatchAttempt> getAttempts() {
        return attempts;
    }

    public void addTeam(MatchTeam team) {
        teams.add(team);
    }

    public void addAttempt(MatchAttempt attempt) {
        attempts.add(attempt);
    }

    public MatchTeam getTeam(MatchSide side) {
        return teams.stream()
                .filter(team -> team.getSide() == side)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A partida não possui o time " + side + "."));
    }

    public String getUserTeamName() {
        return getTeam(MatchSide.USER).getName();
    }

    public String getCpuTeamName() {
        return getTeam(MatchSide.CPU).getName();
    }

    public String getWinnerName() {
        return getTeam(winnerSide).getName();
    }
}
