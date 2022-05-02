import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RMIImpl extends Remote {
    String getLinkToCrawl(int id) throws RemoteException;

    String acceptClientRequest(String link) throws RemoteException;

    void ensureSpidersAreAlive() throws RemoteException;

    void writeToDispatcherLog(String log) throws RemoteException; // status update for certain link

    void processResponseCode(String link, Integer responseCode) throws RemoteException;

    void processOutlinksFromSpider(ArrayList<String> outlinksList) throws RemoteException, MalformedURLException;

    void addToFailedLinks(String link) throws RemoteException;
}
