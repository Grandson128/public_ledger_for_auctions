package BlockChain;

import java.security.NoSuchAlgorithmException;



public class Miner {
	//private double reward;
	public Block mine(Block b) throws NoSuchAlgorithmException {
		while(PoW(b)){
			b.incrementNonce();
			b.generateHash();
		}

		return b;
	}

	public boolean PoW(Block b){
		String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
		return !b.getHash().substring(0,Constants.DIFFICULTY).equals(zeros);
	}


	public boolean validateBlock(Block proposedBlock) throws NoSuchAlgorithmException {
		String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
		String proposedBlockNonceHash = proposedBlock.generateHash(proposedBlock.getNonce());

		boolean nZerosValid = proposedBlockNonceHash.substring(0, Constants.DIFFICULTY).equals(zeros);
		if(nZerosValid && proposedBlockNonceHash.equals(proposedBlock.getHash())){
			return true;
		}
		return false;
	}

}