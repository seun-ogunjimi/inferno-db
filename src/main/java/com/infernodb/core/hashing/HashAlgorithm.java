package com.infernodb.core.hashing;

public enum HashAlgorithm {
    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256");
    private final String algorithm;

    HashAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}