package BlockChain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

public class BlockTest {

//    ID:1
//    Transaction:Trans nº 1
//    Hash:00000d8609c3554d720fec7c47f7c2b80444406d746f0c922d7c208a9f5cff46
//    PrevHash:00000b362cbc3c13cf80cf42e4debe32b533f0230f371bdf079ed3a3e81c1af0
//    Time:1683235584863
//    Nonce:1194141

/*    @Test
    public void testValidBlock() throws NoSuchAlgorithmException {
        System.out.println("Test Valid Block");
        // Arrange
        Block block = new Block(1, "Trans nº 1");
        block.setPreviousHash("00000b362cbc3c13cf80cf42e4debe32b533f0230f371bdf079ed3a3e81c1af0");
        long timeStamp = 1683235584863L;
        block.setTimeStamp(timeStamp);
        block.setNonce(1194140);
        block.setHash("00000d8609c3554d720fec7c47f7c2b80444406d746f0c922d7c208a9f5cff46");
        // Act
        Miner validator = new Miner();
        boolean result = validator.validateBlock(block);

        //String dataToHash = "O=>"+Integer.toString(1) + block.getPreviousHash() + Long.toString(1683235584863L) + Integer.toString(1194141) + "Trans nº 1";
        //System.out.println(dataToHash);

        // Assert
        Assertions.assertTrue(result);
        System.out.println("Passed!\n");
    }

    @Test
    public void testInvalidBlock() throws NoSuchAlgorithmException {
        System.out.println("Test Invalid Block => Wrong Nonce");
        // Arrange
        Block block = new Block(1, "Trans nº 1");
        block.setPreviousHash("00000881f50437709c7c9396789c2fd1ee4bf8ed2fa5c19a3030bb6b5c3f4055");
        long timeStamp = 1683233046515L;
        block.setTimeStamp(timeStamp);
        block.setNonce(1234);
        block.setHash("000008e006cdc1b956b5c85c13dd1eb682df471e535929a5070d9e7a3835e1fc");

        // Act
        Miner validator = new Miner();
        boolean result = validator.validateBlock(block);

        // Assert
        Assertions.assertFalse(result);
        System.out.println("Passed!\n");
    }

    @Test
    public void testInvalidHash() throws NoSuchAlgorithmException {
        System.out.println("Test Invalid Block => Wrong Hash");
        // Arrange
        Block block = new Block(1, "Trans nº 1");
        block.setPreviousHash("00000881f50437709c7c9396789c2fd1ee4bf8ed2fa5c19a3030bb6b5c3f4055");
        long timeStamp = 1683233046515L;
        block.setTimeStamp(timeStamp);
        block.setNonce(1060908);
        block.setHash("000008e006cdc1b956b5c85c13dd1eb682df471e535929a5070d9e7a3835e1fz");

        // Act
        Miner validator = new Miner();
        boolean result = validator.validateBlock(block);

        // Assert
        Assertions.assertFalse(result);
        System.out.println("Passed!\n");
    }*/
}