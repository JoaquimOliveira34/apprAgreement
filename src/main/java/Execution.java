import client.ClientEventsRegister;
import utils.MyMath;
import dispatcher.DispatcherEventsRegister;
import io.atomix.utils.net.Address;

import pt.inesctec.minha.api.Host;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Execution implements Serializable, DispatcherEventsRegister, ClientEventsRegister {
    static final long serialVersionUID = 2L;
    private static int nextId = 0;

    public static synchronized int getNextId() {
        return nextId++;
    }

    // Execution id
    public final int id;
    // Initial configuration
    private final Configuration config;
    // Hosts address
    private transient List<Address> hostsAddress;
    // Processes finished count and future that is completed when count == n
    private final transient CompletableFuture<Void> finished;
    private final transient CompletableFuture<Void> ready;
    private  transient int readyCount;
    // Initial value of processes for each round (round -> processId -> value)
    private final Map<Integer, List<Double>> intermediateValues;
    // Value received by processes for each round (round -> {senderId} )
    private final transient Map<Integer, List<Set<String>>> usefulMessagesSenders;
    private final Map<Integer, Integer> usefulMessagesSendersReduced;
    // Messages received/ignored/sent count by round
    private final Map<Integer, Integer> messagesIgnored, runningProcess;
    private int physicalMessagesReceived, physicalMessagesSent, usefulMessages;

    // Byzantine processes for each round
    public final transient Map<Integer, Set<String>> byzantineIdsByRound;
    // Execution time by all
    private final List<Long> executionTime;
    private final transient List<Double> returnValues;

    public Execution(Configuration configuration) {
        id = getNextId();
        this.config = configuration;
        intermediateValues = new HashMap<>();
        usefulMessagesSenders = new HashMap<>();
        usefulMessagesSendersReduced = new HashMap<>();
        byzantineIdsByRound = new HashMap<>();
        runningProcess = new HashMap<>();
        messagesIgnored = new HashMap<>();
        returnValues = new ArrayList<>();
        finished = new CompletableFuture<>();
        ready = new CompletableFuture<>();
        executionTime = new ArrayList<>();
    }

    public void setHostsAddress(Collection<Host> hostsAddress, int port) {
        this.hostsAddress = hostsAddress.stream()
                .map(h -> io.atomix.utils.net.Address.from(h.getAddress().getHostAddress(), port))
                .collect(Collectors.toList());
    }

    public Configuration getConfiguration() {
        return config;
    }

    /*
     * Registers methods : Used by client and dispatcher to get data and to register events
     */
    public synchronized List<Address> getHostsAddress() {
        List<Address> list = new ArrayList<>(hostsAddress);
        Collections.shuffle(list, config.random);
        return list;
    }

    public boolean getClientDebugMode() {
        return config.clientDebug;
    }

    public synchronized double getInitialValue() {
        return config.getInitialValue();
    }

    public synchronized double getRandomValue() {
        return config.getRandomValue();
    }

    public int getValuesReceivedSize() {
        return config.n - config.t;
    }

    public int getProcessCount() {
        return config.n;
    }

    public int getFaultProcessCount() {
        return config.t;
    }

    public double getEpsilon() {
        return config.e;
    }

    public int getId() {
        return id;
    }

    public synchronized CompletableFuture<Void> getFinished() {
        return finished;
    }

    public synchronized void physicalMessageReceived(int n) {
        physicalMessagesReceived += n;
    }

    public synchronized void physicalMessagesSent(int n) {
        physicalMessagesSent += n;
    }

    public synchronized boolean isFaultyProcess(int round, Address address) {
        if (!byzantineIdsByRound.containsKey(round)) {
            Set<String> set = new HashSet<>();
            while (set.size() < config.t)
                set.add(hostsAddress.get(config.nextInt(config.n)).toString());
            byzantineIdsByRound.put(round, set);
        }
        return byzantineIdsByRound.get(round).contains(address.toString());
    }

    public synchronized void newRoundAchieved(int newRound, Set<Address> valuesSender, double newValue) {
        // intermediate values
        intermediateValues.putIfAbsent(newRound, new ArrayList<>(returnValues));
        intermediateValues.get(newRound).add(newValue);

        // Update messages sets
        if (newRound > 0) {
            Set<String> valuesSenderString = valuesSender.stream().map(Address::toString).collect(Collectors.toSet());
            usefulMessagesSenders.putIfAbsent(newRound - 1, new ArrayList<>());
            List<Set<String>> sets = usefulMessagesSenders.get(newRound - 1);
            if (sets.stream().noneMatch(s -> s.containsAll(valuesSenderString) && valuesSenderString.containsAll(s)))
                sets.add(valuesSenderString);

            // delete usefulMessagesSenders and reduce data to usefulMessagesSendersReduced
            if (intermediateValues.get(newRound).size() == config.n)
                usefulMessagesSendersReduced.put(newRound - 1, usefulMessagesSenders.remove(newRound - 1).size());
            // delete byzantineIdsByRound
            byzantineIdsByRound.remove(newRound - 1);
        }
    }

    public synchronized void finish(Address addr, long nanoTime, int atRound, double withValue, double usefulCount, Map<Integer, Integer> ignoredByRound) {
        executionTime.add(nanoTime);
        returnValues.add(withValue);
        usefulMessages += usefulCount;
        ignoredByRound.forEach((round, count) -> messagesIgnored.merge(round, count, Integer::sum));

        for (int r = 0; r < atRound; r++)
            runningProcess.merge(r, 1, Integer::sum);

        if (runningProcess.get(0) == config.n)
            finished.complete(null);
    }


    /*
     * Reduce methods : Used to extract information from data after finishing
     */

    public int getPhysicalMessagesReceived() {
        return physicalMessagesReceived;
    }

    public int getPhysicalMessagesSent() {
        return physicalMessagesSent;
    }

    public int getPhysicalMessagesLost() {
        return physicalMessagesSent - physicalMessagesReceived;
    }

    public int getMessagesQueuedLost() {
        return getLogicalMessagesSent() - getPhysicalMessagesSent();
    }

    public int getLogicalMessagesSent() {
        return runningProcess.values().stream()
                .mapToInt(r -> r * config.n)
                .reduce(Integer::sum)
                .getAsInt()
                + config.n * config.n; // halt message broadcast

    }

    public int getUsefulMessagesCount() {
        return usefulMessages;
    }

    public int getMessagesIgnoredCount() {
        return messagesIgnored.values().stream().reduce(Integer::sum).get();
    }

    public Map<Integer, Integer> getMessagesIgnored() {
        return messagesIgnored;
    }

    public long getExecutionTime() {
        return (long) executionTime.stream().mapToLong(x -> x).average().getAsDouble();
    }

    public List<Long> getExecutionTimeByProcess() {
        return executionTime;
    }

    public int requiredRounds() {
        for (int round = 0; round < lastRound(); round++) {
            Collection<Double> values = intermediateValues.get(round);
            if (Collections.max(values) - Collections.min(values) <= config.e)
                return round;
        }
        return -1;
    }

    public int lastRoundByAll() {
        int round = 0;
        while (runningProcess.getOrDefault(round, 0) == config.n)
            round++;
        return round - 1;
    }

    public int lastRound() {
        return intermediateValues.size();
    }

    public Map<Integer, Integer> runningByRound() {
        return runningProcess;
    }

    public Map<Integer, Double> amplitudeByRound() {
        return intermediateValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.max(e.getValue()) - Collections.min(e.getValue())));
    }

    public Map<Integer, Double> standardDerivationByRound() {
        return intermediateValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> MyMath.standardDerivation(e.getValue())));
    }

    public Map<Integer, Integer> getUsefulMessagesSenders() {
        return usefulMessagesSendersReduced;
    }

}
