import java.security.InvalidParameterException;

public enum DemoType{
    CHANGE_PROCESS_NUMBER("ch_n"),
    CHANGE_ST("ch_st"),
    CHANGE_FAULTY("ch_f"),
    CHANGE_FANOUT("ch_fanout"),
    CHANGE_DELAY("ch_delay");

    private final String name;

    DemoType(String s) {
        name = s;
    }

    public String toString(){
        return this.name;
    }

    public static DemoType getDemoType(String tag){
        switch (tag){
            case "ch_n":
                return  DemoType.CHANGE_PROCESS_NUMBER;
            case "ch_st":
                return DemoType.CHANGE_ST;
            case "ch_f":
                return  DemoType.CHANGE_FAULTY;
            case "ch_fanout":
                return DemoType.CHANGE_FANOUT;
            case "ch_delay":
                return DemoType.CHANGE_DELAY;
            default:
                throw new InvalidParameterException();
        }
    }

    }

