package com.example.projra3ds.game;

public final class AimTiming {
    private static final double SLOW_MOTION_FACTOR = 0.5;

    private final double speed;
    private double x = 0.18;
    private double y = 0.28;
    private int xDirection = 1;
    private int yDirection = 1;
    private boolean xLocked;
    private boolean yLocked;

    public AimTiming(int finishing) {
        if (finishing < 0 || finishing > 100) {
            throw new IllegalArgumentException("Finalização deve estar entre 0 e 100.");
        }
        speed = (0.35 + (100 - finishing) * 0.012) * SLOW_MOTION_FACTOR;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isXLocked() {
        return xLocked;
    }

    public boolean isYLocked() {
        return yLocked;
    }

    public boolean isXActive() {
        return !xLocked;
    }

    public boolean isYActive() {
        return xLocked && !yLocked;
    }

    public boolean isComplete() {
        return xLocked && yLocked;
    }

    public void advance(double elapsedSeconds) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0) {
            throw new IllegalArgumentException("O tempo decorrido deve ser um valor não negativo.");
        }
        if (isXActive()) {
            double[] next = advanceAxis(x, xDirection, speed * elapsedSeconds);
            x = next[0];
            xDirection = (int) next[1];
        } else if (isYActive()) {
            double[] next = advanceAxis(y, yDirection, speed * 0.73 * elapsedSeconds);
            y = next[0];
            yDirection = (int) next[1];
        }
    }

    public void lockCurrentAxis() {
        if (isXActive()) {
            xLocked = true;
        } else if (isYActive()) {
            yLocked = true;
        }
    }

    private double[] advanceAxis(double position, int direction, double distance) {
        double next = position + direction * distance;
        while (next < 0 || next > 1) {
            if (next > 1) {
                next = 2 - next;
                direction = -1;
            } else {
                next = -next;
                direction = 1;
            }
        }
        return new double[]{next, direction};
    }
}
