package com.example.projra3ds.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team_lineups")
public class TeamLineup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "lineup_batters",
            joinColumns = @JoinColumn(name = "lineup_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @OrderColumn(name = "batting_order")
    private List<Player> batters = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goalkeeper_id")
    private Player goalkeeper;

    protected TeamLineup() {
    }

    public TeamLineup(Team team) {
        this.team = team;
    }

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public List<Player> getBatters() {
        return batters;
    }

    public void setBatters(List<Player> batters) {
        this.batters = new ArrayList<>(batters);
    }

    public Player getGoalkeeper() {
        return goalkeeper;
    }

    public void setGoalkeeper(Player goalkeeper) {
        this.goalkeeper = goalkeeper;
    }
}
