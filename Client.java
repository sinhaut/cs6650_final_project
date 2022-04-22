import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.ArrayList;
import java.sql.Timestamp;

public class Client {
    private static final String clientLogFilePath = "clientLog.txt";
    private static final String prepopulateFileName = "preload.txt";
    private static final String operationsFileName = "operations.txt";

    public static List<String> getListFromFile(String filePath) throws IOException {
        // Load text file of operations to send to the server into a list of strings.
        return getStrings(filePath);
    }

    static List<String> getStrings(String filePath) throws IOException {
        List<String> fileAsList = new ArrayList<>();
        BufferedReader bufferedReader;
        File file = new File(filePath);
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            fileAsList.add(line);
        }

        return fileAsList;
    }

    public static void writeToClientLog(String log) {
        // Append a log message to the client log file.
        File file = new File(clientLogFilePath);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.append(log);
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void run(RMIImpl stub, String fileName) throws IOException{
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<String> operationsList = getListFromFile(fileName);
        for (String operation: operationsList) {
            String result = stub.handleOperation(operation);
            System.out.println("Response from the RMI-server: \"" + result + "\n");
            writeToClientLog("Time: " + timestamp
                    + " | Response from the RMI server: " + result + "\n");
        }
    }

    public static void main(String[] args) {
        // Establish a server connection given a host and port
        //int port = Integer.parseInt(args[1]);
        String host = "localhost";
        int port = 1099;
        if (args.length > 1) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            RMIImpl stub = (RMIImpl) registry.lookup("RMIImpl");
            run(stub, prepopulateFileName);
            run(stub, operationsFileName);
        }
        catch (Exception e) {
            System.out.println("RMIClient exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
