package ndfs.mcndfs_1_improved;

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

import java.io.IOException;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Runnable{
    private static final StateCount stateCount = new StateCount();
    private static final Lock lock = new ReentrantLock();
    private static final Condition isZero = lock.newCondition();
    
    private static final Lock doneLock = new ReentrantLock();
    private static final Condition isDoneCond = doneLock.newCondition();

    private final Graph graph;
    private static final GlobalColors globalColors = new GlobalColors();
    private final Colors colors = new Colors();

    private static int threadsN = 0;

    private static AtomicBoolean result = new AtomicBoolean(false);
    private static AtomicBoolean isDone = new AtomicBoolean(false);     // Remove when possible

    private final int id;


    // Throwing an exception is a convenient way to cut off the search in case a
    // cycle is found.
    private static class CycleFoundException extends Exception {
        private static final long serialVersionUID = 1L;
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
        threadsN++;
    }

    private void dfsRed(State s) throws CycleFoundException{
        colors.color(s, Color.PINK);

        for (State t : permutate(graph.post(s))) {
            if (colors.hasColor(t, Color.CYAN)) {
                Thread.currentThread().interrupt();
            }
            else if(!colors.hasColor(t, Color.PINK) && !globalColors.isRed(t))
                dfsRed(t);
        }
        
        if(s.isAccepting()) {
            lock.lock();
            try{
                stateCount.decrement(s);
                if(stateCount.currentCount(s) == 0) {
                    isZero.signalAll();
                }
                while(stateCount.currentCount(s) != 0);
                    isZero.await();
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
            } finally{
                lock.unlock();
            }
        }
        globalColors.setRed(s);
        colors.color(s, Color.NOTPINK);
    }

    private void dfsBlue(State s) throws CycleFoundException{
        colors.color(s, Color.CYAN);

        for (State t : permutate(graph.post(s))) {
            if(colors.hasColor(t, Color.WHITE) && !globalColors.isRed(t))
                dfsBlue(t);
        }
        if (s.isAccepting()) {
            stateCount.increment(s);
            dfsRed(s);
        }

        colors.color(s, Color.BLUE);
    }

    private void nndfs(State s, int workerId) throws CycleFoundException{
        dfsBlue(s);

    }

    @Override
    public void run() {
        try {
            nndfs(graph.getInitialState(), this.id);
            Thread.currentThread().interrupt();
        } catch (CycleFoundException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e){
            System.out.println("Unexpected exception was caught");
            e.printStackTrace();
        }
    }

    private List<State> permutate(List<State> list){
        int size = list.size();
        if(size <= 1){
            return list;
        } else if(size == 2){
            if(this.id % 2 == 1)
                Collections.swap(list, 0, 1);
        } else{
            Collections.rotate(list, this.id == 0 ? 0 : (size / threadsN) * this.id);
        }
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
}