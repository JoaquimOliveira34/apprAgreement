package utils;

import java.util.*;

public class MyMath {
    // WARNING: These functions can destroy the input list "v". A clone should be pass.

    private static Double average(List<Double> v) {
        return v.stream().mapToDouble(x->x).average().getAsDouble();
    }

    private static List<Double> select(int k, List<Double> V){
        V.sort(Double::compareTo);
        int j = Math.floorDiv(V.size()-1,k);
        List<Double> new_V = new ArrayList<>();
        for(int i = 0; i <= j; i++)
            new_V.add( V.get(i*k));
        return new_V;
    }

    private static List<Double> reduce(int t, List<Double> V){
        for(int i = 0; i < t; i ++){
            V.remove(Collections.max(V));
            V.remove(Collections.min(V));
        }
        return V;
    }

    public static double c(int m, int k){
        return Math.floor((m-1)/(double)k)+1;
    }

    public static double standardDerivation(Collection<Double> numArray) {
        double average = numArray.stream().mapToDouble(x->x).average().getAsDouble();
        double variance = numArray.stream().map((x->Math.pow(x-average,2))).reduce(Double::sum).get()/numArray.size();
        return Math.sqrt(variance);
    }

    public static int maxRounds(int n, int t, double e, Collection<Double> V){
        int m = n-3*t;
        int k = 2*t;
        double range = Collections.max(V) - Collections.min(V);
        return (int) Math.ceil( Math.log(range/e) / Math.log(MyMath.c(m,k)));
    }

    public static double newValue(int t, Collection<Double> V, boolean initialRound){
        if(initialRound)
            return average(reduce(2*t, new ArrayList<>(V)));
        else
            return average(select(2*t, reduce(t, new ArrayList<>(V))));
    }

}
