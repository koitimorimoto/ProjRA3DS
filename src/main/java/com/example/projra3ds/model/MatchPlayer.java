package com.example.projra3ds.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Retrato de um {@link Player} no instante da partida (nome, habilidades e altura como
 * eram). Cada linha representa um papel: uma posição na ordem de batedores
 * ({@code battingOrder} preenchido) ou o goleiro ({@code goalkeeper = true}). Se o goleiro
 * também bate, ele aparece em duas linhas.
 */
@Entity
@Table(name = "match_players")
public class MatchPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_team_id", nullable = false)
    private MatchTeam team;

    @Column(name = "original_player_id")
    private Long originalPlayerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int finishing;

    @Column(nullable = false)
    private int goalkeeping;

    @Column(name = "height_meters", nullable = false, precision = 3, scale = 2)
    private double heightMeters;

    @Column(name = "batting_order")
    private Integer battingOrder;

    @Column(name = "is_goalkeeper", nullable = false)
    private boolean goalkeeper;

    protected MatchPlayer() {
    }

    public MatchPlayer(MatchTeam team, Long originalPlayerId, String name, int finishing,
                       int goalkeeping, double heightMeters, Integer battingOrder,
                       boolean goalkeeper) {
        this.team = team;
        this.originalPlayerId = originalPlayerId;
        this.name = name;
        this.finishing = finishing;
        this.goalkeeping = goalkeeping;
        this.heightMeters = heightMeters;
        this.battingOrder = battingOrder;
        this.goalkeeper = goalkeeper;
        team.addPlayer(this);
    }

    public Long getId() {
        return id;
    }

    public MatchTeam getTeam() {
        return team;
    }

    public Long getOriginalPlayerId() {
        return originalPlayerId;
    }

    public String getName() {
        return name;
    }

    public int getFinishing() {
        return finishing;
    }

    public int getGoalkeeping() {
        return goalkeeping;
    }

    public double getHeightMeters() {
        return heightMeters;
    }

    public Integer getBattingOrder() {
        return battingOrder;
    }

    public boolean isGoalkeeper() {
        return goalkeeper;
    }
}
