package ndfs.mcndfs_1_improved;

import java.util.*;
import graph.State;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

//A lock has to be used when interacting with the StateCount
public class StateCount{
	private final ConcurrentHashMap<State, AtomicInteger> map;
    

	public StateCount(){
		map = new ConcurrentHashMap<State, AtomicInteger>();
	}

	/**
     * Increments the count of a state 1
     * where count indicates the number of threads that invoked dfsRed on that state and have not yet returned 
     *
     * @param state
     *            the state to increment the count of
     *
     */
	public void increment(State s){
		if(map.containsKey(s)){
			//AtomicInteger newStateCount = new AtomicInteger(map.get(s).get() + 1);
			//map.replace(s, newStateCount);
			map.get(s).getAndIncrement();
		} else{
			//AtomicInteger stateCount = new AtomicInteger(1);
			map.put(s, new AtomicInteger(1));
		}
	}

	/**
     * Decrements the count of a state by 1
     * where count indicates the number of threads that invoked dfsRed on that state and have not yet returned 
     *
     * @param state
     *            the state to increment the count of
     *
     */
	public void decrement(State s){
		if(map.containsKey(s)){
			/*AtomicInteger newStateCount = new AtomicInteger(map.get(s).get() - 1);
			if(newStateCount.get() == 0){
				map.remove(s);
			} else {
				//map.replace(s, newStateCount);
				map.get(s).getAndDecrement();
			}*/
			if(map.get(s).decrementAndGet() == 0){
				map.remove(s);
			}
		} else{ //This should never be able to occur, if it does, we are doing something wrong in Worker
			System.out.printf("Something went terribly wrong\n");
			System.exit(-1);
		}
	}

	/**
     * Returns the count of the specified state, if the state is not stored in the hashmap
     * it means that the count is essentially 0, no threads are currently in dfsRed on that state
     *
     * @param state
     *            the state to increment the count of
     *
     * @return the count of the specified state
     */
	public int currentCount(State s){
		/*AtomicInteger count = map.get(s);
		if(count != null){
			//System.out.println(map.values());
			return count.get();
		}*/
		if(map.containsKey(s)){
			try{
				AtomicInteger ret;
				if((ret = map.get(s)) == null){
					return -1;
				}
				return ret.get();
			} catch(Exception e){
				System.out.printf("Error occurred while getting map value\n");
				e.printStackTrace();
			}
		}
		return 0;
		/*AtomicInteger count = map.get(s);
		if(count != null)
			return count.get();
		return 0;*/
	}	
}