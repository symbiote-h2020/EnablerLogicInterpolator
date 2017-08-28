package eu.h2020.symbiote.smeur.eli.model;

public class MessageRequest {
    private String request;

    public MessageRequest(String request) {
        this.request = request;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}
