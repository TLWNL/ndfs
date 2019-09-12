package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;

import ndfs.NDFS;

/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final Worker worker;

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public NNDFS(File promelaFile, int nWorkers) throws FileNotFoundException {

        this.worker = new Worker(promelaFile);

        for(int i = 0; i < nWorkers; i++){
            //make some goddamn workers, just a bunch of em
        }
    }

    @Override
    public boolean ndfs() {
        worker.run();
        return worker.getResult();
    }
}
