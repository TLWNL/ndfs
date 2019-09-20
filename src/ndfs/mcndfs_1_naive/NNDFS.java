package ndfs.mcndfs_1_naive;

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
        //Need both Worker and Thread, cause Thread wont have the getResult(), and we need that badboi
        //to grab that cyclefound bool
        this.length = nWorkers;
        this.threads = new Thread[nWorkers];
        this.workers = new Worker[nWorkers];

        //Create the workers and threads (give the workers an id, maybe we can use that later for how 
        // to traverse this shiet, but not sure yet if thats really necessary)
        for(int i = 0; i < nWorkers; i++){
            this.workers[i] = new Worker(promelaFile, i);

            this.threads[i] = new Thread(this.workers[i]);
            System.out.printf("Worker %d stored\n", i);
        }

    }

    @Override
    public boolean ndfs() {
        //Start them threads
        for(Thread w : this.threads){
            w.start();
        }

        //Grab the results from workers, this now immediately executing, so way before the
        //workers are actually done :/, so its just gonna return false until we make it wait
        return this.workers[0].getResult();
    }
}