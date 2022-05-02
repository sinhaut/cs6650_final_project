import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Participant {
    private static Map<Integer, String> keyValueStore;

    public Participant() {
        keyValueStore = new ConcurrentHashMap<>();
    }

    public String vote() {
        return "YES";
    }

    public synchronized String put(int key, String val) {
        if (keyValueStore.containsKey(key)) {
            keyValueStore.replace(key, val);
            return "Success. Key: " + key +
                    " already exists in key value store. Value has been updated";
        }
        keyValueStore.put(key, val);
        return "Success. Added new key: "
                + key + ", value: " + val + " to key value store.";
    }

    public String get(int key) {
        if (!keyValueStore.containsKey(key)) {
            return "Failure. This key does not exist in the key value store. " +
                    "Cannot get value.";
        }
        return "Success. The value for key: " + key + " is: " + keyValueStore.get(key) + ".";
    }

    public synchronized String delete(int key) {
        if (!keyValueStore.containsKey(key)) {
            return "Failure. This key " + key +
                    " does not exist in the key value store." +
                    " Cannot be deleted.";
        }

        keyValueStore.remove(key);
        return "Success. Deleted key: " + key + " from the key value store.";
    }

    public boolean checkForKey(String[] rawOperation) {
        try {
            Integer.parseInt(rawOperation[1]);
        } catch (NumberFormatException n) {
            return false;
        }
        return true;
    }

    public String doOperation(String message) {
        String[] rawOperation = message.split(" ");
        boolean keyValid = checkForKey(rawOperation);
        String messageToClient = "Request " + message + ". ";

        if (!keyValid) {
            messageToClient += "Received malformed request of length " + message.length();
        } else if (rawOperation[0].equalsIgnoreCase(Operation.PUT.name())) {
            messageToClient += put(Integer.parseInt(rawOperation[1]), rawOperation[2]);
        } else if (rawOperation[0].equalsIgnoreCase(Operation.GET.name())) {
            messageToClient += get(Integer.parseInt(rawOperation[1]));
        } else if (rawOperation[0].equalsIgnoreCase(Operation.DELETE.name())) {
            messageToClient += delete(Integer.parseInt(rawOperation[1]));
        } else {
            messageToClient += "Unable to parse message received from client.";
        }

        return messageToClient;
    }

    enum Operation {
        PUT, DELETE, GET
    }
}
