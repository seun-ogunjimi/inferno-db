package com.infernodb.core.cluster;

public class AbstractNode<I> implements Node<I> {
    private I id;
    private String ipAddress;
    private int port;
    private boolean isAlive;

    public AbstractNode(I id, String ipAddress, int port) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public I getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public boolean isAlive() {
        return isAlive;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
               "id='" + id + '\'' +
               ", ipAddress='" + ipAddress + '\'' +
               ", port=" + port +
               '}';
    }
}
