package jam.global;

/**
 * The command names, used internally and also avaliable from
 * the console
 *   
 * @author Ken Swartz
 *
 */
public interface CommandNames {

	/** Open a hdf file */
	String OPEN_HDF = "open";
	
	/** Reload hdf file */
	String RELOAD_HDF = "reload";

	/** Add hdf file */
	String ADD_HDF = "addfile";

	/** Save to hdf file */
	String SAVE_HDF = "save";
	
	/** Save as to a hdf */ 
	String SAVE_AS_HDF = "saveas";	
	
	/** Open selected histograms in hdf */ 
	String OPEN_SELECTED = "opensel";	

	/** Show dialog for defining a new histogram. */
	String SHOW_NEW_HIST="shownewhist";
	
	/** Exit Jam. */
	String EXIT = "exit";
	
	/** Erase the data in memory */
	String NEW = "new";
	
	/** Show the parameters dialog. */
	String PARAMETERS="parameters";
	
	/** Show the scaler values. */
	String DISPLAY_SCALERS="displayscalers";
	
	/** Show the dialog for zeroing scalers. */
	String SHOW_ZERO_SCALERS="showzeroscalers";
	
	/** Read or zero scalers. */
	String SCALERS="scalers";
	
	/** Export the displayed histogram as a text file. */
	String EXPORT_TEXT="exporttext";		
	
}