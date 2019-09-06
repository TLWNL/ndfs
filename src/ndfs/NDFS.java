package ndfs;

/**
 * This interface specifies the way this framework calls an NDFS implementation
 * and the way the result is passed.
 */
public interface NDFS {

    /**
     * This method determines whether or not the graph has an accepting cycle,
     * and returns a boolean indicating the result.
     *
     * @return whether the graph contains an accepting cycle.
     */
    public boolean ndfs();

    /**
     *
     * PSEUDOCODE ALGORITHM 1
     *
     *  proc nndfs(sI)
     *      dfs_blue(sI)
     *      report no cycle
     *
     *  proc dfs_red(s)
     *      for all t in post(s) do
     *          if t.color = cyan
     *              report cycle & exit
     *          else if t.color = blue
     *              t.color = red
     *              dfs_red(t)
     *
     *  proc dfs_blue(s)
     *      s.color := cyan
     *      for all t in post(s) do
     *          dfs_blue(t)
     *      if s in A
     *          dfs_red(s)
     *          s.color := red
     *      else
     *          s.color = blue
     */
}
