package dispatcher;

import io.atomix.utils.net.Address;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;

import java.util.concurrent.TimeUnit;

public class DispatcherFactory {
    public final int port;
    private final DispatcherEventsRegister register;
    private final boolean isGossipDispatcher;
    private final Integer fanout, delay, delayGroupSize;
    private final TimeUnit delayTimeUnit;

    public DispatcherFactory(int port, DispatcherEventsRegister register, boolean isGossipDispatcher, Integer fanout, Integer delay, Integer delayGroupSize, TimeUnit delayTimeUnit){
        this.port = port;
        this.register = register;
        this.isGossipDispatcher = isGossipDispatcher;
        this.fanout = fanout;
        this.delay = delay;
        this.delayTimeUnit = delayTimeUnit;
        this.delayGroupSize = delayGroupSize;
    }

    public MessageDispatcher getMessageDispatcher(SimulatedPlatform platform){
        MessageDispatcher md;
        if(isGossipDispatcher)
            md = new MutatedGossipDispatcher(Address.from("127.0.0.1", port), register, platform, fanout, delay, delayGroupSize, delayTimeUnit);
        else
            md = new MessageDispatcher(Address.from("127.0.0.1", port), register, platform);
        return md;
    }
}
