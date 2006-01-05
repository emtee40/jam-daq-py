package jam.global;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * This is an class that is used to get and set the properties for Jam.
 * As the properties are loaded before a way to print out messages has
 * been constructed we save the messages so they can be later retrieved.
 * 
 * Diretory for loading configuration is give by define jam.home.
 * jam.home should be defined by -D parameter in command line.
 * 
 * For config we try jam.home which if null we try DEFAULT_USER_HOME
 * For user we try user.home defined in JamConfig.ini 
 * then jam.home defined on the command line then DEFAULT_USER_HOME then 
 * Properties
 * <ul>
 * <li>jam.home
 * <li>host.ip
 * <li>host.IP=iotwo
 * <li>host.portSend=5003
 * <li>host.portRecv=5002
 * <li>target.IP=calvin
 * <li>target.port=5002
 * <li>host-data.IP=iotwo-data
 * <li>host-data.portRecv=10205
 * <li>sort.path=/home/jam/sortfiles
 * <li>sort.stream=jam.sort.L002InputStream
 * <li>cnaf.path=/JAM/CAMAC/CNAFS
 * <li>browser.path=netscape
 * <li>docs.path=documentation
 * <li>jam.home=/home/jam
 * <li>log.path assumed to be jam.home if not given
 * </ul>
 *
 * @author Ken Swartz
 * @author Dale Visser
 * @version 1.4
 * @since 0.5 17 January 1999
 */
public class JamProperties {

	/** 
	 * Machine configuration file name 
	 */
	static final String FILE_CONFIG = "JamConfig.ini";
	/** 
	 * User configuration file name 
	 */
	static final String FILE_USER = "JamUser.ini";

	/**
	 * Path to search for <code>JamConfig.ini</code>
	 */
	public final static String JAM_HOME = "jam.home";
	
	/**
	 * IP address of the socket used to communicate with the
	 * front end.
	 * 
	 * @see jam.FrontEndCommunication
	 */
	public final static String HOST_IP = "host.IP";
	
	/**
	 * Port number for sending messages to the front end.
	 */
	public final static String HOST_PORT_SEND = "host.portSend";
	
	/**
	 * Port number for recieving messages from the front end.
	 */
	public final static String HOST_PORT_RECV = "host.portRecv";
	
	/**
	 * Front end's IP address for communicating with Jam.
	 */
	public final static String TARGET_IP = "target.IP";
	
	/**
	 * Front end's port number for communciating with Jam.
	 */
	public final static String TARGET_PORT = "target.port";
	
	/**
	 * IP address of the socket used to receive data from
	 * the front end.
	 */
	public final static String HOST_DATA_IP = "host-data.IP";
	
	/**
	 * Port number of the socket used to receive data from 
	 * the front end.
	 */
	public final static String HOST_DATA_PORT_RECV = "host-data.portRecv";
	
	/**
	 * Default experiment name to use when naming data files.
	 */
	public final static String EXP_NAME = "exp.name";
	
	/**
	 * Fully qualified name of the default sort routine.
	 * 
	 * @see jam.sort.SortRoutine
	 */
	public final static String SORT_ROUTINE = "sort.routine";
	
	/**
	 * Path to search for and load sort routines.
	 */
	public final static String SORT_CLASSPATH = "sort.classpath";
	
	/**
	 * Set <code>true</code> to default to the classpath used to launch Jam
	 * instead of a given sort classpath.
	 */
	public final static String DEFAULT_SORT_CLASSPATH = "default";
	
	/**
	 * Fully qualified name of the default <code>EventInputStream</code>.
	 * 
	 * @see jam.sort.stream.AbstractEventInputStream
	 */	
	public final static String EVENT_INSTREAM = "event.instream";
	
	/**
	 * Fully qualified name of the default <code>EventOutputStream</code>.
	 * 
	 * @see jam.sort.stream.AbstractEventOutputStream
	 */
	public final static String EVENT_OUTSTREAM = "event.outstream";
	
	/**
	 * Default path to look in for event files to sort offline.
	 */
	public final static String EVENT_INPATH = "event.inpath";
	
