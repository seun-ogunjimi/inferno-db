package com.infernodb.core.cluster;

public class PhysicalNode extends AbstractNode<String> {

    public PhysicalNode(String id, String ipAddress, int port) {
        super(id, ipAddress, port);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
