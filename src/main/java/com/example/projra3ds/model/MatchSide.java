package com.example.projra3ds.model;

/**
 * Lado de um time dentro de uma partida: o time do usuário ou o time controlado pela CPU.
 */
public enum MatchSide {
    USER,
    CPU;

    public static MatchSide fromCpuFlag(boolean cpu) {
        return cpu ? CPU : USER;
    }

    public boolean isCpu() {
        return this == CPU;
    }
}
