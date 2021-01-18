package client;

import io.atomix.utils.net.Address;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ClientEventsRegister {
    // Get clients address
    List<Address> getHostsAddress();

    // Check if a process is byzantine in that round
    boolean isFaultyProcess(int round, Address address);

    // Register new round archived
    void newRoundAchieved(int newRound, Set<Address> values, double newValue);

    void finish(Address addr, long nanoTime, int atRound, double withValue, double usefulCount, Map<Integer, Integer> ignoredMessagesCount);

    boolean getClientDebugMode();

    double getInitialValue();

    double getRandomValue();

    int getValuesReceivedSize();

    int getProcessCount();

    int getFaultProcessCount();

    double getEpsilon();

    int getId();
}

