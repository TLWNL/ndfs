package ndfs.mcndfs_1_improved;

import java.io.File;
import java.io.FileNotFoundException;

import ndfs.NDFS;

/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final Thread[] threads;
    private final Worker[] workers;
    private final int length;

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */


    //Added nWorkers
    public NNDFS(File promelaFile, int nWorkers) throws FileNotFoundException {
        this.length = nWorkers;
        this.threads = new Thread[nWorkers];
        this.workers = new Worker[nWorkers];

        //Not sure how we can do this so storing the workers isnt necessary
        for(int i = 0; i < nWorkers; i++){
            this.workers[i] = new Worker(promelaFile, i);

            this.threads[i] = new Thread(this.workers[i]);
        }

    }

    @Override
    public boolean ndfs() {
        //Start the threads
        for(Thread w : this.threads){
            w.start();
        }

        //System.out.printf("Currently waiting for workers to finish\n");
        Worker.youDoneYet();
        //Worker.youDoneYet();

        for(Thread w: this.threads){
            if(!w.isInterrupted()){
                w.interrupt();
            }
        }

        /**/
        for(Thread w : this.threads){
            try{
                w.join();
            } catch(InterruptedException e){
                e.printStackTrace();
                System.exit(-1);
            }
        }

        //System.out.printf("Retrieving result\n");
        return Worker.getResult();
    }
}