package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import graph.Graph;
import graph.GraphFactory;
import graph.State;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;


/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Runnable{
    /****************For Debugging*****************/
    /*private final PrintStream fileOut;
    private int blue_count = 0;
    private int red_count = 0;
    private int prev_b_count = 0;
    private int prev_r_count = 0;*/
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

    private static final GlobalColors globalColors = new GlobalColors(); //For red dfs
    private final Colors colors = new Colors(); //For blue dfs (and for storing pink, but this is separated from WHITE, CYAN, BLUE)

    private static AtomicBoolean result = new AtomicBoolean(false);
    private static AtomicBoolean isDone = new AtomicBoolean(false);

    private static int threadsN = 0;
    private Random seed;
    private Graph graph;
    private int id;

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
        this.seed = new Random(workerId);
        threadsN++;
    }

    private void dfsRed(State s, int workerId) throws CycleFoundException{
        //Makes sure that thread is terminated if one is already finished
        if(Thread.currentThread().isInterrupted()){
            throw new CycleFoundException();
        }

        colors.color(s, Color.PINK);
        for (State t : permutate(graph.post(s))) {
            if (colors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();
            } else if (!colors.hasColor(t, Color.PINK) && !isRed(t)) {
                dfsRed(t, workerId);
            }
        }
        
        if(s.isAccepting()) {
            lock.lock();
            try{                
                stateCount.decrement(s);
                if(stateCount.currentCount(s) == 0)
                    isZero.signalAll();
                while(stateCount.currentCount(s) != 0 ){
                    isZero.await();
                }
            } catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } finally{
                lock.unlock();
            }
        }

        redLock.lock();
        try{
            globalColors.setRed(s);
        } finally{
            redLock.unlock();
        }
        
        colors.color(s, Color.NOTPINK);
    }

    private void dfsBlue(State s, int workerId) throws CycleFoundException, GraphTraversedException {
        if(Thread.currentThread().isInterrupted()){
            throw new GraphTraversedException();
        }

        colors.color(s, Color.CYAN);
        for (State t : permutate(graph.post(s))) {
            if (colors.hasColor(t, Color.WHITE) && !isRed(t)) {
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

    private void nndfs(State s, int workerId) throws CycleFoundException, GraphTraversedException, InterruptedException {
        dfsBlue(s, workerId);
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        try {
            nndfs(graph.getInitialState(), this.id);
            doneLock.lock();
            try{
                isDone.set(true);
                isDoneCond.signalAll();
            } finally{
                doneLock.unlock();
            }
        } catch (CycleFoundException e) {
            doneLock.lock();
            try{
                result.set(true);
                isDone.set(true);
                isDoneCond.signalAll();
            } finally{
                doneLock.unlock();
            }
        } catch(GraphTraversedException e){
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e){
            System.out.println("Unexpected exception was caught:\n");
            e.printStackTrace();
        }
    }

    private boolean isRed(State s){
        boolean ret;
        redLock.lock();
        try{
            ret = globalColors.isRed(s);
        } finally{
            redLock.unlock();
        }
        return ret;
    }

    //This performs exceptionally terrible
    private List<State> permutate(List<State> list){
        /*System.out.printf("Press enter to start permutation\n");
        try{
            System.in.read();
        } catch(IOException e){
            e.printStackTrace();
        }
        long start = System.nanoTime();*/

        int size = list.size();
        //List<State> newList;
        if(size <= 1){
            //return list;
            //Do nothing
        } else if(size == 2){
            if(this.id % 2 == 1)
                Collections.swap(list, 0, 1);
        } else{
            Collections.rotate(list, size / threadsN * (this.id));
        }
        /*long end = System.nanoTime();
        System.out.printf("Permutation of %d successors took %d nanoseconds\n", size, (end-start));*/
        return list;
    }

    public static boolean getResult() {
        boolean resultFinal;
        doneLock.lock();
        try{
            resultFinal = result.get();
        } finally{
            doneLock.unlock();
        }
        return resultFinal;
    }

    public static void youDoneYet() {
        doneLock.lock();
        try{
            while(isDone.get() != true)
                isDoneCond.await();
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally{
            doneLock.unlock();
        }
    }
}