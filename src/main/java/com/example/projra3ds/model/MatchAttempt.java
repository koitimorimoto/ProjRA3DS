package com.example.projra3ds.model;

import com.example.projra3ds.game.GoalkeeperSide;
import com.example.projra3ds.game.ShotResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma cobrança de pênalti de uma {@link PenaltyMatch}. Guarda tudo o que é necessário para
 * reproduzir a animação: mira, posição final da bola, força, lado escolhido pelo goleiro e
 * resultado. O batedor e o goleiro apontam para os retratos ({@link MatchPlayer}) da própria
 * partida, nunca para o cadastro atual de jogadores.
 */
@Entity
@Table(name = "match_attempts")
public class MatchAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private PenaltyMatch match;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "shooter_side", nullable = false, length = 10)
    private MatchSide shooterSide;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shooter_id", nullable = false)
    private MatchPlayer shooter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "goalkeeper_id", nullable = false)
    private MatchPlayer goalkeeper;

    @Column(name = "aimed_x", nullable = false)
    private double aimedX;

    @Column(name = "aimed_y", nullable = false)
    private double aimedY;

    @Column(name = "ball_x", nullable = false)
    private double ballX;

    @Column(name = "ball_y", nullable = false)
    private double ballY;

    @Column(name = "shot_power", nullable = false)
    private double power;

    @Enumerated(EnumType.STRING)
    @Column(name = "goalkeeper_dive", nullable = false, length = 10)
    private GoalkeeperSide goalkeeperSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "shot_result", nullable = false, length = 10)
    private ShotResult result;

    protected MatchAttempt() {
    }

    public MatchAttempt(PenaltyMatch match, int attemptNumber, int roundNumber,
                        MatchSide shooterSide, MatchPlayer shooter, MatchPlayer goalkeeper,
                        double aimedX, double aimedY, double ballX, double ballY, double power,
                        GoalkeeperSide goalkeeperSide, ShotResult result) {
        this.match = match;
        this.attemptNumber = attemptNumber;
        this.roundNumber = roundNumber;
        this.shooterSide = shooterSide;
        this.shooter = shooter;
        this.goalkeeper = goalkeeper;
        this.aimedX = aimedX;
        this.aimedY = aimedY;
        this.ballX = ballX;
        this.ballY = ballY;
        this.power = power;
        this.goalkeeperSide = goalkeeperSide;
        this.result = result;
        match.addAttempt(this);
    }

    public Long getId() {
        return id;
    }

    public PenaltyMatch getMatch() {
        return match;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public MatchSide getShooterSide() {
        return shooterSide;
    }

    public MatchPlayer getShooter() {
        return shooter;
    }

    public MatchPlayer getGoalkeeper() {
        return goalkeeper;
    }

    public double getAimedX() {
        return aimedX;
    }

    public double getAimedY() {
        return aimedY;
    }

    public double getBallX() {
        return ballX;
    }

    public double getBallY() {
        return ballY;
    }

    public double getPower() {
        return power;
    }

    public GoalkeeperSide getGoalkeeperSide() {
        return goalkeeperSide;
    }

    public ShotResult getResult() {
        return result;
    }
}
