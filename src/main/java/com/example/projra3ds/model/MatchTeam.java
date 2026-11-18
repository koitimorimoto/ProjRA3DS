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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Retrato de um {@link Team} no instante da partida. Não tem chave estrangeira para
 * {@code teams}: guarda só o id original, para que editar ou excluir o time não altere
 * o histórico.
 */
@Entity
@Table(name = "match_teams")
public class MatchTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private PenaltyMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchSide side;

    @Column(name = "original_team_id")
    private Long originalTeamId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "goalkeeper_color", nullable = false, length = 7)
    private String goalkeeperColor;

    @Column(name = "batter_color", nullable = false, length = 7)
    private String batterColor;

    @Column(nullable = false)
    private int goals;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<MatchPlayer> players = new ArrayList<>();

    protected MatchTeam() {
    }

    public MatchTeam(PenaltyMatch match, MatchSide side, Long originalTeamId, String name,
                     String goalkeeperColor, String batterColor, int goals) {
        this.match = match;
        this.side = side;
        this.originalTeamId = originalTeamId;
        this.name = name;
        this.goalkeeperColor = goalkeeperColor;
        this.batterColor = batterColor;
        this.goals = goals;
        match.addTeam(this);
    }

    public Long getId() {
        return id;
    }

    public PenaltyMatch getMatch() {
        return match;
    }

    public MatchSide getSide() {
        return side;
    }

    public Long getOriginalTeamId() {
        return originalTeamId;
    }

    public String getName() {
        return name;
    }

    public String getGoalkeeperColor() {
        return goalkeeperColor;
    }

    public String getBatterColor() {
        return batterColor;
    }

    public int getGoals() {
        return goals;
    }

    public List<MatchPlayer> getPlayers() {
        return players;
    }

    public void addPlayer(MatchPlayer player) {
        players.add(player);
    }

    /** Batedores na ordem de cobrança. */
    public List<MatchPlayer> getBatterSlots() {
        return players.stream()
                .filter(player -> player.getBattingOrder() != null)
                .sorted(Comparator.comparing(MatchPlayer::getBattingOrder))
                .toList();
    }

    public MatchPlayer getGoalkeeperPlayer() {
        return players.stream()
                .filter(MatchPlayer::isGoalkeeper)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O time " + name + " não possui goleiro registrado."));
    }
}
