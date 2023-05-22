package Kademlia;

import Auction.Auction;
import Auction.AuctionApp;
import BlockChain.*;
import Peer.Wallet;
import Utils.Utils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia.*;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Kademlia {

    private static Logger LOGGER;

    private static KBucket kBucket;
    private static KademliaUtils kademliaUtils;
    private static Utils utils;
    private static Wallet wallet;

    private static boolean insideNetwork = false;

    public static Kademlia instance;

    private Kademlia() {

        kademliaUtils = KademliaUtils.getInstance();
        utils = Utils.getInstance();
        wallet = Wallet.getInstance();

        this.LOGGER = utils.getLOGGER();
    }

    public static Kademlia getInstance() {
        if(instance == null){
            instance = new Kademlia();
        }

        return instance;
    }

    private static void closeChannel(ManagedChannel channel){
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            LOGGER.error("[Kademlia] - Exception when closing channel: " + e.getMessage());
        }
    }


    /**
     * Send request to boot Node to enter network
     */
    public void enterKademlia(){
        LOGGER.info("[Kademlia] - Sending initial request to bootstrap");
        String myIP = kademliaUtils.getMyIP();
        String myPort = kademliaUtils.getMyPort();
        String myID;

        if(!myPort.equals(kademliaUtils.getBootstrapPort())){
            long timestamp=doChallenge();

            if(timestamp == -1) {
                LOGGER.error("[Kademlia] - ERROR: Timestamp negative");
                return ;
            }


            myID = doJoin(timestamp);

            if(myID.equals("")) {
                LOGGER.error("[Kademlia] - ERROR: MyID empty");
                return ;
            }

            kademliaUtils.setMyID(myID);
            utils.setMyID(myID);

            setInsideNetwork(true);
            kBucket = new KBucket(new Node(myIP, myPort, myID));
            doFind(kademliaUtils.getMyID());

        }else {
            setInsideNetwork(true);
            kBucket = new KBucket(new Node(myIP, myPort, kademliaUtils.getBootstrapId()));

            kademliaUtils.setMyID(kademliaUtils.getBootstrapId());
            utils.setMyID(kademliaUtils.getBootstrapId());
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(Kademlia::disseminateAuction, 1, 1, TimeUnit.MINUTES);
    }

    public long doChallenge(){
        LOGGER.info("[Kademlia] - Start of Challenge rpc");
        String bootIp = kademliaUtils.getBootstrapIp();
        String bootPort = kademliaUtils.getBootstrapPort();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(bootIp, Integer.parseInt(bootPort)).usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync

        ChallengeResponse response = null;
        try {
            // Send request with a 1000ms timeout
            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).challenge(null);
        } catch (StatusRuntimeException e) {
            LOGGER.error("[KBucket] - Error sending the request! " + e.getMessage());
        }
        closeChannel(channel);

        if(response == null)
            return -1;

        return response.getTimestamp();
    }

    public String doJoin(long timestamp){
        LOGGER.info("[Kademlia] - Start of Join rpc");
        String myPort = kademliaUtils.getMyPort();
        String myIp = kademliaUtils.getMyIP();
        String bootIp = kademliaUtils.getBootstrapIp();
        String bootPort = kademliaUtils.getBootstrapPort();
        ManagedChannel channel = ManagedChannelBuilder.forAddress(bootIp, Integer.parseInt(bootPort)).usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync

        /*Begging mining*/
        String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
        int nonce = 0;
        String hash = myIp + myPort + timestamp + nonce;

        while(!hash.substring(0,Constants.DIFFICULTY).equals(zeros)){
            nonce++;
            hash = myIp + myPort + timestamp + nonce;
            try {
                hash = new String(Hex.encode (utils.hashingSHA2(hash)));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }


        JoinRequest request = JoinRequest.newBuilder().setHash(hash).setIp(myIp).setPort(myPort).setNonce(nonce).setTimestamp(timestamp).build();

        JoinResponse response = null;
        try {
            // Send request with a 1000ms timeout
            response = kademliaStub.join(request);//.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).join(request);
        } catch (StatusRuntimeException e) {
            LOGGER.error("[Kademlia] - Error sending the request! " + e.getMessage());
        }
        closeChannel(channel);

        if(response == null)
            return null;

        if(response.getHasBlock() == 1){
            Block headblock = kademliaUtils.blockProtoToBlock(response.getHeadBlock());
            BlockChain myBlockchain = BlockChain.getInstance();
            myBlockchain.addLocalMinedBlock(headblock);
        }

        return response.getId();
    }

    /**
     * Store logic
     * @param dataToStore -> Object to send
     */
    public void doStore(Object dataToStore){
        LOGGER.info("================================================================");
        if(dataToStore instanceof byte[]){
            doStoreHandleString(dataToStore.toString().getBytes());

        } else if (dataToStore.getClass() == Block.class) {
            LOGGER.info("Sending block store");
            doStoreHandleBlock((Block) dataToStore);
        }else if (dataToStore.getClass() == Auction.class){
            LOGGER.info("Sending Auction store");
            doStoreHandleAuction((Auction) dataToStore);
        }

        LOGGER.info("================================================================");
    }

    public void disseminateStore(Object dataToStore, String key){
        // Send STORE gRPC
        ArrayList[] allNodes = kBucket.getKbucket();

        for(int i = 0; i < 160; i++){
            if(!allNodes[i].isEmpty()){
                for (int x = 0; x < allNodes[i].size(); x++){
                    Node node = (Node) allNodes[i].get(x);
                    sendStoreRequest(node, key, dataToStore);
                }
            }
        }
    }

    /*
    public void doStoreSendAllBlock(Block block, String key){
        // Send STORE gRPC
        ArrayList[] allNodes = kBucket.getKbucket();

        for(int i = 0; i < 160; i++){
            if(!allNodes[i].isEmpty()){
                for (int x = 0; x < allNodes[i].size(); x++){
                    Node node = (Node) allNodes[i].get(x);
                    sendStoreRequest(node, key, block);
                }
            }
        }
    }

    public void doStoreSendAllAuctions(Auction auction, String key){
        // Send STORE gRPC
        ArrayList[] allNodes = kBucket.getKbucket();

        for(int i = 0; i < 160; i++){
            if(!allNodes[i].isEmpty()){
                for (int x = 0; x < allNodes[i].size(); x++){
                    Node node = (Node) allNodes[i].get(x);
                    sendStoreRequest(node, key, auction);
                }
            }
        }
    }
    */
    public void doStoreHandleBlock(Block block){
        byte[] key = kademliaUtils.hashing(block.getHash().getBytes());
        String keyConverted = kademliaUtils.convertByteToBinaryString(key);

        // Do the lookup process
        doFind(keyConverted);
        // Search the closest Nodes on the bucket
        LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);

        // Send STORE gRPC
        Iterator iterator = closestNodes.iterator();
        while(iterator.hasNext()){
            sendStoreRequest((Node) iterator.next(), keyConverted, block);
        }
    }

    public void doStoreHandleAuction(Auction auction){

        byte[] key = utils.hashingSHA1(utils.convertHexaStringToBytes(auction.getTopic())); //kademliaUtils.hashing();
        String keyConverted = kademliaUtils.convertByteToBinaryString(key);

        // Do the lookup process
        doFind(keyConverted);
        // Search the closest Nodes on the bucket
        LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);


        if(auction.isActive()){
            // Send STORE gRPC
            Iterator iterator = closestNodes.iterator();
            while(iterator.hasNext()){
                sendStoreRequest((Node) iterator.next(), keyConverted, auction);
            }
        }else{
            //Auction is Closed, therefore we must send a store transaction to the network

        }

    }

    public void doStoreHandleString(byte[] dataToStore){
        byte[] key = kademliaUtils.hashing(dataToStore);
        String keyConverted = kademliaUtils.convertByteToBinaryString(key);
        String valueConverted = kademliaUtils.convertByteToBinaryString(dataToStore);

        //kademliaUtils.printByteInHexa(value);

        // Do the lookup process
        doFind(keyConverted);

        // Search the closest Nodes on the bucket
        LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);

        // Send STORE gRPC
        Iterator iterator = closestNodes.iterator();
        while(iterator.hasNext()){
            sendStoreRequest((Node) iterator.next(), keyConverted, valueConverted);
        }
    }


    public static void disseminateAuction(){

        if(!isInsideNetwork()) return;

        LOGGER.info("[Kademlia] - Inside of disseminate function");

        AuctionApp auctionApp = AuctionApp.getInstance();

        HashMap<String, Auction> myAuction = auctionApp.getMyAuctions();
        shareKbucket(myAuction);
    }

    public static void shareKbucket(HashMap<String, Auction> values){

        if(values.isEmpty()){
            LOGGER.error("[Kademlia] - Storage empty");
            LOGGER.info("[Kademlia] - End of disseminate function");
            return;
        }

        for (Map.Entry<String, Auction> entry : values.entrySet()) {

            byte[] key = utils.hashingSHA1(utils.convertHexaStringToBytes(entry.getKey()));
            String keyConverted = kademliaUtils.convertByteToBinaryString(key);

            // Do the lookup process
            doFind(keyConverted);

            // Search the closest Nodes on the bucket
            LinkedList<Node> closestNodes = kBucket.getClosestNodes(keyConverted);

            // Send STORE gRPC
            Iterator iterator = closestNodes.iterator();
            while(iterator.hasNext()){
                sendStoreRequest((Node) iterator.next(), entry.getKey(), entry.getValue());
            }
        }

        LOGGER.info("[Kademlia] - End of disseminate function");
    }


    public static int sendStoreRequest(Node toSend, String key, Object dataToSend) {
        LOGGER.info("[Kademlia] - Start of STORE rpc to targetIp = " + toSend.getIp() + " targetPort = " + toSend.getPort());
        //LOGGER.info("[Kademlia] - Request info: key = " + new String(key, StandardCharsets.UTF_8) +
                        //" value = " + new String(value, StandardCharsets.UTF_8));
        Node myNode = kBucket.getMyNode();
        NodeInfo sender = NodeInfo.newBuilder()
                .setId(myNode.getId())
                .setIp(myNode.getIp())
                .setPort(myNode.getPort())
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(toSend.getIp(), Integer.parseInt(toSend.getPort()))
                                                        .usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync

        /**
         *
         * Build the request structure for each type of data we send
         * Block
         * Transaction
         * Auction
         * Bid
         * etc...
         */
        StoreRequest request = null;
        if (dataToSend.getClass() == Block.class) {
            request = StoreRequest.newBuilder()
                    .setSender(sender)
                    .setKey(key)
                    .setBlock(KademliaUtils.blockToBlockProto((Block) dataToSend))
                    .build();
        } else if (dataToSend.getClass() == Auction.class) {
            request = StoreRequest.newBuilder()
                    .setSender(sender)
                    .setKey(key)
                    .setAuction(KademliaUtils.auctionToAuctionProto((Auction) dataToSend))
                    .build();
        }

        if(request == null){
            LOGGER.info("Something went wrong, Request is empty");
            closeChannel(channel);
            return 0;
        }

        StoreResponse response = null;
        try {
            response = kademliaStub.withDeadlineAfter(300000, TimeUnit.MILLISECONDS).store(request);

        } catch (StatusRuntimeException e) {
            LOGGER.error("[Kademlia] - Error sending the STORE request! " + e.getMessage());
        }

        closeChannel(channel);
        
        if(response != null && response.getSuccess()) return 1;

        return 0;
    }




    /*END: Store Logic*/

    public void doFindValue(String hashBlock){
        LOGGER.info("================================================================");

        byte[] keyBytes = kademliaUtils.hashing(hashBlock.getBytes());
        String key = kademliaUtils.convertByteToBinaryString(keyBytes);

        LOGGER.info("[Kademlia] - Start of FIND_VALUE rpc to key = " + key);

        PriorityQueue<NodeWrap> shortlist = convertListToPriorityQueue(kBucket.getClosestNodes(key), key);

        if(shortlist.isEmpty()){
            LOGGER.error("[Kademlia] - Bucket Empty");
            return;
        }

        LinkedList<Node> probedNodes = new LinkedList<>();
        PriorityQueue<NodeWrap> closestNodes = new PriorityQueue<>(new NodeWrapComparator());

        int alpha = kademliaUtils.getAlpha();
        int k = kademliaUtils.getK();


        //   While there are still nodes to send a request
        //       and we didn't probed K nodes
        //       and alpha is bigger than 0 ...

        boolean iHaveValue = false;
        while(probedNodes.size() < k && shortlist.size() > 0 && alpha > 0){

            // Select alpha nodes from shortlist that they haven´t already been contacted
            LinkedList<Node> toSend = new LinkedList<>();

            LOGGER.info("[Kademlia] - Searching for alpha = " + alpha + ":");

            while (toSend.size() != kademliaUtils.getAlpha() && shortlist.size() > 0){
                Node toInsert = shortlist.poll().getNode();
                if(!kademliaUtils.searchList(probedNodes, toInsert)){ toSend.add(toInsert); }
            }

            LOGGER.info("[Kademlia] - Nodes selected to search: ");
            kademliaUtils.printNodeList(toSend);

            // Send request to the alpha nodes
            while (toSend.size() > 0){
                Node nodeToSend = toSend.removeFirst();

                LOGGER.info("[Kademlia] - Sending request to " + nodeToSend.toString());

                LinkedList<Node> response = sendFindValueRequest(nodeToSend.getIp(), nodeToSend.getPort(), key);

                if(response == null){ // We got the value!!!
                    LOGGER.info("[Kademlia] - End of FIND_VALUE rpc to key = " + key);
                    LOGGER.info("================================================================");

                    iHaveValue = true;
                    break;
                }

                //If response success update probedNodes()
                probedNodes.add(nodeToSend);

                // With the nodes responses fill and update shortlist
                // Update closestNodes seen so far
                while (!response.isEmpty()){
                    NodeWrap nodeWrap = new NodeWrap(response.remove(), key);

                    shortlist.add(nodeWrap);

                    if(!kademliaUtils.searchListNodeWrap(closestNodes, nodeWrap)) closestNodes.add(nodeWrap);
                }
            }

            if(iHaveValue) break;

            alpha--;
        }

        LOGGER.info("[Kademlia] - End of FIND_VALUE rpc to key = " + key);

        LOGGER.info("[Kademlia] - Results of the FIND_VALUE rpc: ");

        kademliaUtils.printNodeWrapList(closestNodes);

        Iterator<NodeWrap> iterator = closestNodes.iterator();
        while (iterator.hasNext()){
            Node toAdd = iterator.next().getNode();
            kBucket.insertNode(toAdd.getIp(), toAdd.getId(), toAdd.getPort());
        }

        LOGGER.info("================================================================");
    }


    private LinkedList sendFindValueRequest(String host, String port, String key) {
        //System.out.println("Sending FindValue request...");

        Node myNode = kBucket.getMyNode();

        NodeInfo sender = NodeInfo.newBuilder()
                .setId(myNode.getId())
                .setIp(myNode.getIp())
                .setPort(myNode.getPort())
                .build();

        FindValueRequest findRequest = FindValueRequest.newBuilder()
                .setTargetKey(key)
                .setSender(sender)
                .build();

        /* Node to send the request */
        Node node = new Node(host, port, "");

        ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getIp(), Integer.parseInt(node.getPort())).usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync


        // Synch
        Iterator<FindValueResponse> response;

        LinkedList<Node> responseNodes  = new LinkedList<>();

        try {
            // Send request with a 1000ms timeout
            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).findValue(findRequest);
            LOGGER.info("[Kademlia] - Result for the request: ");

            while (response.hasNext()){
                FindValueResponse nextResponse = response.next();

                if(nextResponse.getNodeOrValueCase() == FindValueResponse.NodeOrValueCase.NODE){

                    NodeInfo nodeResponse = nextResponse.getNode();
                    Node toInsert = new Node(nodeResponse.getIp(), nodeResponse.getPort(), nodeResponse.getId());
                    responseNodes.add(toInsert);
                    LOGGER.info(toInsert.toString());

                }else if(nextResponse.getNodeOrValueCase() == FindValueResponse.NodeOrValueCase.BLOCK){
                    LOGGER.info("[kademlia] - The value is: " + nextResponse.getBlock());

                    // TODO: Implement here what to do with the received block!!
                    //System.out.println(nextResponse.getBlock());
                    //kBucket.storeValues(targetValue, nextResponse.getValue());
                    closeChannel(channel);
                    return null;

                }else{
                    LOGGER.error("[Kademlia] - Error: FindValueResponse without value or nodes");
                }
            }
        } catch (StatusRuntimeException e) {
            LOGGER.error("[Kademlia] - Error sending the request! " + e.getMessage());
        }

        closeChannel(channel);

        return responseNodes;
    }


    public static void doFind(String targetId){

        LOGGER.info("================================================================");

        LOGGER.info("[Kademlia] - Start of FIND_NODE rpc to targetId = " + targetId);

        PriorityQueue<NodeWrap> shortlist = convertListToPriorityQueue(kBucket.getClosestNodes(targetId), targetId);

        if(shortlist.isEmpty()){
            LOGGER.error("[Kademlia] - Bucket Empty");
            return;
        }

        LinkedList<Node> probedNodes = new LinkedList<>();
        PriorityQueue<NodeWrap> closestNodes = new PriorityQueue<>(new NodeWrapComparator());

        int alpha = kademliaUtils.getAlpha();
        int k = kademliaUtils.getK();

        /*
            While there are still nodes to send a request
                and we didn't probed K nodes
                and alpha is bigger than 0 ...
         */
        while(probedNodes.size() < k && shortlist.size() > 0 && alpha > 0){

            // Select alpha nodes from shortlist that they haven´t already been contacted
            LinkedList<Node> toSend = new LinkedList<>();

            LOGGER.info("[Kademlia] - Searching for alpha = " + alpha + ":");

            while (toSend.size() != kademliaUtils.getAlpha() && shortlist.size() > 0){
                Node toInsert = shortlist.poll().getNode();
                if(!kademliaUtils.searchList(probedNodes, toInsert)){ toSend.add(toInsert); }
            }

            LOGGER.info("[Kademlia] - Nodes selected to search: ");
            kademliaUtils.printNodeList(toSend);

            // Send request to the alpha nodes
            while (toSend.size() > 0){
                Node nodeToSend = toSend.removeFirst();

                LOGGER.info("[Kademlia] - Sending request to " + nodeToSend.toString());

                LinkedList<Node> response = sendFindRequest(nodeToSend.getIp(), nodeToSend.getPort(), targetId);

                //If response success update probedNodes()
                probedNodes.add(nodeToSend);

                // With the nodes responses fill and update shortlist
                // Update closestNodes seen so far
                while (!response.isEmpty()){
                    NodeWrap nodeWrap = new NodeWrap(response.remove(), targetId);

                    shortlist.add(nodeWrap);

                    if(!kademliaUtils.searchListNodeWrap(closestNodes, nodeWrap)) closestNodes.add(nodeWrap);
                }
            }

            alpha--;
        }

        LOGGER.info("[Kademlia] - End of FIND_NODE rpc to targetId = " + targetId);

        LOGGER.info("[Kademlia] - Results of the FIND_NODE rpc: ");

        kademliaUtils.printNodeWrapList(closestNodes);

        Iterator<NodeWrap> iterator = closestNodes.iterator();
        while (iterator.hasNext()){
            Node toAdd = iterator.next().getNode();
            kBucket.insertNode(toAdd.getIp(), toAdd.getId(), toAdd.getPort());
        }

        LOGGER.info("================================================================");
    }

    private static LinkedList sendFindRequest(String host, String port, String targetId) {

        //System.out.println("Sending FindNode request...");

        Node myNode = kBucket.getMyNode();

        NodeInfo sender = NodeInfo.newBuilder()
                                    .setId(myNode.getId())
                                    .setIp(myNode.getIp())
                                    .setPort(myNode.getPort())
                                    .build();

        FindNodeRequest findRequest = FindNodeRequest.newBuilder()
                                            .setTargetId(targetId)
                                            .setSender(sender)
                                            .build();

        /* Node to send the request */
        Node node = new Node(host, port, "");

        ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getIp(), Integer.parseInt(node.getPort())).usePlaintext().build();

        KademliaGrpc.KademliaBlockingStub kademliaStub = KademliaGrpc.newBlockingStub(channel); //Sync
        //KademliaGrpc.KademliaStub kademliaStub = KademliaGrpc.newStub(channel); // Async


        /*
        // Async
        kademliaStub.findNode(findRequest, new StreamObserver<FindResponse>(){

            @Override
            public void onNext(FindResponse findResponse) {
                NodeInfo nodeResponse = findResponse.getNode();

                logger.info("\nALPHA: " + alpha + "\nIP: " + nodeResponse.getIp() + "\nID: " + nodeResponse.getId() + "\nPort: " + nodeResponse.getPort() + "\n");

                if(!alreadySeen.contains(nodeResponse)){
                    alreadySeen.add(nodeResponse);
                    if(alpha != 0) sendFindRequest(alpha - 1,  nodeResponse.getIp(), Integer.parseInt(nodeResponse.getPort()), targetId);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error!!");
            }

            @Override
            public void onCompleted() {
                System.out.println("Closing Channel....");
                closeChannel(channel);
            }
        });
        */


        // Synch
        Iterator<FindNodeResponse> response;

        LinkedList<Node> responseNodes  = new LinkedList<>();

        try {
            // Send request with a 1000ms timeout
            response = kademliaStub.withDeadlineAfter(1000, TimeUnit.MILLISECONDS).findNode(findRequest);
            LOGGER.info("[Kademlia] - Result for the request: ");
            while (response.hasNext()){
                NodeInfo nodeResponse = response.next().getNode();
                Node toInsert = new Node(nodeResponse.getIp(), nodeResponse.getPort(), nodeResponse.getId());
                responseNodes.add(toInsert);
                LOGGER.info(toInsert.toString());
            }
        } catch (StatusRuntimeException e) {
            LOGGER.error("[Kademlia] - Error sending the request! " + e.getMessage());
        }

        closeChannel(channel);

        return responseNodes;
    }


    // ==========================================================================
    /*
        UTILS
     */

    private static PriorityQueue<NodeWrap> convertListToPriorityQueue(LinkedList<Node> linkedList, String targetId){

        PriorityQueue<NodeWrap> result = new PriorityQueue<>(new NodeWrapComparator());

        Node node;
        while(!linkedList.isEmpty()){
            node = linkedList.remove();
            result.add(new NodeWrap(node, targetId));
        }

        return result;
    }

    // ==========================================================================
    /*
        Interface to access KBucket functions to test
     */

    public LinkedList<Node> getClosestNodes(String targetId){
        return kBucket.getClosestNodes(targetId);
    }

    public LinkedList<Node> getClosestNodes(String targetId, Node requesterNode){
        return kBucket.getClosestNodes(targetId, requesterNode);
    }

    public void generateNodes(int numberOfNodes){
        kBucket.generateNode(numberOfNodes);
    }

    public void deleteAllNodes(){
        kBucket.deleteAllNodes();
    }

    public void insertNode(String nodeIP, String nodeID, String nodePort){
        kBucket.insertNode(nodeIP, nodeID, nodePort);
    }

    public void printKBucket(){
        kBucket.printKBucket();
    }

    public static boolean isInsideNetwork() {
        return insideNetwork;
    }

    public void setInsideNetwork(boolean insideNetwork) {
        this.insideNetwork = insideNetwork;
    }

    public boolean searchKbucket(String id) {
        // Send STORE gRPC
        ArrayList[] allNodes = kBucket.getKbucket();
        for (int i = 0; i < 160; i++) {
            if (!allNodes[i].isEmpty()) {
                for (int x = 0; x < allNodes[i].size(); x++) {
                    Node node = (Node) allNodes[i].get(x);
                    if (node.getId().equals(id))
                        return true;


                }
            }

        }
        return false;
    }
}

