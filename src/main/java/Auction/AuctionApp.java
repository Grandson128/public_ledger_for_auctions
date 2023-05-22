package Auction;

import BlockChain.Block;
import BlockChain.BlockChain;
import BlockChain.Transaction;
import Kademlia.Kademlia;
import Utils.Utils;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;


/**
 *
 *  It works as an  auction house, all items are available until the close time.
 *  An Auction can have multiple items:
 *      Bids can happen on different items
 *      When the close time happens, all bids are committed and the auction closes.
 *
 *
 *  auctions HashMap should map Key --> Auction
 *      Key should represent the auctioneer.
 *      To be possible to have multiple auctions for one auctioneer:
 *          Key = Hash( auctioneer.privKey,(auctioneer.pubKey + auction.topic))
 *
 */


public class AuctionApp {

    private static AuctionApp instance;

    HashMap<String, Auction> myAuctions;
    HashMap<String, Auction> allAuctions;

    private ArrayList<String> subscribedTopics;

    private Utils utils;
    private Kademlia kademlia;
    private BlockChain blockChain;

    private AuctionApp(){
        this.myAuctions = new HashMap<>();
        this.allAuctions = new HashMap<>();
        this.subscribedTopics = new ArrayList<>();
        this.kademlia = Kademlia.getInstance();
        this.utils = Utils.getInstance();
        this.blockChain = BlockChain.getInstance();
    }

    public static AuctionApp getInstance(){
        if(instance == null){
            instance = new AuctionApp();
        }
        return instance;
    }


    /*
    * My Auctions Logic
    */

    public HashMap<String, Auction> getMyAuctions() {
        return this.myAuctions;
    }

    public void createNewAuction(String itemName, int itemBasePrice){
        Auction auction = new Auction(itemName, itemBasePrice);
        addAuction(auction.getTopic(), auction);
        publishAuction(auction);
    }

    public synchronized void addAuction(String auctionTopic, Auction auction){
        this.myAuctions.put(auctionTopic, auction);
        this.addTopic(auction.getTopic());

        //Close auction when deadline is met
        Timer timer = new Timer();

        long milliseconds = utils.getTimeUntilDeadline(auction.getDeadline());
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeAuction(auctionTopic);
            }
        }, milliseconds);
    }

    public synchronized void closeAuction(String auctionTopic){
        Auction auxAuction = this.myAuctions.get(auctionTopic);

        auxAuction.closeAuction(); // Only print

        //Send Transaction to other nodes
        auxAuction.setActive(false);

        publishEndBid(auxAuction);

        this.myAuctions.remove(auctionTopic);
        this.subscribedTopics.remove(auxAuction.getTopic());
    }

    /*
    * Topics
    */

    public ArrayList<String> getTopics() {
        return subscribedTopics;
    }

    public synchronized void addTopic(String topic){
       this.subscribedTopics.add(topic);
    }
    public synchronized void removeTopic(String topic){
        this.subscribedTopics.remove(topic);
    }

    public void printTopics(){
        this.subscribedTopics.toString();
    }


    /*
    * Network Logic
    */
    public void publishAuction(Auction auction){
        auction.printAuction();
        kademlia.doStore(auction);
    }


    public void publishBid(String topic, int bidValue){

        Auction toSendBid = allAuctions.get(topic);

        if(toSendBid.getCurrentBid() >= bidValue || bidValue <= toSendBid.getBasePrice()){
            System.out.println("Bid value too low!");
        }

        if(toSendBid != null){ //Auction is known and active!!

            Transaction transaction = new Transaction(toSendBid, bidValue, true);

            Block blockToSave;
            try {
                blockToSave = new Block(0, transaction);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            try {
                if(blockChain.addBlockPOW(blockToSave)){
                    kademlia.doStore(blockToSave);
                    subscribedTopics.add(topic);
                    registerBid(transaction);
                }
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }



        }else{ //Auction not known
            System.out.println("There is no auction available with this topic: " + topic.toString());
        }

    }

    private void publishEndBid(Auction toSend){
        SecureRandom rand = new SecureRandom();
        try {
            int randomTime = rand.nextInt(1000);

            //System.out.println("Lets wait " + randomTime + " miliseconds");
            sleep(randomTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //System.out.println("Sending end transaction " + LocalDateTime.now());
        Transaction transaction = new Transaction(toSend, toSend.getCurrentBid(), false);

        Block blockToSave;
        try {
            blockToSave = new Block(0, transaction);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            if(blockChain.addBlockPOW(blockToSave)){
                //System.out.println("Previous Hash: " + blockToSave.getPreviousHash());
                kademlia.doStore(blockToSave);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Auction> getAllAuctions() {
        return allAuctions;
    }

    /**
     * Function to regist bids received from kademlia
     * @param transaction -> transaction received
     */
    public synchronized boolean registerBid(Transaction transaction){
        if(!transaction.isAtive()){
            Logger LOGGER = utils.getLOGGER();
            LOGGER.error("[registerBid] - END OF AUCTION!!!!");
            return true; // No need to register when auction ended
        }

        Auction auction;
        if(myAuctions.containsKey(transaction.getTopic())){
            auction = myAuctions.get(transaction.getTopic());
        }else if(allAuctions.containsKey(transaction.getTopic())){
            auction = allAuctions.get(transaction.getTopic());
        }else{
            return true;
        }


        if(auction.registerBid(transaction.getPrice(), transaction.getBuyerID())){
            return true;
        }

        return false;
    }


    /**
     * Handle Received Auctions
     */

    public synchronized void registerAuction(Auction auction){
        allAuctions.put(auction.getTopic(), auction);
        //Close auction when deadline is met

        Timer timer = new Timer();
        long milliseconds = utils.getTimeUntilDeadline(auction.getDeadline());

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("Close: <"+auction.getTopic()+">");
                closeKnownAuction(auction.getTopic());
            }
        }, milliseconds);
    }

    public synchronized void closeKnownAuction(String topic){

        Auction auxAuction = this.allAuctions.get(topic);
        this.allAuctions.remove(topic);
        if(getSubscribedTopics(topic)){

            auxAuction.setActive(false);
            publishEndBid(auxAuction);

            auxAuction.closeAuction(); //Only print
            this.subscribedTopics.remove(topic);
        }


    }


    public synchronized void printAllAuctions(){
        System.out.println("\u001B[34m" + "All auctions < TOPIC, AUCTION >" + "\u001B[0m");
        for (Map.Entry<String, Auction> entry : allAuctions.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            entry.getValue().printAuction();
        }
    }

    public synchronized void printMyAuctions(){
        System.out.println("\u001B[34m" + "All auctions < TOPIC, AUCTION >" + "\u001B[0m");
        for (Map.Entry<String, Auction> entry : myAuctions.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            entry.getValue().printAuction();
        }
    }

    public synchronized boolean searchAllAuctions(String key){
        return  allAuctions.containsKey(key);
    }

    public synchronized boolean searchMyAuctions(String key){
        return  myAuctions.containsKey(key);
    }

    public boolean getSubscribedTopics(String topicToSearch){

        for (int i = 0; i < subscribedTopics.size(); i++) {
            if(subscribedTopics.get(i).equals(topicToSearch)) return true;
        }
        return false;
    }

}
