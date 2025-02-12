package com.infernodb.core.cluster;

import java.util.Objects;

public class VirtualNode<N extends Node> extends AbstractNode<String> {

    private final N physicalNode;

    public VirtualNode(N physicalNode, int index) {
        super(Objects.requireNonNull(physicalNode, "PhysicalNode cannot be null").getId() + "#" + index,
                Objects.requireNonNull(physicalNode, "PhysicalNode cannot be null").getIpAddress(),
                physicalNode.getPort());
        this.physicalNode = physicalNode;
    }

    public N getPhysicalNode() {
        return physicalNode;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
               "id='" + getId() + '\'' +
               ", physicalNode='" + physicalNode.toString() + '\'' +
               '}';
    }
}
