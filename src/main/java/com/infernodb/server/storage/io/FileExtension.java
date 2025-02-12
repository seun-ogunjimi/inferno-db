package com.infernodb.server.storage.io;

public enum FileExtension {

    METADATA(".meta"),
    DATA(".kv"),
    COMPACT(".cmp");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    public static FileExtension fromString(String ext) {
        for (FileExtension fileExtension : values()) {
            if (fileExtension.extension.equalsIgnoreCase(ext)) {
                return fileExtension;
            }
        }
        return null; // Or throw an exception if you prefer
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return extension;
    }
}