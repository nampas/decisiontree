/**
 * Utility class for printing to screen
 * @author Nathan P
 *
 */
class Log {
	
	private static final boolean DEBUG = true;

	/**
	 * Prints info messages to console
	 * @param tag Tag, usually class name
	 * @param msg Message
	 */
	public static void i(String tag, String msg) {
		System.out.println(tag + " : " + msg);
	}
	
	/**
	 * Prints error messages to console
	 * @param tag Tag, usually class name
	 * @param msg Message
	 */
	public static void e(String tag, String msg) {
		System.err.println(tag + " : " + msg);
	}
	
	/**
	 * Prints debug messages to console
	 * @param tag Tag, usually class name
	 * @param msg Message
	 */
	public static void d(String tag, String msg) {
		if(DEBUG)
			i(tag, msg);
	}
}
