import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static utils.FileUtils.loadFile;
import static utils.FileUtils.selectFile;

public class TestTime {
    static final int delayInit = 16000;
    static final int delayEnd = 6300;

    public static void main(String[] args) throws Exception {
        File f = selectFile(Demo.MAIN_FOLDER);
        System.out.println("\nLoading " + f.getName() + " data..");
        Map<Configuration, List<Execution>> map = new HashMap<>();

        File[] files = f.listFiles();
        if (files == null || files.length == 0)
            throw new FileNotFoundException();

        // load all execution files
        for (File execFile : files) {
            try {
                Execution exec = (Execution) loadFile(execFile);
                map.putIfAbsent(exec.getConfiguration(), new ArrayList<>());
                map.get(exec.getConfiguration()).add(exec);
            } catch (Exception e) {
                if (execFile.getName().endsWith(".ser"))
                    System.err.println("Warning: Cannot load file " + execFile.getName());
            }
        }
        System.out.println("Done!");

        // reduce
        Map<Configuration, Double> diffByConfig = map.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                        .mapToInt(e -> e.getPhysicalMessagesReceived() - e.getUsefulMessagesCount())
                        .average().getAsDouble()));

        // Print first delay with diff less than $delayInit
        try{
            Integer v = diffByConfig.entrySet().stream()
                    .filter(e -> e.getValue() <= delayInit)
                    .sorted(Comparator.comparingInt(e -> e.getKey().delay))
                    .findFirst().get().getKey().delay;

            System.out.println( "First delay where diff < " + delayInit + " has " + v);
        }catch (Exception e){
            e.printStackTrace();
        }

        // Print first delay with diff less than $delayEnd
        Integer v = diffByConfig.entrySet().stream()
                .filter(e -> e.getValue() <= delayEnd)
                .sorted(Comparator.comparingInt(e -> e.getKey().delay))
                .findFirst().get().getKey().delay;

        System.out.println( "First delay where diff < " + delayEnd + " has " + v);
    }

}
