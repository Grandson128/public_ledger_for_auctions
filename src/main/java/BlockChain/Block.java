package BlockChain;

import Utils.Utils;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class Block {

    private int id;
    private int nonce;
    // the below variable is used to store the timestamp of the block in milliseconds.
    private long timeStamp;
    //the variable hash will contain the hash of the block
    private String hash;
    //The previousHash variable contains the hash of the previous block
    private String previousHash;
    private Transaction transaction;
    private Utils utils;

    //private String dataToHash;

    public Block(int id, Transaction transaction) throws NoSuchAlgorithmException {
        this.id = id;
        this.timeStamp = new Date().getTime();
        this.transaction = transaction;
        this.utils = Utils.getInstance();
        generateHash();
    }

    public void generateHash() throws NoSuchAlgorithmException {
        String dataToHash = Integer.toString(id) + previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + transaction.toString();
        String hashValue = new String(Hex.encode(utils.hashingSHA2(dataToHash)));
        this.hash = hashValue;
    }

    public String generateHash(int proposedNonce) throws NoSuchAlgorithmException {
        String dataToHash = Integer.toString(this.id) + this.previousHash + Long.toString(this.timeStamp) + Integer.toString(proposedNonce) + this.transaction;
        //System.out.println("V->"+dataToHash+"\n");
        String hashValue = new String(Hex.encode(utils.hashingSHA2(dataToHash)));
        return hashValue;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }


    public String getPreviousHash() {
        return previousHash;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }
    public void incrementNonce() {
        this.nonce++;
    }

    public int getId() {
        return id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "ID:"+this.id+"\nHash:"+this.hash+"\nPrevHash:"+this.previousHash+"\nTime:"+this.timeStamp+"\nNonce:"+this.nonce+"\n" + this.transaction + "\n";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Block other = (Block) obj;

        if(!this.hash.equals(other.hash)){
            return false;
        }

        return true;
    }
}
