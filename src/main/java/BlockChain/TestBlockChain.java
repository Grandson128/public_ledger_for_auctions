package BlockChain;

import Auction.Auction;
import Auction.AuctionApp;
import Peer.Wallet;
import Utils.Utils;
import Kademlia.KademliaUtils;
import org.bouncycastle.util.encoders.Hex;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.*;

public class TestBlockChain{
	private Scanner scanner;
	private AuctionApp auctionApp;
	private Utils utils;
	private static TestBlockChain instance;

	public TestBlockChain(){
		this.utils = Utils.getInstance();
		this.auctionApp = AuctionApp.getInstance();
		this.scanner = new Scanner(System.in);
	}

	public static TestBlockChain getInstance(){
		if(instance == null){
			instance = new TestBlockChain();
		}
		return instance;
	}


	public Block testNewBlock(){
		BlockChainUtils bchainOperator = new BlockChainUtils();
		Block testBlock;
		String name;
		String basePrice;
		System.out.print("Auction name: ");
		name = scanner.nextLine();

		System.out.print("Base price: ");
		basePrice = scanner.nextLine();
		Wallet wallet = Wallet.getInstance();

		//Build generated auction
		Auction auction = new Auction(name, Integer.parseInt(basePrice));
		auction.setCurrentBid(Integer.parseInt(basePrice)+100);
		auction.setCurrentBidder(utils.getMyID());

		//build a transaction
		//NOTE: transaction already builds its signature based on the auction data, etc..
		Transaction testTransaction = new Transaction(auction, Integer.parseInt(basePrice)+100, true);

		boolean test = utils.verifySignature(testTransaction.getSignature(), testTransaction.getChecksum(), wallet.getPubKey());

		System.out.println(test);

		try {
			testBlock = new Block(1, testTransaction);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		return testBlock;
	}


	public static void main(String[] args) throws NoSuchAlgorithmException {

		String myIp = "localhot";
		String myPort = "1245";
		Utils utils = Utils.getInstance();
		long timestamp = utils.localDateTimeToNumber(LocalDateTime.now());

		/*Begging mining*/
		String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
		int nonce = 0;
		String hash = myIp + myPort + timestamp + nonce;

		while(!hash.substring(0,Constants.DIFFICULTY).equals(zeros)){
			nonce++;
			hash = myIp + myPort + timestamp + nonce;
			try {
				hash = new String(Hex.encode (utils.hashingSHA2(hash)));
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println(hash);

	}
}

/*

TestBlockChain test = new TestBlockChain();
                    Block testBlock = test.testNewBlock();
                    Block testBlock2 = test.testNewBlock();
                    Block testBlock3 = test.testNewBlock();
                    Block testBlock4 = test.testNewBlock();

                    try {
                        myBlockChain.addBlockPOW(testBlock);
                        myBlockChain.addBlockPOW(testBlock2);

                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    BlockChainUtils blockChainUtils = BlockChainUtils.getInstance();
                    testBlock3.setPreviousHash(testBlock.getHash());
                    testBlock4.setPreviousHash(testBlock.getHash());
                    try {
                        blockChainUtils.mine(testBlock3);
                        blockChainUtils.mine(testBlock4);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    myBlockChain.storeBlock("ola", testBlock3);
                    myBlockChain.storeBlock("ola2", testBlock4);


                    myBlockChain.printAllChainsShort();

                    Block testBlock5 = test.testNewBlock();
                    Block testBlock6 = test.testNewBlock();
                    Block testBlock7 = test.testNewBlock();
                    Block testBlock8 = test.testNewBlock();
                    Block testBlock9 = test.testNewBlock();
                    Block testBlock10 = test.testNewBlock();
                    try {
                        myBlockChain.addBlockPOW(testBlock5);
                        myBlockChain.addBlockPOW(testBlock6);
                        myBlockChain.addBlockPOW(testBlock7);
                        myBlockChain.addBlockPOW(testBlock8);
                        myBlockChain.addBlockPOW(testBlock9);
                        myBlockChain.addBlockPOW(testBlock10);


                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    myBlockChain.printAllChainsShort();
                    //kademlia.doStore(testBlock);

*/