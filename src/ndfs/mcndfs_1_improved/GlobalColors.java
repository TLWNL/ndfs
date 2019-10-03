package ndfs.mcndfs_1_improved;

import graph.State;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread safe map that logs whether a state is red or not
 * As stated on manual page, allows for several threads to write at once (if hashes of the States are not
 * in the same region of the hashmap), otherwise the region is locked. Allows for any number of concurrent
 * readers.  
 *
 *
 *
 *
 */

public class GlobalColors{
	private final ConcurrentHashMap<State, AtomicBoolean> map = new ConcurrentHashMap<State, AtomicBoolean>();

	public boolean isRed(State s) {
		if(map.containsKey(s)){
			return map.get(s).get();
		}
		return false;
    }


    public void setRed(State s){
    	if(map.containsKey(s)){
    		map.get(s).getAndSet(true);
    	} else{
    		map.put(s, new AtomicBoolean(true));
    	}
    }
}