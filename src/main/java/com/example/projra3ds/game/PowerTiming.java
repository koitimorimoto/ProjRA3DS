package com.example.projra3ds.game;

/**
 * Barra de força: um marcador vai e volta entre 0 e 1. O objetivo é fixá-lo na zona central.
 *
 * <p>Quanto maior a finalização do batedor, mais lento o marcador e maior a zona central
 * (10% da barra com finalização 0 e 50% com finalização 100, sempre centralizada). Fixar o
 * marcador dentro da zona dá o chute com força máxima (1,0); fixá-lo fora dela é um chute
 * errado, que o motor da partida resolve como bola para fora. Nesse caso {@link #getPower()}
 * ainda devolve uma força menor (até 0 nas extremidades), usada só no tempo de voo da bola.
 */
public final class PowerTiming {
    private static final double SLOW_MOTION_FACTOR = 0.5;
    private static final double MIN_ZONE_WIDTH = 0.10;
    private static final double MAX_ZONE_WIDTH = 0.50;

    private final double speed;
    private final double zoneWidth;
    private double position;
    private int direction = 1;
    private boolean locked;

    public PowerTiming(int finishing) {
        if (finishing < 0 || finishing > 100) {
            throw new IllegalArgumentException("Finalização deve estar entre 0 e 100.");
        }
        speed = (0.35 + (100 - finishing) * 0.012) * SLOW_MOTION_FACTOR;
        zoneWidth = MIN_ZONE_WIDTH + (MAX_ZONE_WIDTH - MIN_ZONE_WIDTH) * finishing / 100.0;
    }

    /** Posição do marcador, de 0 a 1. */
    public double getPosition() {
        return position;
    }

    public double getSpeed() {
        return speed;
    }

    /** Largura da zona central, como fração da barra (0,10 a 0,50). */
    public double getZoneWidth() {
        return zoneWidth;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isInZone() {
        return Math.abs(position - 0.5) <= zoneWidth / 2;
    }

    public void advance(double elapsedSeconds) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0) {
            throw new IllegalArgumentException("O tempo decorrido deve ser um valor não negativo.");
        }
        if (locked) {
            return;
        }
        double next = position + direction * speed * elapsedSeconds;
        while (next < 0 || next > 1) {
            if (next > 1) {
                next = 2 - next;
                direction = -1;
            } else {
                next = -next;
                direction = 1;
            }
        }
        position = next;
    }

    public void lock() {
        locked = true;
    }

    /** Força resultante (0 a 1) para a posição atual do marcador. */
    public double getPower() {
        double halfZone = zoneWidth / 2;
        double distance = Math.abs(position - 0.5);
        if (distance <= halfZone) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - (distance - halfZone) / (0.5 - halfZone));
    }
}
