import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Dispatcher extends UnicastRemoteObject implements RMIImpl {
    public static final String coordinatorLogFilePath = "coordinatorLogs.txt";
    public static int PORT = 1099;
    public  ArrayList<Spider> spiders;
    public  Queue<String> queueOfLinksToCrawl;
    public  ArrayList<String> visitedLinks;
    public  ArrayList<String> blacklistedLinks;
    public  HashMap<Spider, String> spiderToLinkMap;
    public  HashSet<String> setOfFailedLinks;

    public boolean running;
    public Logger logger;

    /*
     * Coordinator constructor initializes 5 participant servers and stores them in list.
     * */
    public Dispatcher() throws RemoteException {
        super();
        this.queueOfLinksToCrawl = new LinkedList<>();
        this.visitedLinks = new ArrayList<>();
        this.blacklistedLinks = new ArrayList<>();
        this.spiderToLinkMap = new HashMap<>();
        this.setOfFailedLinks = new HashSet<>();
        this.running = true;
        this.logger = new Logger();
        this.spiders = new ArrayList<Spider>();

    }

    public void populateSpiders() throws RemoteException {
        int numSpiders = 5;
        for (int i = 0; i < numSpiders; i++) {
            Spider spider = new Spider();
            spiders.add(spider);
            Thread t = new Thread(spider);
            t.start();
        }
    }

    /**
     * Given a string operation, the commitOrNot method confirms that all servers are ready.
     * Callable task allows the commitOrNot to query the servers for TIMEOUT seconds
     * If any server leads to a timeout, the operation is aborted.
     */
    public String acceptClientRequest(String link) throws RemoteException {
        queueOfLinksToCrawl.add(link);
        return "Successfully added " + link + " to the queue of links to crawl.";
    }

    /**
     * If a spider is not living, respawn it.
     * Add the current link that spider is crawling back to the queueOfLinksToCrawl
     */
    public void ensureSpidersAreAlive() throws RemoteException {
        ArrayList<Spider> deadSpiders = new ArrayList<Spider>();
        for (Spider s : spiders) {
            boolean alive = s.spiderIsAlive();
            if (alive == true) {
                deadSpiders.add(s);
            }
            //TODO: figure out how to check that spider responds
            //TODO: set a timeout and if it doesn't respond in the timeout, make a new spider
            //TODO: failed spider's link gets added to the queue again (doesn't get added to the queue)
        }
        for (Spider s : deadSpiders) {
            s.kill(); // ensures the spider terminates.
            Spider newSpider = new Spider();
            spiders.add(newSpider); // replace that spider.
            newSpider.run(); //
        }

    }

    /**
     * Write to a log for the dispatcher
     */
    public void writeToDispatcherLog(String log) throws RemoteException {
        // Write logs to coordinatorLog.txt for every message sent/received from
        File file = new File(coordinatorLogFilePath);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.append(log);
            bufferedWriter.close();
        } catch (IOException e) {
            Logger.log(e.getMessage());
        }
    }

    /**
     * Receives a list of outlinks from a spider and adds them to queue.
     */
    public void processOutlinksFromSpider(ArrayList<String> outlinksList) throws RemoteException {
        for (String link : outlinksList) {
            if (!visitedLinks.contains(link) && !blacklistedLinks.contains(link)) {
                queueOfLinksToCrawl.add(link);
            }
        }
    }

    /**
     * On spider IllegalArgumentException, this will throw.
     * */
    public void addToFailedLinks (String link) throws RemoteException{
        this.setOfFailedLinks.add(link);
        Logger.log("Link failed: "+ link);
    }

    /**
     * Handle failed urls if they have a non-200 response code.
     */
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
     * Returns the link from the top of the queue to a spider to crawl the link.
     */
    public String getLinkToCrawl() throws RemoteException {
        // TODO: lock the queue so 1 spider accesses at a time
        Lock lock = new ReentrantLock();
        lock.lock();
        String topLink = queueOfLinksToCrawl.remove();
        visitedLinks.add(topLink);
        // TODO: add to the spiderToLink hashmap with the spider and the link.
        lock.unlock();
        return topLink;
    }

    public void killSpiders(){
        for (Spider spider : spiders) {
            spider.kill();
        }
    }

    public static void main(String[] args) throws UnknownHostException, AlreadyBoundException, NotBoundException, RemoteException, InterruptedException {
        int port = PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        Dispatcher dispatcher = new Dispatcher();
        try {
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("RMIImpl", dispatcher);
            while(dispatcher.queueOfLinksToCrawl.size()==0){
                TimeUnit.SECONDS.sleep(1); // Wait for client to connect
            }
            Logger.log("Dispatcher bound to RMI listening to port: " + port);
            dispatcher.populateSpiders(); // creates spiders, puts them to work.
            if (dispatcher.visitedLinks.size() >10) {
                Logger.log("Goodbye. ");
                dispatcher.running = false;
                return;
            }
        } catch (Exception e) {
            Logger.log("Failure: " + e.getMessage());
            e.printStackTrace();
        }
    }


}

