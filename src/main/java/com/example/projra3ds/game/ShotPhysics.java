package com.example.projra3ds.game;

public final class ShotPhysics {
    private static final double BASE_FLIGHT_SECONDS = 2.5;
    private static final double FINISHING_FLIGHT_REDUCTION_SECONDS = 0.014;
    private static final double POWER_FLIGHT_REDUCTION_SECONDS = 0.5;
    private static final double BASE_GOALKEEPER_SPEED = 0.35;
    private static final double GOALKEEPER_SPEED_PER_SKILL = 0.006;

    private ShotPhysics() {
    }

    public static double ballFlightSeconds(double power) {
        return ballFlightSeconds(50, power);
    }

    public static double ballFlightSeconds(int finishing, double power) {
        if (finishing < 0 || finishing > 100) {
            throw new IllegalArgumentException("Finalização deve estar entre 0 e 100.");
        }
        if (!Double.isFinite(power) || power < 0 || power > 1) {
            throw new IllegalArgumentException("A força deve estar entre 0 e 1.");
        }
        return BASE_FLIGHT_SECONDS - finishing * FINISHING_FLIGHT_REDUCTION_SECONDS
                - power * POWER_FLIGHT_REDUCTION_SECONDS;
    }

    public static double goalkeeperSpeed(int skill) {
        if (skill < 0 || skill > 100) {
            throw new IllegalArgumentException("Habilidade do goleiro deve estar entre 0 e 100.");
        }
        return BASE_GOALKEEPER_SPEED + skill * GOALKEEPER_SPEED_PER_SKILL;
    }

    public static double goalkeeperXAtImpact(GoalkeeperSide side, int skill, double flightSeconds) {
        if (side == null) {
            throw new IllegalArgumentException("O lado da defesa é obrigatório.");
        }
        if (!Double.isFinite(flightSeconds) || flightSeconds < 0) {
            throw new IllegalArgumentException("O tempo de voo deve ser não negativo.");
        }
        double targetX = switch (side) {
            case LEFT -> -0.5;
            case CENTER -> 0.0;
            case RIGHT -> 0.5;
        };
        double distance = goalkeeperSpeed(skill) * flightSeconds;
        return Math.copySign(Math.min(Math.abs(targetX), distance), targetX);
    }

    public static boolean canSave(double x, double y, GoalkeeperSide side,
                                  int goalkeeperSkill, double power) {
        return canSave(x, y, side, goalkeeperSkill, 50, power);
    }

    public static boolean canSave(double x, double y, GoalkeeperSide side,
                                  int goalkeeperSkill, int finishing, double power) {
        if (x < -1.0 || x > 1.0 || y < 0.0 || y > 1.0) {
            return false;
        }
        double flightSeconds = ballFlightSeconds(finishing, power);
        double keeperX = goalkeeperXAtImpact(side, goalkeeperSkill, flightSeconds);
        double horizontalReach = 0.14 + goalkeeperSkill * 0.0025;
        double verticalReach = 0.18 + goalkeeperSkill * 0.004;
        return Math.abs(x - keeperX) <= horizontalReach
                && Math.abs(y - 0.5) <= verticalReach;
    }
}
