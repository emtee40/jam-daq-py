package jam.commands;

import jam.global.CommandListener;
import jam.global.CommandListenerException;
import jam.global.Broadcaster;
import jam.global.CommandNames;
import jam.global.MessageHandler;
import java.util.Observer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import javax.swing.Action;

/**
 * Class to create commands and execute them
 *
 * @author Ken Swartz
 */
public class JamCmdManager implements CommandListener {

	private MessageHandler msghdlr=null;
	private static JamCmdManager _instance=null;
	private static final Map cmdMap = Collections.synchronizedMap(new HashMap());
	private static final Map instances=Collections.synchronizedMap(new HashMap());
	private Commandable currentCommand;
	
	
	/* initializer block for map */
	static {
		cmdMap.put(CommandNames.OPEN_HDF, OpenHDFCmd.class);
		cmdMap.put(CommandNames.SAVE_HDF, SaveHDFCmd.class);
		cmdMap.put(CommandNames.SAVE_AS_HDF, SaveAsHDFCmd.class);
		cmdMap.put(CommandNames.SAVE_GATES, SaveGatesCmd.class);
		cmdMap.put(CommandNames.ADD_HDF, AddHDFCmd.class);
		cmdMap.put(CommandNames.RELOAD_HDF, ReloadHDFCmd.class);
		cmdMap.put(CommandNames.SHOW_NEW_HIST, ShowDialogNewHistogramCmd.class);
		cmdMap.put(CommandNames.EXIT, ShowDialogExitCmd.class);
		cmdMap.put(CommandNames.NEW, FileNewClearCmd.class);
		cmdMap.put(CommandNames.PARAMETERS, ShowDialogParametersCmd.class);
		cmdMap.put(CommandNames.DISPLAY_SCALERS, ShowDialogScalersCmd.class);
		cmdMap.put(CommandNames.SHOW_ZERO_SCALERS, ShowDialogZeroScalersCmd.class);
		cmdMap.put(CommandNames.SCALERS, ScalersCmd.class);
		cmdMap.put(CommandNames.EXPORT_TEXT, ExportTextFileCmd.class);
		cmdMap.put(CommandNames.EXPORT_DAMM, ExportDamm.class);
		cmdMap.put(CommandNames.EXPORT_SPE, ExportRadware.class);	
		cmdMap.put(CommandNames.PRINT, Print.class);
		cmdMap.put(CommandNames.PAGE_SETUP, PageSetupCmd.class);	
	}
	

	/**
	 * Constructor private as singleton
	 *
	 */
	private JamCmdManager() {
	}
	
	/**
	 * Singleton accessor
	 * @return
	 */
	public static JamCmdManager getInstance () {
		if (_instance==null) {
			_instance=new JamCmdManager();
		}		
		return _instance;
	}
	
	public void setMessageHandler(MessageHandler msghdlr) {
		this.msghdlr = msghdlr;
	}
	
	/**
	 * Perform command with object parameters
	 *
	 * @param strCmd	String key indicating the command
	 * @param cmdParams	Command parameters
	 */
	public boolean performCommand(String strCmd, Object[] cmdParams)
		throws CommandException {
		boolean validCommand=false;
		if (createCmd(strCmd)) {
			if (currentCommand.isEnabled()){
				currentCommand.performCommand(cmdParams);
			} else {
				msghdlr.errorOutln("Disabled command \""+strCmd+"\"");
			}				
			validCommand= true;
		}
		return validCommand;
	}

	/**
	 * Perform command with string parameters
	 *
	 * @param strCmd 		String key indicating the command
	 * @param strCmdParams  Command parameters as strings
	 */
	public boolean performParseCommand(String strCmd, String[] strCmdParams) 
		throws CommandListenerException {
		boolean validCommand=false;
		if (createCmd(strCmd)) {
			if (currentCommand.isEnabled()){
				currentCommand.performParseCommand(strCmdParams);
			} else {
				msghdlr.errorOutln("Disabled command \""+strCmd+"\"");
			}
			validCommand=true;
		} 
		return validCommand;
	}
	
	/**
	 * See if we have the instance created, create it if necessary,
	 * and return whether it was successfully created. 
	 * 
	 * @param strCmd name of the command
	 * @return <code>true</code> if successful, <code>false</code> if 
	 * the given command doesn't exist
	 */
	private boolean createCmd(String strCmd)  {
		final boolean exists=cmdMap.containsKey(strCmd);
		if (exists) {
			final Class cmdClass = (Class)cmdMap.get(strCmd);
			currentCommand = null;
			final boolean created=instances.containsKey(strCmd);
			if (created){
				currentCommand=(Commandable) instances.get(strCmd);
			} else {
				try {
					currentCommand = (Commandable) (cmdClass.newInstance());
					currentCommand.init(msghdlr);
					if (currentCommand instanceof Observer){
						Broadcaster.getSingletonInstance().addObserver(
						(Observer)currentCommand);
					}
				} catch (Exception e) {
					/* There was a problem resolving the command class or 
					 * with creating an instance. This should never happen
					 * if exists==true. */
					throw new RuntimeException(e);
				}
				instances.put(strCmd,currentCommand);
			}
		}
		return exists;
	}
	
	public Action getAction(String strCmd){
		return createCmd(strCmd) ? currentCommand : null;
	}
	
	public void setEnabled(String cmd, boolean enable){
		getAction(cmd).setEnabled(enable);
	}
}
