/*
 */
package jam.global; 

/**
 * Listens to typed commands from the console.
 *
 * @author Ken Swartz
 */
 public interface CommandListener {	
 		 
	/**
	 * Class must implement this command to receive commands.
	 * 
	 * @param command word indecating action to take
	 * @param parameters list of typed numbers which are parameters to the command
	 */
	public void commandPerform(String command, double [] parameters);	
}