	/**
	 * Default path to use for writing event files.
	 */
	public final static String EVENT_OUTPATH = "event.outpath";
	
	/**
	 * Filename to use when writing a pre-sorted event file.
	 */
	public final static String EVENT_OUTFILE = "event.outfile";
	
	/**
	 * Default path for saving HDF files.
	 * 
	 * @see jam.io.hdf.HDFIO
	 */
	public final static String HIST_PATH = "hist.path";
	
	/**
	 * Default path to a tape device. This will probably never
	 * be used.
	 * 
	 * @deprecated
	 */
	public final static String TAPE_DEV = "tape.dev";
	
	/**
	 * Default path to the folder for writing out the console
	 * log.
	 */
	public final static String LOG_PATH = "log.path";
	
	/**
	 * <code>true</code> if the front end should be verbose with it's messages.
	 * The exact usage of this is up to the front end implementer.
	 */
	//public final static String FRONTEND_VERBOSE = "frontend.verbose";
	
	/**
	 * <code>true</code> if the front end should output debugging messages.
	 */
	//public final static String FRONTEND_DEBUG = "frontend.debug";
	
	private final static String NO_ERRORS="No error messages.";
	private final static String NO_WARNINGS="No warning messages.";
		
	private String fileSep = System.getProperty("file.separator");
	private String userHomeDir = System.getProperty("user.home");
	final String userCurrentDir = System.getProperty("user.dir"); 
	private static String os=null;
	private static boolean macosx=false;

	/**
	 * Jam properties
	 */
	private static Properties jamProperties;

	/** file jam config properties read from */
	private File configFile;

	/** file user properties read from */
	private File userFile;
	/** */
	private boolean jamHomeDefined;
	/** message for loading config */
	private String configLoadMessage;
	/** message for loading user config*/
	private String userLoadMessage;
	/** warning when loading config */
	private String configLoadWarning;
	/** warning when loading user */
	private String userLoadWarning;

	private String loadError;

	/** */
	private static MessageHandler msgHandler;
	
	/**
	 * Constructor
	 */
	public JamProperties() {
		jamProperties = new Properties();
		loadProperties();
	}
	
	/**
	 * Returns whether we are running on MacOS X.
	 * 
	 * @return <code>true</code> if the operating system is MacOS X
	 */
	public static boolean isMacOSX(){
		if (os==null){
			os=System.getProperty("os.name");
			macosx=os.equals("Mac OS X");			
		}
		return macosx;
	}
	
	/**
	 * Set where to print messages.
	 * 
	 * @param msgHandler the console
	 */
	public void setMessageHandler(MessageHandler msgHandler) {
		JamProperties.msgHandler = msgHandler;
	}
	
	/**
	 * @return the properties for this Jam process
	 */
	public static Properties getProperties() {
		return jamProperties;
	}

	/**
	 * @param key which property to retrieve
	 * @return the value for the given property
	 */
	public static String getPropString(String key) {
		String rval = "undefined";//default return value
		if (jamProperties.getProperty(key) != null) {
			rval = jamProperties.getProperty(key).trim();
		} else {
			msgHandler.warningOutln(
				"Property " + key + " not defined [JamProperties]");
		}
		return rval;
	}

	/**
	 * @param key which property to retrieve
	 * @return the integer value of the property
	 */
	public static int getPropInt(String key) {
		int rval=0;//default return value
		try {
			if (jamProperties.getProperty(key) != null) {
				rval = Integer.parseInt(jamProperties.getProperty(key).trim());
			} else {
				msgHandler.warningOutln(
					"Property " + key + " not defined [JamProperties]");
			}
		} catch (NumberFormatException nfe) {
			msgHandler.errorOutln(
				"Property " + key + " not an integer [JamProperties]");
		}
		return rval;
	}

	/**
	 * Load the local specific properties from files, using defaults if 
	 * necessary.
	 */
	private final void loadProperties() {
		
		if (System.getProperty(JAM_HOME) != null)
		{
			jamHomeDefined=true;
		}
		else
		{
			jamHomeDefined=false;
		}
		
		//load default configuration
		loadDefaultConfig();
		//load default user
		loadDefaultUser();
		//load config
		loadConfig();
		//load user
		loadUser();
			
	}

