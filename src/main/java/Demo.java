import client.Client;
import client.RunnableConfigurable;
import dispatcher.DispatcherFactory;
import pt.inesctec.minha.api.Entry;
import pt.inesctec.minha.api.Process;
import pt.inesctec.minha.api.World;
import pt.inesctec.minha.sim.atomix.SimulatedAtomixCluster;

import java.io.*;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import static utils.FileUtils.*;

public class Demo {
    // TODO : refactor application_example with new parameters
    public static final String MAIN_FOLDER = "./results";

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println(listConfigFilesNames());
        for( String configFile : listConfigFilesNames()){
            try{
                DemoConfig demoConfig = new DemoConfig(configFile);
                String folderPath = getSimulationFolderName(demoConfig);
                createFolder(folderPath);
                writeStringToFile(folderPath +"/config.conf", demoConfig.toString());

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

    private static List<String> listConfigFilesNames() throws FileNotFoundException {
        List<String> list = listFilesNamesFromFolder("./src/main/resources/");
        list.remove("application_example.conf");
        return list;
    }

    private static String getSimulationFolderName(DemoConfig demoConfig){
        return MAIN_FOLDER + "/" + demoConfig.getDemoType().toString() + ":" + System.currentTimeMillis();
    }

    private static void changeProcessesNumber(DemoConfig demoConfig, String folderPath) throws Exception {
        for(int n :  demoConfig.getProcessNumberList()) {
            Configuration conf = configBuilderHelper(
                    demoConfig,
                    n,
                    demoConfig.getFaultyProcess(),
                    demoConfig.getStandardDerivation(),
                    demoConfig.getDelay(),
                    demoConfig.getFanout());
            runNSimulations(demoConfig.getClientPort(),demoConfig.getRepeat(), folderPath, conf);
        }
    }

    private static void changeStandardDerivation(DemoConfig demoConfig, String folderPath) throws Exception{
        for(int st : demoConfig.getStandardDerivationList()){
            Configuration conf = configBuilderHelper(
                    demoConfig,
                    demoConfig.getProcessNumber(),
                    demoConfig.getFaultyProcess(),
                    st,
                    demoConfig.getDelay(),
                    demoConfig.getFanout());
            runNSimulations(demoConfig.getClientPort(),demoConfig.getRepeat(), folderPath, conf);
        }
    }

    private static void changeFaultyProcessesNumber(DemoConfig demoConfig, String folderPath) throws Exception {
        for(int faulty : demoConfig.getFaultyProcessList()){
            Configuration conf = configBuilderHelper(
                    demoConfig,
                    demoConfig.getProcessNumber(),
                    faulty,
                    demoConfig.getStandardDerivation(),
                    demoConfig.getDelay(),
                    demoConfig.getFanout());
            runNSimulations(demoConfig.getClientPort(),demoConfig.getRepeat(), folderPath, conf);
        }
    }

    private static void changeFanout(DemoConfig demoConfig, String folderPath) throws Exception{
        if( ! demoConfig.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change fanout\" with default dispatcher");

        for(int fanout : demoConfig.getFanoutList()){
            Configuration conf = configBuilderHelper(
                    demoConfig,
                    demoConfig.getProcessNumber(),
                    demoConfig.getFaultyProcess(),
                    demoConfig.getStandardDerivation(),
                    demoConfig.getDelay(),
                    fanout);
            runNSimulations(demoConfig.getClientPort(),demoConfig.getRepeat(), folderPath, conf);
        }
    }

    private static void changeDelay(DemoConfig demoConfig, String folderPath) throws Exception{
        if( ! demoConfig.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change delay\" with default dispatcher");

        for(int delay : demoConfig.getDelayAsList()){
            Configuration conf = configBuilderHelper(
                    demoConfig,
                    demoConfig.getProcessNumber(),
                    demoConfig.getFaultyProcess(),
                    demoConfig.getStandardDerivation(),
                    delay,
                    demoConfig.getFanout());
            runNSimulations(demoConfig.getClientPort(),demoConfig.getRepeat(), folderPath, conf);
        }
    }

    private static void runNSimulations(int clientPort, int repeat, String folderPath, Configuration config) throws Exception {
        for(int i = 0; i< repeat; i++) {
            // requesting JVM for running Garbage Collector
            System.gc();

            World w = new SimulatedAtomixCluster(null, null);
            Execution execution = new Execution(config);
            DispatcherFactory df = new DispatcherFactory(
                    clientPort,
                    execution,
                    config.isGossipDispatcher,
                    config.fanout,
                    config.delay,
                    config.delayGroupSize,
                    config.delayTimeUnit);

            for (int j = 0; j < config.n; j++) {
                Process p = w.createHost().createProcess();
                Entry<RunnableConfigurable> e = p.createEntry(RunnableConfigurable.class, Client.class.getName());
                e.asap().queue().setConfigurationThenRun(execution, df, folderPath + "/logs/");
            }
            execution.setHostsAddress(w.getHosts(), clientPort);
            w.run();

            try {
                execution.getFinished().get();
                writeObjectToFie(folderPath + "/exec" + execution.getId() + ".ser", execution );
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println(config.toString());
    }

    private static Configuration configBuilderHelper(DemoConfig demoConfig, int n, Integer f, int st, Integer delay, Integer fanout){
        return new Configuration.Builder()
                .setProcessesNumber(n)
                .setFaultyProcessesNumber(f)
                .setEpsilon(demoConfig.getEpsilon())
                .setMean(demoConfig.getMean())
                .setStandardDeviation(st)
                .setDelay(delay, demoConfig.getDelayTimeUnit())
                .setDelayGroupSize(demoConfig.getDelayGroupSize())
                .setFanout(fanout)
                .setClientDebugMode(demoConfig.isClientDebug())
                .setIsGossipDispatcher(demoConfig.isGossipDispatcher())
                .build();
    }
}