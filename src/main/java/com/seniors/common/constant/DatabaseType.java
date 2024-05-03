package com.seniors.common.constant;

import lombok.Getter;

@Getter
public enum DatabaseType {
    MASTER("MASTER"),
    SLAVE("SLAVE");

    private final String message;

    DatabaseType(String message) {
        this.message = message;
    }
}