	/**
	 * Read in jam config properties 
	 * for normal use jam.home should be defined by -D parameter in command line
	 * 
	 */
	
	private void loadConfig() {
		
		configLoadWarning = NO_WARNINGS;
		loadError = NO_ERRORS;
		String fileName = "";
		FileInputStream fis;
		boolean fileRead=false;
		
		try {
			
			//if jam.home is property given we must use it. 
			if (jamHomeDefined) {
				final File file = new File(
					System.getProperty(JAM_HOME),FILE_CONFIG);
				fileName = file.getPath();
				if (file.exists()) {
					fis = new FileInputStream(file);
					jamProperties.load(fis);
					configLoadMessage =
						"Read configuration properties from file: "
							+ file.getPath();
					 fileRead=true;
					//could not find default		    		
				} else {
					configLoadWarning =
						"Cannot find machine configuration file: JamConfig.ini, in directory defined by jam.home = "
							+ System.getProperty("jam.home")
							+ " ";
					configLoadMessage = "Using default configuration";
				}
			} 
//			try userCurrentDir no jam.home defined			
			if (!fileRead){
				configFile =
					new File(userCurrentDir,FILE_CONFIG);
				fileName = configFile.getPath();
				if (configFile.exists()) {
					fis = new FileInputStream(configFile);
					jamProperties.load(fis);
					configLoadMessage =
						"Read configuration properties from file: "
							+ configFile.getPath();
					fileRead=true;
				} else {
					configLoadWarning =
						"Cannot find machine configuration file "
							+ FILE_CONFIG+", in current directory "
							+ userCurrentDir;
					configLoadMessage = "Using default configuration";
				}
			}
		} catch (FileNotFoundException fnfe) {
			loadError =
				"Jam configuration file, "+fileName+",  not found.";
			showErrorMessage(fnfe, loadError);
		} catch (IOException ioe) {
			loadError =
				"Could not read configuration file, "
					+ fileName+".";
			showErrorMessage(ioe, loadError);
		}
			
	}
	/**
	 * read in user jam properties
	 * try user.home from JamConfig.ini first 	
	 *
	 */
	private void loadUser() {
		
		userLoadWarning = NO_WARNINGS;
		loadError = NO_ERRORS;
		String fileName = "";
		FileInputStream fis;
		boolean fileRead=false;
						
		try {
			//try  userHomeDir			
			userFile = new File(userHomeDir,FILE_USER);
	        fileName = userFile.getPath();			
			if (userFile.exists()) {
				fis = new FileInputStream(userFile);
				jamProperties.load(fis);
				userLoadMessage =
					"Read user configuration file: " + 
					userFile.getPath();
				fileRead=true;
						    
			}  
			
			//try  userCurrentDir
			if (!fileRead) {
				userFile = new File(userCurrentDir, FILE_USER);
				fileName = userFile.getPath(); 
				if (userFile.exists()) {
					fis = new FileInputStream(userFile);
					jamProperties.load(fis);
					userLoadWarning =
						"Cannot find user configuration file "
							+ FILE_USER
							+ " in user home directory "+userHomeDir;
					userLoadMessage =
						"Read user configuration from file " + userFile.getPath();
					fileRead=true;
					//use default properties			   
				} else {
					userLoadWarning =
						"Cannot find user configuration file "+ FILE_USER+
							  ", in user home directory "+userHomeDir+
							  " or in current directory "+userCurrentDir;
						userLoadMessage = "Using default user properties";
				}
			}
            // try variable jam.home directory
			if (!fileRead && jamHomeDefined) {
					userFile =new File(System.getProperty(JAM_HOME),FILE_USER);
					fileName = userFile.getPath();
					if (userFile.exists()) {
						fis = new FileInputStream(userFile);
						jamProperties.load(fis);
						userLoadMessage =
							"Read user configuration file: " + userFile.getPath();
						fileRead=true;
					}
			}
		} catch (FileNotFoundException fnfe) {
			loadError =
				"Jam user configuration file, "+fileName+",  not found.";
			showErrorMessage(fnfe, loadError);
		} catch (IOException ioe) {
			loadError =
				"Could not read user configuration file, "
					+ fileName+".";
			showErrorMessage(ioe, loadError);
		}
	}
	
