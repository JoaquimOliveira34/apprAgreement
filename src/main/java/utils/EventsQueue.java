package utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EventsQueue<E> {
    //TODO RENAME OBJECT
    private Queue<Set<E>> queue;
    private CompletableFuture<Void> cf;

    public EventsQueue(){
        queue = new LinkedList<>();
    }

    public synchronized void pushEvents(List<E> events){
        Iterator<Set<E>> it = queue.iterator();
        boolean itEmpty = false;
        for( E event : events){
            if( !itEmpty && it.hasNext()  ){
                it.next().add(event);
            }else{
                itEmpty = true;
                Set<E> eventSet = new HashSet<>();
                eventSet.add(event);
                queue.add(eventSet);
            }
        }
    }

    public synchronized Set<E> popNextEventsSet(){
        try{
            return queue.poll();
        }finally {
            if(queue.isEmpty() && cf != null) cf.complete(null);
        }
    }

    public synchronized int eventsWaiting(){
        return queue.stream().mapToInt(Set::size).sum();
    }

    public synchronized boolean isEmpty(){
        return queue.isEmpty();
    }
}
