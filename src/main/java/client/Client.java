package client;

import dispatcher.DispatcherFactory;
import dispatcher.Message;
import dispatcher.MessageDispatcher;
import io.atomix.utils.net.Address;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;
import utils.FileUtils;
import utils.MyMath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static utils.FileUtils.openFile;

public class Client implements RunnableConfigurable {
    private static final String HALT_TAG = "halt";
    private static final String ROUND_TAG = "round";

    // Initialized on thread start
    private SimulatedPlatform platform;
    private ClientEventsRegister register;
    private MessageDispatcher dispatcher;
    private FileUtils logFile;

    // Client state
    private int currentRound, roundsToBeExecuted;
    private final Map<Address, Double> currentValuesReceived;
    private final Map<Integer,List<Message> > haltMessages;
    private final Map<Integer,List<Message>> futureMsg;
    private final CompletableFuture<Void> allDone;
    private int haltMessagesCount;
    private boolean isRunning;

    // Monitoring data
    private final Map<Integer,Integer> ignoredMessagesCount;
    private int usefulMessagesCount;

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
        this.dispatcher = factory.getMessageDispatcher(platform);
        this.register = register;
        if(register.getClientDebugMode()){
            logFile = new FileUtils(logsPathName,register.getId() + "-" + dispatcher.getAddress().toString() + ".csv" );
            logFile.write("Round,Values size, Event ");
        }
        run();
    }

    @Override
    public void run() {
        currentRound = 0;
        double initialValue = register.getInitialValue();
        logNewRoundArchived(initialValue);
        register.newRoundAchieved( currentRound, null, initialValue);

        dispatcher.registerHandler(ROUND_TAG, this::receiveRoundMsg);
        dispatcher.registerHandler(HALT_TAG, this::receiveHaltMsg);

        dispatcher.start().thenRun( () -> broadcastRoundMsg(initialValue, 0));
    }

    //  Receivers Methods
    private synchronized void receiveHaltMsg(Message msg){
        usefulMessagesCount ++;
        logHaltMessageReceived(msg);
        haltMessages.putIfAbsent(msg.round, new ArrayList<>());
        haltMessages.get(msg.round).add(msg);
        if( msg.round == currentRound && isRunning){
            currentValuesReceived.put(msg.getSender(), msg.value);
            if ( currentValuesReceived.size() == register.getValuesReceivedSize())
                newRoundArchived();
        }
        if( ++haltMessagesCount == register.getProcessCount())
            allDone.complete(null);
    }

    private synchronized void receiveRoundMsg(Message msg){
        // Ignore message
        if(msg.round < this.currentRound || !isRunning ){
            logMessageIgnored(msg);
            ignoredMessagesCount.merge(msg.round, 1, Integer::sum);
            return;
        }

        // Future messages
        if(msg.round > this.currentRound) {
            logFutureMessage(msg);
            futureMsg.putIfAbsent(msg.round, new ArrayList<>());
            futureMsg.get(msg.round).add(msg);
            return;
        }

        // Useful message
        usefulMessagesCount ++;
        logNewMessageReceived(msg);
        currentValuesReceived.put(msg.getSender(), msg.value);

        // If new round can be archived
        if( currentValuesReceived.size() == register.getValuesReceivedSize() )
            newRoundArchived();
    }

    private synchronized void newRoundArchived(){
        currentRound ++;

        //Calculate max rounds to be executed if if the first round
        if( currentRound == 1)
            this.roundsToBeExecuted = MyMath.maxRounds(register.getProcessCount(), register.getFaultProcessCount(),
                    register.getEpsilon(), currentValuesReceived.values());

        //Calculate new value
        double value = MyMath.newValue(register.getFaultProcessCount(), currentValuesReceived.values(), currentRound ==1);
        logNewRoundArchived(value);
        register.newRoundAchieved(currentRound,new HashSet<>(currentValuesReceived.keySet()), value);

        currentValuesReceived.clear();

        if( currentRound == roundsToBeExecuted + 1) {
            // If last round is archived
            broadcastHaltMsg( value, currentRound);
            isRunning = false;
            allDone.thenRun( () -> {
                // stop to process messages
                dispatcher.unregisterHandler(HALT_TAG);
                dispatcher.unregisterHandler(ROUND_TAG);
                dispatcher.close();
                //Log and report data
                futureMsg.values().stream().flatMap(List::stream).forEach(m -> ignoredMessagesCount.merge(m.round, 1, Integer::sum));
                register.finish(dispatcher.getAddress(), platform.nanoTime(), currentRound, value,  usefulMessagesCount, ignoredMessagesCount);
                logFinished(value);
                logFile.close();
            });
        }else{
            broadcastRoundMsg( value, currentRound);

            // Get halt messages
            haltMessages.entrySet().stream()
                    .filter(entry-> entry.getKey() <= currentRound)
                    .limit(register.getValuesReceivedSize())
                    .forEach( e -> e.getValue().forEach( m -> currentValuesReceived.put(m.getSender(),m.value)));

            if( currentValuesReceived.size() == register.getValuesReceivedSize())
                newRoundArchived();

            List<Message> messages = futureMsg.remove(currentRound);
            if( messages != null)
                messages.forEach( m -> receiveRoundMsg(m));
        }
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

    // Log methods
    private void logNewMessageReceived(Message msg) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", New Message : " + msg.value + " from " + msg.getSender().toString());
    }

    private void logMessageIgnored(Message msg) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", Message Ignored : from " + msg.getSender().toString());
    }

    private void logHaltMessageReceived(Message msg) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", Halt Message received : from " + msg.getSender().toString() + " at round " + msg.round);
    }

    private void logNewRoundArchived(double value) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", Round archived ( of " + roundsToBeExecuted + ") : new value " + value);
    }

    private void logFinished(double value) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", Finished with value " + value);
    }

    private void logFutureMessage(Message msg) {
        if( !register.getClientDebugMode())
            return;
        logFile.write(currentRound + "," + currentValuesReceived.values().size() + ", Future message to round " + msg.round);
    }

}