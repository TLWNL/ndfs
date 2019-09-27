package ndfs.mcndfs_1_naive;

/**
 * This class is for the local colors of the states
 * essentially the separation is done by storing colors WHITE, CYAN, BLUE && whether the state is pink
 * in separate variables, due to the locality of the combination of these (and due to it being inherently sequential)
 * these two will never interfere
 *
 * Currently also allows for RED to be stored, but this is done independently from where the combination mentioned above is stored,
 * we will create an extra class specifically for storing RED, mostly to
 * 1. reduce the amount of memory used (now its this complete object just for storing RED)
 * 2. improving readability of code
 */
public class StateColor {	
	private Color color = Color.WHITE;
	private boolean pink = false;

	/**
	 * Constructor that creates StateColor object with specified color and boolean value for pink
	 *
	 * @param c Color of this StateColor object
	 * @param b boolean indicating whether that
	 *
	 */
	public StateColor(Color c, boolean b){
		this.color = c;
		this.pink = b;
	}

	/**
	 * Function that returns the color of of this object
	 *
	 * @return Color enum
	 */ 
	public Color getColor(){
		return this.color;
	}

	/**
	 * Set the color of this object to a different color, probly not necessary
	 *
	 * @param c Color of this StateColor object
	 *
	 */
	public void setColor(Color c){
		this.color = c;
	}

	public boolean getPinkBool(){
		return this.pink;
	}

	public void setPinkBool(boolean b){
		this.pink = b;
	}
}
