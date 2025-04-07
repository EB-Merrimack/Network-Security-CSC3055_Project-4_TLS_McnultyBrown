package common.protocol.messages;

public class StatusMessage {
    // Change payload to String for easier handling of textual data
    public String payload;

    // Constructor for easy instantiation
    public StatusMessage(String payload) {
        this.payload = payload;
    }

    // Optional: Override toString() for easier debugging/logging
    @Override
    public String toString() {
        return "StatusMessage{payload='" + payload + "'}";
    }
}
