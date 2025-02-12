package com.infernodb.core.hashing;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class ConsistentHashing<T extends HashKey<String>> {
    private static final int RING_SIZE = 1000;
    private final int ringSize;
    private final TreeMap<Integer, T> ring = new TreeMap<>();
    private final int numberOfVirtualNodes;
    private final HashFunction<String> hashFunction;

    public ConsistentHashing(int numberOfVirtualNodes) throws NoSuchAlgorithmException {
        this(null, numberOfVirtualNodes, HashAlgorithm.MD5, RING_SIZE);
    }

    public ConsistentHashing(int numberOfVirtualNodes, HashAlgorithm hashAlgorithm) throws NoSuchAlgorithmException {
        this(null, numberOfVirtualNodes, hashAlgorithm, RING_SIZE);
    }

    public ConsistentHashing(T[] nodes, int numberOfVirtualNodes, HashAlgorithm hashAlgorithm, int ringSize) throws NoSuchAlgorithmException {
        this.numberOfVirtualNodes = Math.max(numberOfVirtualNodes, 1);
        this.ringSize = Math.max(ringSize, RING_SIZE);
        this.hashFunction = HashFunction.of(hashAlgorithm);
        Optional.ofNullable(nodes).stream().flatMap(Arrays::stream).forEach(this::addNode);
    }

    public void addNode(T node) {
        Objects.requireNonNull(node, "Node cannot be null");
        for (var i = 0; i < numberOfVirtualNodes; i++) {
            var hash = computeHash(getNodeKey(node, i));
            ring.put(hash, node);
        }
    }

    public void removeNode(T node) {
        Objects.requireNonNull(node, "Node cannot be null");
        for (var i = 0; i < numberOfVirtualNodes; i++) {
            var hash = computeHash(getNodeKey(node, i));
            ring.remove(hash);
        }
    }

    public T getNode(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (ring.isEmpty()) {
            return null;
        }
        var hash = computeHash(key);
        /*var tailMap = ring.tailMap(hash);
        var nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);*/

        var entry = ring.ceilingEntry(hash);
        return Optional.ofNullable(entry).orElseGet(ring::firstEntry).getValue();
    }

    private String getNodeKey(T node, int index) {
        var key = Objects.requireNonNull(node.getKey(), "Node key cannot be null");
        return key + "#" + index;
    }

    private int computeHash(String key) {
        return Math.abs(hashFunction.hash(key) % ringSize);
    }

    public T get(Object key) {
        Objects.requireNonNull(key, "Key cannot be null");
        if (ring.isEmpty()) {
            return null;
        }
        int hash = computeHash(key.toString());
        T value = ring.get(hash);
        if (value == null) {
            var tailMap = ring.tailMap(hash);
            value = (tailMap.isEmpty() ? ring.firstEntry() : tailMap.firstEntry()).getValue();
        }
        return value;
    }
}
