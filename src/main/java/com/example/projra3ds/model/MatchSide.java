package com.example.projra3ds.model;

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
