import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import utils.ListGenerator;

import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigFileLoader {
    // TODO : Maybe? Replace Configuration.builder by this

    private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.NANOSECONDS;
    private final Config conf;
    private final DemoType demoType;
    private final Configuration.Builder confBuilder ;

    // Constructors
    public ConfigFileLoader(String filename){
        this.conf = ConfigFactory.load(filename);
        confBuilder = new Configuration.Builder();
        demoType = getDemoType();

        confBuilder.setEpsilon(getEpsilon())
                .setMean(getMean())
                .setDelayGroupSize(getDelayGroupSize())
                .setClientDebugMode(isClientDebug())
                .setIsGossipDispatcher(isGossipDispatcher());

        switch (demoType){
            case CHANGE_PROCESS_NUMBER:
                confBuilder.setFaultyProcessesNumber(getFaultyProcess())
                        .setStandardDeviation(getStandardDerivation())
                        .setDelay(getDelay(), getDelayTimeUnit())
                        .setFanout(getFanout());
                break;
            case CHANGE_FAULTY:
                confBuilder.setProcessesNumber(getProcessNumber())
                    .setStandardDeviation(getStandardDerivation())
                    .setDelay(getDelay(), getDelayTimeUnit())
                    .setFanout(getFanout());
                break;
            case CHANGE_ST:
                confBuilder.setProcessesNumber(getProcessNumber())
                        .setFaultyProcessesNumber(getFaultyProcess())
                        .setDelay(getDelay(), getDelayTimeUnit())
                        .setFanout(getFanout());
                break;
            case CHANGE_DELAY:
                confBuilder.setProcessesNumber(getProcessNumber())
                        .setFaultyProcessesNumber(getFaultyProcess())
                        .setStandardDeviation(getStandardDerivation())
                        .setFanout(getFanout());
                break;
            case CHANGE_FANOUT:
                confBuilder.setProcessesNumber(getProcessNumber())
                        .setFaultyProcessesNumber(getFaultyProcess())
                        .setStandardDeviation(getStandardDerivation())
                        .setDelay(getDelay(), getDelayTimeUnit());
        }
    }

    public int getRepeat(){
        return conf.getConfig("demo").getInt("repeat");
    }

    public List<Integer> getStandardDerivationList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("st");
    }

    public List<Integer> getProcessNumberList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("n");
    }

    public List<Integer> getFaultyProcessList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("f");
    }

    public List<Integer> getDelayAsList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("delay");
    }

    public List<Integer> getFanoutList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("fanout");
    }

    public int getClientPort(){
        return conf.getConfig("client").getInt("port");
    }

    public Configuration.Builder getConfigBuilder() {
        return confBuilder;
    }

    public boolean isGossipDispatcher(){
        return conf.getConfig("client").getBoolean("gossip_dispatcher");
    }

    public TimeUnit getDelayTimeUnit() {
        try{
            return conf.getConfig("demo").getEnum( TimeUnit.class ,"delay_unit");
        }catch (ConfigException.Missing e){
            return DELAY_TIME_UNIT;
        }
    }

    @Override
    public String toString() {
        return conf.toString();
    }

    // More Client Getters
    private boolean isClientDebug(){
        return conf.getConfig("client").getBoolean("debug");
    }

    // More Demo Getters
    DemoType getDemoType(){
        return DemoType.getDemoType(conf.getConfig("demo").getString("demo_type"));
    }

    private double getEpsilon(){
        return conf.getConfig("demo").getDouble("epsilon");
    }

    private int getMean(){
        return conf.getConfig("demo").getInt("mean");
    }

    private int getStandardDerivation(){
        return conf.getConfig("demo").getInt("st");
    }

    private int getProcessNumber(){
        return conf.getConfig("demo").getInt("n");
    }

    private Integer getFaultyProcess() {
        try {
            return conf.getConfig("demo").getInt("f");
        }catch (ConfigException.Missing e){
            return null;
        }
    }

    private Integer getDelay() {
        try{
            return conf.getConfig("demo").getInt("delay");
        }catch (ConfigException.Missing e){
            return null;
        }
    }

    private Integer getDelayGroupSize() {
        try{
            return conf.getConfig("demo").getInt( "delay_group_size");
        }catch (ConfigException.Missing e){
            return null;
        }
    }

    private Integer getFanout() {
        try{
            return conf.getConfig("demo").getInt("fanout");
        }catch( ConfigException.Missing e){
            return null;
        }
    }

    private List<Integer> getDemoValueAsList(String key) throws InvalidPropertiesFormatException {
        Config c = conf.getConfig("demo").getConfig(key);
        String prop = c.getString("generator");
        switch (prop){
            case "exponential":
                return ListGenerator.exponential( c.getInt("start"), c.getInt("end"), c.getInt("base"));
            case "linear":
                return ListGenerator.linear( c.getInt("start"), c.getInt("end"), c.getInt("step"));
            case "from_values":
                return c.getIntList("list");
            default:
                throw new InvalidPropertiesFormatException("Invalid dispatcher property: " + prop );
        }
    }

}
