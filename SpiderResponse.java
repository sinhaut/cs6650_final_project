import java.util.ArrayList;

public class SpiderResponse {
    private String incomingUrl;
    private ArrayList<String> outgoingUrls;
    private int statusCode;
    private String result;

    public SpiderResponse(String incomingUrl) {
        this.incomingUrl = incomingUrl;
        this.outgoingUrls = new ArrayList<String>();
        this.statusCode = 0; // placeholder
        this.result = "";
    }

    public SpiderResponse(String incomingUrl, ArrayList<String> outgoingUrls, int statusCode, String result) {
        this.incomingUrl = incomingUrl;
        this.outgoingUrls = outgoingUrls;
        this.statusCode = statusCode;
        this.result = result;
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

    public void setResult(String result) {
        this.result = result;
    }
}
