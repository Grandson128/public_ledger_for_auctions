package Auction;

import Utils.Utils;
import Peer.Wallet;
import org.bouncycastle.util.encoders.Hex;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;


public class Auction {

    //private AuctionItem item;
    private HashMap<String, Integer> bids = null;

    /* String -> Node ID */
    private LinkedList<String> subscribers = null;

    private final String name;
    private final int basePrice;
    private String sellerID;
    private PublicKey sellerPubKey;

    private int currentBid;
    private String currentBidder;

    //Topic will be a hash of the auction data + a random
    private String topic;
    boolean active;
    private final LocalDateTime deadline;

    Utils utils;
    Wallet wallet;

    /**
     * Constructor to add my own Auctions
     * @param name -> Name of the auction
     * @param basePrice -> Min value of the auction
     */
    public Auction(String name, int basePrice) {
        utils = Utils.getInstance();
        wallet = Wallet.getInstance();

        this.bids = new HashMap<>();

        LocalDateTime deadline = LocalDateTime.now().plusSeconds(Utils.auctionTimeDuration);
        long time = utils.localDateTimeToNumber(deadline);
        this.deadline = utils.numberToLocalDateTime(time);
        this.active = true;
        this.name = name;
        this.basePrice = basePrice;
        this.currentBid = basePrice;
        this.currentBidder = utils.getMyID();
        this.sellerID = utils.getMyID();
        this.sellerPubKey = wallet.getPubKey();

        String dataToHash = LocalDateTime.now().toString() + deadline.toString() + name + basePrice + sellerID + sellerPubKey.toString();
        try {
            this.topic =  utils.bytesToHexString(utils.hashingSHA2(dataToHash));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor to received auctions (Auctions Block)
     * */
    public Auction(String name, int basePrice, int currentBid, String currentBidder, LocalDateTime deadline, String topic, String sellerID, PublicKey sellerPubKey){
        this.name = name;
        this.basePrice = basePrice;
        this.currentBid = currentBid;
        this.currentBidder = currentBidder;
        this.deadline = deadline;
        this.topic = topic;
        this.sellerID = sellerID;
        this.sellerPubKey = sellerPubKey;
        this.active = true;
        this.bids = new HashMap<>();
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getTopic() {
        return topic;
    }

    public boolean registerBid(int amount, String bidderID){
        if (amount > currentBid && LocalDateTime.now().isBefore(this.deadline) && this.active == true) {
            currentBid = amount;
            currentBidder = bidderID;
            bids.put(bidderID, amount);
            return true;
        }

        return false;
    }

    public void closeAuction() {
        System.out.println("\n\n\n");
        System.out.println(topic + "was closed");
        System.out.println(name + ": " + currentBidder + " - " + currentBid);
        System.out.println("\n\n\n \u001B[34m" + "\n$ " + "\u001B[0m");
    }


    public HashMap<String, Integer> getBids() {
        return bids;
    }

    public String getName() {
        return name;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public String getCurrentBidder() {
        return currentBidder;
    }

    public String getSellerID() { return sellerID; }

    public boolean isActive() {
        return active;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void printAuction(){
        System.out.println("Auction:\n");
        System.out.println("Seller ID: " + this.sellerID);
        System.out.println("Seller PubKey: " + this.sellerPubKey);
        System.out.println("Name: " + this.getName());
        System.out.println("Base Price: " + this.getBasePrice());
        System.out.println("Deadline: " + this.getDeadline().toString());
        System.out.println("Topic: <" + this.topic +">");
        System.out.println("\nActual bid: " + this.getCurrentBid());
        System.out.println("Bidder ID: " + this.getCurrentBidder());
        System.out.println("---------------------------------------");
    }



    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }

    public void setCurrentBidder(String currentBidder) {
        this.currentBidder = currentBidder;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String auctionInfoToHash() {
        StringBuilder sb = new StringBuilder();

        sb.append("Auction Name: ").append(name).append("\n");
        sb.append("Base Price: ").append(basePrice).append("\n");
        sb.append("Seller: ").append(sellerID).append("\n");
        sb.append("SellerPubKey: ").append(sellerPubKey).append("\n");
        sb.append("Current Bid: ").append(currentBid).append("\n");
        sb.append("Current Bidder: ").append(currentBidder).append("\n");
        sb.append("Topic: ").append(topic).append("\n");
        sb.append("Active: ").append(active).append("\n");
        sb.append("Deadline: ").append(deadline).append("\n");

        /*
        sb.append("Bids:\n");
        for (String bidder : bids.keySet()) {
            int bidAmount = bids.get(bidder);
            sb.append("  Bidder: ").append(Utils.bytesToHexString(utils.convertPubKeytoBytes(bidder))).append(", Amount: ").append(bidAmount).append("\n");
        }
        */
        return sb.toString();
    }

    public PublicKey getSellerPubKey() {
        return sellerPubKey;
    }


}
