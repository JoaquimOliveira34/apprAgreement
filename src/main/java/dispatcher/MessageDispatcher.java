package dispatcher;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.MessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MessageDispatcher {
    private int messagesReceived, messagesSent;
    protected final ManagedMessagingService ms;
    protected final Serializer serializer;
    protected final ScheduledExecutorService executor;
    protected final DispatcherEventsRegister register;

    public MessageDispatcher(Address address, DispatcherEventsRegister register, SimulatedPlatform platform) {
        this.register = register;
        this.executor = platform.newScheduledThreadPool();
        this.ms = platform.newMessagingService(null, address, new MessagingConfig());
        this.serializer = new SerializerBuilder()
                .addType(Message.class)
                .addType(Address.class)
                .build();
    }

    public Address getAddress() {
        return ms.address();
    }

    private synchronized void incrementMReceived(){
        messagesReceived++;
    }

    private synchronized void incrementMSent(){ messagesSent++; }

    public synchronized void registerHandler(String s, Consumer<Message> consumer) {
        this.registerHandler(s, (o,m) -> {
            Message msg = serializer.decode(m);
            if( msg.getSender() == null)
                msg.setSender(o);
            consumer.accept(msg);
        });
    }

    final synchronized protected void registerHandler(String s, BiConsumer<Address, byte[]> consumer) {
        this.ms.registerHandler(s, (o, m) -> {
            incrementMReceived();
            consumer.accept(o,m);
        }, this.executor);
    }

    public synchronized void broadcast(String tag, List<Address> participants, Message msg) {
        priorityBroadcast(tag, participants, msg);
    }

    public synchronized void broadcast(String tag, List<Address> participants, List<Message> msgList) {
        if (participants.size() != msgList.size())
            throw new IndexOutOfBoundsException("Should be one message for each participant. Messages = " +
                    msgList.size() + " participant = " + participants.size());

        for (int i = 0; i < participants.size(); i++) {
            Address addr = participants.get(i);
            Message msg = msgList.get(i);
            executor.schedule(() -> sendAsync(addr, tag, serializer.encode(msg)), 1, TimeUnit.NANOSECONDS);
        }
    }

    public final synchronized void priorityBroadcast(String tag, List<Address> participants, Message msg) {
        byte[] encoded = serializer.encode(msg);
        participants.forEach( addr ->
                executor.schedule(() -> sendAsync(addr, tag, encoded), 1, TimeUnit.NANOSECONDS));
    }

    protected final synchronized void sendAsync(Address address,String tag, byte[] bytes){
        executor.schedule( () -> {
            ms.sendAsync(address, tag, bytes);
            incrementMSent();
        }, 1, TimeUnit.NANOSECONDS);
    }

    public synchronized CompletableFuture<MessagingService> start() {
        return this.ms.start();
    }

    public void close(){
        // called when all processes has finished
        ms.stop();
        executor.shutdown();

        // report messages
        register.physicalMessageReceived(messagesReceived);
        register.physicalMessagesSent(messagesSent);
    }

    public synchronized void unregisterHandler(String tag){
        this.ms.unregisterHandler(tag);
    }
}