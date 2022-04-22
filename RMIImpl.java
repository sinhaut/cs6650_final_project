import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

public interface RMIImpl extends Remote {
    AtomicInteger commitOrNot() throws RemoteException;
    void globalAbort(String operation) throws RemoteException;
    void globalCommit(String operation) throws RemoteException;
    void writeToCoordinatorLog(String log) throws RemoteException;
    String handleOperation(String operation) throws RemoteException;
}
