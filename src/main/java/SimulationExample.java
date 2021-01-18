import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.utils.net.Address;
import pt.inesctec.minha.api.Entry;
import pt.inesctec.minha.api.Host;
import pt.inesctec.minha.api.Process;
import pt.inesctec.minha.api.World;
import pt.inesctec.minha.sim.atomix.SimulatedAtomixCluster;
import pt.inesctec.minha.sim.atomix.SimulatedPlatform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationExample implements Runnable {

    public static class Sender implements Runnable {
        static public Address to;

        public void run() {
            final SimulatedPlatform platform = SimulatedPlatform.instance();
            ScheduledExecutorService ex = platform.newScheduledThreadPool();
            ManagedMessagingService ms = platform.newMessagingService("example", Address.from("127.0.0.1", 12345), new MessagingConfig());

            ms.start().thenRun(()-> {

                for(int i = Integer.MIN_VALUE; i< Integer.MAX_VALUE; i++) {
                    for(int j = Integer.MIN_VALUE; j< Integer.MAX_VALUE; j++) {
                        continue;
                    }
                }

                if( false) {
                    ms.sendAsync(to, "hello", "world!".getBytes())
                            .exceptionally((e) -> {
                                e.printStackTrace();
                                return null;
                            });
                }
               ex.schedule( () -> System.out.println( "Check"), 1 , TimeUnit.NANOSECONDS);
               ex.schedule( () -> System.out.println( "Check"), 10 , TimeUnit.NANOSECONDS);
               ex.schedule( () -> System.out.println( "Check"), 20, TimeUnit.NANOSECONDS);
               ex.schedule( () -> System.out.println( "Time: " + platform.nanoTime()), 1 , TimeUnit.MILLISECONDS);
               // System.out.println( "Time: " + platform.nanoTime());
            });
        }
    }

    public static void main(String[] args) throws Exception {
        World w = new SimulatedAtomixCluster(null, null);
        /*
        Host h = w.createHost();
        h.createProcess().createEntry(Runnable.class, SimulationExample.class.getName()).asap().queue().run();
        Sender.to =  new Address(h.getAddress().getHostAddress(), 12345);
        */
        w.createHost().createProcess().createEntry(Runnable.class, Sender.class.getName()).asap().queue().run();
        w.run();
    }

    public void run() {
        final SimulatedPlatform platform = SimulatedPlatform.instance();

        ScheduledExecutorService ex = platform.newScheduledThreadPool();
        ManagedMessagingService ms = platform.newMessagingService("example", Address.from("127.0.0.1", 12345), new MessagingConfig());

        ms.registerHandler("hello", (a,m)-> {
            System.out.println("ola");
        }, ex);

        ms.start();
    }
}
