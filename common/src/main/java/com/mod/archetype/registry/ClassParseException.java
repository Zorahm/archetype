package com.mod.archetype.registry;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class ClassParseException extends Exception {

    private final ResourceLocation fileId;
    @Nullable
    private final String field;

    public ClassParseException(ResourceLocation fileId, @Nullable String field, String message) {
        super(formatMessage(fileId, field, message));
        this.fileId = fileId;
        this.field = field;
    }

    public ClassParseException(ResourceLocation fileId, @Nullable String field, String message, Throwable cause) {
        super(formatMessage(fileId, field, message), cause);
        this.fileId = fileId;
        this.field = field;
    }

    public ResourceLocation getFileId() {
        return fileId;
    }

    @Nullable
    public String getField() {
        return field;
    }

    private static String formatMessage(ResourceLocation fileId, @Nullable String field, String message) {
        if (field != null) {
            return "Error parsing class '" + fileId + "' at field '" + field + "': " + message;
        }
        return "Error parsing class '" + fileId + "': " + message;
    }
}
