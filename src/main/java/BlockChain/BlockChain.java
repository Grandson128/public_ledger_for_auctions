package BlockChain;

import Kademlia.Kademlia;
import Utils.Utils;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

public class BlockChain{
    private List<Block> chain;

    private List<Block> unconfirmedBlocks;
    private ArrayList<List<Block>> conflictChains;
    private static BlockChain instance;
    private Kademlia kademlia;
    private BlockChainUtils blockChainUtils;
    private Utils utils;
    public BlockChain(){
        this.chain = new ArrayList<>();
        this.conflictChains = new ArrayList<>();
        this.unconfirmedBlocks = new ArrayList<>();
        this.kademlia = Kademlia.getInstance();
        this.blockChainUtils = BlockChainUtils.getInstance();
        this.utils = Utils.getInstance();
    }

    public static BlockChain getInstance() {
        if(instance == null){
            instance = new BlockChain();
        }

        return instance;
    }

    public ArrayList<List<Block>> getConflictChains() {
        return conflictChains;
    }

    public synchronized void addLocalMinedBlock(Block newMinedBlock){
        if(conflictChains.isEmpty()){
            //If there are no forks, add to the standard chain
            this.chain.add(newMinedBlock);
        }else{
            //In case of forked chains, always add new blocks to the biggest chain
            conflictChains.get(0).add(newMinedBlock);
            sortAllListsBySize();

            //Try to resolve forks
            //Here no concurrency trouble happens
            tryResolveForks();
        }
    }

    public synchronized List<Block> getBlockChain(){
        return this.chain;
    }

    public synchronized boolean isEmpty(){
        if (this.chain.isEmpty() == true){
            return true;
        }else {
            return false;
        }
    }
    public synchronized int size(){
        return this.chain.size();
    }

    @Override
    public synchronized String toString(){
        String text = "";

        for(Block b:this.chain){
            text+=b.toString()+"\n";
        }

        return text;
    }

    public synchronized boolean addBlockPOW(Block block) throws NoSuchAlgorithmException {

        if(conflictChains.isEmpty()){
            //If chain is empty previous hash is genesis hash, else, it's the head block hash
            if(this.chain.isEmpty()){
                block.setPreviousHash(Constants.GENESIS_PREV_HASH);
            }else {
                block.setPreviousHash(blockChainUtils.getHeadHash(this.chain));
            }
        }else{
            //If we have conflict chains, we mine a new block to the chain that is bigger
            //Sort all chains by size first
            sortAllListsBySize();
            //Pick the one that comes first
            String prevHash = blockChainUtils.getHeadHash(conflictChains.get(0));
            block.setPreviousHash(prevHash);
        }


        //Mine Block
        Block minedBlock = blockChainUtils.mine(block);
        if (blockChainUtils.validateBlock(minedBlock)){
            //Add Mined Block
            addLocalMinedBlock(minedBlock);
            if(!conflictChains.isEmpty())
                tryResolveForks();

            return true;
        }
        return false;
    }

    /**
     * Network logic
     */

