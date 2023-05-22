package Kademlia;

import java.math.BigInteger;
import java.util.Comparator;

class Node{

    private String ip;
    private String port;
    private String id; /* Binary mode */

    public Node(String ip, String port, String id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public boolean compare(Node otherNode){

        if(this.getId().equals(otherNode.getId()) &&
            this.getIp().equals(otherNode.getIp()) &&
            this.getPort().equals(otherNode.getPort()))
        {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Node{" + "ip='" + ip + '\'' + ", port='" + port + '\'' + ", id='" + id + '\'' + '}';
    }

}

class NodeWrap{

    private Node node;
    private KademliaUtils kademliaUtils;
    private BigInteger distance;


    public NodeWrap(Node node, String targetID) {
        this.node = node;
        this.kademliaUtils = KademliaUtils.getInstance();
        this.distance = kademliaUtils.xorDistance(targetID, node.getId());
    }

    public Node getNode() {
        return node;
    }

    public BigInteger getDistance() {
        return distance;
    }

    public boolean compare(NodeWrap otherNode){

        if(this.node.getId().equals(otherNode.node.getId()) &&
                this.node.getIp().equals(otherNode.node.getIp()) &&
                this.node.getPort().equals(otherNode.node.getPort()) &&
                this.getDistance().equals(otherNode.getDistance()))
        {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "NodeWrap{" + "node=" + node.toString() + ", distance=" + distance + '}';
    }
}

class NodeWrapComparator implements Comparator<NodeWrap>{

    @Override
    public int compare(NodeWrap node1, NodeWrap node2) {
        return node1.getDistance().compareTo(node2.getDistance());
    }

}