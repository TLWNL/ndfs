package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

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
    private int blue_count = 0;
    private int red_count = 0;


    //This bad boi is for shared count
    private static final StateCount stateCount = new StateCount();
    private static final Lock lock = new ReentrantLock();
    private static final Lock doneLock = new ReentrantLock();
    private static final Condition isZero = lock.newCondition();
    private static final Condition isDoneCond = doneLock.newCondition();

    private final Graph graph;
    private static final Colors globalColors = new Colors();
    private final Colors colors = new Colors();
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
    }

    //Add an extra parameter: int N indicating which direction a thread should take
    //when traversing the state space
    private void dfsRed(State s, int workerId) throws CycleFoundException {
        red_count++;
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
        //////////////////colors.color(s, Color.PINK);
        for (State t : graph.post(s)) {
            if (colors.hasColor(t, Color.CYAN)) {
//                System.out.printf("bc=%d  rc=%d when accept cycle is detected\n", blue_count, red_count);
                throw new CycleFoundException();
            } else if (!colors.hasColor(t, Color.PINK) && !globalColors.hasColor(t, Color.RED)) {
                dfsRed(t, workerId);
            }
        }
        if (s.isAccepting()) {
//            System.out.println(this.id + " LOCKING/n");
            lock.lock();
            try{
                stateCount.decrement(s);
                
                /*if(stateCount.currentCount(s) == 0)
                    isZero.signalAll();*/
//                System.out.printf("Worker %d now waiting for count == 0\n", this.id);
                //isZero.signalAll();
                while(stateCount.currentCount(s) != 0){
//                    System.out.println(this.id + " AWAITING/n");
                    isZero.await();     // !!!!!
                    System.out.printf(" [Still waitin: %d] ", this.id);
                }
                //System.out.printf("Worker %d DONE waiting, count == 0\n", this.id);
            } catch(InterruptedException e){
                e.printStackTrace();
            } finally{
//                System.out.println(this.id + " UNLOCKING");
                lock.unlock();
            }

            //condition.await(), need to fix this bitch over here, this most likely where it gets stuck
        }
        globalColors.color(s, Color.RED);
        colors.color(s, Color.WHITE);
    }

    //Add an extra parameter: int i indicating which direction a thread should take
    //when traversing the state space
    private void dfsBlue(State s, int workerId) throws CycleFoundException {
        blue_count++;
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
//        System.out.printf("%d: is accepting = %s\n", blue_count, s.isAccepting());
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
        //This is the original caller of the algorithm
        /*Pseudo:
            dfs_blue(s, 1) || ... || dfs_blue(s, N)
            this is what we have to alter this function to
        */
        // add integer for dfs_blue(s,N);
        dfsBlue(s, workerId);
    }

    @Override
    public void run() {
        try {
            //This just to see that they both executing, can unbox the sleep down there if ya wanna see fo sho
            //then you also gotta uncomment the extra catch
            //System.out.printf("Current thread: %d\n", this.id);
            
            /*Thread.sleep(10000);*/
            
            nndfs(graph.getInitialState(), this.id); //Add parameter here: int N to indicate traversal of current thread
            //System.out.printf("It does finish the program tho\n");
            ////////////////////////System.out.printf("Thread %d setting isDone to true now\n", this.id);
            doneLock.lock();
            try{
                isDone.set(true);
                ////////////////////////System.out.printf("Result in thread %d == %s", this.id, isDone.get());
            } finally{
                doneLock.unlock();
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
        
        /*doneLock.lock();
        try{
            System.out.printf("Now waiting for result..\n");
            while(!isDone.get() || result.get()){
                //System.out.printf("%s=", isDone.get());
                isDoneCond.await();
            }
        } catch(InterruptedException ie){
            ie.printStackTrace();
        } finally{
            doneLock.unlock();
        }

        System.out.printf("Done = %s, Result: %s\n", isDone, result);*/
        boolean resultFinal;
        doneLock.lock();
        try{
            resultFinal = result.get();
        } finally{
            doneLock.unlock();
        }
        ////////////////////////System.out.printf("%s is getting returned as result\n", resultFinal);
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