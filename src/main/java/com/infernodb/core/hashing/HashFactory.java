package com.infernodb.core.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HashFactory {

    private final MessageDigest md;

    private HashFactory(HashAlgorithm algorithm) throws NoSuchAlgorithmException {
        md = java.security.MessageDigest.getInstance(algorithm.getAlgorithm());
    }

    public static HashFunction<String> of(HashAlgorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new IllegalArgumentException("Hash algorithm cannot be null");
        }
        return new HashFactory(algorithm)::hash;
    }

    public int hash(String key) {
        md.reset();
        byte[] bytes = md.digest(key.getBytes());
        return Arrays.hashCode(bytes);
    }
}
