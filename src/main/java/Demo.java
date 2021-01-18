import client.Client;
import client.RunnableConfigurable;
import dispatcher.DispatcherFactory;
import pt.inesctec.minha.api.Entry;
import pt.inesctec.minha.api.Process;
import pt.inesctec.minha.api.World;
import pt.inesctec.minha.sim.atomix.SimulatedAtomixCluster;

import java.io.*;
import java.util.InvalidPropertiesFormatException;

import static utils.FileUtils.*;

public class Demo {
    public static final String MAIN_FOLDER = "./results";
    public static final String[] CONFIG_FILES = new String[]{
            "run_without",
            "run_with_1",
            "run_with_2",
            "run_with_3",
            "run_with_4",
            "run_with_5",
            "run_with_6",
            "run_with_7",
            "run_with_8",
            "run_with_9",
            "run_with_10",
            "run_with_11",
            // "change_delay",
            // "change_fanout",
          };

    public static void main(String[] args){
        for( String configFile : CONFIG_FILES){
            try{
                DemoConfig demoConfig = new DemoConfig(configFile);
                String folderPath = Demo.MAIN_FOLDER + "/" + demoConfig.getDemoType().toString() + ":" + System.currentTimeMillis();
                createFolder(folderPath);
                saveString(folderPath +"/config.conf", demoConfig.toString());

                switch (demoConfig.getDemoType()){
                    case CHANGE_PROCESS_NUMBER:
                        changeProcessesNumber(demoConfig, folderPath);
                        break;
                    case CHANGE_FAULTY:
                        changeFaultyProcessesNumber(demoConfig, folderPath);
                        break;
                    case CHANGE_ST:
                        changeStandardDerivation(demoConfig, folderPath);
                        break;
                    case CHANGE_FANOUT:
                        changeFanout(demoConfig, folderPath);
                        break;
                    case CHANGE_DELAY:
                        changeDelay(demoConfig, folderPath);
                        break;
                    default:
                        System.err.println("Error: Bad demo run type");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void changeProcessesNumber(DemoConfig demoConfig, String folderPath) throws Exception {
        Integer f = demoConfig.getFaultyProcess(-1);
        for(int n :  demoConfig.getProcessNumberList()) {
            Configuration.Builder configBuilder = new Configuration.Builder()
                    .setProcessesNumber(n)
                    .setEpsilon(demoConfig.getEpsilon())
                    .setMean(demoConfig.getMean())
                    .setStandardDeviation(demoConfig.getStandardDerivation())
                    .setClientDebugMode(demoConfig.isClientDebug())
                    .setDelay(demoConfig.getDelay(), demoConfig.getDelayTimeUnit())
                    .setFanout(demoConfig.getFanout());

            if(f > 0) configBuilder.setFaultyProcessesNumber(f);
            runNSimulations(demoConfig, folderPath, configBuilder.build());
        }
    }

    private static void changeStandardDerivation(DemoConfig demoConfig, String folderPath) throws Exception{
        for(int st : demoConfig.getStandardDerivationList())
            runNSimulations(demoConfig, folderPath,  new Configuration.Builder()
                    .setProcessesNumber(demoConfig.getProcessNumber())
                    .setEpsilon(demoConfig.getEpsilon())
                    .setMean(demoConfig.getMean())
                    .setStandardDeviation(st)
                    .setClientDebugMode(demoConfig.isClientDebug())
                    .setDelay(demoConfig.getDelay(), demoConfig.getDelayTimeUnit())
                    .setFanout(demoConfig.getFanout())
                    .build());
    }

    private static void changeFaultyProcessesNumber(DemoConfig demoConfig, String folderPath) throws Exception {
        for(int faulty : demoConfig.getFaultyProcessList())
             runNSimulations(demoConfig, folderPath, new Configuration.Builder()
                     .setProcessesNumber(demoConfig.getProcessNumber())
                     .setFaultyProcessesNumber(faulty)
                     .setEpsilon(demoConfig.getEpsilon())
                     .setMean(demoConfig.getMean())
                     .setStandardDeviation(demoConfig.getStandardDerivation())
                     .setClientDebugMode(demoConfig.isClientDebug())
                     .setDelay(demoConfig.getDelay(), demoConfig.getDelayTimeUnit())
                     .setFanout(demoConfig.getFanout())
                     .build());
    }

    private static void changeFanout(DemoConfig demoConfig, String folderPath) throws Exception{
        if( ! demoConfig.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change fanout\" with default dispatcher");

        for(int fanout : demoConfig.getFanoutList())
            runNSimulations(demoConfig, folderPath, new Configuration.Builder()
                    .setProcessesNumber(demoConfig.getProcessNumber())
                    .setEpsilon(demoConfig.getEpsilon())
                    .setMean(demoConfig.getMean())
                    .setStandardDeviation(demoConfig.getStandardDerivation())
                    .setClientDebugMode(demoConfig.isClientDebug())
                    .setDelay(demoConfig.getDelay(), demoConfig.getDelayTimeUnit())
                    .setFanout(fanout)
                    .build());
    }

    private static void changeDelay(DemoConfig demoConfig, String folderPath) throws Exception{
        if( ! demoConfig.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change delay\" with default dispatcher");

        for(int delay : demoConfig.getDelayAsList())
            runNSimulations(demoConfig, folderPath, new Configuration.Builder()
                    .setProcessesNumber(demoConfig.getProcessNumber())
                    .setEpsilon(demoConfig.getEpsilon())
                    .setMean(demoConfig.getMean())
                    .setStandardDeviation(demoConfig.getStandardDerivation())
                    .setClientDebugMode(demoConfig.isClientDebug())
                    .setDelay(delay, demoConfig.getDelayTimeUnit())
                    .setFanout(demoConfig.getFanout())
                    .build());
    }

    private static void runNSimulations(DemoConfig demoConfig, String folderPath, Configuration config) throws Exception {
        for(int i = 0; i< demoConfig.getRepeat(); i++) {
            // requesting JVM for running Garbage Collector
            System.gc();

            World w = new SimulatedAtomixCluster(null, null);
            Execution execution = new Execution(config);
            DispatcherFactory df = new DispatcherFactory(demoConfig.getClientPort(), execution, demoConfig.isGossipDispatcher(), config.fanout, config.delay, config.delayTimeUnit);

            for (int j = 0; j < config.n; j++) {
                Process p = w.createHost().createProcess();
                Entry<RunnableConfigurable> e = p.createEntry(RunnableConfigurable.class, Client.class.getName());
                e.asap().queue().setConfigurationThenRun(execution, df, folderPath + "/logs/");
            }
            execution.setHostsAddress(w.getHosts(), demoConfig.getClientPort());
            w.run();

            try {
                execution.getFinished().get();
                saveObject(folderPath + "/exec" + execution.getId() + ".ser", execution );
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println(config.toString());
    }

}