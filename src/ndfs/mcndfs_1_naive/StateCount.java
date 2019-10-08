package ndfs.mcndfs_1_naive;

import java.util.*;
import graph.State;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

//A lock has to be used when interacting with the StateCount
public class StateCount{
	private final Map<State, AtomicInteger> map;
    
	public StateCount(){
		map = new HashMap<State, AtomicInteger>();
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
			map.get(s).getAndIncrement();
		} else {
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
		AtomicInteger count = map.get(s);
		if(count != null){
			return count.get();
		}
		return 0;
	}	
}