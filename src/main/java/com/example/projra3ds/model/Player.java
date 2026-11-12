package com.example.projra3ds.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "players")
public class Player {
    public static final double DEFAULT_HEIGHT_METERS = 1.75;
    public static final double MIN_HEIGHT_METERS = 1.0;
    public static final double MAX_HEIGHT_METERS = 2.5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int finishing;

    @Column(nullable = false)
    private int goalkeeping;

    @Column(name = "height_meters", nullable = false, precision = 3, scale = 2)
    private double heightMeters = DEFAULT_HEIGHT_METERS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    protected Player() {
    }

    public Player(String name, int finishing, int goalkeeping) {
        this.name = name;
        this.finishing = finishing;
        this.goalkeeping = goalkeeping;
    }

    public Player(String name, int finishing, int goalkeeping, double heightMeters) {
        this(name, finishing, goalkeeping);
        setHeightMeters(heightMeters);
    }

    @PrePersist
    private void setCreationTime() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFinishing() {
        return finishing;
    }

    public void setFinishing(int finishing) {
        this.finishing = finishing;
    }

    public int getGoalkeeping() {
        return goalkeeping;
    }

    public void setGoalkeeping(int goalkeeping) {
        this.goalkeeping = goalkeeping;
    }

    public double getHeightMeters() {
        return heightMeters;
    }

    public void setHeightMeters(double heightMeters) {
        validateHeightMeters(heightMeters);
        this.heightMeters = heightMeters;
    }

    public static void validateHeightMeters(double heightMeters) {
        if (!Double.isFinite(heightMeters) || heightMeters < MIN_HEIGHT_METERS
                || heightMeters > MAX_HEIGHT_METERS
                || Math.abs(heightMeters * 100 - Math.rint(heightMeters * 100)) > 1e-8) {
            throw new IllegalArgumentException(
                    "A altura deve estar entre 1,00 m e 2,50 m, com até duas casas decimais.");
        }
    }

    public static boolean isSkillInRange(int skill) {
        return skill >= 0 && skill <= 100;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
