import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SpiderImpl extends Remote {
    boolean spiderIsAlive() throws RemoteException;
}
