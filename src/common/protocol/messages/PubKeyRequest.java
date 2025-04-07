package common.protocol.messages;

public class PubKeyRequest {
    public String type;
    public String user;

    public PubKeyRequest() {
        this.type = "PubKeyRequest";
    }

    public PubKeyRequest(String user) {
        this.type = "PubKeyRequest";
        this.user = user;
    }
}
