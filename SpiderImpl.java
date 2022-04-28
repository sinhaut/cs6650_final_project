import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public interface SpiderImpl extends Remote {
    boolean spiderIsAlive() throws RemoteException;
}
