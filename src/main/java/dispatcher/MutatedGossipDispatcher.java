package dispatcher;

import io.atomix.utils.net.Address;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MutatedGossipDispatcher extends MessageDispatcher {
    /*
     * Dispatcher inspired on Gossip approach without messages retransmission.
     * For each message broadcast:
     *  * $fanout$ processes are selected to send the message immediately
     *  * For the rest, the message is scheduled to be sent with differents delays
     *  * The sending can be canceled if a message has been received from the target for a round greater than the scheduled message
     */
    private final int delay;
    private final TimeUnit delayTimeUnit;
    private final int fanout;
    private final Map<Address, Integer> roundsByAddress;

    public MutatedGossipDispatcher(Address address, DispatcherEventsRegister register, SimulatedPlatform platform, int fanout, int delay, TimeUnit delayTimeUnit) {
        super(address,register,platform);
        this.roundsByAddress = new HashMap<>();
        this.fanout = fanout;
        this.delay = delay;
        this.delayTimeUnit = delayTimeUnit;
    }

    @Override
    public void registerHandler(String s, Consumer<Message> consumer) {
        super.registerHandler(s, (from, bytes) ->{
            Message msg = serializer.decode(bytes);
            if( msg.getSender() == null)
                msg.setSender(from);
            roundsByAddress.merge(msg.getSender(), msg.round, Integer::max);
            consumer.accept(msg);
        });
    }

    @Override
    public synchronized void broadcast(String tag, List<Address> participants, Message msg) {
        super.broadcast(tag, participants.subList(0,fanout), msg);
        for(int i = fanout; i < participants.size(); i ++){
            final Address addr = participants.get(i);
            final long msgDelay = delay * (long) (i-fanout+1);
            executor.schedule(() -> sendMessage(addr, tag, msg), msgDelay, delayTimeUnit);
        }
     }

    @Override
    public synchronized void broadcast(String tag, List<Address> participants, List<Message> msgList) {
        super.broadcast(tag, participants.subList(0,fanout), msgList.subList(0,fanout));

        for (int i = fanout; i < participants.size(); i++) {
            Address addr = participants.get(i);
            Message msg = msgList.get(i);
            final long msgDelay = delay * (long) (i-fanout+1);
            executor.schedule(() -> sendMessage(addr, tag, msg), msgDelay, delayTimeUnit);
        }
    }

    private synchronized void sendMessage(Address addr, String tag, Message msg){
        if (roundsByAddress.getOrDefault(addr, 0) <= msg.round)
            sendAsync(addr,tag, serializer.encode(msg));
    }
}