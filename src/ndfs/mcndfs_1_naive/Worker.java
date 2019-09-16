package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import graph.Graph;
import graph.GraphFactory;
import graph.State;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Runnable{

    private final Graph graph;
    private final Colors colors = new Colors();
    private boolean result = false;
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
    private void dfsRed(State s) throws CycleFoundException {
        /*
            Pseudo:
            s.pink[i] = true;
            for(t in post_r_i(s)) do{
                if(t.color[i] == CYAN)
                    report cycle and exit;
                if(!t.pink[i] && !t.red[i])
                    dfs_red(t, i);
            }
            if(s.isAccepting()){
                s.count--;
                while(!s.count == 0);
            }
            s.red[i] = true;
            s.pink[i] = false;
        */
        for (State t : graph.post(s)) {
            if (colors.hasColor(t, Color.CYAN)) {
                throw new CycleFoundException();
            } else if (colors.hasColor(t, Color.BLUE)) {
                colors.color(t, Color.RED);
                dfsRed(t); 
            }
        }
    }

    //Add an extra parameter: int i indicating which direction a thread should take
    //when traversing the state space
    private void dfsBlue(State s) throws CycleFoundException {
        /*
            Pseudo:
            s.color[i] = CYAN
            for all t in post_b_i() do:
                if(t.color[i] == white)
                    report cycle and exit;
            if(s.isAccepting()){
                s.count++;
                dfs_red(s, i);
            }
            s.color[i] = BLUE;
        */
        colors.color(s, Color.CYAN);
        for (State t : graph.post(s)) {
            if (colors.hasColor(t, Color.WHITE)) {
                dfsBlue(t);
            }
        }
        if (s.isAccepting()) {
            dfsRed(s);
            colors.color(s, Color.RED);
        } else {
            colors.color(s, Color.BLUE);
        }
    }

    //Add an extra parameter: int N indicating which direction a thread should take
    //when traversing the state space
    private void nndfs(State s) throws CycleFoundException {
        //This is the original caller of the algorithm
        /*Pseudo:
            dfs_blue(s, 1) || ... || dfs_blue(s, N)
            this is what we have to alter this function to
        */
        dfsBlue(s);
    }

    @Override
    public void run() {
        try {
            //This just to see that they both executing, can unbox the sleep down there if ya wanna see fo sho
            //then you also gotta uncomment the extra catch
            System.out.printf("Current thread: %d\n", this.id);
            
            /*Thread.sleep(10000);*/
            
            nndfs(graph.getInitialState()); //Add parameter here: int N to indicate traversal of current thread
        } catch (CycleFoundException e) {
            result = true;
        }
        /*catch(InterruptedException e){
            //ignore
        }*/
    }

    public boolean getResult() {
        return result;
    }
}