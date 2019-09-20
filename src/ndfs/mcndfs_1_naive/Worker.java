package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import graph.Graph;
import graph.GraphFactory;
import graph.State;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Runnable{
    //This bad boi is for shared count
    private static StateCount stateCount = new StateCount();
    private final ReentrantLock lock = new ReentrantLock(true); 

    private final Graph graph;
    private static final Colors globalColors = new Colors();
    private final Colors colors = new Colors();
    //private static int stateCount = 0;
    private static boolean result = false;

    private static boolean isDone = false;

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
        colors.color(s, Color.PINK);
        for (State t : graph.post(s)) {
            if (colors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();
            } else if (!colors.hasColor(t, Color.PINK) && !globalColors.hasColor(t, Color.RED)) {
                dfsRed(t, workerId);
            }
        }
        if (s.isAccepting()) {
            lock.lock();
            try{
                stateCount.decrement(s);
            } finally{
                lock.unlock();
            }

            //condition.await(), need to fix this bitch over here, this most likely where it gets stuck
            while(stateCount.currentCount(s) != 0);
        }
        globalColors.color(s, Color.RED);
        colors.color(s, Color.WHITE);
    }

    //Add an extra parameter: int i indicating which direction a thread should take
    //when traversing the state space
    private void dfsBlue(State s, int workerId) throws CycleFoundException {
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
            // <- that aint precisely the same tho, they always just check for  == white, i think ours should just do the same
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
            isDone = true;
        } catch (CycleFoundException e) {
            result = true;
            isDone = true;
        }
        /*catch(InterruptedException e){
            //ignore
        }*/
    }

    public static boolean getResult() {
        System.out.printf("Now waiting for result..\n");
        while(!isDone || result);
        System.out.printf("Result: %s\n", result);
        return result;
    }
}