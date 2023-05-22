package Kademlia;

import Auction.Auction;
import BlockChain.Transaction;
import BlockChain.Constants;
import Auction.Bid;
import Auction.AuctionApp;
import BlockChain.Block;
import BlockChain.BlockChain;
import Utils.Utils;
import io.grpc.stub.StreamObserver;
import kademlia.*;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class KademliaService extends KademliaGrpc.KademliaImplBase{

    private Logger LOGGER;

    Kademlia kademlia;
    KademliaUtils kademliaUtils;
    Utils utils;
    BlockChain myBlockChain;
    AuctionApp auctionApp;

    public KademliaService(Kademlia kademlia) {
        this.kademlia = kademlia;
        this.kademliaUtils = KademliaUtils.getInstance();
        this.utils = Utils.getInstance();
        this.myBlockChain = BlockChain.getInstance();
        this.auctionApp = AuctionApp.getInstance();
        LOGGER = utils.getLOGGER();
    }

    @Override
    public void ping(NodeInfo request, StreamObserver<NodeInfo> responseObserver) {

        if(!kademlia.isInsideNetwork()) return;

        LOGGER.info("[KademliaService] - Inside Ping request!");

        NodeInfo.Builder response = NodeInfo.newBuilder();

        response.setId(request.getId());
        response.setIp(request.getIp());
        response.setPort(request.getPort());

        responseObserver.onNext(response.build()); // Build response and send
        responseObserver.onCompleted(); // End connection
    }

    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {

        if(!kademlia.isInsideNetwork()) return;

        LOGGER.info("[KademliaService] - Inside Store request!");

        if (request.getBlockOrAuctionCase() == StoreRequest.BlockOrAuctionCase.BLOCK) {
            LOGGER.info("Received a block from " + request.getSender().getPort());
            Block recvBlock = kademliaUtils.blockProtoToBlock(request.getBlock());

            if (utils.verifySignature(recvBlock.getTransaction().getSignature(), recvBlock.getTransaction().getChecksum(), recvBlock.getTransaction().getBuyerPublicKey()) ){
                if(!myBlockChain.searchBlock(recvBlock)){
                    LOGGER.info("I don't have this block! " + request.getSender().getPort());

                    LOGGER.error(request.getBlock().toString());

                    Auction auction = null;
                    Transaction test = recvBlock.getTransaction();
                    if(test.isAtive()){
                        if(auctionApp.searchMyAuctions(test.getTopic())){
                            auction = auctionApp.getMyAuctions().get(test.getTopic());
                            auction.registerBid(recvBlock.getTransaction().getPrice(), recvBlock.getTransaction().getBuyerID());
                        }else if(auctionApp.getAllAuctions().containsKey(test.getTopic())){
                            auction = auctionApp.getAllAuctions().get(test.getTopic());
                            auction.registerBid(recvBlock.getTransaction().getPrice(), recvBlock.getTransaction().getBuyerID());
                        }
                    }

                    myBlockChain.storeBlock(request.getKey(), recvBlock);
                    LOGGER.info("Lets disseminate this block " + request.getSender().getPort());
                    kademlia.disseminateStore(recvBlock, request.getKey());

                    String topic = recvBlock.getTransaction().getTopic();
                    boolean ative = recvBlock.getTransaction().isAtive();
                    if(auctionApp.getSubscribedTopics(topic)){
                        System.out.println("\n\n\n");
                        System.out.println("Received update from topic " + topic + " with ative = " + ative);
                        System.out.println("Current Bid: " + recvBlock.getTransaction().getPrice());
                        System.out.println("Buyer ID: " + recvBlock.getTransaction().getBuyerID());
                        System.out.print("\u001B[34m" + "\n$ " + "\u001B[0m");
                    }

                }

                LOGGER.error("I already have this block! " + request.getSender().getPort());

            }else{
                LOGGER.error("Signature error!");
            }

        }else if (request.getBlockOrAuctionCase() == StoreRequest.BlockOrAuctionCase.AUCTION) {
            LOGGER.info("Received an auction");

            SignatureProto signatureProto = request.getAuction().getSignature();

            String hash = signatureProto.getHash();
            PublicKey publicKey = utils.convertBytestoPubKey(signatureProto.getPublicKey().toByteArray());

            if ( utils.verifySignature(signatureProto.getSignature().toByteArray(), hash, publicKey) ){

                if(!auctionApp.searchAllAuctions(request.getAuction().getTopic()) && !request.getAuction().getSellerID().equals(utils.getMyID())){
                    LOGGER.info("I don't have this auction");
                    Auction recvAuction = KademliaUtils.auctionProtoToAuction(request.getAuction());
                    auctionApp.registerAuction(recvAuction);
                    LOGGER.info("Lets disseminate this auction");
                    kademlia.disseminateStore(recvAuction, request.getKey());
                }

                LOGGER.error("I already have this auction!");
            }else{
                LOGGER.error("Signature error!");
            }
        }

        NodeInfo myInfo = NodeInfo.newBuilder().setId(kademliaUtils.getMyID())
                .setIp(kademliaUtils.getMyIP())
                .setPort(kademliaUtils.getMyPort())
                .build();

        StoreResponse response = StoreResponse.newBuilder()
                .setSender(myInfo)
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }



    @Override
    public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {

        if(!kademlia.isInsideNetwork()) return;

        LOGGER.info("[KademliaService] - Inside findNode Request!");

        String targetId = request.getTargetId();
        Node senderRequest = KademliaUtils.nodeInfoToNode(request.getSender());

        /*
        System.out.println("[findNode] Target ID: " + targetId);
        System.out.println("[findNode] Sender ID: " + request.getSender().getId());
        System.out.println("[findNode] Sender IP: " + request.getSender().getIp());
        System.out.println("[findNode] Sender Port: " + request.getSender().getPort());
        */

        LinkedList<Node> closestNodes = kademlia.getClosestNodes(targetId, senderRequest);

        for (Node closestNode : closestNodes) {

            NodeInfo.Builder nodeInfo = NodeInfo.newBuilder();
            nodeInfo.setPort(closestNode.getPort());
            nodeInfo.setId(closestNode.getId());
            nodeInfo.setIp(closestNode.getIp());

            FindNodeResponse response = FindNodeResponse.newBuilder()
                    .setNode(nodeInfo)
                    .build();

            responseObserver.onNext(response);
            /*
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            */
        }

        responseObserver.onCompleted();

        kademlia.insertNode(request.getSender().getIp(), request.getSender().getId(), request.getSender().getPort());
    }

    @Override
    public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {

        if(!kademlia.isInsideNetwork()) return;

        LOGGER.info("[KademliaService] - Inside findValue Request!");

        String targetKey = request.getTargetKey();

        LOGGER.info("[KademliaService] - Received findValue with targetID: " + targetKey);
        Node senderRequest = KademliaUtils.nodeInfoToNode(request.getSender());

        Block block = myBlockChain.searchGetBlock(targetKey);

        if(block != null){
            LOGGER.info("[KademliaService] - targetValue found!");
            FindValueResponse response = FindValueResponse.newBuilder()
                    .setBlock(KademliaUtils.blockToBlockProto(block))
                    .build();

            responseObserver.onNext(response);

            responseObserver.onCompleted();

            return;
        }

        //String targetId = kademliaUtils.convertByteToBinaryString(targetValue);
        //targetId = kademliaUtils.convertStringHexToBinary(targetId);

        LinkedList<Node> closestNodes = kademlia.getClosestNodes(targetKey, senderRequest);

        for (Node closestNode : closestNodes) {

            NodeInfo.Builder nodeInfo = NodeInfo.newBuilder();
            nodeInfo.setPort(closestNode.getPort());
            nodeInfo.setId(closestNode.getId());
            nodeInfo.setIp(closestNode.getIp());

            FindValueResponse response = FindValueResponse.newBuilder()
                    .setNode(nodeInfo)
                    .build();

            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();

        kademlia.insertNode(request.getSender().getIp(), request.getSender().getId(), request.getSender().getPort());
    }


    @Override
    public void challenge(ChallengeRequest request, StreamObserver<ChallengeResponse> responseObserver) {
        LOGGER.info("[KademliaService] - Inside Challenge request!");

        ChallengeResponse.Builder response = ChallengeResponse.newBuilder();

        long mili = utils.localDateTimeToNumber(LocalDateTime.now());
        LocalDateTime aux = utils.numberToLocalDateTime(mili);
        mili = utils.localDateTimeToNumber(aux);

        response.setTimestamp(mili);

        responseObserver.onNext(response.build()); // Build response and send
        responseObserver.onCompleted(); // End connection
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        LOGGER.info("[KademliaService] - Inside Join request!");
        String myId = "";
        String myIp= request.getIp();
        String myPort = request.getPort();
        long nonce = request.getNonce();
        long timestamp = request.getTimestamp();

        String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
        String dataToHash = myIp + myPort + timestamp + nonce;

        String hash = null;
        try {
            hash = new String(Hex.encode(utils.hashingSHA2(dataToHash)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if(hash.substring(0, Constants.DIFFICULTY).equals(zeros)){
            LOGGER.info("[KademliaService] - Hash Validated");
            myId = utils.convertByteToBinaryString(utils.hashingSHA1(hash.getBytes(StandardCharsets.UTF_8)));
        }

        int i =0;
        while(kademlia.searchKbucket(myId)){
            try {
                i++;
                hash = new String(Hex.encode(utils.hashingSHA2(dataToHash+i)));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            myId = utils.convertByteToBinaryString(utils.hashingSHA1(hash.getBytes(StandardCharsets.UTF_8)));
        }

        JoinResponse.Builder response = JoinResponse.newBuilder();
        response.setId(myId);

        if(myBlockChain.getConflictChains().isEmpty()){
            if (!myBlockChain.getBlockChain().isEmpty()){
                int size = myBlockChain.getBlockChain().size();
                Block headhBlock = myBlockChain.getBlockChain().get(size-1);
                response.setHeadBlock(KademliaUtils.blockToBlockProto(headhBlock));
                response.setHasBlock(1);
            } else {
                response.setHasBlock(0);
            }
        }else {
            int size = myBlockChain.getConflictChains().get(0).size();
            Block headhBlock = myBlockChain.getConflictChains().get(0).get(size-1);
            response.setHeadBlock(KademliaUtils.blockToBlockProto(headhBlock));
            response.setHasBlock(1);
        }



        responseObserver.onNext(response.build()); // Build response and send
        responseObserver.onCompleted(); // End connection
    }
}
