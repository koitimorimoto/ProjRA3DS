package com.example.projra3ds.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {
    public static final String DEFAULT_GOALKEEPER_COLOR = "#FFD54F";
    public static final String DEFAULT_BATTER_COLOR = "#1565C0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 120)
    private String country;

    @Column(name = "goalkeeper_color", nullable = false, length = 7)
    private String goalkeeperColor = DEFAULT_GOALKEEPER_COLOR;

    @Column(name = "batter_color", nullable = false, length = 7)
    private String batterColor = DEFAULT_BATTER_COLOR;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "team")
    private List<Player> players = new ArrayList<>();

    protected Team() {
    }

    public Team(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public Team(String name, String country, String goalkeeperColor, String batterColor) {
        this(name, country);
        setGoalkeeperColor(goalkeeperColor);
        setBatterColor(batterColor);
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGoalkeeperColor() {
        return goalkeeperColor;
    }

    public void setGoalkeeperColor(String goalkeeperColor) {
        this.goalkeeperColor = normalizeColor(goalkeeperColor, "goleiro");
    }

    public String getBatterColor() {
        return batterColor;
    }

    public void setBatterColor(String batterColor) {
        this.batterColor = normalizeColor(batterColor, "batedor");
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public static String normalizeColor(String color, String role) {
        if (color == null || !color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(
                    "A cor do " + role + " deve estar no formato #RRGGBB.");
        }
        return color.toUpperCase(java.util.Locale.ROOT);
    }

}
