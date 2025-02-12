package com.infernodb.core.cluster;

public interface Node<I> {

    I getId();

    String getIpAddress();

    int getPort();

    boolean isAlive();

}
