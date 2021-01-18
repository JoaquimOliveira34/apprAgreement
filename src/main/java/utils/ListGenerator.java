package utils;

import java.util.ArrayList;
import java.util.List;

public class ListGenerator {
    public static List<Integer> linear(int initInclusive, int endExclusive, int step){
        ArrayList<Integer> list = new ArrayList<>();
        for(int i = initInclusive; i < endExclusive; i+=step)
            list.add(i);
        return list;
    }

    public static List<Integer> exponential(int initInclusive, int endExclusive, int base){
        ArrayList<Integer> list = new ArrayList<>();
        for(int i = 1; i < endExclusive; i*= base){
            if( i >= initInclusive)
                list.add(i);
        }
        return list;
    }
}