	private void showErrorMessage(Exception e, String extra){
		final String cname=getClass().getName();
		final String message=e.getMessage()+"; "+extra;
		if (msgHandler==null){
			JOptionPane.showMessageDialog(null,message,
			cname,JOptionPane.ERROR_MESSAGE);
		} else {
			msgHandler.errorOutln(cname+"--"+message);
		}
	}

	/**
	 * Request to print output messages to console.
	 * 
	 * @param console the console
	 */
	public void outputMessages(MessageHandler console) {
		
		if (!jamHomeDefined) {
			console.warningOutln("Jam home variable not defined, to define use -Djam.home=<directory>");
		}

		if (loadError != NO_ERRORS) {
			console.warningOutln(loadError);
		}
		if (configLoadWarning != NO_WARNINGS) {
			console.warningOutln(configLoadWarning);
		}
		console.messageOutln(configLoadMessage);
		if (userLoadWarning != NO_WARNINGS) {
			console.warningOutln(userLoadWarning);
		}
		console.messageOutln(userLoadMessage);
	}

	/**
	 * Load default configuration properties.
	 */
	private void loadDefaultConfig() {
		jamProperties.setProperty(JAM_HOME,(new File(userCurrentDir)).getPath());
		jamProperties.setProperty(HOST_IP,"calvin");
		jamProperties.setProperty(HOST_PORT_SEND,"5003");
		jamProperties.setProperty(HOST_PORT_SEND,"5002");
		jamProperties.setProperty(HOST_PORT_RECV,"5005");
		jamProperties.setProperty(TARGET_IP,"hobbes");
		jamProperties.setProperty(TARGET_PORT,"5002");
		jamProperties.setProperty(HOST_DATA_IP,"calvin-data");
		jamProperties.setProperty(HOST_DATA_PORT_RECV,"10205");
	}

	/**
	 * Load default user properties.
	 */
	private void loadDefaultUser() {
		jamProperties.setProperty(EXP_NAME,"default_");
		jamProperties.setProperty(SORT_ROUTINE,"jam.sort.Example");
		jamProperties.setProperty(SORT_CLASSPATH,DEFAULT_SORT_CLASSPATH);
		jamProperties.setProperty(HIST_PATH,(new File(userHomeDir,"spectra")).getPath());
		jamProperties.setProperty(EVENT_INPATH,
		(new File(userHomeDir,"events")).getPath());
		jamProperties.setProperty(EVENT_OUTPATH,
		(new File(userHomeDir,"presort")).getPath());
		jamProperties.setProperty(EVENT_OUTFILE,"sortout.evn");
		jamProperties.setProperty(TAPE_DEV,"/dev/rmt/0");
		jamProperties.setProperty(LOG_PATH,(new File(userHomeDir)).getPath());
		jamProperties.setProperty(EVENT_INSTREAM,
		"jam.sort.stream.YaleCAEN_InputStream");
		jamProperties.setProperty(EVENT_OUTSTREAM,"jam.sort.stream.YaleOutputStream");
	}
	
	private static final String TRUE=Boolean.toString(true);
	private static final String FALSE=Boolean.toString(false);
	
	/**
	 * Set a boolean property.
	 * 
	 * @param key the name of the property
	 * @param val the new value
	 */
	static public void setProperty(String key, boolean val){
		if (val){
			jamProperties.setProperty(key,TRUE);
		} else {
			jamProperties.setProperty(key,FALSE);
		}
	}
	
	/**
	 * Get the value of a boolean property.
	 * 
	 * @param key the name of the property
	 * @return <code>true</code> if equal to 
	 * <code>Boolean.toString(true)</code>
	 */
	static public boolean getBooleanProperty(String key){
		return jamProperties.getProperty(key).equals(TRUE);
	} 
}
