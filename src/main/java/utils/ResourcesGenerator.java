package utils;

import java.io.FileWriter;
import java.io.IOException;

public class ResourcesGenerator {
    private static final String path = "./src/main/resources/";

    public static void main(String[] args) throws IOException {
        int n = 201;
        int fanoutGeneratorStep = n/23;
        int repeat = 3;
        int delayGroupSize = fanoutGeneratorStep; // or 1
        int[] delayValues = new int[]{
                100000,
                500000,
                1000000,
                2000000,
                3000000,
                4000000,
                8000000,
                16000000,
                32000000,
                64000000,
                120000000,
        };

        writeToFile(path + "run_without.conf", formatWithoutDispatcher(repeat, n));
        for(int i = 0; i < delayValues.length; i ++)
            writeToFile(path + "run_with_" + (i+1) +".conf", formatWithDispatcher(repeat,n,fanoutGeneratorStep, delayValues[i], delayGroupSize));
    }

    private static String formatWithDispatcher(int repeat, int n, int fanoutStep, int delay, int delayGroupSize){
        return  "# Process configuration:\n" +
                "client : {\n" +
                "    debug : false\n" +
                "    port : 12345\n" +
                "    gossip_dispatcher : true\n" +
                "}\n" +
                "\n" +
                "# Simulation configuration:\n" +
                "demo : {\n" +
                "    repeat : " + repeat + "\n" +
                "    epsilon : 0.001\n" +
                "    mean: 100\n" +
                "    st : 100\n" +
                "    demo_type : ch_fanout\n" +
                "    n : " + n + "\n" +
                "    fanout : {generator: linear, start: 0, end: "+n+", step: "+ fanoutStep+ "}\n" +
                "    delay : " + delay + "\n" +
                "    delay_group_size : " + delayGroupSize +" \n" +
                "}\n" +
                "\n" +
                "# Minha4 configuration:\n" +
                "network : {\n" +
                "    latency : { distribution : uniform, lower : 1000000, upper : 2000000}\n" +
                "    reliability : 1\n" +
                "}\n" +
                "\n" +
                "cpu : {\n" +
                "    step : 1000\n" +
                "}";
    }

    private static String formatWithoutDispatcher(int repeat, int n){
        return "# Process configuration:\n" +
                "client {\n" +
                "    debug : false\n" +
                "    port : 12345\n" +
                "    gossip_dispatcher : false\n" +
                "}\n" +
                "\n" +
                "# Simulation configuration:\n" +
                "demo : {\n" +
                "    repeat : " + repeat + "\n" +
                "    epsilon : 0.001\n" +
                "    mean: 100\n" +
                "    st : 100\n" +
                "    demo_type : ch_n\n" +
                "    n : { generator : from_values, list: ["+ n +"] }\n" +
                "}\n" +
                "\n" +
                "# Minha4 configuration:\n" +
                "network : {\n" +
                "    latency : { distribution : uniform, lower : 1000000, upper : 2000000}\n" +
                "    reliability : 1\n" +
                "}\n" +
                "\n" +
                "cpu : {\n" +
                "    step : 1000\n" +
                "}";
    }

    private static void writeToFile(String fileName, String data) throws IOException {
        FileWriter myWriter = new FileWriter(fileName);
        myWriter.write(data);
        myWriter.close();
    }
}
