package BlockChain;

import Auction.Auction;
import Peer.Wallet;
import Utils.Utils;
import com.google.type.DateTime;
import kademlia.TransactionProto;
import org.bouncycastle.util.encoders.Hex;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.LocalDateTime;

public class Transaction {
    private Utils utils;
    private Wallet wallet;

    private String topic;
    private String itemName;
    private int price;
    private LocalDateTime timestamp;


    private String hash;
    private PublicKey auctioneerPublicKey;
    private byte[] signature;

    private String buyerID;
    private String buyerIP;
    private String buyerPort;
    private PublicKey buyerPublicKey;
    private boolean isActive;

    /**
     *
     * @param auction - Respective Auction to which the transaction is addressed (Keep in mind this can be used as a bid too)
     * checksum - This will be the Hash of the Auction Data + AuctioneerPubKey + BuyerInfo(PubKey, ID, IP, Port)
     * @param auctioneerPublicKey - Public Key of who published the auction
     * signature - This is the CheckSum SIGNED by the PRIVATE KEY of who is sending the Transaction/Bid
     * @param buyerID - Identification of the Node Sending the Transaction
     * @param buyerIP - IP of the Node Sending the Transaction
     * @param buyerPort - Port of the Node Sending the Transaction
     */

    public Transaction(Auction auction, PublicKey auctioneerPublicKey, String buyerID, String buyerIP, String buyerPort, boolean isActive) {
        /*this.utils = Utils.getInstance();
        this.wallet = Wallet.getInstance();

        this.itemName = auction.getName();
        this.price = auction.getCurrentBid();
        this.timestamp = auction.getDeadline();
        this.topic = auction.getTopic();
        this.buyerPublicKey = auction.getCurrentBidder();
        this.auctioneerPublicKey = auctioneerPublicKey;
        this.buyerID = buyerID;
        this.buyerIP = buyerIP;
        this.buyerPort = buyerPort;
        this.isActive = isActive;


        //Generate and set CheckSum
        generateCheckSum(auction, auctioneerPublicKey, buyerID, buyerIP, buyerPort);

        //Generate and set Signature
        generateSignature(this.hash);*/

    }



    /**
     * Create a transaction My node -> others
     */
    public Transaction(Auction auction, int amount, boolean isActive){
        this.utils = Utils.getInstance();
        this.wallet = Wallet.getInstance();

        this.itemName = auction.getName();
        this.topic = auction.getTopic();
        this.price = amount;

        this.buyerID = utils.getMyID();
        this.buyerIP = utils.getMyIP();
        this.buyerPort = utils.getMyPort();
        this.isActive = isActive;

        long mili = utils.localDateTimeToNumber(LocalDateTime.now());
        this.timestamp = utils.numberToLocalDateTime(mili);

        this.buyerPublicKey = wallet.getPubKey();
        this.auctioneerPublicKey = auction.getSellerPubKey();
        this.hash = generateCheckSum(auction, auctioneerPublicKey, buyerID, buyerIP, buyerPort);
        this.signature = generateSignature(this.hash);
    }

    /**
     *
     * Creates a new transaction from a transaction received by the network
     */
    public Transaction(TransactionProto transactionProto) {
        this.utils = Utils.getInstance();
        this.itemName = transactionProto.getName();
        this.price = transactionProto.getFinalPrice();
        this.timestamp = utils.numberToLocalDateTime(transactionProto.getTimestamp());
        this.topic = transactionProto.getTopic();
        this.buyerPublicKey = utils.convertBytestoPubKey(transactionProto.getSignature().getPublicKey().toByteArray());
        this.hash = transactionProto.getSignature().getHash();
        this.auctioneerPublicKey = utils.convertBytestoPubKey(transactionProto.getAuctionPublicKey().toByteArray());
        this.signature = transactionProto.getSignature().getSignature().toByteArray();
        this.buyerID = transactionProto.getBuyerInfo().getId();
        this.buyerIP = transactionProto.getBuyerInfo().getIp();
        this.buyerPort = transactionProto.getBuyerInfo().getPort();
        this.isActive = transactionProto.getAtive();
    }


    public String generateCheckSum(Auction auction, PublicKey auctioneerPublicKey, String buyerID, String buyerIP, String buyerPort){
        String dataToHash = auction.auctionInfoToHash() + Utils.bytesToHexString(utils.convertPubKeytoBytes(auctioneerPublicKey)) + buyerID + buyerIP + buyerPort;
        String hashedData;
        try {
            hashedData = new String(Hex.encode(utils.hashingSHA2(dataToHash)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return hashedData;
    }

    public byte[] generateSignature(String checksum){
        byte[] transactionSignature = utils.signHash(wallet.getPrivKey(), checksum);

        return transactionSignature;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public PublicKey getBuyerPublicKey() {
        return buyerPublicKey;
    }

    public void setBuyerPublicKey(PublicKey buyerPublicKey) {
        this.buyerPublicKey = buyerPublicKey;
    }

    public String getChecksum() {
        return hash;
    }

    public void setChecksum(String checksum) {
        this.hash = checksum;
    }

    public PublicKey getAuctioneerPublicKey() {
        return auctioneerPublicKey;
    }

    public void setAuctioneerPublicKey(PublicKey auctioneerPublicKey) {
        this.auctioneerPublicKey = auctioneerPublicKey;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(String buyerID) {
        this.buyerID = buyerID;
    }

    public String getBuyerIP() {
        return buyerIP;
    }

    public void setBuyerIP(String buyerIP) {
        this.buyerIP = buyerIP;
    }

    public String getBuyerPort() {
        return buyerPort;
    }

    public void setBuyerPort(String buyerPort) {
        this.buyerPort = buyerPort;
    }

    public boolean isAtive() {
        return isActive;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Item Name: ").append(itemName).append("\n");
        sb.append("Price: ").append(price).append("\n");
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("Topic: ").append(topic).append("\n");
        sb.append("Buyer Public Key: ").append(Utils.bytesToHexString(utils.convertPubKeytoBytes(buyerPublicKey))).append("\n");
        sb.append("Checksum: ").append(hash).append("\n");
        sb.append("Auctioneer Public Key: ").append(Utils.bytesToHexString(utils.convertPubKeytoBytes(auctioneerPublicKey))).append("\n");
        sb.append("Signature: ").append(Utils.bytesToHexString(signature)).append("\n");
        sb.append("Buyer ID: ").append(buyerID).append("\n");
        sb.append("Buyer IP: ").append(buyerIP).append("\n");
        sb.append("Buyer Port: ").append(buyerPort).append("\n");

        return sb.toString();
    }


}
