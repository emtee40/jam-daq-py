package jam.io.hdf;

import jam.data.AbstractHist1D;
import jam.data.DataParameter;
import jam.data.Gate;
import jam.data.Group;
import jam.data.Histogram;
import jam.data.func.AbstractCalibrationFunction;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.io.DataIO;
import jam.io.FileOpenMode;
import jam.util.FileUtilities;
import jam.util.StringUtilities;
import jam.util.SwingWorker;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

/**
 * Reads and writes HDF files containing spectra, scalers, gates, and additional
 * useful information.
 * 
 * @version 0.5 November 98, January 2005
 * @author Dale Visser, Ken Swartz
 * @since JDK1.1
 */
public final class HDFIO implements DataIO, JamFileFields {

	private static final String HDF_FILE_EXT ="hdf";
	
    /**
     * Last file successfully read from or written to for all instances of
     * HDFIO.
     * 
     * see #readFile
     */
    private static File lastGoodFile;

    private static final Object LVF_MONITOR = new Object();

    private static final Preferences PREFS = Preferences
            .userNodeForPackage(HDFIO.class);

    private static final String LFILE_KEY = "LastValidFile";

    static {
        lastGoodFile = new File(PREFS.get(LFILE_KEY, System
                .getProperty("user.dir")));
    }

    /**
     * Number of steps in progress, 
     * 1 for converting objects, 10 for writing them out
     */    
    private static final int MONITOR_STEPS_READ_WRITE=11;		//1 Count DD's, 10 read objects
    private static final int MONITOR_STEPS_OVERHEAD_WRITE=3;	//2 for start of read object
    private static final int MONITOR_STEPS_OVERHEAD_READ=1;		//

    private static final FileUtilities FILE_UTIL = FileUtilities.getInstance();
    
    /**
     * Parent frame.
     */
    private final Frame frame;
    
    AsyncProgressMonitor asyncMonitor;
    /**
     * Where messages get sent (presumably the console).
     */
    private final MessageHandler msgHandler; 

    private final StringUtilities stringUtil;    
    /**
     * <code>HDFile<code> object to read from.
     */
    private HDFile inHDF;

    private final AsyncListener doNothing = new AsyncListener(){
        public void completedIO(String message, String errorMessage){
            //do nothing
        }
    };
    private AsyncListener asListener=doNothing;
	
    private final ConvertJamObjToHDFObj jamToHDF;

    private final ConvertHDFObjToJamObj hdfToJam;
    
    private String uiMessage;
    private String uiErrorMsg;
    private int groupCount=0;
    private int histCount=0;
    private int gateCount=0;
    private int scalerCount=0;
    private int paramCount=0;
    private Group firstLoadedGroup;

    /**
     * Class constructor handed references to the main class and message
     * handler.
     * 
     * @param parent
     *            the parent window
     * @param console
     *            where to send output
     */
    public HDFIO(Frame parent, MessageHandler console) {
        frame = parent;
        msgHandler = console;
        stringUtil = StringUtilities.instance();
        asyncMonitor = new AsyncProgressMonitor(frame);
        jamToHDF = new ConvertJamObjToHDFObj();
        hdfToJam = new ConvertHDFObjToJamObj();
    }
    
    /* --------------------- Begin DataIO Interface Methods ------------------ */

    public void writeFile(File file) {
        writeFile(file, Collections.EMPTY_LIST, Collections.EMPTY_LIST, true,
                true);
    }

    public void writeFile(File file, Group group) {
        final List groupList = Collections.singletonList(group);
        writeFile(file, groupList, Collections.EMPTY_LIST, true, true);
    }

    public void writeFile(final File file, List histograms) {
        writeFile(file, Collections.EMPTY_LIST, histograms, true, true);
    }
    
    /**
     * Read in an HDF file.
     * @param mode
     *            whether to open or reload
     * @param infile
     *            file to load 
     */
    public boolean readFile(FileOpenMode mode, File infile) {
    	File [] inFiles = new File[1];
    	inFiles[0]=infile;
    	return readFile(mode, inFiles, null, null);
    }
    
