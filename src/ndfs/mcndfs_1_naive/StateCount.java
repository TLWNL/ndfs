package ndfs.mcndfs_1_naive;

import java.util.*;
import graph.State;

//A lock has to be used when interacting with the StateCount
public class StateCount{
	private static final Map<State, Integer> map =new HashMap<State, Integer>();
    

	public StateCount(){
	}

	//Increments the count variable indicating threads at State s
	public void increment(State s){
		if(map.containsKey(s)){
			Integer newStateCount = new Integer(map.get(s).intValue() + 1);
			map.replace(s, newStateCount);
		} else{
			Integer stateCount = new Integer(1);
			map.put(s, stateCount);
		}
	}

	//Decrements the count variable indicating threads at State s
	public void decrement(State s){
		if(map.containsKey(s)){
			Integer newStateCount = new Integer(map.get(s).intValue() + 1);
			map.replace(s, newStateCount);
		} else{ //This should never be able to occur, if it does, we are doing something wrong in Worker
			System.err.printf("Something went terribly wrong\n");
			System.exit(-1);
		}
	}

	//Returns the current count indicating number of threads at State s
	public int currentCount(State s){
		if(map.containsKey(s))
			return map.get(s).intValue();
		return 0;
	}	
}