package client;

import dispatcher.Message;
import utils.FileUtils;

public class Logger {
    private final FileUtils logFile;

    Logger(String path, String fileName, boolean clientDebugMode){
        if(clientDebugMode){
            logFile = new FileUtils(path,  fileName + ".log");
        }else{
            logFile = null;
        }

    }

    public void DEBUG(String logMessage, Client client){
        if ( logFile != null)
            logFile.write( logMessage + " - " + serializeClient(client));
    }

    public void DEBUG(String logMessage, Client client, Message msg){
        if ( logFile != null){
            logFile.write( logMessage + " - " + serializeMessage(msg) + " - " + serializeClient(client));
        }
    }

    public String serializeClient(Client c){
        return "Client{" +
                "round=" + c.currentRound + "/" + c.roundsToBeExecuted + ", " +
                "valuesReceivedSize=" + c.currentValuesReceived.values().size() +
                '}';
    }

    public String serializeMessage(Message m){
        return m.toString();
    }

    public void close() {
        if ( logFile != null) logFile.close();
    }

}
