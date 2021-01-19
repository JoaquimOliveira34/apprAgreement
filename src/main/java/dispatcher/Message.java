package dispatcher;

import io.atomix.utils.net.Address;

public class Message {
    public final double value;
    public final int round;
    private Address sender;

    public Message(double value, int round) {
        this.value = value;
        this.round = round;
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
            return  msg.value == this.value &&
                    msg.round == this.round &&
                    msg.sender.equals(this.sender);
        }
        return false;
    }
}
