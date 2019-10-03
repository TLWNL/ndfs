package ndfs.mcndfs_1_improved;

import java.util.HashMap;
import java.util.Map;

import graph.State;
import java.util.concurrent.*;

/**
 * This class provides a color map for graph states.
 */
public class Colors {

    private final HashMap<State, StateColor> map = new HashMap<State, StateColor>();

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
        if(color == Color.PINK){
            StateColor sc = map.get(state);
            if(sc == null){
                return false;
            }
            return sc.getPinkBool();

        } else{ //If not checking for PINK, we are checking for WHITE, CYAN, BLUE
            StateColor sc = map.get(state);
            if(sc == null){
                //If there is no entry for this state in the map
                //and the request was for WHITE, means its unexplored (hence true)
                //Otherwise, for any other color, its false
                if(color == Color.WHITE)
                    return true;
                else
                    return false;
            } else{
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
        /*StateColor sc = new StateColor(color, false);
        
        if(color == Color.PINK){
            sc.setPinkBool(true);
            StateColor temp = map.get(state);

            if(temp == null){
                map.put(state, sc);
            } else{
                temp.setPinkBool(true);
            }
        } else {
            StateColor temp = map.get(state);

            if(color == Color.NOTPINK){
                temp.setPinkBool(false);
                //map.replace(state, temp);
                return;
            } else{
                if(temp == null){
                    map.put(state, sc);
                } else {
                    temp.setColor(color);
                    //map.replace(state, temp);
                }
            }
        }*/

        StateColor sc = map.get(state);
        if(sc == null){
            switch(color){
                case PINK:
                    map.put(state, new StateColor(Color.WHITE, true));
                    break;
                case NOTPINK:
                    map.put(state, new StateColor(Color.WHITE, false));
                    break;
                default:
                    map.put(state, new StateColor(color, false));
                    break;
            }
        } else {
            switch(color){
                case PINK:
                    sc.setPinkBool(true);
                    break;
                case NOTPINK:
                    sc.setPinkBool(false);
                    break;
                default:
                    sc.setColor(color);
                    break;
            }
        }
    }
}
