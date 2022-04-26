import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.*;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

public class Dispatcher extends UnicastRemoteObject implements RMIImpl {
    public static final String coordinatorLogFilePath = "coordinatorLogs.txt";
    public static int PORT = 1099;
    public static ArrayList<Spider> spiders = new ArrayList<Spider>();
    public static Queue<String> queueOfLinksToCrawl;
    public static ArrayList<String> visitedLinks;
    public static ArrayList<String> blacklistedLinks;
    public static HashMap<Spider, String> spiderToLinkMap;
    public static HashSet<String> setOfFailedLinks;
    /*
    * Coordinator constructor initializes 5 participant servers and stores them in list.
    * */
    public Dispatcher() throws RemoteException {
        super();
        Spider s1 = new Spider();
        Spider s2 = new Spider();
        Spider s3 = new Spider();
        Spider s4 = new Spider();
        Spider s5 = new Spider();
        spiders.add(s1);
        spiders.add(s2);
        spiders.add(s3);
        spiders.add(s4);
        spiders.add(s5);
        queueOfLinksToCrawl = new LinkedList<>();
        visitedLinks = new ArrayList<>();
        blacklistedLinks = new ArrayList<>();
        spiderToLinkMap = new HashMap<>();
        setOfFailedLinks = new HashSet<>();
    }

    /**
    Given a string operation, the commitOrNot method confirms that all servers are ready.
    Callable task allows the commitOrNot to query the servers for TIMEOUT seconds
    If any server leads to a timeout, the operation is aborted.
    * */
    public String acceptClientRequest(String link) throws RemoteException{
        queueOfLinksToCrawl.add(link);
        return "Successfully added " + link + " to the queue of links to crawl.";
    }

    /**
     If a spider is not living, respawn it.
     Add the current link that spider is crawling back to the queueOfLinksToCrawl
     * */
    public void ensureSpidersAreAlive() throws RemoteException {
        for (Spider s: spiders) {
            //TODO: figure out how to check that spider responds
            //TODO: set a timeout and if it doesn't respond in the timeout, make a new spider
            //TODO: failed spider's link gets added to the queue again (doesn't get added to the queue)
        }
    }

    /**
    Write to a log for the dispatcher
    * */
    public void writeToDispatcherLog(String log) throws RemoteException{
        // Write logs to coordinatorLog.txt for every message sent/received from
        File file = new File(coordinatorLogFilePath);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.append(log);
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     Receives a list of outlinks from a spider and adds them to queue.
     * */
    public void processOutlinksFromSpider(ArrayList<String> outlinksList) throws RemoteException {
        for (String link: outlinksList) {
            if (!visitedLinks.contains(link) && !blacklistedLinks.contains(link)) {
                queueOfLinksToCrawl.add(link);
            }
        }
    }

    /**
     Handle failed urls if they have a non-200 response code.
     * */
    public void processResponseCode(String link, Integer responseCode) throws RemoteException {

        if (responseCode != 200) {
            // if the url leads to 2 failures, blacklist it
            if (setOfFailedLinks.contains(link)) {
                blacklistedLinks.add(link);
            } else {
                // if it is the first failure, add it to failed set and readd to queue
                setOfFailedLinks.add(link);
                queueOfLinksToCrawl.add(link);
            }
        }
    }

    /**
     Returns the link from the top of the queue to a spider to crawl the link.
     * */
    public String getLinkToCrawl() throws RemoteException {
        // TODO: lock the queue so 1 spider accesses at a time
        String topLink = queueOfLinksToCrawl.remove();
        visitedLinks.add(topLink);
        // TODO: add to the spiderToLink hashmap with the spider and the link.
        return topLink;
    }

    public static void main(String[] args) throws RemoteException{
        int port = PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Dispatcher dispatcher = new Dispatcher();

        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.bind("RMIImpl", dispatcher);
            System.out.println("Dispatcher bound to RMI listening to port: " + port);
        } catch (Exception e) {
            System.out.println("Failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

