package com.modekoo.parse.enums;

import lombok.Getter;

@Getter
public enum FieldType {
    OBJECT("O"),
    LIST("L"),
    ARRAY("A"),
    STRING("S"),
    NUMBER("N"),
    LIST_COUNT("LN"),
    JSON_STRING("JS"),
    JSON_OBJECT("JO");

    private final String code;

    FieldType(String code) {
        this.code = code;
    }
}