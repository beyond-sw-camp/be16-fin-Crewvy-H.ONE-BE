package com.crewvy.common.entity;

public enum Bool {
    FALSE,
    TRUE;

    public Boolean toBoolean() {
        return this == TRUE;
    }

    public static Bool fromBoolean(Boolean bool) {
        return bool ? TRUE : FALSE;
    }
}
