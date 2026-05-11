package com.mod.archetype.registry;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

public class ClassParseException extends Exception {

    private final Identifier fileId;
    @Nullable
    private final String field;

    public ClassParseException(Identifier fileId, @Nullable String field, String message) {
        super(formatMessage(fileId, field, message));
        this.fileId = fileId;
        this.field = field;
    }

    public ClassParseException(Identifier fileId, @Nullable String field, String message, Throwable cause) {
        super(formatMessage(fileId, field, message), cause);
        this.fileId = fileId;
        this.field = field;
    }

    public Identifier getFileId() {
        return fileId;
    }

    @Nullable
    public String getField() {
        return field;
    }

    private static String formatMessage(Identifier fileId, @Nullable String field, String message) {
        if (field != null) {
            return "Error parsing class '" + fileId + "' at field '" + field + "': " + message;
        }
        return "Error parsing class '" + fileId + "': " + message;
    }
}
