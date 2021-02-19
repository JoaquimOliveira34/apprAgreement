package dispatcher;

public interface DispatcherEventsRegister {
    // Register n physical messages received
    void physicalMessageReceived(int n);

    // Register n physical messages sent
    void physicalMessagesSent(int n);
}
