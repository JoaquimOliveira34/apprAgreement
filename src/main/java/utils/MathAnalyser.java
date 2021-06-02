package utils;

import java.security.SecureRandom;
import java.util.*;

public class MathAnalyser {
    static final String splitter = ";";
    static double e = 0.001;

    public static void main(String[] args) {
        // changeFaultyProcessNumber(150);
        // changeProcessNumberWithStaticF(26, 150, 5);
        changeProcessNumberWithCrescF(6, 150);
    }


    // Plot functions
    private static void changeFaultyProcessNumber(int n) {
        List<Double> initialValuesEx = mostWeightOnExtremes(n,  100, 1100);
        List<Double> initialValuesRep = repeat(n , 500);
        List<Double> initialValuesND = fromNormalDistribution(n, 500, 250);
        List<Double> initialValuesLin = linear(n,  100, 1000/n);

        System.out.println("Max rounds : Change Faulty Process Number (n=" +n + ")");
        // System.out.println("n" + splitter + "f" + splitter + "Rep_rounds"+ splitter + "Ex_rounds"+ splitter + "ND_rounds"+ splitter + "Lin_rounds");
        System.out.println("n" + splitter + "f" + splitter + "rounds");

        for(int f = 1; f < n/5; f ++) {
            String r = MyMath.maxRounds(n, f, e, initialValuesRep) + splitter +
                    MyMath.maxRounds(n, f, e, initialValuesEx) + splitter +
                    MyMath.maxRounds(n, f, e, initialValuesND) + splitter +
                    MyMath.maxRounds(n, f, e, initialValuesLin);
            System.out.println(n + splitter + f + splitter + MyMath.maxRounds(n, f, e, initialValuesLin));
        }
    }

    private static void changeProcessNumberWithStaticF(int nMin, int nMax, int f) {
        System.out.println("Max rounds : Change Process Number (f static)");
        System.out.println("n" + splitter + "f" + splitter + "rounds");

        for(int n = nMin; n < nMax; n ++) {
            List<Double> initialValues = linear(n,  100, 1000/n);
            System.out.println( n + splitter + f + splitter + MyMath.maxRounds(n, f, e, initialValues));
        }

        // new GraphPlotter.GraphPlotterBuilder()
        //        .setTitle("Rounds determination function")
        //        .setXAxisLabel("n")
        //        .setYAxisLabel("rounds")
        //        .addIntegerSeries(map, "f="+f)
        //        .build()
        //        .setVisible(true);
    }

    private static void changeProcessNumberWithCrescF(int nMin, int nMax) {
        System.out.println("Max rounds : Change Process Number (f max)");
        System.out.println("n" + splitter + "f" + splitter + "rounds");

        for(int n = nMin; n < nMax; n ++) {
            List<Double> initialValues = linear(n,  100, 1000/n);
            int f = (n-1)/5;
            System.out.println( n + splitter + f + splitter + MyMath.maxRounds(n, f, e, initialValues));
        }
    }

    // C Function
    private static void changeFaultyProcessNumber_C(int n){
        HashMap<Integer, Double> map = new HashMap<>();
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

    // Initial values generators
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
