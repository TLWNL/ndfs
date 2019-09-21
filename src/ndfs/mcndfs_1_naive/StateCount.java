package ndfs.mcndfs_1_naive;

import java.util.*;
import graph.State;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

//A lock has to be used when interacting with the StateCount
public class StateCount{
	private static final ConcurrentMap<State, AtomicInteger> map = new ConcurrentHashMap<State, AtomicInteger>();
    

	public StateCount(){
	}

	//Increments the count variable indicating threads at State s
	public void increment(State s){
		if(map.containsKey(s)){
			AtomicInteger newStateCount = new AtomicInteger(map.get(s).get() + 1);
			////////////////////////System.out.printf("Before incr: ");
			////////////////////////System.out.println(map.values());
			map.replace(s, newStateCount);
			////////////////////////System.out.printf("After incr: ");
			////////////////////////System.out.println(map.values());
		} else{
			AtomicInteger stateCount = new AtomicInteger(1);
			map.put(s, stateCount);
			////////////////////////System.out.printf("After add: ");
			////////////////////////System.out.println(map.values());
		}
	}

	//Decrements the count variable indicating threads at State s
	public void decrement(State s){
		if(map.containsKey(s)){
			AtomicInteger newStateCount = new AtomicInteger(map.get(s).get() - 1);
			////////////////////////System.out.printf("Before decr: ");
			////////////////////////System.out.println(map.values());
			map.replace(s, newStateCount);
			////////////////////////System.out.printf("After decr: ");
			////////////////////////System.out.println(map.values());
		} else{ //This should never be able to occur, if it does, we are doing something wrong in Worker
			System.out.printf("Something went terribly wrong\n");
			System.exit(-1);
		}
	}

	//Returns the current count indicating number of threads at State s
	public int currentCount(State s){
		if(map.containsKey(s)){
			//System.out.println(map.values());
			return map.get(s).get();
		}
		return -1;
	}	
}