    public synchronized void storeBlock(String key, Block block){
        //Add block to chain
        try {
            //This function checks for difficulty and if the hash(Block + Block.nonce) = Block.hash
            if(blockChainUtils.validateBlock(block)){
                if(handleNetWorkBlock(block)){
                    //Try to resolve forks
                    //Here no concurrency trouble happens
                    if(!conflictChains.isEmpty())
                        tryResolveForks();
                }
                //Try to resolve forks
                //Here no concurrency trouble happens
                tryResolveForks();
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public synchronized boolean searchBlock(Block block){
        boolean blockExists=false;
        if(conflictChains.isEmpty()){
            return chain.contains(block);
        }else{
            for (List<Block> chain : conflictChains){
                if(chain.contains(block))
                    blockExists=true;
            }
        }
        return blockExists;
    }

    public synchronized int findTargetPreviousBlockIndex(Block recvBlock, List<Block> contextChain) {
        for (int i = 0; i < contextChain.size(); i++) {
            if (contextChain.get(i).getHash().equals(recvBlock.getPreviousHash())) {
                return i; // Return the index when the object is found
            }
        }
        return -1; // Return -1 if the object is not found in the list
    }



    /**
     *
     * BlockChain Conflict Logic
     */
    public synchronized boolean handleNetWorkBlock(Block recvBlock){
        /*1º
        No conflict chains
        New Block points to Chain HeadHash*/
        if(conflictChains.isEmpty() && recvBlock.getPreviousHash().equals(blockChainUtils.getHeadHash(this.chain))){
            this.chain.add(recvBlock);
            return true;
        } else if(conflictChains.isEmpty() && !recvBlock.getPreviousHash().equals(blockChainUtils.getHeadHash(this.chain))){
            /*2º
              No conflict Chains
              New Block doesn't point to Chain HeadHash*/
            //Search for block
            int indexOfConflict = findTargetPreviousBlockIndex(recvBlock, this.chain);
            if (indexOfConflict == -1){
                //Block referred by Received Block not fount
                if(!unconfirmedBlocks.contains(recvBlock)){
                    //Check existence and add block to unconfirmed list
                    //TODO: Ask Network for the Block that if referred by recvBlock
                    unconfirmedBlocks.add(recvBlock);
                }
                return false;
            }else if(indexOfConflict >= 0) {
                //Previous Block from received block was found
                //See if depth is bigger than what we allow
                if(blockChainUtils.allowConflict(this.chain, indexOfConflict)){
                    //Conflict must be handled with forking the chain

                    //Create a new BlockChain that as all the blocks previous to the conflict
                    List<Block> newConflictChain = blockChainUtils.cloneBlockChain(this.chain, indexOfConflict);
                    //Confirm again that the recvBlock prevHash points to the head hash
                    if (blockChainUtils.getHeadHash(newConflictChain).equals(recvBlock.getPreviousHash())){
                        newConflictChain.add(recvBlock);
                        //add main chain to list too
                        conflictChains.add(this.chain);
                        conflictChains.add(newConflictChain);

                        //Try to resolve unconfirmed blocks
                        triggerUnconfirmedResolve(recvBlock.getHash());

                        sortAllListsBySize();
                        return true;
                    }else{
                        //System.out.println("Received block prevHash not equal to new Conflict Chain head hash");
                        return false;
                    }
                }else {
                    //conflict already to far back, deny the received block
                    //TODO: Send message back, received block should be mined again
                    return false;
                }
            }
        } else if (!conflictChains.isEmpty()) {
            // For every chain in the list check:
            //  If the recvBlock points to a block, that is at the head or at most Constants.CONFLICT_DEPTH
            //      If head: add and sort all lists
            //      If not head: Check Allow conflict
            //            If allow conflict: do the fork procedure to the respective chain

            boolean blockCanBeAdded = false;
            List<Block> listToAddBlock = new ArrayList<>();
            int indexConflictPoint = -1;

            for (List<Block> conflictChain : conflictChains){
                if(blockCanBeAdded) {
                    break;
                }
                if(!conflictChain.isEmpty()){
                    indexConflictPoint = findTargetPreviousBlockIndex(recvBlock, conflictChain);
                    if(recvBlock.getPreviousHash().equals(blockChainUtils.getHeadHash(conflictChain))){
                        //New Block points to Chain HeadHash
                        listToAddBlock=conflictChain;
                        blockCanBeAdded = true;
                    }else if( indexConflictPoint >= 0 && blockChainUtils.allowConflict(conflictChain, indexConflictPoint)){
                        //ReceivedBlock cn be added to the list and the fork is valid
                        listToAddBlock=conflictChain;
                        blockCanBeAdded = true;
                    }
                }
            }

            if(blockCanBeAdded){
                if(handleAddBlockForConflictChain(recvBlock, listToAddBlock)){
                    //Sort all chains
                    sortAllListsBySize();
                }
            }else {
                //Block referenced by recvBlock wasn't found in any chain
                if(!unconfirmedBlocks.contains(recvBlock)){
                    unconfirmedBlocks.add(recvBlock);
                }
            }

        }

        return false;
    }

    public synchronized boolean handleAddBlockForConflictChain(Block recvBlock, List<Block> conflictChain){
        boolean wasBlockAdded = false;
        if(recvBlock.getPreviousHash().equals(blockChainUtils.getHeadHash(conflictChain))) {
            //New Block points to Chain HeadHash
            conflictChain.add(recvBlock);
            //Try to resolve unconfirmed blocks
            triggerUnconfirmedResolve(recvBlock.getHash());

            wasBlockAdded = true;
        }else {
            //New Block doesn't point to Chain HeadHash
            //Search for block
            int indexOfConflict = findTargetPreviousBlockIndex(recvBlock, conflictChain);
            if (indexOfConflict == -1){
                //Previous Block from received block was not found
                wasBlockAdded = false;
            }else if(indexOfConflict >= 0) {
                //Previous Block from received block was found
                //See if depth is bigger than what we allow
                if(blockChainUtils.allowConflict(conflictChain, indexOfConflict)){
                    //Fork is allowed
                    //Create a new BlockChain that as all the blocks previous to the conflict
                    List<Block> newConflictChain = blockChainUtils.cloneBlockChain(conflictChain, indexOfConflict);
                    //Confirm again that the recvBlock prevHash points to the head hash
                    if (blockChainUtils.getHeadHash(newConflictChain).equals(recvBlock.getPreviousHash())){
                        newConflictChain.add(recvBlock);
                        conflictChains.add(newConflictChain);
                        //Try to resolve unconfirmed blocks
                        triggerUnconfirmedResolve(recvBlock.getHash());
                        wasBlockAdded = true;
                    }else{
                        //System.out.println("[Conflicts] Received block prevHash not equal to new Conflict Chain head hash");
                    }
                }else{
                    //conflict already to far back, deny the received block
                    //TODO: Send message back, received block should be mined again
                    wasBlockAdded=false;
                }

            }
        }
        return wasBlockAdded;
    }


    /**
     * Checks if the provided Hash matches with any previouHash of the unconfirmed blocks
     * if it does, starts to try to resolve the unconfirmed blocks
     * @param hash
     * @return
     */
    public synchronized void triggerUnconfirmedResolve(String hash){
        if(!unconfirmedBlocks.isEmpty()){
            for (Block unconfirmedBlock : unconfirmedBlocks){
                if(unconfirmedBlock.getPreviousHash().equals(hash)){
                    tryResolveUnconfirmedBlocks();
                }
            }
        }
    }

    public synchronized void tryResolveUnconfirmedBlocks(){
        if(!unconfirmedBlocks.isEmpty()){
            for (Block unconfirmedBlock : unconfirmedBlocks){
                if(handleNetWorkBlock(unconfirmedBlock)){
                    //if the unconfirmed block is added to the chain, resolve it
                    resolveUnconfirmedBlock(unconfirmedBlock);
                }
            }
        }
    }

    public synchronized void resolveUnconfirmedBlock(Block confirmedBlock){
        if(unconfirmedBlocks.contains(confirmedBlock)){
            unconfirmedBlocks.remove(confirmedBlock);
        }
    }

    public synchronized void sortAllListsBySize(){
        // Sort the list based on the sizes of the sub-lists
        if (this.conflictChains != null && !this.conflictChains.isEmpty()){
            this.conflictChains.sort(new Comparator<>() {
                @Override
                public int compare(List<Block> list1, List<Block> list2) {
                    return -Integer.compare(list1.size(), list2.size());
                }
            });

            this.chain = this.conflictChains.get(0);
        }
    }


    public void printAllChains(){
        String text = "";

        if(conflictChains.isEmpty()){
            System.out.println(this.chain.toString());
        }else{
            int i=1;
            for (List<Block> conflictChain : conflictChains){
                System.out.println("\n\n\r<==LIST "+i+" ==>");
                text="";
                for(Block b: conflictChain){
                    text+=b.toString()+"\n";
                }
                System.out.println(text);
                i++;
            }
        }


    }

    public void printAllChainsShort(){
        String text = "";

        if(conflictChains.isEmpty()){
            for (Block b : this.chain) {
                text += "\nPrevHash:" + b.getPreviousHash() + "\nHash:" + b.getHash() + "\n";
            }
            System.out.println(text);

        }else {
            int i = 1;
            for (List<Block> conflictChain : conflictChains) {
                System.out.println("\n\n\r<==LIST " + i + " ==>");
                text = "";
                for (Block b : conflictChain) {
                    text += "\nPrevHash:" + b.getPreviousHash() + "\nHash:" + b.getHash() + "\n";
                }
                System.out.println(text);
                i++;
            }
        }
    }


    public synchronized boolean addBlockFromClosedForkPOW(Block block) throws NoSuchAlgorithmException {
        if(conflictChains.isEmpty()){
            //If chain is empty previous hash is genesis hash, else, it's the head block hash
            if(this.chain.isEmpty()){
                block.setPreviousHash(Constants.GENESIS_PREV_HASH);
            }else {
                block.setPreviousHash(blockChainUtils.getHeadHash(this.chain));
            }
        }else{
            //If we have conflict chains, we mine a new block to the chain that is bigger
            //Sort all chains by size first
            sortAllListsBySize();
            //Pick the one that comes first
            String prevHash = blockChainUtils.getHeadHash(conflictChains.get(0));
            block.setPreviousHash(prevHash);
            block.generateHash();
        }

        //Mine Block
        Block minedBlock = blockChainUtils.mine(block);
        if (blockChainUtils.validateBlock(minedBlock)){
            //Add Mined Block
            addLocalMinedBlock(minedBlock);
            return true;
        }
        return false;
    }

    public synchronized void handleForkTerminate(List<Block> fork){

        for (Block blockToDiscard : fork){
            //What to do with the discarded blocks?
            //For now, mine them and send them to network again
            if(!conflictChains.get(0).contains(blockToDiscard)){
                //Block not in the main chain
                try {
                    //Mine block
                    //Clear Hash, if we keep the previous hash the block will pass the local validation in the mining function
                    //because already has the correct difficulty
                    if(addBlockFromClosedForkPOW(blockToDiscard)){
                        //Send to netWork
                        kademlia.doStore(blockToDiscard);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //When all discarded blocks are handled, remove fork from the fork list
        this.conflictChains.remove(fork);
    }

    public synchronized void tryResolveForks(){
        Stack<List<Block>> forksToClose = new Stack<>();
        sortAllListsBySize();
        if(!conflictChains.isEmpty()) {
            int biggestForkSize = this.conflictChains.get(0).size();
            for (List<Block> fork : this.conflictChains) {
                if (biggestForkSize - fork.size() > Constants.CONFLICT_DEPTH) {
                    forksToClose.add(fork);
                }
            }

            if (!forksToClose.isEmpty()) {
                while (!forksToClose.isEmpty()) {
                    handleForkTerminate(forksToClose.pop());
                }
            } else {
                //System.out.println("No forks can be resolved");
            }
        }
    }


    /*
    * Util
    * */

    public synchronized Block searchGetBlock(String key){
        Block blockToReturn = null;

        if(conflictChains.isEmpty()){
            int size = this.chain.size()-1;
            for (int i=size; i>=0; i--){
                Block block = this.chain.get(i);
                byte[] blockHashToKey = utils.hashingSHA1(block.getHash().getBytes());
                String keyConverted = utils.convertByteToBinaryString(blockHashToKey);
                if(keyConverted.equals(key)){
                    return block;
                }
            }
        }else {
            for (List<Block> chain : conflictChains){
                int size = chain.size()-1;
                for (int i=size; i>=0; i--){
                    Block block = chain.get(i);
                    byte[] blockHashToKey = utils.hashingSHA1(block.getHash().getBytes());
                    String keyConverted = utils.convertByteToBinaryString(blockHashToKey);
                    if(keyConverted.equals(key)){
                        return block;
                    }
                }
            }

        }

        return blockToReturn;
    }
}
