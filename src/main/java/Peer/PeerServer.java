package Peer;

import Kademlia.Kademlia;
import Kademlia.KademliaService;
import Utils.Utils;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

public class PeerServer implements Runnable{

    private Logger LOGGER;

    private int port;

    private Kademlia kademlia;
    private Utils utils;

    public PeerServer(int port)
    {
        this.port = port;
        this.kademlia = Kademlia.getInstance();
        this.utils = Utils.getInstance();

        this.LOGGER = utils.getLOGGER();
    }

    @Override
    public void run() {

        Server server =  ServerBuilder.forPort(port).addService(new KademliaService(kademlia)).build();

        try {
            server.start();
        } catch (IOException e) {
            LOGGER.error("[PeerServer] - server.start()");
        }

        LOGGER.info("Server started at " + server.getPort());

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            LOGGER.error("[PeerServer] - server.awaitTermination()");
        }

    }

}

