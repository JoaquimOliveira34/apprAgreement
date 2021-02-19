package dispatcher;

import io.atomix.utils.net.Address;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MutatedGossipDispatcher extends MessageDispatcher {
    /*
     * Dispatcher inspired on Gossip approach without messages retransmission.
     * For each message broadcast:
     *  * $fanout$ processes are selected to message be send immediately
     *  * For the remaining processes ( $n$ - $fanout$ ) the message is delayed.
     *  * Messages are delayed in group ( with size = $delayGroupSize$) each one with a unique $delay$ multiple.
     *  * The sending can be canceled if a message has been received from the target with a round greater than the scheduled message
     */
    private final int delay;
    private final TimeUnit delayTimeUnit;
    private final int delayGroupSize;
    private final int fanout;
    private final Map<Address, Integer> roundsByAddress;

    public MutatedGossipDispatcher(Address address,
                                   DispatcherEventsRegister register,
                                   SimulatedPlatform platform,
                                   int fanout,
                                   int delay,
                                   int delayGroupSize,
                                   TimeUnit delayTimeUnit) {
        super(address,register,platform);
        this.roundsByAddress = new HashMap<>();
        this.fanout = fanout;
        this.delay = delay;
        this.delayTimeUnit = delayTimeUnit;
        this.delayGroupSize = delayGroupSize;
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
        // this is not the best option for performance but keep code easier to maintain
        broadcast(tag, participants, Collections.nCopies(participants.size(), msg));
     }

    @Override
    public synchronized void broadcast(String tag, List<Address> participants, List<Message> msgList) {
        super.broadcast(tag, participants.subList(0,fanout), msgList.subList(0,fanout));
        Map<Address, Message> messagesGroup = new HashMap<>();
        int groupDelay = delay;
        for(int i = fanout; i < participants.size(); i ++){
            if( messagesGroup.size() == delayGroupSize ){
                Map<Address, Message> messagesGroupClone = new HashMap<>(messagesGroup);
                executor.schedule(() -> sendMessage(messagesGroupClone, tag), groupDelay, delayTimeUnit);
                groupDelay += delay;
                messagesGroup.clear();
            }
            messagesGroup.put(participants.get(i), msgList.get(i));
        }
        if( ! messagesGroup.isEmpty())
            executor.schedule(() -> sendMessage(messagesGroup, tag), groupDelay, delayTimeUnit);

    }

    private synchronized void sendMessage(Map<Address, Message> messages, String tag){
        for( Address target : messages.keySet()){
            Message msg = messages.get(target);
            if (roundsByAddress.getOrDefault(target, 0) <= msg.round)
                sendAsync(target, tag, serializer.encode(msg));
        }
    }
}