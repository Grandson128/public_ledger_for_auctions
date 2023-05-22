package Utils;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Utils {
    private static Utils instance;
    private Logger LOGGER;

    public static final long auctionTimeDuration = 60*1; //minutes
    private String myID;
    private String myIP;
    private String myPort;

    public static Utils getInstance(){
        if(instance == null){
            instance = new Utils();
        }
        return instance;
    }

    public byte[] hashingSHA2(String elementToHash) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(elementToHash.getBytes(StandardCharsets.UTF_8));
        return hash;
    }

    public byte[] hashingSHA1(byte[] elementToHash) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        byte[] hash = digest.digest(elementToHash);
        return hash;
    }

    public byte[] convertBinaryStringToBytes(String binary){
        // Convert the binary string to a BigInteger
        BigInteger bigInteger = new BigInteger(binary, 2);

        // Convert the BigInteger to a byte array
        byte[] byteArray = bigInteger.toByteArray();
        return byteArray;
    }

    public byte[] convertHexaStringToBytes(String hexa){
        // Convert the binary string to a BigInteger
        BigInteger bigInteger = new BigInteger(hexa, 16);

        // Convert the BigInteger to a byte array
        byte[] byteArray = bigInteger.toByteArray();
        return byteArray;
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

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static KeyPair getKeyPair(){
        KeyPairGenerator keyGen = null; //elliptic curve
        try {
            keyGen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            // Initialize the key generator and generate a KeyPair
            /**
             * The secp256r1 curve is based on the NIST recommended elliptic curve known as P-256.
             * It offers a good balance between security and computational efficiency.
             */
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());   //recommended curve with 256 bytes of security
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        KeyPair keyPair = keyGen.generateKeyPair();
        return  keyPair;
    }

    public byte[] convertPubKeytoBytes(PublicKey pubKey){
        return pubKey.getEncoded();
    }

    public PublicKey convertBytestoPubKey(byte[] byteKey){
        // Create X509EncodedKeySpec from the byte array
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
        // Get the RSA key factory
        KeyFactory keyFactory = null;
        PublicKey publicKey = null;
        try {
            keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            // Generate the PublicKey object
            publicKey = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return  publicKey;
    }


    public boolean verifySignature(byte[] signature, String hash, PublicKey pubKey){
        if(signature==null){
            return false;
        }

        Signature sign = null;
        boolean res = false;
        try {
            sign = Signature.getInstance("SHA256withECDSA");
            sign.initVerify(pubKey);
            sign.update(convertHexaStringToBytes(hash));
            res = sign.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }

        return res;
    }


    public byte[] signHash(PrivateKey privKey, String hash){
        Signature sign = null;
        byte[] signature = new byte[0];

        try {
            sign = Signature.getInstance("SHA256withECDSA");
            sign.initSign(privKey);
            sign.update(convertHexaStringToBytes(hash));
            signature = sign.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }

        return  signature;
    }

    public long getTimeUntilDeadline(LocalDateTime received){
        LocalDateTime now = LocalDateTime.now();
        return Duration.between(now,received).toMillis();
    }


    public long localDateTimeToNumber(LocalDateTime received){
        // Convert LocalDateTime to int milliseconds
        Instant instant = received.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public LocalDateTime numberToLocalDateTime(long timeInMillis){
        // Convert milliseconds to LocalDateTime
        Instant instant = Instant.ofEpochMilli(timeInMillis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }


    /**
     * Crypto
     */

    private static byte[] encryptText(String plainText, PrivateKey privateKey) throws Exception {
        // Initialize the Cipher instance for encryption
        Cipher cipher = Cipher.getInstance("SHA256withECDSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        // Encrypt the text
        return cipher.doFinal(plainText.getBytes());
    }

    private static byte[] decryptText(byte[] encryptedBytes,  PublicKey publicKey) throws Exception {
        // Initialize the Cipher instance for decryption
        Cipher cipher = Cipher.getInstance("SHA256withECDSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        // Decrypt the encrypted bytes
        return cipher.doFinal(encryptedBytes);
    }

    public void printByteInHexa(byte[] input){
        for(byte b : input){
            System.out.print(String.format("%02x ", b));
        }
        System.out.println("");
    }

    /**
     * SETTER AND GETTER
     */

    public void setLOGGER(Logger LOGGER) {
        this.LOGGER = LOGGER;
    }

    public Logger getLOGGER() {
        return LOGGER;
    }

    public String getMyID() {
        return myID;
    }

    public void setMyID(String myID) {
        this.myID = myID;
    }

    public String getMyIP() {
        return myIP;
    }

    public void setMyIP(String myIP) {
        this.myIP = myIP;
    }

    public String getMyPort() {
        return myPort;
    }

    public void setMyPort(String myPort) {
        this.myPort = myPort;
    }

}
