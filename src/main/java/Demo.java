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
    public static final String MAIN_FOLDER = "./results";

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println(listConfigFilesNames());
        for( String configFile : listConfigFilesNames()){
            try{
                ConfigFileLoader configFileLoader = new ConfigFileLoader(configFile);
                String folderPath = getSimulationFolderName(configFileLoader);
                createFolder(folderPath);
                writeStringToFile(folderPath +"/config.conf", configFileLoader.toString());

                switch (configFileLoader.getDemoType()){
                    case CHANGE_PROCESS_NUMBER:
                        changeProcessesNumber(configFileLoader, folderPath);
                        break;
                    case CHANGE_FAULTY:
                        changeFaultyProcessesNumber(configFileLoader, folderPath);
                        break;
                    case CHANGE_ST:
                        changeStandardDerivation(configFileLoader, folderPath);
                        break;
                    case CHANGE_FANOUT:
                        changeFanout(configFileLoader, folderPath);
                        break;
                    case CHANGE_DELAY:
                        changeDelay(configFileLoader, folderPath);
                        break;
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

    private static String getSimulationFolderName(ConfigFileLoader configFileLoader){
        return MAIN_FOLDER + "/" + configFileLoader.getDemoType().toString() + ":" + System.currentTimeMillis();
    }

    private static void changeProcessesNumber(ConfigFileLoader configFileLoader, String folderPath) throws Exception {
        for(int n :  configFileLoader.getProcessNumberList()) {
            Configuration conf = configFileLoader.getConfigBuilder().setProcessesNumber(n).build();
            runNSimulations(configFileLoader.getClientPort(), configFileLoader.getRepeat(), folderPath, conf);
        }
    }

    private static void changeStandardDerivation(ConfigFileLoader configFileLoader, String folderPath) throws Exception{
        for(int st : configFileLoader.getStandardDerivationList()){
            Configuration conf = configFileLoader.getConfigBuilder().setStandardDeviation(st).build();
            runNSimulations(configFileLoader.getClientPort(), configFileLoader.getRepeat(), folderPath, conf);
        }
    }

    private static void changeFaultyProcessesNumber(ConfigFileLoader configFileLoader, String folderPath) throws Exception {
        for(int faulty : configFileLoader.getFaultyProcessList()){
            Configuration conf = configFileLoader.getConfigBuilder().setFaultyProcessesNumber(faulty).build();
            runNSimulations(configFileLoader.getClientPort(), configFileLoader.getRepeat(), folderPath, conf);
        }
    }

    private static void changeFanout(ConfigFileLoader configFileLoader, String folderPath) throws Exception{
        if( ! configFileLoader.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change fanout\" with default dispatcher");

        for(int fanout : configFileLoader.getFanoutList()){
            Configuration conf = configFileLoader.getConfigBuilder().setFanout(fanout).build();
            runNSimulations(configFileLoader.getClientPort(), configFileLoader.getRepeat(), folderPath, conf);
        }
    }

    private static void changeDelay(ConfigFileLoader configFileLoader, String folderPath) throws Exception{
        if( ! configFileLoader.isGossipDispatcher())
            throw new InvalidPropertiesFormatException("Cannot run demo type \"change delay\" with default dispatcher");

        for(int delay : configFileLoader.getDelayAsList()){
            Configuration conf = configFileLoader.getConfigBuilder().setDelay(delay, configFileLoader.getDelayTimeUnit()).build();
            runNSimulations(configFileLoader.getClientPort(), configFileLoader.getRepeat(), folderPath, conf);
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

}