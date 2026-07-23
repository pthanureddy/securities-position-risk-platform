package com.pthanureddy.markets.domain;

public enum Side {
    BUY(1),
    SELL(-1);

    private final int multiplier;

    Side(int multiplier) {
        this.multiplier = multiplier;
    }

    public int multiplier() {
        return multiplier;
    }
}

