package utils;

import java.security.SecureRandom;
import java.util.*;

public class MathAnalyser {
    static double e = 0.001;

    public static void main(String[] args) {
        HashMap<Integer, Double> map = new HashMap<>();
        int n = 125;
        for(int f = 1; f < 25; f ++){
            double v = MyMath.c(n - 3 * f, 2 * f);
            map.put(f, v);
            System.out.println(v);
        }
        new GraphPlotter.GraphPlotterBuilder()
                .setTitle("c function")
                .setXAxisLabel("f")
                .setYAxisLabel("result")
                .addDoubleSeries(map, "n=500")
                .build()
                .setVisible(true);

    }


    // Plot functions
    private static void changeFaultyProcessNumber(int n) {
        Map<Integer, Integer> map = new HashMap<>();
        List<Double> initialValues = mostWeightOnExtremes(n,  100, 1100);

        plotInitialValues(initialValues);

        for(int f = 0; f < n/5; f ++)
            map.put(f,MyMath.maxRounds(n, f, e, initialValues));

        new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Rounds determination function")
                .setXAxisLabel("f")
                .setYAxisLabel("rounds")
                .addIntegerSeries(map, "n="+n)
                .build()
                .setVisible(true);
    }

    private static void changeProcessNumberWithStaticF(int nMin, int nMax, int f) {
        Map<Integer, Integer> map = new HashMap<>();

        for(int n = nMin; n < nMax; n ++) {
            List<Double> initialValues = linear(n,  100, 10);
            map.put(n, MyMath.maxRounds(n, f, e, initialValues));
        }

        new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Rounds determination function")
                .setXAxisLabel("n")
                .setYAxisLabel("rounds")
                .addIntegerSeries(map, "f="+f)
                .build()
                .setVisible(true);
    }

    private static void changeProcessNumberWithCrescF(int nMin, int nMax) {
        Map<Integer, Integer> roundsByP = new HashMap<>();
        Map<Integer, Integer> faulty = new HashMap<>();

        for(int n = nMin; n < nMax; n ++) {
            List<Double> initialValues = linear(n,  100, 10);
            int f = (n-1)/5;
            faulty.put(n, f);
            roundsByP.put(n, MyMath.maxRounds(n, f, e, initialValues));
        }

        new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Rounds determination function")
                .setXAxisLabel("n")
                .setYAxisLabel("count")
                .addIntegerSeries(roundsByP, "rounds")
                //.addIntegerSeries(faulty, "faulty process")
                .build()
                .setVisible(true);
    }


    // Initial values generator
    private static List<Double> repeat(int count, double value) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < count; i++)
            list.add(value);
        return list;
    }

    private static List<Double> linear(int count, double base, double step) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < count; i++)
            list.add(base + i * step);
        return list;
    }

    private static List<Double> fromNormalDistribution(int count, int mean, int st) {
        SecureRandom r = new SecureRandom();
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < count; i++)
            list.add(r.nextGaussian() * st + mean);
        return list;
    }

    private static List<Double> mostWeightOnExtremes(int count, double min, double max) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i < count / 2)
                list.add(min);
            else
                list.add(max);
        }
        return list;
    }


    private static void plotInitialValues(List<Double> values){
        HashMap<Integer, Integer> map = new HashMap<>();
        for( Double v : values)
            map.merge( v.intValue(), 1, Integer::sum);

        new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Initial Values")
                .setXAxisLabel("values")
                .setYAxisLabel("count")
                .addIntegerSeries(map, "values")
                .build()
                .setVisible(true);

    }
}
