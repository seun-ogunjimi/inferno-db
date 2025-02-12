package com.infernodb.core.cluster;

import java.util.SortedMap;
import java.util.TreeMap;

public class Cluster {

    /**
     * Map is used for consistent hashing implementation.
     * To store the hash of the node and the node itself
     * and the hash is used to find the node in the ring.
     * TreeMap is used because it is an implementation of sorted map.
     **/
    private SortedMap<Integer, Node> hashingMap = new TreeMap<>();


}
