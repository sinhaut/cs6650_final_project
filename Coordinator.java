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

public class Coordinator extends UnicastRemoteObject implements RMIImpl {
    public static final String coordinatorLogFilePath = "coordinatorLogs.txt";
    public static int TIMEOUT = 10000;
    public static final int TOTAL_SERVERS = 5;
    public static int PORT = 1099;
    public static ArrayList<Participant> servers = new ArrayList<Participant>();
    public static String messageToClient;

    /*
    * Coordinator constructor initializes 5 participant servers and stores them in list.
    * */
    public Coordinator() throws RemoteException {
        super();
        Participant s1 = new Participant();
        Participant s2 = new Participant();
        Participant s3 = new Participant();
        Participant s4 = new Participant();
        Participant s5 = new Participant();
        servers.add(s1);
        servers.add(s2);
        servers.add(s3);
        servers.add(s4);
        servers.add(s5);
    }

    /*
    * commitOrNot() queries all participant servers initialized in the constructor
    * All servers return "YES" for the purpose of this project
    * */
    public synchronized AtomicInteger commitOrNot() throws RemoteException {
        AtomicInteger num_votes = new AtomicInteger(0);
        for (Participant s: servers) {
            if (s.vote() == "YES") {
                num_votes.getAndIncrement();
            }
        }
        return num_votes;
    }

    /*
    * Given a string operation, the commitOrNot method confirms that all servers are ready.
    * Callable task allows the commitOrNot to query the servers for TIMEOUT seconds
    * If any server leads to a time out, the operation is aborted.
    * */
    public String handleOperation(String operation) throws RemoteException{
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<AtomicInteger> task = new Callable<AtomicInteger>() {
            public AtomicInteger call() {
                try {
                    return commitOrNot();}
                catch (RemoteException e) {
                    return null;
                }
            }
        } ;

        Future<AtomicInteger> future = executor.submit(task);
        Integer servers_ready = 0;

        // Query a future within TIMEOUT seconds to confirm how many servers are ready
        try {
            servers_ready = future.get(TIMEOUT, TimeUnit.MILLISECONDS).get();
        } catch (TimeoutException e) {
            System.out.println("Timeout limit reached in accessing servers.");
        } catch (InterruptedException | ExecutionException e2 ) {
            System.out.println("Interrupted or execution exception.");
        }

        // All servers ready leads to global commit; else, globalAbort
        if (servers_ready == TOTAL_SERVERS) {
            globalCommit(operation);
        } else {
            globalAbort(operation);
        }

        return messageToClient;
    }

    /*
    * All servers commit the string operation.
    * */
    public synchronized void globalCommit(String operation) throws RemoteException{
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        writeToCoordinatorLog("Time: " + timestamp +
                " | Global Commit for Message: " + operation + "\n");

        for (Participant s: servers) {
            messageToClient = s.doOperation(operation);
        }
    }

    public synchronized void globalAbort(String operation) throws RemoteException{
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        writeToCoordinatorLog("Time: " + timestamp +
                " | Global Abort for Message: " + operation + "\n");
        messageToClient = "Aborted: " + operation + "\n";
    }

    public synchronized void writeToCoordinatorLog(String log) throws RemoteException{
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

    public static void main(String[] args) throws RemoteException{
        int port = PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Coordinator coordinator = new Coordinator();

        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.bind("RMIImpl", coordinator);
            System.out.println("Coordinator bound to RMI listening to port: " + port);
        } catch (Exception e) {
            System.out.println("Failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

