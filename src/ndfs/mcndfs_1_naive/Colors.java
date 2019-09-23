package ndfs.mcndfs_1_naive;

import java.util.HashMap;
import java.util.Map;

import graph.State;

/**
 * This class provides a color map for graph states.
 */
public class Colors {

    private final Map<State, StateColor> map = new HashMap<State, StateColor>();

    /**
     * Returns <code>true</code> if the specified state has the specified color,
     * <code>false</code> otherwise.
     *
     * @param state
     *            the state to examine.
     * @param color
     *            the color
     * @return whether the specified state has the specified color.
     */
    public boolean hasColor(State state, Color color) {

        // Coloring the state PINK means that StateColor.pink (boolean) will be queried
        // This is quite a mess right now, no clear structure in how this works
        // If we do end up making a separate class for storing
        if(color == Color.PINK){
            StateColor sc = map.get(state);
            if(sc == null){
                return false;
            }
            return sc.getPinkBool();

        } else{
            if (color == Color.WHITE || color == Color.RED) {
                StateColor sc = map.get(state);
                if(sc == null && color == Color.WHITE){
                    return true;
                } else if (sc == null){
                    return false;
                }
                return sc.getColor() == color;
            } else {
                StateColor sc = map.get(state);
                if(sc == null){
                    return false;
                }
                return sc.getColor() == color;
            }
        }
    }

    /**
     * Gives the specified state the specified color.
     *
     * @param state
     *            the state to color.
     * @param color
     *            color to give to the state.
     */
    public void color(State state, Color color) {
        StateColor sc = new StateColor(color, false);

        if(color == Color.PINK){
            sc.setPinkBool(true);
            StateColor temp = map.get(state);

            if(temp == null){
                map.put(state, sc);
            } else{
                temp.setPinkBool(true);
                map.replace(state, temp);
            }
        } else {
            StateColor temp = map.get(state);

            if(color == Color.NOTPINK){
                temp.setPinkBool(false);
                map.replace(state, temp);
                return;
            } else{
                if(temp == null){
                    map.put(state, sc);
                } else {
                    temp.setColor(color);
                    map.replace(state, temp);
                }
            }
            
            
        }
    }
}
