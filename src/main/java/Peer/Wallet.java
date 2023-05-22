package Peer;

import Utils.Utils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Wallet {

    private KeyPair keyPair;
    private static Wallet instance;

    private Wallet() {
        keyPair = Utils.getKeyPair();
    }

    public static Wallet getInstance(){
        if(instance == null){
            instance = new Wallet();
        }
        return instance;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPubKey(){
        return keyPair.getPublic();
    }

    public PrivateKey getPrivKey(){
        return keyPair.getPrivate();
    }
}
