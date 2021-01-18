import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import utils.ListGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DemoConfig  {
    private static final int DELAY = 0;
    private static final int FANOUT = 0;
    private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.NANOSECONDS;
    private final Config conf;

    // Constructors
    public DemoConfig(String filename){
        this.conf = ConfigFactory.load(filename);
    }

    // Client Getters
    public boolean isGossipDispatcher(){
        return conf.getConfig("client").getBoolean("gossip_dispatcher");
    }

    public boolean isClientDebug(){
        return conf.getConfig("client").getBoolean("debug");
    }
    
    public int getClientPort(){
        return conf.getConfig("client").getInt("port");
    }

    // Demo Getters
    public DemoType getDemoType(){
        return DemoType.getDemoType(conf.getConfig("demo").getString("demo_type"));
    }

    public int getRepeat(){
        return conf.getConfig("demo").getInt("repeat");
    }

    public double getEpsilon(){
        return conf.getConfig("demo").getDouble("epsilon");
    }

    public int getMean(){
        return conf.getConfig("demo").getInt("mean");
    }

    public int getStandardDerivation(){
        return conf.getConfig("demo").getInt("st");
    }

    public List<Integer> getStandardDerivationList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("st");
    }

    public int getProcessNumber(){
        return conf.getConfig("demo").getInt("n");
    }

    public List<Integer> getProcessNumberList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("n");
    }

    public List<Integer> getFaultyProcessList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("f");
    }

    public Integer getFaultyProcess(int defaultValue) {
        try {
            return conf.getConfig("demo").getInt("f");
        }catch (ConfigException.Missing e){
            return defaultValue;
        }
    }

    public int getDelay() {
        try{
            return conf.getConfig("demo").getInt("delay");
        }catch (ConfigException.Missing e){
            return DELAY;
        }
    }

    public List<Integer> getDelayAsList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("delay");
    }

    public TimeUnit getDelayTimeUnit() {
        try{
            return conf.getConfig("demo").getEnum( TimeUnit.class ,"delay_unit");
        }catch (ConfigException.Missing e){
            return DELAY_TIME_UNIT;
        }
    }

    public int getFanout() {
        try{
            return conf.getConfig("demo").getInt("fanout");
        }catch( ConfigException.Missing e){
            return FANOUT;
        }
    }

    public List<Integer> getFanoutList() throws InvalidPropertiesFormatException {
        return getDemoValueAsList("fanout");
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

    @Override
    public String toString() {
        return conf.toString();
    }
}
