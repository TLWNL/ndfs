package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import graph.Graph;
import graph.GraphFactory;
import graph.State;
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
    private int prev_r_count = 0;
    /****************For Debugging*****************/

    //This bad boi is for shared count
    private static final StateCount stateCount = new StateCount();
    private static final Lock lock = new ReentrantLock();
    private static final Condition isZero = lock.newCondition();
    
    private static final Lock doneLock = new ReentrantLock();
    private static final Condition isDoneCond = doneLock.newCondition();

    private final Graph graph;
    private static final Colors globalColors = new Colors();
    private final Colors colors = new Colors(); //For blue dfs
    private final Colors pink = new Colors(); //For pink in red dfs
    //private static int stateCount = 0;
    private static AtomicBoolean result = new AtomicBoolean(false);

    private static AtomicBoolean isDone = new AtomicBoolean(false);

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

        /*String fileName = "./out" + this.id + ".txt";
        this.fileOut = new PrintStream(fileName);*/
    }

    //Add an extra parameter: int N indicating which direction a thread should take
    //when traversing the state space
    private void dfsRed(State s, int workerId) throws CycleFoundException {
        //fileOut.printf("Thread %d entered dfsRed: [count:%d]\n", this.id, red_count);
        /*
            Pseudo:
            s.pink[i] = true; 
            for(t in post_r_i(s)) do{
            -- With post_r_i we denote the permutation of sucessors used in the blue/red DFS by worker i.
                if(t.color[i] == CYAN)
                    report cycle and exit;
                if(!t.pink[i] && !t.red[i])
                    dfs_red(t, i);
            }
            if(s.isAccepting()){
                s.count--;
                while(!s.count == 0);
            }
            s.red = true;
            s.pink[i] = false;
        */
        pink.color(s, Color.PINK);
        for (State t : graph.post(s)) {
            if (colors.hasColor(t, Color.CYAN)) {
                //System.out.printf("bc=%d  rc=%d when accept cycle is detected\n", blue_count, red_count);
                throw new CycleFoundException();
            } else if (!pink.hasColor(t, Color.PINK) && !globalColors.hasColor(t, Color.RED)) {
                dfsRed(t, workerId);
            }
        }
        if (s.isAccepting()) {
            
            lock.lock();
            try{
                //int preValue = stateCount.currentCount(s);
                stateCount.decrement(s);
                int postValue = stateCount.currentCount(s);
                if(postValue == 0)
                    isZero.signalAll();
                
                //fileOut.printf("Thread %d: iteration [%d] gonna wait for StateCount == 0\n", this.id, red_count);
                while(stateCount.currentCount(s) != 0 && !isDone.get()){
                    //fileOut.printf("Thread %d encountered current StateCount: %d\n", this.id, stateCount.currentCount(s));
                    isZero.await(); 
                }
                //fileOut.printf("Thread %d: iteration [%d] done waiting for StateCount == 0\n", this.id, red_count);
                //System.out.printf("Worker %d DONE waiting, count == 0\n", this.id);
            } catch(InterruptedException e){
                e.printStackTrace();
            } finally{
                lock.unlock();
            }
        }
        globalColors.color(s, Color.RED);
        pink.color(s, Color.WHITE);
    }

    //Add an extra parameter: int i indicating which direction a thread should take
    //when traversing the state space
    private void dfsBlue(State s, int workerId) throws CycleFoundException {
        //fileOut.printf("Thread %d entered dfsBlue: [count:%d]\n", this.id, blue_count);
        /*
            Pseudo:
            s.color[i] = CYAN
            for all t in post_b_i() do:
                if(t.color[i] == white && !red)
                    report cycle and exit;
            if(s.isAccepting()){
                s.count++;
                dfs_red(s, i);
            }
            s.color[i] = BLUE;
        */
        // Sets the color of the local hashmap to cyan
        colors.color(s, Color.CYAN);
        for (State t : graph.post(s)) {
            // Should check the local hashmap for the thread for white and the global for red!
            if (colors.hasColor(t, Color.WHITE) && !globalColors.hasColor(t, Color.RED)) {
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

    //Add an extra parameter: int N indicating which direction a thread should take
    //when traversing the state space
    private void nndfs(State s, int workerId) throws CycleFoundException {
        dfsBlue(s, workerId);
    }

    @Override
    public void run() {
        System.out.printf("Running thread %d now...\n", this.id);
        try {            
            nndfs(graph.getInitialState(), this.id); 
            doneLock.lock();
            try{
                isDone.set(true);
                ////////////////////////System.out.printf("Result in thread %d == %s", this.id, isDone.get());
            } finally{
                doneLock.unlock();
                System.out.printf("Thread %d has terminated successfully\n", this.id);
            }
        } catch (CycleFoundException e) {
            doneLock.lock();
            try{
                result.set(true);
                isDone.set(true);
                //isDoneCond.signalAll();
                ////////////////////////System.out.printf("Result in thread %d == %s", this.id, isDone.get());
                //System.out.printf("This happen?\n");
            } finally{
                doneLock.unlock();
                System.out.printf("Thread %d has terminated successfully\n", this.id);
                //System.out.printf("This happen too?\n");
            }
            

        } catch (Exception e){
            ////////////////////////System.out.println("Unexpected exception was caught");
        }
        /*catch(InterruptedException e){
            //ignore
        }*/
        ////////////////////////System.out.printf("Thread %d has terminated with bc=%d and rc=%d\n", this.id, blue_count, red_count);
    }

    public static boolean getResult() {
        System.out.printf("Done = %s, Result: %s\n", isDone, result);
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
}