package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import graph.Graph;
import graph.GraphFactory;
import graph.State;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Runnable{
    /****************For Debugging*****************/
    //private final PrintStream fileOut;
    private int blue_count = 0;
    private int red_count = 0;
    private int prev_b_count = 0;
    //private int prev_r_count = 0;*/
    /****************For Debugging*****************/

    //StateCount is custom class that provides a hashmap, maybe we can combine the attributes
    //of this with where we store whether a state is red, suppose we could also try synchronized blocks
    //inside the methods of StateCount and Colors, then we might be able to reduce waiting times more
    private static final StateCount stateCount = new StateCount();
    private static final Lock lock = new ReentrantLock();
    private static final Condition isZero = lock.newCondition();

    private static final Lock doneLock = new ReentrantLock();
    private static final Condition isDoneCond = doneLock.newCondition();


    private static final Lock redLock = new ReentrantLock();
    private static final Condition redFreeCond = redLock.newCondition();

    private final Graph graph;
    private static final Colors globalColors = new Colors(); //For red dfs
    private static final GlobalStates gS = new GlobalStates();
    private final Colors colors = new Colors(); //For blue dfs (and for storing pink, but this is separated from WHITE, CYAN, BLUE)

    private static AtomicBoolean result = new AtomicBoolean(false);

    private static AtomicBoolean isDone = new AtomicBoolean(false);

    private final int id;



    //private static final ConcurrentHashMap<State, AtomicBoolean> map = new ConcurrentHashMap<State, AtomicBoolean>();

    // Throwing an exception is a convenient way to cut off the search in case a
    // cycle is found.
    private static class CycleFoundException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class GraphTraversedException extends Exception {
        private static final long serialVersionUID = 2L;
    }

    /**
     * Constructs a Worker object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public Worker(File promelaFile, int workerId) throws FileNotFoundException {
        this.graph = GraphFactory.createGraph(promelaFile);
        this.id = workerId;

    }

    private void dfsRed(State s, int workerId) throws CycleFoundException, GraphTraversedException {
        //red_count++;
        //Makes sure that thread is terminated if one is already finished
        if(Thread.currentThread().isInterrupted()){
            throw new GraphTraversedException();
        }

        colors.color(s, Color.PINK);
        for (State t : permutate(graph.post(s))) {
            if (colors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();

            //Previously had to call the Worker.isRed function so to lock the var, thats not necessary
            //anymore, now can just straight up query GlobalStates class for some state t
            //this doesnt even use a lock, concurrent reads may have to wait if the region of the hashmap
            //in which state t lies, is currently being written, otherwise, all threads can grab the value
            //at the same time 
            } else if(!colors.hasColor(t, Color.PINK) && !gS.isRed(t)){
                dfsRed(t, workerId);
            }
        }
        
        if(s.isAccepting()) {
//            lock.lock();
//            try{
                stateCount.decrement(s);
//                int postValue = stateCount.currentCount(s);
                if(stateCount.currentCount(s) == 0)
                    isZero.signalAll();
                while(stateCount.currentCount(s) != 0 ){
                    isZero.await();
                }
//            }
//        catch(InterruptedException e){
//                e.printStackTrace();
//            }
////        finally{
//                lock.unlock();
//            }
        }
        
        
        /*redLock.lock();
        try{
            //globalColors.color(s, Color.RED);
            setRed(s, true);
        } finally{
            redLock.unlock();
        }*/
        //instead of having the lock here, now the locking is done deep inside hashmap
        //which essentially places the locks really barely around the writes, instead of 
        //having the locks around invocation of several more methods
        gS.setRed(s);
        
        colors.color(s, Color.NOTPINK);
    }

    private void dfsBlue(State s, int workerId) throws CycleFoundException, GraphTraversedException {
        blue_count++;
        if(prev_b_count < blue_count - 100000){
            System.out.printf("Thread %d, bc = %d\n", this.id, blue_count);
            prev_b_count = blue_count;
        }

        if(Thread.currentThread().isInterrupted()){
            throw new GraphTraversedException();
        }
        colors.color(s, Color.CYAN);
        for (State t : permutate(graph.post(s))) {
            //same here eh
            if(colors.hasColor(t, Color.WHITE) && !gS.isRed(t)){
                dfsBlue(t, workerId);
            }
        }
        if (s.isAccepting()) {
            lock.lock();
            try{
                stateCount.increment(s);
            } finally{
                lock.unlock();

            }
            dfsRed(s, workerId);
        }

        colors.color(s, Color.BLUE);
    }

    private void nndfs(State s, int workerId) throws CycleFoundException, GraphTraversedException {
        System.out.printf("Thread %d started\n", this.id);
        dfsBlue(s, workerId);
        //System.out.printf("Thread %d ended, bc=%d rc=%d\n", this.id, this.blue_count, this.red_count);
    }

    @Override
    public void run() {
        try {
            nndfs(graph.getInitialState(), this.id);
            doneLock.lock();
            try{
                isDone.set(true);
            } finally{
                doneLock.unlock();
            }
        } catch (CycleFoundException e) {
            doneLock.lock();
            try{
                //System.out.printf("Setting result to true\n");
                result.set(true);
                isDone.set(true);
            } finally{
                doneLock.unlock();
            }
        } catch(GraphTraversedException e){
            //Simply exit, as some other thread has reported the result
            //System.out.printf("Thread %d is done\n", this.id);
        } catch (Exception e){
            System.out.println("Unexpected exception was caught");
        }
    }

    /*private boolean isRed(State s, Color c){
        boolean ret;
        System.out.printf("About to check red\n");
        redLock.lock();
        try{
            //ret = globalColors.hasColor(s, c);
            ret = gS.isRed(s);
            System.out.printf("Red: %s\n", ret);
        } finally{
            redLock.unlock();
        }
        return ret;
    }*/

    //This performs exceptionally terrible
    private List<State> permutate(List<State> list){
        int size = list.size();
        List<State> newList;
        if(size <= 1){
            return list;
        } else if(size == 2){
            if(this.id % 2 == 1)
                Collections.swap(list, 0, 1);
        } else{
            Collections.shuffle(list, new Random(this.id));
        }
        return list;
    }

    public static boolean getResult() {
        //System.out.printf("Done = %s, Result: %s\n", isDone, result);
        boolean resultFinal;
        doneLock.lock();
        try{
            resultFinal = result.get();
        } finally{
            doneLock.unlock();
        }
        return resultFinal;
    }

    public static boolean youDoneYet(){
        boolean ret;
        doneLock.lock();
        try{
            ret = isDone.get();
        } finally{
            doneLock.unlock();
        }
        //System.out.println("Going hard in this bitch");
        return ret;
    }


    /*public static boolean isRed(State s) {
        if(map.containsKey(s)){
            return map.get(s).get();
        }
        return false;
    }

    public static void setRed(State s){
        if(map.containsKey(s)){
            map.get(s).getAndSet(true);
        } else{
            map.put(s, new AtomicBoolean(true));
        }
    }*/


}