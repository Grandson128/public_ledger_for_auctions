package Auction;

import Utils.Utils;
import com.google.protobuf.ByteString;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Bid {

    private Utils utils;
    private Logger LOGGER;

    String hash;

    private LocalDateTime timeStamp;
    private String topic;
    private int bidValue;
    private String seller;
    private String buyer;

    public Bid(String topic, int bidValue, String seller) {

        utils = Utils.getInstance();

        LOGGER = utils.getLOGGER();

        this.topic = topic;
        this.bidValue = bidValue;
        this.seller = seller;
        this.buyer = utils.getMyID();

        LocalDateTime time = LocalDateTime.now();
        long timeLong = utils.localDateTimeToNumber(time);
        this.timeStamp = utils.numberToLocalDateTime(timeLong);

        try {
            hash = Utils.bytesToHexString(createHash());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Bid(String topic, int bidValue, String seller, String buyer, LocalDateTime timeStamp) {

        utils = Utils.getInstance();

        LOGGER = utils.getLOGGER();

        this.topic = topic;
        this.bidValue = bidValue;
        this.seller = seller;
        this.buyer = buyer;

        this.timeStamp = timeStamp;

        try {
            hash = Utils.bytesToHexString(createHash());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    private byte[] createHash() throws NoSuchAlgorithmException {

        String toHash = topic + bidValue + seller + buyer + timeStamp.toString();

        byte[] hashRes = utils.hashingSHA2(toHash);

        return hashRes;
    }

    public String getHash() {
        return hash;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public String getTopic() {
        return topic;
    }

    public int getBidValue() {
        return bidValue;
    }

    public String getSeller() {
        return seller;
    }

    public String getBuyer() {
        return buyer;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "utils=" + utils +
                ", LOGGER=" + LOGGER +
                ", hash=" + hash +
                ", timeStamp=" + timeStamp +
                ", topic=" + topic +
                ", bidValue=" + bidValue +
                ", seller='" + seller + '\'' +
                ", buyer='" + buyer + '\'' +
                '}';
    }
}
