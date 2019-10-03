package ndfs.mcndfs_1_naive;

import graph.State;
import java.util.*;
import java.util.concurrent.atomic.*;

public class GlobalColors{
	private final HashMap<State, AtomicBoolean> map = new HashMap<State, AtomicBoolean>();

	public boolean isRed(State s){
		if(map.containsKey(s))
			return map.get(s).get();
		return false;
	}

	public void setRed(State s){
		if(map.containsKey(s))
			map.get(s).getAndSet(true);
		else
			map.put(s, new AtomicBoolean(true));
	}


}