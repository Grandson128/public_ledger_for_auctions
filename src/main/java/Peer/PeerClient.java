package Peer;

import Auction.AuctionApp;
import BlockChain.*;
import Kademlia.Kademlia;
import Utils.Utils;

import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class PeerClient implements Runnable{

    private Scanner scanner;
    private Kademlia kademlia;

    private Utils utils;
    private BlockChain myBlockChain;
    private AuctionApp auctionApp;

    private void printMenu()
    {
        System.out.println("\n");
        System.out.println("Command List:");
        System.out.println("---------------------------------------------------------------------------");
        System.out.println("|        Kademlia       |       BlockChain       |         Auction        |");
        System.out.println("---------------------------------------------------------------------------");
        System.out.println("| 1 - enterKademlia()   | 4 - storeBlock()       | 6 - startAuction()     |");
        System.out.println("| 2 - printKBucket()    | 5 - printBlockChain()  | 7 - printAllAuctions() |");
        System.out.println("| 3 - findValue()       |                        | 8 - printMyAuctions()  |");
        System.out.println("|                       |                        | 9 - sendBid()          |");
        System.out.println("|                       |                        | 10 - searchTopic()     |");
        System.out.println("---------------------------------------------------------------------------");
        //System.out.println("5 - findNode()");
        //System.out.println("7 - store()");
        //System.out.println("8 - printStoredValues()");
        //System.out.println("9 - doFindValue()");

    }

    public PeerClient() {
        this.kademlia = Kademlia.getInstance();
        this.myBlockChain = BlockChain.getInstance();
        this.auctionApp = AuctionApp.getInstance();
        this.utils = Utils.getInstance();
        this.scanner = new Scanner(System.in);
        printMenu();
    }

    @Override
    public void run() {
        while (true){

            String IP;
            String ID;
            String Port;

            System.out.print("\u001B[34m" + "\n$ " + "\u001B[0m");
            String command = scanner.nextLine();


            if(!kademlia.isInsideNetwork() && !command.equals("1")){
                System.out.println("Error - Node needs to enter in the network");
                continue;
            }

            switch (command){
                case "help":
                    printMenu();
                    break;

                case "2":
                    kademlia.printKBucket();
                    //sendPingRequest("localhost", 1050);
                    break;
                /*
                case "2":
                    System.out.print("IP: ");
                    IP = scanner.nextLine();
                    System.out.print("ID: ");
                    ID = scanner.nextLine();
                    System.out.print("PORT: ");
                    Port = scanner.nextLine();

                    kademlia.insertNode(IP, ID, Port );
                    break;

                case "5":
                    System.out.print("Target ID: ");
                    ID = scanner.nextLine();

                    //kademlia.sendFindRequest(3,"localhost", String.valueOf(9090),targetID);
                    kademlia.doFind(ID);
                    break;

                case "3":
                    kademlia.deleteAllNodes();
                    break;

                case "4":
                    System.out.print("Number of Nodes: ");
                    String number = scanner.nextLine();

                    kademlia.generateNodes(Integer.parseInt(number));
                    break;
                */
                case "1":
                    kademlia.enterKademlia();
                    break;

                case "3":
                    String key;
                    System.out.print("Key to search: ");
                    key = scanner.nextLine();

                    kademlia.doFindValue(key);
                    break;

                case "4":

                    TestBlockChain test = new TestBlockChain();
                    Block testBlock = test.testNewBlock();

                    try {
                        myBlockChain.addBlockPOW(testBlock);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }


                    myBlockChain.printAllChainsShort();
                    kademlia.doStore(testBlock);
                    break;

                case "5":
                    //System.out.println(myBlockChain.toString());
                    myBlockChain.printAllChainsShort();
                    break;

                case "6":
                    String name;
                    String basePrice;

                    System.out.print("Auction name: ");
                    name = scanner.nextLine();

                    System.out.print("Base price: ");
                    basePrice = scanner.nextLine();

                    auctionApp.createNewAuction(name, Integer.parseInt(basePrice));
                    break;

                case "7":
                    auctionApp.printAllAuctions();
                    break;

                case "8":
                    auctionApp.printMyAuctions();
                    break;

                case "9":

                    System.out.print("Topic: ");
                    String topic = scanner.nextLine();

                    System.out.print("Bid Value: ");
                    String amount = scanner.nextLine();

                    auctionApp.publishBid(topic, Integer.parseInt(amount));
                    break;
                case "10":

                    System.out.print("Topic: ");
                    String topicToSearch = scanner.nextLine();
                    System.out.print(auctionApp.searchAllAuctions(topicToSearch));


                    break;
                default:
                    System.out.println("Command not found!");
                    break;
            }

        }
    }
}