    /**
     * Read in an HDF file.
     * @param infile
     *            file to load
     * @param mode
     *            whether to open or reload
     * @param group
     * 			  group to read in
     * @return <code>true</code> if successful
     */
    public boolean readFile(FileOpenMode mode, File infile, Group group) {
    	File [] inFiles = new File[1];
    	inFiles[0]=infile;    	
    	final List groupList= new ArrayList();
    	groupList.add(group);
    	return readFile(mode, inFiles, groupList, null);
    }

    /**
     * Read in an HDF file.
     * @param infile
     *            file to load
     * @param mode
     *            whether to open or reload
     * @param histNames names of <code>Histogram</code>'s to read in
     * @return <code>true</code> if successful
     */
    public boolean readFile(FileOpenMode mode, File infile, List histAttributeList) {
        File[] inFiles = new File[1];
        inFiles[0] = infile;
        return readFile(mode, inFiles, null, histAttributeList);
    }

    
    
    /* --------------------- End DataIO Interface Methods ------------------ */

    /**
     * Write out an HDF file, specifying whether scalers and parameters should
     * be included.
     * 
     * @param file to write to
     * @param writeData whether to write histograms and scalers
     * @param wrtSettings whether to write gates and parameters
     */
    public void writeFile(final File file, boolean writeData,
            boolean wrtSettings) {
        writeFile(file, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                writeData, wrtSettings);
    }
    
    /** 
     * Create list of groups and histograms to write out.
     * Use selected groups to create list of histograms or
     * use selected histograms to create list of gates 
     * 
     * @param file to write to
     * @param groups if given, groups to write
     * @param histograms to write if groups not given
     * @param writeData whether to write histograms and scalers
     * @param wrtSettings whether to write gates and parameters
     */
    private void writeFile(final File file, final List groups, final List histograms,
            boolean writeData, boolean wrtSettings) {
        /* Groups specified determines histograms */
        final List groupsToUse;
        final List histsToUse;
        final boolean haveGroups = !groups.isEmpty();
        final boolean haveHists = !histograms.isEmpty();
        if (haveGroups) {
            groupsToUse = groups;
            histsToUse = new ArrayList();
            final Iterator iterGroup = groups.iterator();
            while (iterGroup.hasNext()) {
                final Group currGroup = (Group) iterGroup.next();
                histsToUse.addAll(currGroup.getHistogramList());
            }
        } else if (haveHists) {
            /* Histograms specified determines groups. */
            groupsToUse = new ArrayList();
            histsToUse = histograms;
            final Iterator iterHist = histsToUse.iterator();
            while (iterHist.hasNext()) {
                final Histogram hist = (Histogram) iterHist.next();
                if (!groupsToUse.contains(hist.getGroup())) {
                    groupsToUse.add(hist.getGroup());
                }
            }
        } else {
            /* Neither groups nor histograms specified */
            groupsToUse = Group.getGroupList();
            histsToUse = new ArrayList();
            final Iterator iterGroup = groupsToUse.iterator();
            while (iterGroup.hasNext()) {
                final Group currGroup = (Group) iterGroup.next();
                histsToUse.addAll(currGroup.getHistogramList());
            }
        }
        //Append .hdf to file name
        String path =file.getParent();        
        String fileName = FILE_UTIL.changeExtension(file.getName(), HDF_FILE_EXT, FileUtilities.APPEND_ONLY);
        String fileFullName = path+File.separator+fileName;        
        File appendFile = new File(fileFullName);
        
        if (FILE_UTIL.overWriteExistsConfirm(appendFile)) {
            spawnAsyncWriteFile(appendFile, groupsToUse, histsToUse, writeData, wrtSettings);
        }
    }
    
    
    /**
     * Read in an HDF file.
     * 
     * @param inFiles
     *            files to load
     * @param mode
     *            whether to open or reload
     * @param histNames
     *            list of names of histograms to read in
     * @param groupNames list of names of groups to read in
     * @return <code>true</code> if successful
     */
    public boolean readFile(FileOpenMode mode, File[] inFiles, List groupList,
            List histAttributeList) {
        boolean rval = true;
        fileLoop: for (int i = 0; i < inFiles.length; i++) {
            final File infile = inFiles[i];
            if (!infile.isFile()) {
                msgHandler.errorOutln("Cannot find file " + infile + ".");
                rval = false;
                break fileLoop;
            }
            if (!HDFile.isHDFFile(infile)) {
                msgHandler.errorOutln("File " + infile
                        + " is not a valid HDF file.");
                rval = false;
                break fileLoop;
            }
        }
        if (rval) {
            spawnAsyncReadFile(mode, inFiles, groupList, histAttributeList);
        }
        return rval;
    }
    
