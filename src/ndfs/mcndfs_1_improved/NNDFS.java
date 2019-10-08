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
    private final int length;

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */

    public NNDFS(File promelaFile, int nWorkers) throws FileNotFoundException{
        this.length = nWorkers;
        this.threads = new Thread[nWorkers];

        for(int i = 0; i < nWorkers; i++){
            this.threads[i] = new Thread(new Worker(promelaFile, i));
        }
    }

    @Override
    public boolean ndfs(){
        //Start the threads
        for(Thread w : this.threads){
            w.start();
        }

        //Wait for all threads to finish executing
        for(Thread w: this.threads){
            try{
                w.join();
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        return Worker.getResult();
    }
}