package dispatcher;

import io.atomix.utils.net.Address;

public class Message {
    public final double value;
    public final int round;
    private Address sender;
    private String type;

    public Message(double value, int round) {
        this.value = value;
        this.round = round;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Address getSender() {
        return sender;
    }

    public void setSender(Address sender) {
        this.sender = sender;
    }

    public boolean equals(Object Object) {
        if (this == Object)
            return true;

        if (Object instanceof Message) {
            Message msg = (Message) Object;
            return msg.value == this.value && msg.round == this.round && msg.sender.equals(this.sender) && msg.type.equals(this.type);
        }
        return false;
    }
}
