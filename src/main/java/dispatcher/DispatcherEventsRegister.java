package dispatcher;

import io.atomix.utils.net.Address;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DispatcherEventsRegister {
    // Register n physical messages received
    void physicalMessageReceived(int n);

    // Register n physical messages sent
    void physicalMessagesSent(int n);
}
