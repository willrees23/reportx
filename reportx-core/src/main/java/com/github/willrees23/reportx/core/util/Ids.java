package com.github.willrees23.reportx.core.util;

import java.util.Objects;
import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static String shortCaseId(UUID id) {
        Objects.requireNonNull(id, "id");
        return id.toString().substring(0, 8);
    }
}
