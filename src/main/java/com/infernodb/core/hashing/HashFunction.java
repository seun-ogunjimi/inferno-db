package com.infernodb.core.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@FunctionalInterface
public interface HashFunction<T> {

    static <T> HashFunction<T> of(HashAlgorithm algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Hash algorithm cannot be null");
        }
        return new HashFunctionImpl<>(algorithm);
    }

    int hash(T key);

    final class HashFunctionImpl<T> implements HashFunction<T> {

        private final MessageDigest md;

        private HashFunctionImpl(HashAlgorithm algorithm) throws NoSuchAlgorithmException {
            md = java.security.MessageDigest.getInstance(algorithm.getAlgorithm());
        }

        public int hash(T key) {
            var keyBytes = (key instanceof byte[] k) ? k : key.toString().getBytes();
            md.reset();
            byte[] bytes = md.digest(keyBytes);
            return Arrays.hashCode(bytes);
        }
    }
}