    /*
     * non-javadoc: Asyncronized write
     */
    private void spawnAsyncWriteFile(final File file, final List groups,
            final List histograms, final boolean writeData,
            final boolean wrtSettings) {
        uiMessage = "";
        uiErrorMsg = "";
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                asyncWriteFile(file, groups, histograms, writeData, wrtSettings);
                System.gc();
                return null;
            }

            public void finished() {
                if (uiErrorMsg.equals("")) {
                    msgHandler.messageOutln(uiMessage);
                } else {
                    msgHandler.errorOutln(uiErrorMsg);
                }
            }
        };
        worker.start();
    }
    
    /*
     * non-javadoc: Asyncronized read
     */
    private void spawnAsyncReadFile(final FileOpenMode mode, final File [] inFiles, final List groupList, final List histAttributeList) {
    	uiMessage="";
    	uiErrorMsg ="";

    	final SwingWorker worker = new SwingWorker() {
            public Object construct() {
            	File infile=null;
            	//FIXME KBS Test change thread priority to make monitor pop up sooner
            	Thread.yield();
            	Thread thisTread =Thread.currentThread();
            	thisTread.setPriority(thisTread.getPriority()-1);
            	//End test 
            	int numberFiles = inFiles.length;
                asyncMonitor.setup("Reading HDF file", "Reading Objects", 
                		(MONITOR_STEPS_READ_WRITE+MONITOR_STEPS_OVERHEAD_READ)*numberFiles);
                firstLoadedGroup =null;

    			try {                
    				//Loop for all files
    				for (int i=0;i<inFiles.length;i++) {
	        			infile =inFiles[i];
	        			if (mode==FileOpenMode.ADD_OPEN_ONE) {
	        				if (i==0) {
	        					asyncReadFileGroup(infile, FileOpenMode.OPEN, groupList, histAttributeList);
	        				} else {
	        					final List groupListOpen = Group.getGroupList(); 
	        				   asyncReadFileGroup(infile, FileOpenMode.ADD, groupListOpen, histAttributeList);
	        				}
	        			} else {
        					asyncReadFileGroup(infile, mode, groupList, histAttributeList);	        				
	        			}
            			displayMessage();
	        		}
        		}catch (Exception e) {
        			uiErrorMsg ="Unknown Error reading file "+infile.getName()+", "+e;
        			e.printStackTrace();
        			asyncMonitor.close();
        		}
                asyncMonitor.close();
            	return null;
            }
            /* Runs on the event-dispatching thread. */
            public void finished() {
                if (!uiErrorMsg.equals("")) {
                    msgHandler.errorOutln(uiErrorMsg);
                }
                synchronized (asListener) {
                    asListener.completedIO(uiMessage, uiErrorMsg);
                }
            }
        };
        worker.start();     	
    }

    private void displayMessage() {
		final Runnable runner = new Runnable() {
	        public void run() {
	        	if (uiErrorMsg.equals("")) {
	        		msgHandler.messageOutln(uiMessage);
	        	} else {
	                msgHandler.errorOutln(uiErrorMsg);
	        	}
	        	uiMessage="";
	        	uiErrorMsg="";
	        }
	    };    			
    	        
        try {
            SwingUtilities.invokeAndWait(runner);
        } catch (Exception e) {
            e.printStackTrace(); 
        }		
    	        
    }
    /**
     * Given separate vectors of the writeable objects, constructs and writes
     * out an HDF file containing the contents. Null or empty
     * <code>Vector</code> arguments are skipped.
     * 
     * @param file
     *            disk file to write to
     * @param histograms
     *            list of <code>Histogram</code> objects to write
     * @param groups
     *            list of <code>Group</code>'s to write
     * @param writeScalers whether to write out histograms scalers
     * @param writeParams whether to write out gates, calibration and parameters
     */
    synchronized private void asyncWriteFile(File file, List groups,
            List histograms, boolean writeData, boolean writeSettings) {
        final StringBuffer message = new StringBuffer();
        /* reset all counters */
        groupCount = 0;
        histCount = 0;
        gateCount = 0;
        scalerCount = 0;
        paramCount = 0;
        AbstractData.clearAll();
        jamToHDF.addDefaultDataObjects(file.getPath());
        asyncMonitor.setup("Saving HDF file", "Converting Objects",
                MONITOR_STEPS_READ_WRITE + MONITOR_STEPS_OVERHEAD_WRITE);
		final Preferences prefs = HDFPrefs.PREFS;
		final boolean suppressEmpty = prefs.getBoolean(
				HDFPrefs.SUPPRESS_WRITE_EMPTY, true);     
        asyncMonitor.increment();
        
        HDFile out = null;
        try {
			convertJamToHDF(groups, histograms, writeData, writeSettings, suppressEmpty);
			
            out = new HDFile(file, "rw", asyncMonitor, MONITOR_STEPS_READ_WRITE);
            asyncMonitor.setNote("Writing Data Objects");
            out.writeFile();
            asyncMonitor.setNote("Closing File");
            
            message.append("Saved ").append(file.getName()).append(" (");
            message.append(groupCount).append(" groups");
            message.append(", ").append(histCount).append(" histograms");
            message.append(", ").append(gateCount).append(" gates");
            message.append(", ").append(scalerCount).append(" scalers");
            message.append(", ").append(paramCount).append(" parameters");
            message.append(")");
            
        } catch (FileNotFoundException e) {
            uiErrorMsg = "Opening file: " + file.getName();
        } catch (HDFException e) {
            uiErrorMsg = "Exception writing to file '" + file.getName() + "': "
                    + e.toString();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                uiErrorMsg = "Closing file " + file.getName();
            }
            asyncMonitor.close();
        }
        
        AbstractData.clearAll();
        out = null; //allows Garbage collector to free up memory
        setLastValidFile(file);
        uiMessage = message.toString();
    }
    
    
    /**
	 * Read in an HDF file
	 * 
	 * @param infile
	 *            file to load
	 * @param mode
	 *            whether to open or reload
	 * @param histNames
	 *            names of histograms to read, null if all
	 * @return <code>true</code> if successful
	 */
	synchronized private boolean asyncReadFileGroup(final File infile,
			final FileOpenMode mode, final List existingGrps,
			final List histAttrList) {
		boolean rval = true;
		final StringBuffer message = new StringBuffer();
		//reset all counters
		groupCount = 0;
		histCount = 0;
		gateCount = 0;
		scalerCount = 0;
		paramCount = 0;
		try {
			AbstractData.clearAll();
			//Read in objects
			inHDF = new HDFile(infile, "r", asyncMonitor,
					MONITOR_STEPS_READ_WRITE);
			inHDF.setLazyLoadData(true);
			/*
			 * read file into set of AbstractHData's, set their internal
			 * variables
			 */
			inHDF.readFile();
			AbstractData.interpretBytesAll();
			asyncMonitor.increment();
			final String fileName = FILE_UTIL.removeExtensionFileName(infile
					.getName());
			if (hdfToJam.hasVGroupRootGroup()) {
				convertHDFToJam(mode, fileName, existingGrps,
						histAttrList);
			} else {
				convertHDFToJamOriginal(mode, fileName, existingGrps,
						histAttrList);
			}
			/* Create output message. */
			if (mode == FileOpenMode.OPEN) {
				message.append("Opened ").append(infile.getName());
			} else if (mode == FileOpenMode.OPEN_MORE) {
				message.append("Opened Additional ").append(infile.getName());
			} else if (mode == FileOpenMode.RELOAD) {
				message.append("Reloaded ").append(infile.getName());
			} else { //ADD
				message.append("Adding counts in ").append(infile.getName());
				//FIXME currently only add one group
				message.append(" to groups ");
				for (int i = 0; i < existingGrps.size(); i++) {
					final String groupName = ((Group) existingGrps.get(0))
							.getName();
					if (0 < i) {
						message.append(", ");
					}
					message.append(groupName);
				}
			}
			message.append(" (");
			message.append(groupCount).append(" groups");
			message.append(", ").append(histCount).append(" histograms");
			message.append(", ").append(gateCount).append(" gates");
			message.append(", ").append(scalerCount).append(" scalers");
			message.append(", ").append(paramCount).append(" parameters");
			message.append(')');
		} catch (FileNotFoundException e) {
			uiErrorMsg = "Opening file: " + infile.getPath()
					+ " Cannot find file or file is locked";
		} catch (HDFException e) {
			uiErrorMsg = "Reading file: '" + infile.getName() + "', Exception "
					+ e.toString();
			rval = false;
		} finally {
			try {
				inHDF.close();
			} catch (IOException except) {
				uiErrorMsg = "Closing file " + infile.getName();
				rval = false;
			}
			/* destroys reference to HDFile (and its AbstractHData's) */
			inHDF = null;
		}
		AbstractData.clearAll();
		setLastValidFile(infile);
		uiMessage = message.toString();
		return rval;
	}
    
    /**
	 * Read the histograms in.
	 * 
	 * @param infile
	 *            to read from
	 * @return list of attributes
	 * @throws HDFException
	 *             if something goes wrong
	 */
    public List readHistogramAttributes(File infile) throws HDFException {
        List rval = new ArrayList();
        if (!HDFile.isHDFFile(infile)) {
            rval = Collections.EMPTY_LIST;
            throw new HDFException("File:" + infile.getPath()
                    + " is not an HDF file.");
        }
        try {
            AbstractData.clearAll();
            /* Read in histogram names */
            inHDF = new HDFile(infile, "r");
            inHDF.setLazyLoadData(true);
            inHDF.readFile();
            AbstractData.interpretBytesAll();
            
    		HistogramAttributes.clear();
            if (hdfToJam.hasVGroupRootGroup()) {
                rval.addAll(loadHistogramAttributesGroup());
            } else {
                rval.addAll(loadHistogramAttributesOriginal());
            }
        } catch (FileNotFoundException e) {
            throw new HDFException("Opening file: " + infile.getPath()
                    + " Cannot find file or file is locked");
        } catch (HDFException e) {
            throw new HDFException("Reading file: '" + infile.getName()
                    + "', Exception " + e.toString());
        } finally {
            try {
                inHDF.close();
            } catch (IOException except) {
                //NOP Bury exception
            }
            inHDF = null;
        }
        AbstractData.clearAll();
        return rval;
    }
    /**
     * Convert Jam objects to HDF DataObjects
     * 
     * @param groups
     * @param histList
     * @param writeData
     * @param wrtSettings
     * @param suppressEmpty
     * @throws HDFException
     */
    private void convertJamToHDF(List groups, List histList,
            boolean writeData, boolean wrtSettings, boolean suppressEmpty) throws HDFException {
        final VirtualGroup globalGroups = jamToHDF.addGroupSection();
        final VirtualGroup globalHists = jamToHDF.addHistogramSection();
        final VirtualGroup globalGates = jamToHDF.addGateSection();
        final VirtualGroup globalScaler = jamToHDF.addScalerSection();
        final VirtualGroup globalParams = jamToHDF.addParameterSection();
        /* Loop for all groups */
        final Iterator groupsIter = groups.iterator();
        while (groupsIter.hasNext()) {
            final Group group = (Group) groupsIter.next();
            final VirtualGroup vgGroup = jamToHDF.convertGroup(group);
            globalGroups.add(vgGroup);
            /* Loop for all histograms */
            final Iterator histsIter = group.getHistogramList().iterator();
            while (histsIter.hasNext()) {
                final Histogram hist = (Histogram) histsIter.next();
                //Histogram is in histogram list
                if (histList.contains(hist)) {
                    final VirtualGroup histVGroup = jamToHDF
                            .addHistogramGroup(hist);
                    vgGroup.add(histVGroup);
                    //backward compatible
                    globalHists.add(histVGroup);
                    final boolean histDefined = hist.getArea() > 0 ||!suppressEmpty;
                    if (writeData &&histDefined) {
                        jamToHDF.convertHistogram(histVGroup, hist);
                        histCount++;
                    }                    
                    
                    //Add calibrations
                    if ((writeData||wrtSettings) && hist.getDimensionality()==1) { 
                    	final AbstractCalibrationFunction calFunc = ((AbstractHist1D)hist).getCalibration();
                    	if (calFunc!=null) {
                    		final VDataDescription calibDD=jamToHDF.convertCalibration(calFunc);
                    		histVGroup.add(calibDD);
                    	}
                    }
                    
                    /* Loop for all gates */
                    final Iterator gatesIter = hist.getGates().iterator();
                    while (gatesIter.hasNext()) {
                        final Gate gate = (Gate) gatesIter.next();
                        if (wrtSettings && gate.isDefined()) {
                            final VirtualGroup gateVGroup = jamToHDF
                                    .convertGate(gate);
                            histVGroup.add(gateVGroup);
                            //backward compatiable
                            globalGates.add(gateVGroup);
                            gateCount++;
                        }
                    } //end loop gates
                }
            } //end loop histograms
            /* Convert all scalers */
            if (writeData) {
                final List scalerList = group.getScalerList();
                if (scalerList.size() > 0) {
                    final VirtualGroup vgScalers = jamToHDF.addScalerSection();
                    vgGroup.add(vgScalers);
                    final VDataDescription vddScalers = jamToHDF
                            .convertScalers(scalerList);
                    vgScalers.add(vddScalers);
                    if (group == Group.getSortGroup()) {
                        /* here for backwards compatibility */
                        globalScaler.add(vddScalers);
                    }
                }
                scalerCount = scalerList.size();
            }
            /* Convert all parameters */
            if (wrtSettings) {
                final List paramList = DataParameter.getParameterList();
                if (paramList.size() > 0) {
                    final VirtualGroup vgParams = jamToHDF
                            .addParameterSection();
                    vgGroup.add(vgParams);
                    if (group == Group.getSortGroup()) {
                        final VDataDescription vddParams = jamToHDF
                                .convertParameters(paramList);
                        vgParams.add(vddParams);
                        /* Backwards compatible */
                        globalParams.add(vddParams);
                        paramCount = paramList.size();
                    }
                }
            }
            groupCount++;
        }
    }
    /**
     * Convert a the HDF DataObjects to Jam objects
     *  
     * @param mode
     * @param existingGroupList
     * @param histAttributeList
     * @param fileName
     * @throws HDFException
     */
    private void convertHDFToJam(FileOpenMode mode, String fileName, List existingGroupList, List histAttributeList) throws HDFException {
        hdfToJam.setInFile(inHDF);
	    //Find groups	
	    final List groupVirtualGroups = hdfToJam.findGroups(mode, existingGroupList);

	    //Loop over groups
	    final Iterator groupIter =groupVirtualGroups.iterator();
	    GROUPLIST:while (groupIter.hasNext()) {
	    	final VirtualGroup currentVGroup = (VirtualGroup)groupIter.next();
	    	Group currentGroup=null;
	    	final List histList;
	    	//Get the current group for the rest of the operation
	    	if ( mode==FileOpenMode.OPEN || mode==FileOpenMode.OPEN_MORE ) {
	    		currentGroup =hdfToJam.convertGroup(currentVGroup, fileName, histAttributeList, mode);
	    		if (currentGroup!=null)
	    			groupCount++;
	    	} else {
	    		String groupName = hdfToJam.readVirtualGroupName(currentVGroup); 
	    		if (hdfToJam.containsGroup(groupName, existingGroupList)) {
	    			currentGroup = Group.getGroup(groupName);
	    		}
	    	}
	    	
	    	//No histograms in group
	    	if (currentGroup ==null) {
	    		continue GROUPLIST;
	    	}
	    	
	    	//Keep track of first loaded group
	    	if (firstLoadedGroup==null)
	    		firstLoadedGroup =currentGroup;
	    	
	        //Find histograms
	    	histList =hdfToJam.findHistograms(currentVGroup, null);
	    	
	        //Loop over histograms
	    	final Iterator histIter =histList.iterator();
	    	 while (histIter.hasNext()) {
	    	 	final VirtualGroup histVGroup = (VirtualGroup)histIter.next();
	    	 	final Histogram hist =(Histogram)hdfToJam.convertHistogram(currentGroup, histVGroup,  histAttributeList, mode);
	    	 	if (hist!=null)
	    	 		histCount++;
	    	 	//Load gates and calibration if not add
	    	 	if (hist!=null && mode != FileOpenMode.ADD) {
                	final List gateList = hdfToJam.findGates(histVGroup, hist.getType());
                	
                	//Loop over gates
                	final Iterator gateIter =gateList.iterator();
       	    	 	while (gateIter.hasNext()) {
       	    	 		final VirtualGroup gateVGroup = (VirtualGroup)gateIter.next();
       	    	 		Gate gate = hdfToJam.convertGate(hist, gateVGroup, mode);
       	    	 		if (gate!=null)
       	    	 			gateCount++;
       	    	 	}
	                /* Load calibration. */ 
	                final VDataDescription vddCalibration = hdfToJam.findCalibration(histVGroup);
	                if (vddCalibration!=null) {
	                	hdfToJam.convertCalibration(hist, vddCalibration);
	                }
	    	 	} //End Load gates and calibration
                
	    	 } //Loop Histogram end
	    	 //Load scalers
	    	 final List scalerList = hdfToJam.findScalers(currentVGroup);
	    	 if (!scalerList.isEmpty()) {
	    	 	scalerCount =hdfToJam.convertScalers(currentGroup, (VirtualGroup)scalerList.get(0), mode);
	    	 } 
	    	 //Load Parameters
	    	 final List paramList = hdfToJam.findParameters(currentVGroup);
	    	 if (!paramList.isEmpty()) {
	    	 	paramCount = hdfToJam.convertParameters(currentGroup, (VirtualGroup)paramList.get(0), mode);
	    	 }	    	  
	    } //Loop group end
    }
    
    /**
     * Convert a the HDF DataObjects to Jam objects for old format files
     * 
     * @param mode how to read the data into memory
     * @param groups list of existing groups
     * @param histAttrs list of histogram attributes?
     * @param fileName ???
     * @throws HDFException
     */
    private void convertHDFToJamOriginal(FileOpenMode mode, String fileName,
            List groups, List histAttrs) throws HDFException {
        hdfToJam.setInFile(inHDF);
        Group currentGroup = null;
        //Set group
        if ((mode == FileOpenMode.OPEN)) {
            currentGroup = Group.createGroup(Group.DEFAULT_NAME, null,
                    Group.Type.FILE);
        } else if (mode == FileOpenMode.OPEN_MORE) {
            currentGroup = Group.createGroup(Group.DEFAULT_NAME, fileName,
                    Group.Type.FILE);
        } else if (mode == FileOpenMode.ADD) {
            currentGroup = (Group) groups.get(0);
            //so use current group
        } else if (mode == FileOpenMode.RELOAD) {
            final JamStatus status = JamStatus.getSingletonInstance();
            final Group sortGroup = Group.getSortGroup();
            status.setCurrentGroup(sortGroup);
            currentGroup = status.getCurrentGroup();
        }
        /* Keep track of first loaded group */
        if (firstLoadedGroup == null) {
            firstLoadedGroup = currentGroup;
        }
        groupCount = 0;
        
        histCount = hdfToJam.convertHistogramsOriginal(currentGroup, mode,
                histAttrs);
        final VDataDescription vddScalers = hdfToJam.findScalersOriginal();
        if (vddScalers != null) {
            scalerCount = hdfToJam.convertScalers(currentGroup, vddScalers,
                    mode);
        }
        if (mode != FileOpenMode.ADD) {
            gateCount = hdfToJam.convertGatesOriginal(currentGroup, mode);
            /* clear if opening and there are histograms in file */
            final VDataDescription vddParam = hdfToJam
                    .findParametersOriginal();
            if (vddParam != null) {
                paramCount = hdfToJam.convertParameters(currentGroup,
                        vddParam, mode);
            }
        }
    }
    
    /*
     * non-javadoc: Reads in the histogram and hold them in a tempory array
     * 
     * @exception HDFException thrown if unrecoverable error occurs
     */
    private List loadHistogramAttributesGroup() throws HDFException {
    	final ArrayList lstHistAtt = new ArrayList();
	    final FileOpenMode mode=FileOpenMode.ATTRIBUTES;
	    hdfToJam.setInFile(inHDF);	    
	    //Find groups	
	    final List groupVirtualGroups = hdfToJam.findGroups(mode, null);
	    groupCount =groupVirtualGroups.size();
	    //Loop over groups
	    final Iterator groupIter =groupVirtualGroups.iterator();
	    while (groupIter.hasNext()) {
	    	final VirtualGroup currentVGroup = (VirtualGroup)groupIter.next();
	    	final String groupName =hdfToJam.readVirtualGroupName(currentVGroup);
	        //Find histograms
	    	final List histList =hdfToJam.findHistograms(currentVGroup, null);
	    	histCount = histList.size();
	        //Loop over histograms
	    	final Iterator histIter =histList.iterator();
	    	 while (histIter.hasNext()) {
	    	 	final VirtualGroup histVGroup = (VirtualGroup)histIter.next();
	    	 	final HistogramAttributes histAttributes =hdfToJam.convertHistogamAttributes(groupName, histVGroup,  mode);
	    	 	lstHistAtt.add(histAttributes);
	    	 }
	    }
	    return lstHistAtt;
    }
    
    /*
     * non-javadoc: Reads in the histogram and hold them in a tempory array
     * 
     * @exception HDFException thrown if unrecoverable error occurs
     */
    private List loadHistogramAttributesOriginal() throws HDFException {
        final ArrayList lstHistAtt = new ArrayList();
        hdfToJam.setInFile(inHDF);
        final VirtualGroup hists = VirtualGroup.ofName(HIST_SECTION);
        /* only the "histograms" VG (only one element) */
        if (hists != null) {
            /* Histogram iterator */
            final Iterator iter = hists.getObjects().iterator();
            // loop begin
            while (iter.hasNext()) {
                final VirtualGroup currHistGrp = (VirtualGroup) (iter.next());
                final HistogramAttributes histAttributes = hdfToJam.convertHistogamAttributes(
                					Group.DEFAULT_NAME, currHistGrp, FileOpenMode.ATTRIBUTES);
                lstHistAtt.add(histAttributes);
            }
            //after loop
        }
        return lstHistAtt;
    }

    /**
     * Determines whether a <code>List</code> passed to it
     * <ol>
     * <li>exists, and</li>
     * <li>
     * </ol>
     * has any elements.
     * 
     * @param list
     *            the list to check
     * @return true if the given list exists and has at least one element
     */
    private boolean hasContents(List list) {
        final boolean val = (list != null) && (!list.isEmpty());
        return val;
    }
    
    /**
     * @return last file successfully read from or written to.
     */
    public static File getLastValidFile() {
        synchronized (LVF_MONITOR) {
            return lastGoodFile;
        }
    }

    private static void setLastValidFile(File file) {
        synchronized (LVF_MONITOR) {
            lastGoodFile = file;
            PREFS.put(LFILE_KEY, file.getAbsolutePath());
        }
    }
    public Group getFirstLoadGroup() {
    	return firstLoadedGroup;
    }
    /**
     * Set the listener.
     * @param listener the new listener
     */
    public void setListener(AsyncListener listener) {
        synchronized (asListener) {
            if (listener == null) {
                removeListener();
            } else {
                asListener = listener;
            }
        }
    }
    
    /**
     * Unset the listener.
     *
     */
    public void removeListener() {
        synchronized (asListener) {
            asListener = doNothing;
        }
    }
     
	/**
	 * Interface to be called when asynchronized IO is completed.
	 */
    public interface AsyncListener {
        /**
         * Called when asychronous IO is completed
         * @param message if normal completion
         * @param errorMessage if an error occurs
         */
    	public void completedIO(String message, String errorMessage);
    }    
}


