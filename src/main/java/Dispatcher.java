import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Dispatcher extends UnicastRemoteObject implements RMIImpl {
    public static final String coordinatorLogFilePath = "coordinatorLogs.txt";
    public static int PORT = 1099;
    public static int MAX_LINKS_TO_CRAWL = 10;
    public ArrayList<Spider> spiders;
    public BlockingQueue<String> queueOfLinksToCrawl;
    public ArrayList<String> visitedLinks;
    public ArrayList<String> blacklistedLinks;
    public HashMap<Integer, String> spiderToLinkMap;
    public HashSet<String> setOfFailedLinks;
    ArrayList<Thread> threads;

    public boolean running;
    public Logger logger;
    public int spiderIdentifier;

    /**
     * Coordinator constructor initializes 5 participant servers and stores them in list.
     **/
    public Dispatcher() throws RemoteException {
        super();
        this.queueOfLinksToCrawl = new LinkedBlockingQueue<>();
        this.visitedLinks = new ArrayList<>();
        this.blacklistedLinks = new ArrayList<>();
        this.spiderToLinkMap = new HashMap<>();
        this.setOfFailedLinks = new HashSet<>();
        this.running = true;
        this.logger = new Logger();
        this.spiders = new ArrayList<Spider>();
        this.spiderIdentifier = 0;
        this.threads = new ArrayList<>();

    }

    /**
     *
     * */
    public void populateSpiders() throws RemoteException {
        int numSpiders = 5;

        for (int i = 0; i < numSpiders; i++) {
            Spider spider = new Spider(i);
            spiders.add(spider);
            Thread t = new Thread(spider);
            threads.add(t);
        }
        for (Thread t : threads) {
            t.start(); //spiders start working
        }

        this.spiderIdentifier = spiders.size();
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
            boolean alive = true;
            try {
                alive = s.getRunning();
            }
            catch (Exception e) {
                alive = false;
            }
            if (!alive) {
                System.out.println("Spider not alive " + s.identifier);
                deadSpiders.add(s);
                String linkToAddBack = spiderToLinkMap.get(s.identifier);
                queueOfLinksToCrawl.add(linkToAddBack);
            }
        }

        for (Spider s : deadSpiders) {
            s.kill(); // ensures the spider terminates.
            this.spiderIdentifier++;
            Spider newSpider = new Spider(this.spiderIdentifier);
            spiders.add(newSpider); // replace that spider.
            Thread t = new Thread(newSpider);
            threads.add(t);
            t.start(); //
        }

    }

    /**
     * Write to a log for the dispatcher.
     */
    public synchronized void writeToDispatcherLog(String log) throws RemoteException {
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
    public synchronized void processOutlinksFromSpider(ArrayList<String> outlinksList) throws RemoteException, MalformedURLException {
        for (String link : outlinksList) {
            if (!visitedLinks.contains(link) && !blacklistedLinks.contains(link)
                    && !queueOfLinksToCrawl.contains(link)) {

                queueOfLinksToCrawl.add(link);
            }
        }
    }

    /**
     * On spider IllegalArgumentException, this will throw.
     */
    public synchronized void addToFailedLinks(String link) throws RemoteException {
        this.setOfFailedLinks.add(link);
        Logger.log("Link failed: " + link);
    }

    /**
     * Handle failed urls if they have a non-200 response code.
     */
    public synchronized void processResponseCode(String link, Integer responseCode) throws RemoteException {
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
    public synchronized String getLinkToCrawl(int id) throws RemoteException {
        if (queueOfLinksToCrawl.isEmpty()) {
            System.out.println("Queue of links to crawl is empty");
            System.exit(0);
        }
        String topLink = queueOfLinksToCrawl.remove();
        visitedLinks.add(topLink);
        spiderToLinkMap.put(id, topLink);

        return topLink;
    }

    public void killSpiders() throws InterruptedException {
        for (Spider spider : spiders) {
            spider.kill();
        }

        for (Thread t: threads) {
            t.join(10000);
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
            while (dispatcher.queueOfLinksToCrawl.size() == 0) {
                TimeUnit.SECONDS.sleep(1); // Wait for client to connect
            }
            Logger.log("Dispatcher bound to RMI listening to port: " + port);
            dispatcher.populateSpiders(); // creates spiders, puts them to work.
            int currvisited = 0;

            while (currvisited < MAX_LINKS_TO_CRAWL) {
                if (dispatcher.visitedLinks.size() > currvisited) {
                    dispatcher.ensureSpidersAreAlive();
                }
                currvisited = dispatcher.visitedLinks.size();
            }

            dispatcher.killSpiders();
            dispatcher.running = false;
            Logger.log("Goodbye. ");
            System.out.println("Spiders killed and dispatcher is no longer running. Exiting");
            System.exit(0);
            //return;

        } catch (Exception e) {
            Logger.log("Failure: " + e.getMessage());
            e.printStackTrace();
        }
    }


}

