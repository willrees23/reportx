package com.github.willrees23.reportx.core.util;

import java.time.Instant;
import java.time.ZoneId;

@FunctionalInterface
public interface Clock {

    Instant now();

    static Clock system() {
        return Instant::now;
    }

    static Clock fixed(Instant instant) {
        java.time.Clock fixed = java.time.Clock.fixed(instant, ZoneId.systemDefault());
        return fixed::instant;
    }
}
