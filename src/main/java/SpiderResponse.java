import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Spider response is  a class to handle storing the crawl data responses together neatly.
 * */
public class SpiderResponse {
    private String incomingUrl;
    private ArrayList<String> outgoingUrls;
    private Integer statusCode;
    private String result;

    public SpiderResponse(String incomingUrl) {
        this.incomingUrl = incomingUrl;
        this.outgoingUrls = new ArrayList<String>();
        this.statusCode = 0; // placeholder
        this.result = "";
    }

    public String getIncomingUrl() {
        return incomingUrl;
    }

    public void setIncomingUrl(String incomingUrl) {
        this.incomingUrl = incomingUrl;
    }

    public ArrayList<String> getOutgoingUrls() {
        return outgoingUrls;
    }

    public void setOutgoingUrls(ArrayList<String> outgoingUrls) {
        this.outgoingUrls = outgoingUrls;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result, int id) {
        this.result = result;
        this.writeResultToFile(result, id);
    }

    public void writeResultToFile(String result, int id) {
        String[] all = this.incomingUrl.toLowerCase().split("/");
        String fName = all[all.length - 1];
        String filePath = "index/spider" + id + fName + ".txt";
        File file = new File(filePath);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(result);
            bufferedWriter.close();
        } catch (IOException e) {
            Logger.log(e.getMessage());
        }
    }
}
