package com.infernodb.core.hashing;

@FunctionalInterface
public interface HashKey<T> {
    T getKey();
}
