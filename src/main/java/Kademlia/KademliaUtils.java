package Kademlia;

import Auction.Auction;
import Auction.Bid;
import BlockChain.Block;
import BlockChain.Transaction;
import Peer.Wallet;
import Utils.Utils;
import com.google.common.math.BigIntegerMath;
import com.google.protobuf.ByteString;
import kademlia.*;

import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;


public class KademliaUtils {

    private static KademliaUtils instance;
    private static Utils utils;
    private static Wallet wallet;

    private Logger LOGGER;

    private String myIP;
    private String myID;
    private String myPort;

    /* Number max of elements in kbucket */
    private final int k = 20;
    /* Degree of parallelism in network*/
    private final int alpha = 3;

    /* Bootstrap IP */
    private String bootstrapId = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private final String bootstrapIp = "localhost";
    private final String bootstrapPort = "9090";

    private KademliaUtils() {
        //String input = bootstrapIp + bootstrapPort;
        //bootstrapId = generateId(input);

        utils = Utils.getInstance();
        wallet = Wallet.getInstance();

        LOGGER = utils.getLOGGER();
    }

    public static KademliaUtils getInstance(){
        if(instance == null){
            instance = new KademliaUtils();
        }
        return instance;
    }

    public byte[] hashing(byte[] elementToHash) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] hash = digest.digest(elementToHash);
        return hash;
    }

    public void printByteInHexa(byte[] input){
        for(byte b : input){
            System.out.print(String.format("%02x ", b));
        }
        System.out.println("");
    }

    public String convertByteToBinaryString(byte[] input){

        String result;

        BigInteger inputBigInt = new BigInteger(1, input);
        result = inputBigInt.toString(2);

        // pad with leading zeros if necessary (binary string must be 160 bits)
        //while (result.length() < 160) {
        while (result.length() % 8 != 0) {
                result = "0" + result;
        }

        return result;

    }

    public String convertStringHexToBinary(String hexInput){
        byte[] binaryData = new BigInteger(hexInput, 16).toByteArray();

        return convertByteToBinaryString(binaryData);
    }

    public String generateId(String input){
        byte[] idHash = hashing(input.getBytes(StandardCharsets.UTF_8));
        String myID = convertByteToBinaryString(idHash);
        /*
        System.out.println("Id = " + input);
        System.out.print("Id hash = ");
        printByteInHexa(idHash);
        System.out.println("myID = " + myID);
        */

        return myID;
    }

    public byte[] generateIdByte(String input){
        return hashing(input.getBytes(StandardCharsets.UTF_8));
    }

    public <T> boolean searchList(T list, Node toSearch){

        Iterator<Node> iterator = null;

        if(list instanceof LinkedList){
            iterator = ((LinkedList) list).iterator();
        }else if (list instanceof PriorityQueue){
            iterator = ((PriorityQueue) list).iterator();
        }else if(list instanceof ArrayList){
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()){
            Node next = iterator.next();
            if(next.compare(toSearch)) return true;
        }

        return false;
    }

    public <T> boolean searchListNodeWrap(T list, NodeWrap toSearch){

        Iterator<NodeWrap> iterator = null;

        if(list instanceof LinkedList){
            iterator = ((LinkedList) list).iterator();
        }else if (list instanceof PriorityQueue){
            iterator = ((PriorityQueue) list).iterator();
        }else if(list instanceof ArrayList){
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()){
            NodeWrap next = iterator.next();
            if(next.compare(toSearch)) return true;
        }

        return false;
    }

    /**
     * Search for the position of a Node
     *
     * @param list -> List where to search
     * @param toSearch -> Node to search
     * @returm if node is present return the pos otherwise return -1
     */
    public <T> int searchPos(T list, Node toSearch){

        Iterator<Node> iterator = null;

        if(list instanceof LinkedList){
            iterator = ((LinkedList) list).iterator();
        }else if (list instanceof PriorityQueue){
            iterator = ((PriorityQueue) list).iterator();
        }else if(list instanceof ArrayList){
            iterator = ((ArrayList) list).iterator();
        }

        int pos = -1;
        while (iterator.hasNext()){
            Node next = iterator.next();
            pos += 1;
            if(next.compare(toSearch)) return pos;
        }

        return -1;
    }

    public <T> void printNodeList(T list){

        Iterator<Node> iterator = null;

        if(list instanceof LinkedList){
            iterator = ((LinkedList) list).iterator();
        }else if (list instanceof PriorityQueue){
            iterator = ((PriorityQueue) list).iterator();
        }else if(list instanceof ArrayList){
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()){
           LOGGER.info(iterator.next().toString());
        }
    }

    public <T> void printNodeWrapList(T list){

        Iterator<NodeWrap> iterator = null;

        if(list instanceof LinkedList){
            iterator = ((LinkedList) list).iterator();
        }else if (list instanceof PriorityQueue){
            iterator = ((PriorityQueue) list).iterator();
        }else if(list instanceof ArrayList){
            iterator = ((ArrayList) list).iterator();
        }

        while (iterator.hasNext()){
            LOGGER.info(iterator.next().getNode().toString());
        }
    }

    public BigInteger xorDistance (String targetId, String myId){
        BigInteger target = new BigInteger(targetId, 2);
        BigInteger my = new BigInteger(myId, 2);

        //System.out.println("target: " + target + " with length = " + target.bitLength() + "\nmy: " + my + " with length = " + my.bitLength() + "\ndistance: " + distance);

        return my.xor(target);
    }

    public int findDistancePos(String targetId, String myId) {

        BigInteger distance = xorDistance(targetId, myId);

        int pos = 0;

        if (BigInteger.valueOf(pos).compareTo(distance) != 0){
            pos = BigIntegerMath.log2(distance, RoundingMode.DOWN);
        }
        //System.out.println("Value of distance: " + distance + "  pos: " + pos);
        return pos;
    }

    public static Node nodeInfoToNode(NodeInfo nodeInfo){
        return new Node(nodeInfo.getIp(), nodeInfo.getPort(), nodeInfo.getId());
    }


    public static Transaction transactionProtoToTransaction(TransactionProto transactionProto){
        Transaction newTransaction = new Transaction(transactionProto);
        return newTransaction;
    }

    public static TransactionProto transactionToTransactionProto(Transaction transaction){
        SignatureProto signatureProto = SignatureProto.newBuilder()
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .setHash(transaction.getChecksum())
                .setPublicKey(ByteString.copyFrom(utils.convertPubKeytoBytes(transaction.getBuyerPublicKey())))
                .build();

        NodeInfo buyerInfo = NodeInfo.newBuilder()
                .setId(transaction.getBuyerID())
                .setIp(transaction.getBuyerIP())
                .setPort(transaction.getBuyerPort())
                .build();

        return TransactionProto.newBuilder()
                .setName(transaction.getItemName())
                .setFinalPrice(transaction.getPrice())
                .setTimestamp(utils.localDateTimeToNumber(transaction.getTimestamp()))
                .setAtive(transaction.isAtive())
                .setTopic(transaction.getTopic())
                .setAuctionPublicKey(ByteString.copyFrom(utils.convertPubKeytoBytes(transaction.getAuctioneerPublicKey())))
                .setSignature(signatureProto)
                .setBuyerInfo(buyerInfo)
                .build();

    }
    public static BlockProto blockToBlockProto(Block block){
        return BlockProto.newBuilder()
                .setId(block.getId())
                .setHash(block.getHash())
                .setNonce(block.getNonce())
                .setPrevioushash(block.getPreviousHash())
                .setTimestamp(block.getTimeStamp())
                .setTransaction(transactionToTransactionProto( block.getTransaction()))
                .build();
    }

    public Block blockProtoToBlock(BlockProto blockProto){
        Block block;
        try {
            block = new Block(blockProto.getId(), transactionProtoToTransaction(blockProto.getTransaction()));
            block.setPreviousHash(blockProto.getPrevioushash());
            block.setHash(blockProto.getHash());
            block.setNonce((int) blockProto.getNonce());
            block.setTimeStamp(blockProto.getTimestamp());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if (block == null){
            this.LOGGER.info("\nKADEMLIA-UTILS:Something went wrong building a block from a proto block, the block is empty\n");
        }

        return block;
    }

    public static AuctionProto auctionToAuctionProto(Auction auction) {
        // Convert LocalDateTime to int milliseconds
        long timestampMillis = utils.localDateTimeToNumber(auction.getDeadline());

        byte[] auctionSignature = utils.signHash(wallet.getPrivKey(), auction.getTopic());
        byte[] pubKeytoBytes = wallet.getPubKey().getEncoded();

        SignatureProto signatureProto = SignatureProto.newBuilder()
                .setSignature(ByteString.copyFrom(auctionSignature))
                .setHash(auction.getTopic())
                .setPublicKey(ByteString.copyFrom(pubKeytoBytes))
                .build();

        AuctionProto res = AuctionProto.newBuilder()
                .setName(auction.getName())
                .setCurrentBid(auction.getCurrentBid())
                .setCurrentBidder(auction.getCurrentBidder())
                .setBasePrice(auction.getBasePrice())
                .setDeadline(timestampMillis)
                .setTopic(auction.getTopic())
                .setSignature(signatureProto)
                .setSellerID(auction.getSellerID())
                .setSellerPubKey(ByteString.copyFrom(utils.convertPubKeytoBytes(auction.getSellerPubKey())))
                .build();

//        System.out.println(res.toString());
        return res;

    }


    public static Auction auctionProtoToAuction(AuctionProto auctionProto) {

        LocalDateTime localDateTime = utils.numberToLocalDateTime(auctionProto.getDeadline());

        PublicKey key = utils.convertBytestoPubKey(auctionProto.getSellerPubKey().toByteArray());
        Auction auction = new Auction(auctionProto.getName(),
                                        auctionProto.getBasePrice(),
                                        auctionProto.getCurrentBid(),
                                        auctionProto.getCurrentBidder(),
                                        localDateTime,
                                        auctionProto.getTopic(), auctionProto.getSellerID(), key);

        return auction;
    }


    /*
        Set and Get
     */

    public int getK() {
        return k;
    }

    public int getAlpha() {
        return alpha;
    }

    public String getBootstrapIp() {
        return bootstrapIp;
    }

    public String getBootstrapPort() {
        return bootstrapPort;
    }

    public String getBootstrapId() {
        return bootstrapId;
    }

    public static BigInteger generate(){
        return new BigInteger(160,new SecureRandom());
    }

    public String getMyIP() {
        return myIP;
    }

    public void setMyIP(String myIP) {
        this.myIP = myIP;
    }

    public String getMyID() {
        return myID;
    }

    public void setMyID(String myID) {
        this.myID = myID;
    }

    public String getMyPort() {
        return myPort;
    }

    public void setMyPort(String myPort) {
        this.myPort = myPort;
    }

}
