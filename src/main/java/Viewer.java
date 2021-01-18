import org.javatuples.Triplet;
import utils.FileUtils;
import utils.GraphPlotter;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static utils.FileUtils.loadFile;
import static utils.FileUtils.selectFile;

public class Viewer {
    // print table
    public static final boolean printResultsAsTable= false;
    // select folder/simulation to resume by terminal interaction
    public static final boolean interactiveMode = false;
    // Special interactive mode to select folder/simulation and file/execution to analise.
    private static final boolean openSingleExecution = false;
    // if true graphs will be plotted, if false graphs will be export to .jpg file
    public static final boolean printGraphs = false;
    // force to plot round graphs
    private static final boolean plotRoundGraph = false;
    // plot gossip messages type
    private static final boolean plotGossipMessages = true;

    // dealy -> fanout, message, time
    private static final Map<Integer, Triplet<Integer, Integer, Double>> bestTimeMap = new HashMap<>();
    private static final Map<Integer, Triplet<Integer, Integer, Double>> bestMessageMap = new HashMap<>();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (openSingleExecution) {
            viewSingleExecutionConfiguration();
            return;
        }
        if (interactiveMode) {
            viewSimulationData(selectFile(Demo.MAIN_FOLDER));
            return;
        }
        File folders = new File(Demo.MAIN_FOLDER);
        for (File demoFile : folders.listFiles()) {
            try {
                viewSimulationData(demoFile);
            } catch (Exception e) { e.printStackTrace(); }
        }
        if( printResultsAsTable){
            System.out.println("Delay\tFanout\tMessage\tTime\tFanout\tMessage\tTime");
            int timeRound = 1000000;

            DecimalFormat df = new DecimalFormat("#####");
            DecimalFormat df_2 = new DecimalFormat("#####.##");

            for( Integer delay : bestMessageMap.keySet().stream().sorted().collect(Collectors.toList())){
                Triplet<Integer, Integer, Double> v = bestTimeMap.get(delay);
                Triplet<Integer, Integer, Double> v2 = bestMessageMap.get(delay);
                System.out.print(delay/1000000.0 + "\t" + v.getValue0() + "\t" + df.format(v.getValue1()) + "\t" +  df_2.format(v.getValue2()/timeRound) + "\t" );
                System.out.println( v2.getValue0()  + "\t" + df.format(v2.getValue1())  + "\t" + df_2.format(v2.getValue2()/timeRound));
            }
        }
    }

    public static void viewSimulationData(File demoFolder) throws IOException {
        System.out.println("\nLoading " + demoFolder.getName() + " data..");
        Map<Configuration, List<Execution>> map = new HashMap<>();

        File[] files = demoFolder.listFiles();
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

        if (map.size() == 1) {
            plotRoundGraphs(map.values().stream().findFirst().get(), demoFolder);
            return;
        }

        if (plotRoundGraph)
            plotRoundGraphs(map.values().stream().flatMap(List::stream).collect(Collectors.toList()), demoFolder);

        switch (DemoType.getDemoType(demoFolder.getName().split(":")[0])) {
            case CHANGE_PROCESS_NUMBER:
                plotExecutionGraphs(map.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().get(0).getConfiguration().n, e -> e.getValue())),
                        "n",
                        demoFolder);
                break;
            case CHANGE_FAULTY:
                plotExecutionGraphs(map.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().get(0).getConfiguration().t, e -> e.getValue())),
                        "f",
                        demoFolder);
                break;
            case CHANGE_ST:
                plotExecutionGraphs(map.entrySet().stream().collect(Collectors.toMap(e -> (int) e.getValue().get(0).getConfiguration().standardDeviation, e -> e.getValue())),
                        "desvio padrão",
                        demoFolder);
                break;
            case CHANGE_FANOUT:
                plotExecutionGraphs(map.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().get(0).getConfiguration().fanout, e -> e.getValue())),
                        "fanout",
                        demoFolder);
                break;
            case CHANGE_DELAY:
                plotExecutionGraphs(map.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().get(0).getConfiguration().delay, e -> e.getValue())),
                        "delay",
                        demoFolder);
                break;
            default:
                System.err.println("Warning: Unknown demo run type");
        }
    }

    public static void plotExecutionGraphs(Map<Integer, List<Execution>> results, String xAxisLabel, File folder) throws IOException {
        if ( printResultsAsTable) {
            Configuration c = results.values().stream().findFirst().get().get(0).getConfiguration();
            collectFanoutByBestTimeAndMessages(results, c.delay);
        }else{
            plotRoundsCount(xAxisLabel, results, folder.getAbsolutePath() + "/roundsCount.png");
            plotMessagesCount(xAxisLabel, results, folder.getAbsolutePath() + "/messagesCount");
            plotNanoTimeConsuming(xAxisLabel, results, folder.getAbsolutePath() + "/timeConsuming.png");
        }

    }

    public static void plotRoundGraphs(List<Execution> executions, File folder) throws IOException {
        if (executions.size() == 0)
            return;

        FileUtils.saveString(folder.getAbsolutePath() + "/resume.csv", executionResumeAsCsv(executions) );

        plotDiffMSetsReceivedByRound(executions, folder.getAbsolutePath() + "/diffMSetsByRound.png");
        plotStandDerivationByRound(executions, folder.getAbsolutePath() + "/stByRound.png");
        plotAmplitudeByRound(executions, folder.getAbsolutePath() + "/amplitudeByRound.png");
        plotMessagesByRound(executions, folder.getAbsolutePath() + "/messagesByRound.png");
        plotRunningByRound(executions, folder.getAbsolutePath() + "/runningByRound.png");
        plotExecutionTimeByProcess(executions, folder.getAbsolutePath() + "/timeByProcess.png");
    }


    /*
     * Plot types
     */

    // Plot rounds by round graphs
    private static void plotRunningByRound(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Running")
                .setXAxisLabel("rounds")
                .setYAxisLabel("process");

        for (Execution exc : executions) {
            gpBuilder.addIntegerSeries(exc.runningByRound(), exc.id + "");
        }

        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotMessagesByRound(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Messages ignored")
                .setXAxisLabel("rounds")
                .setYAxisLabel("messages");

        for (Execution exc : executions) {
            gpBuilder.addIntegerSeries(exc.getMessagesIgnored(), exc.id + "");
        }
        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotStandDerivationByRound(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Standard derivation")
                .setXAxisLabel("rounds")
                .setYAxisLabel("st");

        for (Execution exc : executions)
            gpBuilder.addDoubleSeries(exc.standardDerivationByRound(), exc.id + "");

        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotAmplitudeByRound(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Amplitude")
                .setXAxisLabel("rounds")
                .setYAxisLabel("amplitude");

        for (Execution exc : executions)
            gpBuilder.addDoubleSeries(exc.amplitudeByRound(), exc.id + "");

        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotDiffMSetsReceivedByRound(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Different messages set")
                .setXAxisLabel("rounds")
                .setYAxisLabel("Messages sets count");

        for (Execution e : executions)
            gpBuilder.addIntegerSeries(e.getUsefulMessagesSenders(), "" + e.id);


        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotExecutionTimeByProcess(List<Execution> executions, String fileName) throws IOException {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Execution time")
                .setXAxisLabel("nano seconds")
                .setYAxisLabel("process id");

        for (Execution e : executions) {
            List<Long> list = e.getExecutionTimeByProcess();
            Map<Integer, Long> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++)
                map.put(i, list.get(i));
            gpBuilder.addLongSeries(map, "" + e.id);
        }

        GraphPlotter gp = gpBuilder.build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    // Plot execution graphs
    private static void plotRoundsCount(String xAxisLabel, Map<Integer, List<Execution>> results, String fileName) throws IOException {
        Map<Integer, Double> results_requiredRounds = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::requiredRounds)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> results_totalRoundsByAll = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::lastRoundByAll)
                        .average()
                        .getAsDouble()));

        GraphPlotter gp = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Rondas")
                .setXAxisLabel(xAxisLabel)
                .setYAxisLabel("Total")
                .addDoubleSeries(results_requiredRounds, "Rondas necessárias")
                .addDoubleSeries(results_totalRoundsByAll, "Ronda máxima executada por todos")
                .build();
        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    private static void plotMessagesCount(String xAxisLabel, Map<Integer, List<Execution>> results, String fileName) throws IOException {
        Map<Integer, Double> resultsIgnoredMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToInt(Execution::getMessagesIgnoredCount)
                        .average()
                        .orElse(0d)));

        Map<Integer, Double> resultsUsefulMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getUsefulMessagesCount)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> resultsReceivedMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getPhysicalMessagesReceived)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> resultsLogicSentMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getLogicalMessagesSent)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> resultsPhysicalSentMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getPhysicalMessagesSent)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> resultQueuedMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getMessagesQueuedLost)
                        .average()
                        .getAsDouble()));

        Map<Integer, Double> resultsLostMessages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToDouble(Execution::getPhysicalMessagesLost)
                        .average()
                        .getAsDouble()));

        GraphPlotter gp1, gp2;
        String fileName1, fileName2;

        if (plotGossipMessages) {
            gp1 = new GraphPlotter.GraphPlotterBuilder()
                    .setTitle("Mensagens Recebidas")
                    .setXAxisLabel(xAxisLabel)
                    .setYAxisLabel("Total")
                    .addDoubleSeries(resultsIgnoredMessages, "Ignoradas")
                    .addDoubleSeries(resultsUsefulMessages, "Uteis")
                    .addDoubleSeries(resultsReceivedMessages, "F. Recebidas")
                    .build();

            gp2 = new GraphPlotter.GraphPlotterBuilder()
                    .setTitle("Mensagens Enviadas")
                    .setXAxisLabel(xAxisLabel)
                    .setYAxisLabel("Total")
                    .addDoubleSeries(resultsLogicSentMessages, "L. Enviadas")
                    .addDoubleSeries(resultsPhysicalSentMessages, "F. Enviadas")
                    .addDoubleSeries(resultsLostMessages, "Perdidas")
                    .addDoubleSeries(resultQueuedMessages, "queued lost")
                    .build();
            fileName1 = fileName + "_received.png";
            fileName2 = fileName + "_sent.png";
        } else {
            gp1 = new GraphPlotter.GraphPlotterBuilder()
                    .setTitle("Mensagens")
                    .setXAxisLabel(xAxisLabel)
                    .setYAxisLabel("Total")
                    .addDoubleSeries(resultsIgnoredMessages, "Ignoradas")
                    .addDoubleSeries(resultsUsefulMessages, "Uteis")
                    .addDoubleSeries(resultsLogicSentMessages, "L. Enviadas")
                    .addDoubleSeries(resultsLostMessages, "Perdidas")
                    .build();
            fileName1 = fileName + ".png";
        }

        if (printGraphs) {
            gp1.setVisible(true);
            if (gp2 != null) gp2.setVisible(true);
        } else {
            gp1.saveAsPNGTo(new FileOutputStream(fileName1));
            if (gp2 != null) gp2.saveAsPNGTo(new FileOutputStream(fileName2));
        }
    }

    private static void plotNanoTimeConsuming(String xAxisLabel, Map<Integer, List<Execution>> results, String fileName) throws IOException {
        Map<Integer, Double> time = results.entrySet().stream()
                .collect(Collectors.toMap( e -> e.getKey(), e -> e.getValue().stream()
                        .mapToLong(Execution::getExecutionTime)
                        .average()
                        .getAsDouble()));

        GraphPlotter gp = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Time consuming")
                .setXAxisLabel(xAxisLabel)
                .setYAxisLabel("nano seconds")
                .addDoubleSeries(time, "time")
                .build();

        if (printGraphs)
            gp.setVisible(true);
        else
            gp.saveAsPNGTo(new FileOutputStream(fileName));
    }

    // Extra views
    private static String executionResumeAsCsv(List<Execution> executions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Execution,Rounds,,Logical Messages,,,,Physical Messages,,,Time\n");
        sb.append("id,Required,By all,Sent,Useful, Ignored, Queued lost, Sent,Received, Lost, ms\n");

        for (Execution ex : executions) {
            sb.append(ex.id).append(",").append(ex.requiredRounds()).append(",");
            sb.append(ex.lastRoundByAll()).append(",").append(ex.getLogicalMessagesSent()).append(",");
            sb.append(ex.getUsefulMessagesCount()).append(",").append(ex.getMessagesIgnoredCount()).append(",");
            sb.append(ex.getMessagesQueuedLost()).append(',');
            sb.append(ex.getPhysicalMessagesSent()).append(",").append(ex.getPhysicalMessagesReceived()).append(",");
            sb.append(ex.getPhysicalMessagesLost()).append(",").append(ex.getExecutionTime()).append("\n");
        }
        return sb.toString();
    }

    public static void viewSingleExecutionConfiguration() throws IOException, ClassNotFoundException {
        File folder = selectFile(Demo.MAIN_FOLDER);
        Execution exec = (Execution) loadFile(selectFile(folder.getAbsolutePath()));
        System.out.println(exec.getConfiguration().toString());
        // Print some info
        System.out.println("messages ignored " + exec.getMessagesIgnoredCount());
        System.out.println("time " + exec.getExecutionTime());
    }

    private static void collectFanoutByBestTimeAndMessages(Map<Integer, List<Execution>> results, int delay){
        int bestFanout4Messages, bestFanout4Time;
        Double bestMessages, bestTime;

        // Create maps
        Map<Integer, Double> messages = results.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .mapToInt(Execution::getMessagesIgnoredCount)
                        .average()
                        .orElse(0d)));

        Map<Integer, Double> time = results.entrySet().stream()
                .collect(Collectors.toMap( e -> e.getKey(), e -> e.getValue().stream()
                        .mapToLong(Execution::getExecutionTime)
                        .average()
                        .getAsDouble()));

        // Find fanout values
        bestFanout4Messages = 0;
        bestMessages = messages.get(0);
        for (int f : messages.keySet()){
            if (messages.get(f) < bestMessages) {
                bestMessages = messages.get(f);
                bestFanout4Messages = f;
            }
        }
        bestFanout4Time = 0;
        bestTime = time.get(0);
        for (int f : time.keySet()){
            if (time.get(f) < bestTime) {
                bestTime = time.get(f);
                bestFanout4Time = f;
            }
        }

        bestMessageMap.put(delay, new Triplet(bestFanout4Messages, bestMessages, time.get(bestFanout4Messages)));
        bestTimeMap.put(delay, new Triplet(bestFanout4Time, messages.get(bestFanout4Time), bestTime));
    }

}
