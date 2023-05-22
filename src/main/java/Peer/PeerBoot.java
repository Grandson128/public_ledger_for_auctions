package Peer;

import Auction.AuctionApp;
import BlockChain.BlockChain;
import Kademlia.Kademlia;
import Kademlia.KademliaUtils;
import Utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import static java.lang.Thread.sleep;

public class PeerBoot {

    String IP;
    int port;
    String portString;

    private static Logger LOGGER = null;

    public PeerBoot(String IP) {
        this.IP = IP;
        this.port = 9090;
        this.portString = "9090";

        LOGGER = LogManager.getLogger(Peer.class);

        System.setProperty("filename", port + ".log");
        Configurator.initialize(null, "log4j2.xml");

    }

   private static void printInfo(PeerBoot peer){
       System.out.println("\n\n========================");
       System.out.println("Node info:");
       System.out.println("IP = " + peer.IP);
       System.out.println("Port = " + peer.port);
       System.out.println("========================\n\n");
   }

   public static void main(String[] args){

       PeerBoot peer = new PeerBoot(args[0]);

       printInfo(peer);

       try {
           sleep(1000);
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }

       Utils utils = Utils.getInstance();
       utils.setLOGGER(LOGGER);
       utils.setMyIP(peer.IP);
       utils.setMyPort(peer.portString);

       Wallet wallet = Wallet.getInstance();

       KademliaUtils kademliaUtils = KademliaUtils.getInstance();
       kademliaUtils.setMyIP(peer.IP);
       kademliaUtils.setMyPort(peer.portString);

       Kademlia kademlia = Kademlia.getInstance();
       LOGGER.info("Kademlia ready!");

       BlockChain myBlockChain = BlockChain.getInstance();
       AuctionApp auctionApp = AuctionApp.getInstance();

       new Thread(new PeerServer(peer.port)).start();
       LOGGER.info("Server ready!");

       try {
           sleep(500);
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }

       new Thread(new PeerClient()).start();
       LOGGER.info("Client ready!");
   }
}
