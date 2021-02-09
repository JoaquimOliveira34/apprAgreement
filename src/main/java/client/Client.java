package client;

import dispatcher.DispatcherFactory;
import dispatcher.Message;
import dispatcher.MessageDispatcher;
import io.atomix.utils.net.Address;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;
import utils.MyMath;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Client implements RunnableConfigurable {
    private static final String HALT_TAG = "halt";
    private static final String ROUND_TAG = "round";
    private static final int MAX_ROUNDS = 20;
    private static final int WAIT_TIME = 500_000;
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.NANOSECONDS;

    // Initialized on thread start
    private SimulatedPlatform platform;
    private ClientEventsRegister register;
    private MessageDispatcher dispatcher;

    // Client state
    public int currentRound, roundsToBeExecuted;
    public final Map<Address, Double> currentValuesReceived;
    private double currentValue;
    private final Map<Integer,List<Message> > haltMessages;
    private final Map<Integer,List<Message>> futureMsg;
    private final CompletableFuture<Void> allDone;
    private int haltMessagesCount;
    private boolean isRunning;
    private ScheduledExecutorService scheduled;

    // Monitoring data
    private final Map<Integer,Integer> ignoredMessagesCount;
    private int usefulMessagesCount;
    private Logger logger;

    public Client(){
        this.haltMessages = new HashMap<>();
        this.currentValuesReceived = new HashMap<>();
        this.ignoredMessagesCount = new HashMap<>();
        this.futureMsg = new HashMap<>();
        this.allDone = new CompletableFuture<>();
        this.isRunning = true;
    }

    public void setConfigurationThenRun(ClientEventsRegister register, DispatcherFactory factory, String logsPathName){
        this.platform = SimulatedPlatform.instance();
        this.scheduled = platform.newScheduledThreadPool();
        this.dispatcher = factory.getMessageDispatcher(platform);
        this.register = register;
        this.logger = new Logger(logsPathName, register.getId() + "-" + dispatcher.getAddress().toString(),  register.getClientDebugMode());
        run();
    }

    @Override
    public void run() {
        currentRound = 0;
        roundsToBeExecuted = MAX_ROUNDS;
        currentValue = register.getInitialValue();
        logger.DEBUG("Client running", this);
        register.newRoundAchieved( currentRound, null, currentValue);

        dispatcher.registerHandler(ROUND_TAG, this::receiveRoundMsg);
        dispatcher.registerHandler(HALT_TAG, this::receiveHaltMsg);

        dispatcher.start().thenRun( () -> broadcastRoundMsg(currentValue, 0));
        scheduled.schedule(this::newRoundArchived, WAIT_TIME, WAIT_TIME_UNIT );
        // task = scheduled.scheduleAtFixedRate(this::newRoundArchived, WAIT_TIME, WAIT_TIME, WAIT_TIME_UNIT);
    }

    //  Receivers Methods
    private synchronized void receiveHaltMsg(Message msg){
        logger.DEBUG("Halt Message received", this, msg);

        usefulMessagesCount ++;

        haltMessages.putIfAbsent(msg.round, new ArrayList<>());
        haltMessages.get(msg.round).add(msg);
        if( msg.round == currentRound && isRunning)
            addMessageToCollection(msg);

        if( ++haltMessagesCount == register.getProcessCount())
            allDone.complete(null);
    }

    private synchronized void receiveRoundMsg(Message msg){
        // Ignore message
        if(msg.round < this.currentRound || !isRunning ){
            logger.DEBUG("Message Ignored", this, msg);
            ignoredMessagesCount.merge(msg.round, 1, Integer::sum);
            return;
        }

        // Future messages
        if(msg.round > this.currentRound) {
            logger.DEBUG("Future message received", this, msg);
            futureMsg.putIfAbsent(msg.round, new ArrayList<>());
            futureMsg.get(msg.round).add(msg);
            return;
        }

        // Useful message
        usefulMessagesCount ++;
        logger.DEBUG("New Message received", this, msg);
        addMessageToCollection(msg);
    }

    private synchronized void addMessageToCollection(Message msg){
        currentValuesReceived.put(msg.getSender(), msg.value);
    }

    private synchronized void newRoundArchived(){
        if( !isRunning)
            return;

        currentRound ++;

        if( hasEnoughValuesToApplyF() ){
            // Calculate new value to use during next round
            currentValue = MyMath.newValue(register.getFaultProcessCount(), currentValuesReceived.values(), currentRound ==1);
            logger.DEBUG("New round archived with new value", this);
        }else{
            // Use old value to use during next round
            logger.DEBUG("New round archived with old value", this);
        }

        register.newRoundAchieved(currentRound,new HashSet<>(currentValuesReceived.keySet()), currentValue);
        currentValuesReceived.clear();

        if( currentRound == roundsToBeExecuted + 1) {
            // Finish if last round is archived
            broadcastHaltMsg( currentValue, currentRound);
            logger.DEBUG("Client ready to finish", this);
            isRunning = false;
            // TODO: Improve finish method and how data is reported ( messages lost, etc..)
            register.finish(dispatcher.getAddress(), platform.nanoTime(), currentRound, currentValue,  usefulMessagesCount, ignoredMessagesCount);

            allDone.thenRun( () -> {
                // stop to process messages
                dispatcher.unregisterHandler(HALT_TAG);
                dispatcher.unregisterHandler(ROUND_TAG);
                dispatcher.close();
                //Log and report data
                futureMsg.values().stream().flatMap(List::stream).forEach(m -> ignoredMessagesCount.merge(m.round, 1, Integer::sum));
                logger.DEBUG("Client Finished", this);
                logger.close();
            });
        }else{
            broadcastRoundMsg( currentValue, currentRound);

            // Set a new timeout to change round
            scheduled.schedule(this::newRoundArchived, WAIT_TIME, WAIT_TIME_UNIT );

            // Process halt messages
            haltMessages.entrySet().stream()
                    .filter(entry-> entry.getKey() <= currentRound)
                    .limit(register.getValuesReceivedSize())
                    .forEach( e -> e.getValue().forEach( m -> currentValuesReceived.put(m.getSender(),m.value)));


            // Process round messages queued
            List<Message> messages = futureMsg.remove(currentRound);
            if( messages != null)
                messages.forEach(this::receiveRoundMsg);
        }
    }

    private boolean hasEnoughValuesToApplyF(){
        // TODO : Check if this is ok
        int t = register.getFaultProcessCount();
        return MyMath.c(currentValuesReceived.size()- t,t) > 0;
    }

    //  Broadcast  Methods
    private void broadcastRoundMsg(double value, int round){
        List<Address> participants = register.getHostsAddress();

        if( register.isFaultyProcess(currentRound, dispatcher.getAddress())){
            List<Message> msg = new ArrayList<>(participants.size());
            for(int i = 0; i < participants.size(); i ++)
                msg.add(i,new Message(register.getRandomValue(), round));
            dispatcher.broadcast(ROUND_TAG, participants, msg);
        }
        else{
            Message msg = new Message(value, round);
            dispatcher.broadcast(ROUND_TAG, participants, msg);
        }
    }

    private void broadcastHaltMsg(double value, int round){
        Message msg = new Message(value, round);
        dispatcher.priorityBroadcast(HALT_TAG, register.getHostsAddress(), msg);
    }

}