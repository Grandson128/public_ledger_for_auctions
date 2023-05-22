package BlockChain;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import Utils.Utils;


public class BlockChainUtils {
    private static BlockChainUtils instance;
    public static BlockChainUtils getInstance() {
        if(instance == null){
            instance = new BlockChainUtils();
        }

        return instance;
    }


    public synchronized String getHeadHash(List<Block> chain){
        if(chain.isEmpty()){
            return Constants.GENESIS_PREV_HASH;
        }
        return chain.get(chain.size()-1).getHash();
    }

    /**
     * Proof-of-work Logic
     */

    public synchronized Block mine(Block b) throws NoSuchAlgorithmException {
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


    /**
     *
     * BlockChain Conflict Logic
     */

    public synchronized boolean allowConflict(List<Block> chain, int indexOfConflict){
        int numberOfBlocksAhead = chain.size() - indexOfConflict;
        if(numberOfBlocksAhead < Constants.CONFLICT_DEPTH){
            return true;
        }
        return false;
    }

    public synchronized List<Block> cloneBlockChain(List<Block> blockChainToClone, int indexOfConflict){
        List<Block> cloneBlockList = new ArrayList<>();
        for (int i=0; i<=indexOfConflict; i++){
            cloneBlockList.add( blockChainToClone.get(i) );
        }
        return cloneBlockList;
    }


    public synchronized String mineKademliaChallenge(String myPort, String myIP, long timestamp) throws NoSuchAlgorithmException {
        int nonce = 0;
        String hash = myIP + myPort + timestamp + nonce;
        Utils utils = Utils.getInstance();
        while(kademliaPOW(hash)){
            nonce++;
            hash = myIP + myPort + timestamp + nonce;
            utils.hashingSHA2(hash);
        }

        return hash;
    }

    public boolean kademliaPOW(String hash){
        String zeros = new String(new char[Constants.DIFFICULTY]).replace('\0','0');
        return !hash.substring(0,Constants.DIFFICULTY).equals(zeros);
    }
}
