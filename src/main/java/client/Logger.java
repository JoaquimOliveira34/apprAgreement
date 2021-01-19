package client;

import dispatcher.Message;
import io.atomix.utils.net.Address;
import utils.FileUtils;

import java.util.Map;

public class Logger {
    private final FileUtils logFile;

    Logger(String path, String fileName){
        logFile = new FileUtils(path, fileName);
        logFile.write("Round,Values size, Event ");
    }

    void logNewMessageReceived(int atRound, Map<Address, Double> valuesReceived, Message msg) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", New Message : " + msg.value + " from " + msg.getSender().toString());
    }

    void logMessageIgnored(int atRound, Map<Address, Double> valuesReceived, Message msg) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", Message Ignored : from " + msg.getSender().toString());
    }

    void logHaltMessageReceived(int atRound, Map<Address, Double> valuesReceived, Message msg) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", Halt Message received : from " + msg.getSender().toString() + " at round " + msg.round);
    }

    void logNewRoundArchived(int atRound, Map<Address, Double> valuesReceived, double value) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", Round " +atRound + " archived: new value " + value);
    }

    void logFinished(int atRound, Map<Address, Double> valuesReceived, double value) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", Finished with value " + value);
    }

    void logFutureMessage(int atRound, Map<Address, Double> valuesReceived, Message msg) {
        logFile.write(atRound + "," + valuesReceived.values().size() + ", Future message to round " + msg.round);
    }

    public void close() {
        logFile.close();
    }
}
