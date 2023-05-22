package Auction;

import Peer.Wallet;
import Utils.Utils;
import com.google.protobuf.ByteString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.awt.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TestApp {

    public static String keyToBase64String(final Key key) {
        return bytesToBase64(key.getEncoded());
    }

    private static String bytesToBase64(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {

        String test = "Hello World!";
        Utils utils = Utils.getInstance();

        byte[] hash = utils.hashingSHA2(test);

        HashMap<String, String> map = new HashMap<>();



        String hex = utils.bytesToHexString(hash);

        map.put(hex, test);
        /*
        for (Map.Entry<byte[], String> entry : map.entrySet()) {
            System.out.println("Key: " + new String(entry.getKey()));
            System.out.println("Key: " + utils.bytesToHexString(entry.getKey()));
            hex = utils.bytesToHexString(entry.getKey());
            System.out.println("Value: " + entry.getValue());
        }

        boolean containsKey = false;
        for (byte[] key : map.keySet()) {
            if (Arrays.equals(key, byt)) {
                containsKey = true;
                break;
            }
        }
        */

        byte[] byt = utils.convertHexaStringToBytes(hex);
        String key = utils.bytesToHexString(byt);

        String get = map.get(key);

        System.out.println(get);
    }
}
