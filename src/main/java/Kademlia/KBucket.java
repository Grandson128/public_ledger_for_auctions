package Kademlia;

import Utils.Utils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia.KademliaGrpc;
import kademlia.NodeInfo;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KBucket {

    private KademliaUtils kademliaUtils;
    private Utils utils;
    private Logger LOGGER;
    private ArrayList[] kbucket = new ArrayList[160];
    private Node myNode;

    public KBucket(Node myNode) {

        this.myNode = myNode;
        kademliaUtils = KademliaUtils.getInstance();
        utils = Utils.getInstance();

        LOGGER = utils.getLOGGER();

        /* Start kbucket with empty list of Node */
        for(int i = 0; i < 160; i++) kbucket[i] = new ArrayList<Node>();

        if(!myNode.getId().equals(kademliaUtils.getBootstrapId())) {
            insertNode(kademliaUtils.getBootstrapIp(), kademliaUtils.getBootstrapId(), kademliaUtils.getBootstrapPort());
        }
    }


    // ==================================================================

    /* TODO: Function only to test */
    public synchronized void deleteAllNodes(){
        for(int i = 0; i < 160; i++) kbucket[i].clear();
    }

    public void generateNode(int numberOfNodes){

        for(int i = 0; i < numberOfNodes; i++){
            String IP = "localhost" + i;
            String Port = "170" + i;
            String ID = new BigInteger(160,new SecureRandom()).toString(2);

            insertNode(IP, ID, Port);
        }
    }


    /**
     * Check if a node is alive or not
     *
     * @param targetIp -> IP of the target
     * @param targetPort -> Port of the target
     * @return True if node respond and false if there isn't a response
     */
    public boolean doPing(String targetIp, String targetPort) {
        LOGGER.info("[KBucket] - Start of PING rpc to targetIp = " + targetIp + " targetPort = " + targetPort);

        NodeInfo sender = NodeInfo.newBuilder()
                .setId(myNode.getId())
                .setIp(myNode.getIp())
                .setPort(myNode.getPort())
                .build();


        // TODO: Change this : Plaintext without TLS !!
        ManagedChannel channel = ManagedChannelBuilder.forAddress(targetIp, Integer.parseInt(targetPort)).usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync

        boolean result = false;

        try {
            NodeInfo response;

            // Send request with a 1000ms timeout
            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).ping(sender);

            LOGGER.info("[KBucket] - Result for the ping request: ");
            LOGGER.info("[KBucket] - Ip = " + response.getIp() + " port = " + response.getPort() +" Id = " + response.getId());

            if(myNode.getPort().equals(response.getPort())
                    && myNode.getIp().equals(response.getIp())
                    && myNode.getId().equals(response.getId())){
                result = true;
            }

        } catch (StatusRuntimeException e) {
            LOGGER.error("[KBucket] - Error sending the request! " + e.getMessage());
        }

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            LOGGER.error("[KBucket] - Exception when closing channel: " + e.getMessage());
        }

        return result;
    }

    public synchronized void insertNode(String nodeIP, String nodeID, String nodePort){

        LOGGER.info("[KBucket] - Inserting Node...");
        Node nodeToInsert = new Node(nodeIP, nodePort, nodeID);

        int distance = kademliaUtils.findDistancePos(nodeToInsert.getId(), myNode.getId());

        /* If node already exists on the bucket, move it to the end of the bucket */
        if(kademliaUtils.searchList(kbucket[distance], nodeToInsert)){
            LOGGER.info("[KBucket] - Node already on the bucket, lets move the node to the end of bucket!");

            int pos = kademliaUtils.searchPos(kbucket[distance], nodeToInsert);

            kbucket[distance].remove(pos);
            kbucket[distance].add(nodeToInsert);

            return;
        }

        /*
        *  If bucket is full ping the first node on the bucket:
        *  1 - If the node fails to respond, remove it and add the new node
        *  2 - If the node answers, the new node is ignored
        */
        if(kbucket[distance].size() >= kademliaUtils.getK()){
            LOGGER.info("[KBucket] - Bucket Full, lets ping the first node on the bucket!");

            Node toPing = (Node) kbucket[distance].get(0);

            if(!doPing(toPing.getIp(), toPing.getPort())){
                LOGGER.info("[KBucket] - The head of the bucket doesn't answer, removing and heading the new node");
                kbucket[distance].remove(0);
                kbucket[distance].add(nodeToInsert);
            }

            return;
        }

        kbucket[distance].add(nodeToInsert);
    }

    /**
     * Return the closestNodes to the targetId or key
     *
     * @param target - Id to search
     * @return linkedList with all closestNodes
     */
    public synchronized LinkedList<Node> getClosestNodes(String target){

        LinkedList<Node> closestNodes = new LinkedList<Node>();

        int distance = kademliaUtils.findDistancePos(target, myNode.getId());
        int alphaValues = kademliaUtils.getAlpha();

        if(kbucket[distance].size() < alphaValues){

            /* Copy all available Nodes */
            for(int i = 0; i < kbucket[distance].size(); i++){
                Node element = (Node) kbucket[distance].get(i);

                //if(element.getId().equals(requesterID)) continue;

                closestNodes.add(element);
                alphaValues--;
            }


            /* Search for more Nodes if needed */
            for(int i = 1; alphaValues != 0 && (distance-i >= 0 || distance+i < 160); i++){

                /* lower */
                if (distance-i >= 0 && kbucket[distance-i].size() != 0)
                {
                    //System.out.println("[getClosestNodes] - Searching on distance: " + (distance-i));
                    for(int x = 0; x < kbucket[distance-i].size() && alphaValues != 0; x++){
                        //System.out.println("[getClosestNodes] - I found one!!");
                        Node element = (Node) kbucket[distance-i].get(x);

                        //if(element.getId().equals(requesterID)) continue;

                        closestNodes.add(element);
                        alphaValues--;
                    }
                }

                if(alphaValues == 0) break;

                /* higher */
                if (distance+i < 160 && kbucket[distance+i].size() != 0)
                {
                    //System.out.println("[getClosestNodes] - Searching on distance: " + (distance+i));
                    for(int x = 0; x < kbucket[distance+i].size() && alphaValues != 0; x++){
                        //System.out.println("[getClosestNodes] - I found one!!");
                        Node element = (Node) kbucket[distance+i].get(x);

                        //if(element.getId().equals(requesterID)) continue;

                        closestNodes.add(element);
                        alphaValues--;
                    }
                }
            }

            if(alphaValues == 0)    return closestNodes;

            LOGGER.info("[getClosestNodes] - Cant find " + kademliaUtils.getAlpha() + " values, " +
                                    "only " + (kademliaUtils.getAlpha() - alphaValues) + " values");

            return closestNodes;
        }

        for(int i = 0; i < alphaValues; i++){
            Node element = (Node) kbucket[distance].get(i);

            //if(element.getId().equals(requesterID)) continue;

            closestNodes.add(element);
        }

        return closestNodes;
    }

    /**
     * Return the closestNodes to the targetId
     * without the requester Node if it is present.
     *
     * @param targetId - Id to search
     * @param requesterNode - Node that sent the request
     * @return linkedList with all closestNodes
     */
    public synchronized LinkedList<Node> getClosestNodes(String targetId, Node requesterNode){

        LinkedList<Node> closestNodes = new LinkedList<Node>();

        int distance = kademliaUtils.findDistancePos(targetId, myNode.getId());
        int alphaValues = kademliaUtils.getAlpha();

        int listSize = kbucket[distance].size();
        if(kademliaUtils.searchList(kbucket[distance], requesterNode)) listSize--;

        if(listSize < alphaValues){

            /* Copy all available Nodes */
            for(int i = 0; i < kbucket[distance].size(); i++){
                Node element = (Node) kbucket[distance].get(i);

                if(element.compare(requesterNode)) continue;

                closestNodes.add(element);
                alphaValues--;
            }


            /* Search for more Nodes if needed */
            for(int i = 1; alphaValues != 0 && (distance-i >= 0 || distance+i < 160); i++){

                /* lower */
                if (distance-i >= 0 && kbucket[distance-i].size() != 0)
                {
                    //System.out.println("[getClosestNodes] - Searching on distance: " + (distance-i));
                    for(int x = 0; x < kbucket[distance-i].size() && alphaValues != 0; x++){
                        //System.out.println("[getClosestNodes] - I found one!!");
                        Node element = (Node) kbucket[distance-i].get(x);

                        if(element.compare(requesterNode)) continue;

                        closestNodes.add(element);
                        alphaValues--;
                    }
                }

                if(alphaValues == 0) break;

                /* higher */
                if (distance+i < 160 && kbucket[distance+i].size() != 0)
                {
                    //System.out.println("[getClosestNodes] - Searching on distance: " + (distance+i));
                    for(int x = 0; x < kbucket[distance+i].size() && alphaValues != 0; x++){
                        //System.out.println("[getClosestNodes] - I found one!!");
                        Node element = (Node) kbucket[distance+i].get(x);

                        if(element.compare(requesterNode)) continue;

                        closestNodes.add(element);
                        alphaValues--;
                    }
                }
            }

            if(alphaValues == 0)    return closestNodes;

            LOGGER.info("[getClosestNodes] - Cant find " + kademliaUtils.getAlpha() + " values, " +
                                    "only " + (kademliaUtils.getAlpha() - alphaValues) + " values");

            return closestNodes;
        }

        for(int i = 0; i < alphaValues; i++){
            Node element = (Node) kbucket[distance].get(i);

            if(element.compare(requesterNode)) continue;

            closestNodes.add(element);
        }

        return closestNodes;
    }

    public synchronized void printKBucket(){

        System.out.println("\n============ KBUCKET ============ ");

        for(int i = 0; i < 160; i++){

            if(!kbucket[i].isEmpty()){
                System.out.println("\u001B[34m" + "Distance " + i + "\u001B[0m");
                for (int x = 0; x < kbucket[i].size(); x++){
                    Node node = (Node) kbucket[i].get(x);
                    System.out.println(node.toString());
                }
                System.out.println();
            }
        }

        System.out.println("=================================");
    }

    public Node getMyNode() {
        return myNode;
    }

    public ArrayList[] getKbucket() {
        return kbucket;
    }

}
