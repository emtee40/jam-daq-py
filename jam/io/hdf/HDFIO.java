package jam.io.hdf;

import jam.data.AbstractHist1D;
import jam.data.DataBase;
import jam.data.DataParameter;
import jam.data.Gate;
import jam.data.Group;
import jam.data.Histogram;
import jam.data.Scaler;
import jam.data.control.AbstractControl;
import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.JamProperties;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.io.DataIO;
import jam.io.FileOpenMode;
import jam.util.StringUtilities;
import jam.util.SwingWorker;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Polygon;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

/**
 * Reads and writes HDF files containing spectra, scalers, gates, and additional
 * useful information.
 * 
 * @version 0.5 November 98, January 2005
 * @author Dale Visser, Ken Swartz
 * @since JDK1.1
 */
public final class HDFIO implements DataIO, JamHDFFields {

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

    static private final List EMPTY_LIST = Collections
            .unmodifiableList(new ArrayList());

    static {
        lastGoodFile = new File(PREFS.get(LFILE_KEY, System
                .getProperty("user.dir")));
    }

    /**
     * Number of steps in progress, 
     * 1 for converting objects, 10 for writing them out
     */    
    private final int STEPS_WRITE_PROGRESS=10;
    private final int STEPS_CONVERT_PROGRESS=1;
    /**
     * Parent frame.
     */
    private final Frame frame;
    
    AsyncProgressMonitor asyncMonitor;
    /**
     * Where messages get sent (presumably the console).
     */
    private final MessageHandler msgHandler;

    private VirtualGroup allHists, allGates;

    /**
     * <code>HDFile<code> object to read from.
     */
    private HDFile inHDF;

    private final ConvertJamObjToHDFObj jamToHDF;

    //FIXME private ConvertHDFObjToJamObj convertHDFToJam;

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
        asyncMonitor = new AsyncProgressMonitor(frame);
        jamToHDF = new ConvertJamObjToHDFObj();
        //FIXME convertHDFToJam = new ConvertHDFObjToJamObj();
    }

    /**
     * Writes out to a specified file all the currently held spectra, gates,
     * scalers, and parameters.
     * 
     * @param file
     *            to write to
     */
    public void writeFile(File file) {
        writeFile(true, true, true, true, file);
    }

    /**
     * Writes out to a specified file a list of histograms and gates, scalers,
     * and parameters.
     * 
     * @param file
     *            the to write to
     * @param histograms
     *            list of histograms to write
     */
    public void writeFile(File file, List histograms) {
        /* Check to overwrite if file exists. */
        if (!overWriteExistsConfirm(file)) {
            return;
        }
        /* Histogram list */
        final List histList = new ArrayList();
        final Iterator iterHist = histograms.iterator();
        while (iterHist.hasNext()) {
            final Histogram hist = (Histogram) iterHist.next();
            if (hist.getArea() > 0) {
                histList.add(hist);
            }
        }
        /* Gate list */
        final List gateList = new ArrayList();
        final Iterator iter = Gate.getGateList().iterator();
        while (iter.hasNext()) {
            final Gate gate = (Gate) (iter.next());
            final Histogram histGate = gate.getHistogram();
            /*
             * Gate defined and Does the histogram List contain this gate's
             * histogram
             */
            if (gate.isDefined() && histList.contains(histGate)) {
                gateList.add(Gate.getGateList());
            }
        }
        final List scalerList = Scaler.getScalerList();
        final List paramList = DataParameter.getParameterList();
        spawnAsyncWriteFile(file, histList, gateList, scalerList, paramList);
    }

    /**
     * Writes out (to a specific file) the currently held spectra, gates, and
     * scalers, subject to the options given. Sets separately which data
     * writeFile should actually output. Not writing histograms when you are
     * saving tape data can significantly save time when you have many 2-d
     * spectra.
     * 
     * @param wrthist
     *            if true, Histograms will be written
     * @param wrtgate
     *            if true, Gates will be written
     * @param wrtscalers
     *            if true, scaler values will be written
     * @param wrtparams
     *            if true, parameter values will be written
     * @param file
     *            to write to
     */
    public void writeFile(boolean wrthist, boolean wrtgate, boolean wrtscalers,
            boolean wrtparams, final File file) {
        if (overWriteExistsConfirm(file)) {
            /* Histogram list */
            final List tempHist = wrthist ? Histogram.getHistogramList()
                    : EMPTY_LIST;
            final Iterator iter = tempHist.iterator();
            final List histList = new ArrayList();
            while (iter.hasNext()) {
                final Histogram hist = (Histogram) iter.next();
                if (hist.getArea() > 0) {
                    histList.add(hist);
                }
            }
            /* Gate list */
            final List gateList = new ArrayList();
            if (wrtgate) {
                gateList.addAll(Gate.getGateList());
            }
            final Iterator gateIterator = gateList.iterator();
            while (gateIterator.hasNext()) {
                final Gate gate = (Gate) (gateIterator.next());
                if (!gate.isDefined()) {
                    gateIterator.remove();
                }
            }
            final List scaler = wrtscalers ? Scaler.getScalerList()
                    : EMPTY_LIST;
            final List parameter = wrtparams ? DataParameter.getParameterList()
                    : EMPTY_LIST;
            spawnAsyncWriteFile(file, histList, gateList, scaler, parameter);
        }
    }

    /*
     * non-javadoc: Asyncronized write
     */
    private void spawnAsyncWriteFile(final File file, final List histograms,
            final List gates, final List scalers, final List parameters) {
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                asyncWriteFile(file, histograms, gates, scalers, parameters);
            }
        });
        thread.start();
    }

    /**
     * Given separate vectors of the writeable objects, constructs and writes
     * out an HDF file containing the contents. Null or empty
     * <code>Vector</code> arguments are skipped.
     * 
     * @param file
     *            disk file to write to
     * @param hists
     *            list of <code>Histogram</code> objects to write
     * @param gates
     *            list of <code>Gate</code> objects to write
     * @param scalers
     *            list of <code>Scaler</code> objects to write
     * @param parameters
     *            list of <code>Parameter</code> objects to write
     */
    private void asyncWriteFile(File file, List hists, List gates, List scalers,
            List parameters) {
        final StringBuffer message = new StringBuffer();
        int progress = 1;
        DataObject.clearAll();
        addDefaultDataObjects(file.getPath());
        final int totalToDo = hists.size() + gates.size() + scalers.size()
                + parameters.size();
        
        asyncMonitor.setup("Saving HDF file", "Converting Objects", 
        					STEPS_WRITE_PROGRESS+STEPS_CONVERT_PROGRESS);
        message.append("Saved ").append(file.getName()).append(" (");
        if (hasContents(hists)) {
            addHistogramSection();
            message.append(hists.size()).append(" histograms");
            final Iterator iter = hists.iterator();
            while (iter.hasNext()) {
                final Histogram hist = (Histogram) iter.next();
                final VirtualGroup histVGroup = jamToHDF.convertHistogram(hist);
                allHists.addDataObject(histVGroup);
                //addHistogram((Histogram) (temp.next()));
                //asyncMonitor.increment();
            }
        }
        if (hasContents(gates)) {
            addGateSection();
            message.append(", ").append(gates.size()).append(" gates");
            final Iterator iter = gates.iterator();
            while (iter.hasNext()) {
                final Gate gate = (Gate) iter.next();
                final VirtualGroup gateVGroup = jamToHDF.convertGate(gate);
                allGates.addDataObject(gateVGroup);
                //addGate((Gate) (temp.next()));
                //asyncMonitor.increment();
            }
        }
        if (hasContents(scalers)) {
            jamToHDF.convertScalers(scalers);
            //addScalerSection(scalers);
            message.append(", ").append(scalers.size()).append(" scalers");
            //asyncMonitor.increment();            
        }
        if (hasContents(parameters)) {
            jamToHDF.convertParameters(parameters);
            //addParameterSection(parameters);
            message.append(", ").append(parameters.size())
                    .append(" parameters");
            //asyncMonitor.increment();
        }
        asyncMonitor.increment();
        message.append(")");
        msgHandler.messageOut("", MessageHandler.END);
        HDFile out;
        try {
            synchronized (this) {
                out = new HDFile(file, "rw", asyncMonitor, STEPS_WRITE_PROGRESS);
                asyncMonitor.setNote("Writing Data Objects");
            }
            out.writeFile();
            asyncMonitor.setNote("Closing File");
            out.close();
        } catch (FileNotFoundException e) {
            msgHandler.errorOutln("Opening file: " + file.getName());
        } catch (HDFException e) {
            msgHandler.errorOutln("Exception writing to file '"
                    + file.getName() + "': " + e.toString());
        } catch (IOException e) {
            msgHandler.errorOutln("Exception writing to file '"
                    + file.getName() + "': " + e.toString());
        }
        synchronized (this) {
            DataObject.clearAll();
            out = null; //allows Garbage collector to free up memory
        }
        asyncMonitor.close();
        outln(message.toString());
        setLastValidFile(file);
        System.gc();
    }

    /*
     * non-javadoc: Confirm overwrite if file exits
     */
    private boolean overWriteExistsConfirm(File file) {
        final boolean writeConfirm = file.exists() ? JOptionPane.YES_OPTION == JOptionPane
                .showConfirmDialog(frame, "Replace the existing file? \n"
                        + file.getName(), "Save " + file.getName(),
                        JOptionPane.YES_NO_OPTION)
                : true;
        if (writeConfirm) {
            /* we've confirmed overwrite with the user. */
            file.delete();
        }

        return writeConfirm;
    }

    /**
     * Read in an HDF file.
     * 
     * @param infile
     *            file to load
     * @param mode
     *            whether to open or reload
     * @return <code>true</code> if successful
     */
    public boolean readFile(FileOpenMode mode, File infile) {
    	spawnAsyncReadFile(mode, infile, null);
        return true;
    }
    /**
     * Read in an HDF file.
     * 
     * @param infile
     *            file to load
     * @param mode
     *            whether to open or reload
     * @return <code>true</code> if successful
     */
    public boolean readFile(FileOpenMode mode, File infile,List histNames) {
    	spawnAsyncReadFile(mode, infile, histNames);
        return true;
    }

    /*
     * non-javadoc: Asyncronized write
     */
    public void spawnAsyncReadFile(final FileOpenMode mode, final File infile, final List histNames) {

    	final SwingWorker worker = new SwingWorker() {

            public Object construct() {
            	asyncReadFile(mode, infile, histNames);
            	return null;
            }

            //Runs on the event-dispatching thread.
            public void finished() {
            	//FIXME KBS should move to someplace else or have callback
            	JamStatus STATUS=JamStatus.getSingletonInstance();            	
            	Broadcaster BROADCASTER=Broadcaster.getSingletonInstance();
            	            	
        		AbstractControl.setupAll();
        		BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_ADD);
        		        			
        		Histogram firstHist = (Histogram)Group.getCurrentGroup().getHistogramList().get(0);        		
        		STATUS.setCurrentHistogram(firstHist);
        		BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_SELECT, firstHist);
        		STATUS.getFrame().repaint();
            	
            }
        };
        worker.start();     	
    	//Need to use worker 
    	/*
        final Thread thread = new Thread(new Runnable() {
            public void run() {
            	asyncReadFile(mode, infile, histNames);
            }
        }); 
        thread.start();
        */    	
        //asyncReadFile(mode, infile, histNames);
    }

    /**
     * Read in an HDF file
     * 
     * @param infile
     *            file to load
     * @param modes
     *            whether to open or reload
     * @param histNames
     *            names of histograms to read, null if all
     * @return <code>true</code> if successful
     */
    public boolean asyncReadFile(FileOpenMode mode, File infile, List histNames) {
        boolean rval = true;

        if (!HDFile.isHDFFile(infile)) {
            msgHandler.errorOutln(infile + " is not a valid HDF File!");
            rval = false;
        }
        asyncMonitor.setup("Reading HDF file", "Reading Objects", 
				STEPS_WRITE_PROGRESS+STEPS_CONVERT_PROGRESS);
        
        if (rval) {
            final StringBuffer message = new StringBuffer();
            try {
                if (mode == FileOpenMode.OPEN) {
                    message.append("Opened ").append(infile.getName()).append(
                            " (");
                    DataBase.getInstance().clearAllLists();
                } else if (mode == FileOpenMode.OPEN_ADDITIONAL) {
                    message.append("Opened Additional ").append(
                            infile.getName()).append(" (");
                } else if (mode == FileOpenMode.RELOAD) {
                    message.append("Reloaded ").append(infile.getName())
                            .append(" (");
                } else { //ADD
                    message.append("Adding histogram counts in ").append(
                            infile.getName()).append(" (");
                }

                DataObject.clearAll();

                //Read in objects
                synchronized (this) {
                    inHDF = new HDFile(infile, "r", asyncMonitor, STEPS_WRITE_PROGRESS);
                }
                inHDF.seek(0);
                 /* read file into set of DataObject's, set their internal variables */
                inHDF.readFile();

                asyncMonitor.setNote("Parsing objects");
                asyncMonitor.increment();
                
                DataObject.interpretBytesAll();

                //Set group
                if (mode == FileOpenMode.OPEN) {
                    Group.clearList();
                    Group.createGroup(infile.getName(), Group.Type.FILE);
                } else if (mode == FileOpenMode.OPEN_ADDITIONAL) {
                    Group.createGroup(infile.getName(), Group.Type.FILE);
                } else if (mode == FileOpenMode.RELOAD) {
                    final String sortName = JamStatus.getSingletonInstance()
                            .getSortName();
                    Group.setCurrentGroup(sortName);
                } // else mode == FileOpenMode.ADD, so use current group
                
                //FIXME takes a long time needs to update progress
                final int numHists = getHistograms(mode, histNames);
                message.append(numHists).append(" histograms");
                final int numScalers = getScalers(mode);
                message.append(", ").append(numScalers).append(" scalers");

                if (mode != FileOpenMode.ADD) {

                    final int numGates = getGates(mode);
                    /* clear if opening and there are histograms in file */
                    message.append(", ").append(numGates).append(" gates");

                    final int numParams = getParameters(mode);
                    message.append(", ").append(numParams)
                            .append(" parameters");
                }
                message.append(')');
                msgHandler.messageOutln(message.toString());
                inHDF.close();
                synchronized (this) {
                    /* destroys reference to HDFile (and its DataObject's) */
                    inHDF = null;
                }

                setLastValidFile(infile);
            } catch (HDFException except) {
                msgHandler.errorOutln(except.toString());
                except.printStackTrace();
                rval = false;
            } catch (IOException except) {
                msgHandler.errorOutln(except.toString());
                except.printStackTrace();
                rval = false;
            }

            DataObject.clearAll();
            System.gc();
            asyncMonitor.close();
        }
        return rval;
    }

    /**
     * Read the histograms in.
     * 
     * @param infile
     *            to read from
     * @return list of attributes
     */
    public List readHistogramAttributes(File infile) {
        final List rval;
        if (HDFile.isHDFFile(infile)) {
            rval = new ArrayList();
            try {
                DataObject.clearAll();
                /* Read in histogram names */
                inHDF = new HDFile(infile, "r");
                inHDF.readFile();
                rval.addAll(loadHistogramAttributes());
                inHDF.close();
            } catch (HDFException except) {
                msgHandler.errorOutln(except.toString());
            } catch (IOException except) {
                msgHandler.errorOutln(except.toString());
            }
            DataObject.clearAll();
        } else {
            msgHandler.errorOutln(infile + " is not a valid HDF File!");
            rval = Collections.EMPTY_LIST;
        }
        return rval;
    }

    /*
     * non-javadoc: Reads in the histogram and hold them in a tempory array
     * 
     * @exception HDFException thrown if unrecoverable error occurs
     */
    private List loadHistogramAttributes() throws HDFException {
        final ArrayList lstHistAtt = new ArrayList();
        NumericalDataGroup ndg = null;
        /* I check ndgErr==null to determine if error bars exist */
        NumericalDataGroup ndgErr = null;
        /* get list of all VG's in file */
        final java.util.List groups = DataObject.ofType(DataObject.DFTAG_VG);
        final VirtualGroup hists = VirtualGroup.ofName(groups,
                JamHDFFields.HIST_SECTION);
        /* only the "histograms" VG (only one element) */
        ScientificData sdErr = null;
        if (hists != null) {
            /* get list of all DIL's in file */
            final java.util.List labels = DataObject
                    .ofType(DataObject.DFTAG_DIL);
            /* get list of all DIA's in file */
            final java.util.List annotations = DataObject
                    .ofType(DataObject.DFTAG_DIA);
            final Iterator temp = hists.getObjects().iterator();
            while (temp.hasNext()) {
                final VirtualGroup current = (VirtualGroup) (temp.next());
                final java.util.List tempVec = DataObject.ofType(current
                        .getObjects(), DataObject.DFTAG_NDG);
                final NumericalDataGroup[] numbers = new NumericalDataGroup[tempVec
                        .size()];
                tempVec.toArray(numbers);
                if (numbers.length == 1) {
                    ndg = numbers[0]; //only one NDG -- the data
                } else if (numbers.length == 2) {
                    if (DataIDLabel.withTagRef(labels, DataObject.DFTAG_NDG,
                            numbers[0].getRef()).getLabel().equals(
                            JamHDFFields.ERROR_LABEL)) {
                        ndg = numbers[1];
                        ndgErr = numbers[0];
                    } else {
                        ndg = numbers[0];
                        ndgErr = numbers[1];
                    }
                } else {
                    throw new HDFException("Invalid number of data groups ("
                            + numbers.length + ") in NDG.");
                }
                final ScientificData sciData = (ScientificData) (DataObject
                        .ofType(ndg.getObjects(), DataObject.DFTAG_SD).get(0));
                final ScientificDataDimension sdd = (ScientificDataDimension) (DataObject
                        .ofType(ndg.getObjects(), DataObject.DFTAG_SDD).get(0));
                final DataIDLabel numLabel = DataIDLabel.withTagRef(labels, ndg
                        .getTag(), ndg.getRef());
                final int number = Integer.parseInt(numLabel.getLabel());
                final byte histNumType = sdd.getType();
                sciData.setNumberType(histNumType);
                final int histDim = sdd.getRank();
                sciData.setRank(histDim);
                final int sizeX = sdd.getSizeX();
                final int sizeY = (histDim == 2) ? sdd.getSizeY() : 0;
                final DataIDLabel templabel = DataIDLabel.withTagRef(labels,
                        current.getTag(), current.getRef());
                final DataIDAnnotation tempnote = DataIDAnnotation.withTagRef(
                        annotations, current.getTag(), current.getRef());
                final String name = templabel.getLabel();
                final String title = tempnote.getNote();
                if (ndgErr != null) {
                    sdErr = (ScientificData) (DataObject.ofType(ndgErr
                            .getObjects(), DataObject.DFTAG_SD).get(0));
                    sdErr.setRank(histDim);
                    sdErr.setNumberType(NumberType.DOUBLE);
                    /*
                     * final ScientificDataDimension sddErr
                     * =(ScientificDataDimension)(in.ofType(
                     * ndgErr.getObjects(),DataObject.DFTAG_SDD).get(0));
                     */
                }
                /* read in data */
                final Object dataArray;
                if (histDim == 1) {
                    if (histNumType == NumberType.INT) {
                        dataArray = sciData.getData1d(inHDF, sizeX);
                    } else { //DOUBLE
                        dataArray = sciData.getData1dD(inHDF, sizeX);
                    }
                    /*
                     * if (ndgErr != null) {
                     * histogram.setErrors(sdErr.getData1dD(sizeX)); }
                     */
                } else { //2d
                    if (histNumType == NumberType.INT) {
                        dataArray = sciData.getData2d(inHDF, sizeX, sizeY);
                    } else {
                        dataArray = sciData.getData2dD(inHDF, sizeX, sizeY);
                    }
                }
                /* Add histogram */
                final HistogramAttributes histAtt = new HistogramAttributes();
                histAtt.name = name;
                histAtt.title = title;
                histAtt.number = number;
                histAtt.sizeX = sizeX;
                histAtt.sizeY = sizeY;
                histAtt.histDim = histDim;
                histAtt.histNumType = histNumType;
                histAtt.dataArray = dataArray;
                if (ndgErr != null) {
                    histAtt.errorArray = sdErr.getData1dD(inHDF, sizeX);
                }
                lstHistAtt.add(histAtt);
            }
        } //hist !=null
        return lstHistAtt;
    }

    /*
     * non-javadoc: Add default objects always needed.
     * 
     * Almost all of Jam's number storage needs are satisfied by the type
     * hard-coded into the class <code> NumberType </code> . This method creates
     * the <code> NumberType </code> object in the file that gets referred to
     * repeatedly by the other data elements. LibVersion Adds data element
     * giving version of HDF libraries to use (4.1r2).
     * 
     * @see jam.io.hdf.NumberType
     */
    private void addDefaultDataObjects(String fileID) {
        new LibVersion(); //DataObjects add themselves
        NumberType.createDefaultTypes();
        new JavaMachineType();
        new FileIdentifier(fileID);
        addFileNote();
    }

    /**
     * Add a text note to the file, which includes the state of
     * <code>JamProperties</code>.
     * 
     * @see jam.global.JamProperties
     */
    private void addFileNote() {
        final String noteAddition = "\n\nThe histograms when loaded into jam are displayed starting at channel zero up\n"
                + "to dimension-1.  Two-dimensional data are properly displayed with increasing channel\n"
                + "number from the lower left to the lower right for, and from the lower left to the upper\n"
                + "left."
                + "All error bars on histogram counts should be considered Poisson, unless a\n"
                + "Numerical Data Group labelled 'Errors' is present, in which case the contents\n"
                + "of that should be taken as the error bars.";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String header = "Jam Properties at time of save:";
        try {
            JamProperties.getProperties().store(baos, header);
        } catch (IOException ioe) {
            throw new UndeclaredThrowableException(ioe,
                    "Unable to serialize properties.");
        }
        final String notation = baos.toString() + noteAddition;
        new FileDescription(notation);
    }

    /**
     * Adds data objects for the virtual group of histograms.
     */
    private void addHistogramSection() {
        synchronized (this) {
            allHists = new VirtualGroup(HIST_SECTION, FILE_SECTION);
        }
        new DataIDLabel(allHists, HIST_SECTION);
    }

    /**
     * 
     * /** looks for the special Histogram section and reads the data into
     * memory.
     * 
     * @param mode
     *            whether to open or reload
     * @param sb
     *            summary message under construction
     * @param histNames
     *            names of histograms to read, null if all
     * @exception HDFException
     *                thrown if unrecoverable error occurs
     * @throws IllegalStateException
     *             if any histogram apparently has more than 2 dimensions
     * @return number of histograms
     */
    private int getHistograms(FileOpenMode mode, List histNames)
            throws HDFException {
        int numHists = 0;
        /* get list of all VG's in file */
        final List groups = DataObject.ofType(DataObject.DFTAG_VG);
        final VirtualGroup hists = VirtualGroup.ofName(groups, HIST_SECTION);
        /* only the "histograms" VG (only one element) */
        if (hists != null) {
            //Message
            numHists = hists.getObjects().size();

            /* get list of all DIL's in file */
            final List labels = DataObject.ofType(DataObject.DFTAG_DIL);
            /* get list of all DIA's in file */
            final List annotations = DataObject.ofType(DataObject.DFTAG_DIA);
            //Histogram iterator
            final Iterator temp = hists.getObjects().iterator();
            while (temp.hasNext()) {
                final VirtualGroup current = (VirtualGroup) (temp.next());
                NumericalDataGroup ndg = null;
                /* I check ndgErr==null to determine if error bars exist */
                NumericalDataGroup ndgErr = null;
                /* only the "histograms" VG (only one element) */
                ScientificData sdErr = null;
                final List tempVec = DataObject.ofType(current.getObjects(),
                        DataObject.DFTAG_NDG);
                final NumericalDataGroup[] numbers = new NumericalDataGroup[tempVec
                        .size()];
                tempVec.toArray(numbers);
                if (numbers.length == 1) {
                    ndg = numbers[0]; //only one NDG -- the data
                } else if (numbers.length == 2) {
                    if (DataIDLabel.withTagRef(labels, DataObject.DFTAG_NDG,
                            numbers[0].getRef()).getLabel().equals(ERROR_LABEL)) {
                        ndg = numbers[1];
                        ndgErr = numbers[0];
                    } else {
                        ndg = numbers[0];
                        ndgErr = numbers[1];
                    }
                } else {
                    throw new IllegalStateException(
                            "Invalid number of data groups (" + numbers.length
                                    + ") in NDG.");
                }
                final ScientificData sciData = (ScientificData) (DataObject
                        .ofType(ndg.getObjects(), DataObject.DFTAG_SD).get(0));
                final ScientificDataDimension sdd = (ScientificDataDimension) (DataObject
                        .ofType(ndg.getObjects(), DataObject.DFTAG_SDD).get(0));
                final DataIDLabel numLabel = DataIDLabel.withTagRef(labels, ndg
                        .getTag(), ndg.getRef());
                final int number = Integer.parseInt(numLabel.getLabel());
                final byte histNumType = sdd.getType();
                sciData.setNumberType(histNumType);
                final int histDim = sdd.getRank();
                sciData.setRank(histDim);
                final int sizeX = sdd.getSizeX();
                int sizeY = 0;
                if (histDim == 2) {
                    sizeY = sdd.getSizeY();
                }
                final DataIDLabel templabel = DataIDLabel.withTagRef(labels,
                        current.getTag(), current.getRef());
                final DataIDAnnotation tempnote = DataIDAnnotation.withTagRef(
                        annotations, current.getTag(), current.getRef());
                final String name = templabel.getLabel();
                final String title = tempnote.getNote();
                if (ndgErr == null) {
                    sdErr = null;
                } else {
                    sdErr = (ScientificData) (DataObject.ofType(ndgErr
                            .getObjects(), DataObject.DFTAG_SD).get(0));
                    sdErr.setRank(histDim);
                    sdErr.setNumberType(NumberType.DOUBLE);
                }
                /* Given name list check that that the name is in the list */
                if (histNames == null || histNames.contains(name)) {
                    createHistogram(mode, name, title, number, histNumType,
                            histDim, sizeX, sizeY, sciData, sdErr);
                }
            }
        }
        return numHists;
    }

    /*
     * non-javadoc: Create a histgram given all the attributes and data
     */
    private void createHistogram(FileOpenMode mode, String name, String title,
            int number, byte histNumType, int histDim, int sizeX, int sizeY,
            ScientificData sciData, ScientificData sdErr) throws HDFException {
        Histogram histogram;
        final StringUtilities util = StringUtilities.instance();
        if (mode.isOpenMode()) {
            if (histDim == 1) {
                if (histNumType == NumberType.INT) {
                    histogram = Histogram.createHistogram(sciData.getData1d(
                            inHDF, sizeX), name, title);
                } else { //DOUBLE
                    histogram = Histogram.createHistogram(sciData.getData1dD(
                            inHDF, sizeX), name, title);
                }
                if (sdErr != null) {
                    ((AbstractHist1D) histogram).setErrors(sdErr.getData1dD(
                            inHDF, sizeX));
                }
            } else { //2d
                if (histNumType == NumberType.INT) {
                    histogram = Histogram.createHistogram(sciData.getData2d(
                            inHDF, sizeX, sizeY), name, title);
                } else {
                    histogram = Histogram.createHistogram(sciData.getData2dD(
                            inHDF, sizeX, sizeY), name, title);
                }
            }
            histogram.setNumber(number);
        } else if (mode == FileOpenMode.RELOAD) {
            final Group group = Group.getSortGroup();
            histogram = group.getHistogram(util.makeLength(name,
                    Histogram.NAME_LENGTH));
            if (histogram != null) {
                if (histDim == 1) {
                    if (histNumType == NumberType.INT) {
                        histogram.setCounts(sciData.getData1d(inHDF, sizeX));
                    } else {
                        histogram.setCounts(sciData.getData1dD(inHDF, sizeX));
                    }
                } else { // 2-d
                    if (histNumType == NumberType.INT) {
                        histogram.setCounts(sciData.getData2d(inHDF, sizeX,
                                sizeY));
                    } else {
                        histogram.setCounts(sciData.getData2dD(inHDF, sizeX,
                                sizeY));
                    }
                }
            }
        } else if (mode == FileOpenMode.ADD) {
            final Group group = Group.getCurrentGroup();
            histogram = group.getHistogram(util.makeLength(name,
                    Histogram.NAME_LENGTH));
            if (histogram != null) {
                if (histDim == 1) {
                    if (histNumType == NumberType.INT) {
                        histogram.addCounts(sciData.getData1d(inHDF, sizeX));
                    } else {
                        histogram.addCounts(sciData.getData1dD(inHDF, sizeX));
                    }
                } else { // 2-d
                    if (histNumType == NumberType.INT) {
                        histogram.addCounts(sciData.getData2d(inHDF, sizeX,
                                sizeY));
                    } else {
                        histogram.addCounts(sciData.getData2dD(inHDF, sizeX,
                                sizeY));
                    }
                }
            }
        }
    }

    /**
     * Adds data objects for the virtual group of gates.
     */
    private void addGateSection() {
        synchronized (this) {
            allGates = new VirtualGroup(GATE_SECTION, FILE_SECTION);
        }
        new DataIDLabel(allGates, GATE_SECTION);
    }

    /*
     * non-javadoc: Retrieve the gates from the file.
     * 
     * @param mode whether to open or reload @throws HDFException thrown if
     * unrecoverable error occurs
     */
    private int getGates(FileOpenMode mode) throws HDFException {
        int numGates = 0;
        final StringUtilities util = StringUtilities.instance();
        Gate gate = null;
        /* get list of all VG's in file */
        final List groups = DataObject.ofType(DataObject.DFTAG_VG);
        /* get only the "gates" VG (only one element) */
        final VirtualGroup gates = VirtualGroup.ofName(groups, GATE_SECTION);
        final List annotations = DataObject.ofType(DataObject.DFTAG_DIA);
        if (gates != null) {
            numGates = gates.getObjects().size();
            final Iterator temp = gates.getObjects().iterator();
            while (temp.hasNext()) {
                final VirtualGroup currVG = (VirtualGroup) (temp.next());
                final VdataDescription vdd = (VdataDescription) (DataObject
                        .ofType(currVG.getObjects(), DataObject.DFTAG_VH)
                        .get(0));
                if (vdd == null) {
                    msgHandler
                            .warningOutln("Problem processing a VH in HDFIO!");
                } else {
                    final Vdata data = (Vdata) (DataObject.getObject(
                            DataObject.DFTAG_VS, vdd.getRef()));
                    //corresponding VS
                    final int numRows = vdd.getNumRows();
                    final String gname = currVG.getName();
                    final String hname = DataIDAnnotation.withTagRef(
                            annotations, currVG.getTag(), currVG.getRef())
                            .getNote();
                    if (mode.isOpenMode()) {
                        final String groupName = Group.getCurrentGroup()
                                .getName();
                        final String histFullName = groupName + "/"
                                + util.makeLength(hname, Histogram.NAME_LENGTH);
                        final Histogram hist = Histogram
                                .getHistogram(histFullName);
                        gate = hist == null ? null : new Gate(gname, hist);
                    } else { //reload
                        gate = Gate.getGate(util.makeLength(gname,
                                Gate.NAME_LENGTH));
                    }
                    if (gate != null) {
                        if (gate.getDimensionality() == 1) { //1-d gate
                            gate.setLimits(data.getInteger(0, 0).intValue(),
                                    data.getInteger(0, 1).intValue());
                        } else { //2-d gate
                            final Polygon shape = new Polygon();
                            for (int i = 0; i < numRows; i++) {
                                shape.addPoint(
                                        data.getInteger(i, 0).intValue(), data
                                                .getInteger(i, 1).intValue());
                            }
                            gate.setLimits(shape);
                        }
                    }
                }
            }
        }
        return numGates;
    }

    /*
     * non-javadoc: Retrieve the scalers from the file.
     * 
     * @param mode whether to open, reload or add @throws HDFException if there
     * is a problem retrieving scalers
     */
    private int getScalers(FileOpenMode mode) throws HDFException {
        int numScalers = 0;
        final VdataDescription vdd = VdataDescription.ofName(DataObject
                .ofType(DataObject.DFTAG_VH), SCALER_SECT);
        /* only the "scalers" VH (only one element) in the file */
        if (vdd != null) {
            /* get the VS corresponding to the given VH */
            final Vdata data = (Vdata) (DataObject.getObject(
                    DataObject.DFTAG_VS, vdd.getRef()));
            numScalers = vdd.getNumRows();
            for (int i = 0; i < numScalers; i++) {
                final String sname = data.getString(i, 1);
                final Scaler scaler = mode.isOpenMode() ? new Scaler(sname,
                        data.getInteger(i, 0).intValue()) : Scaler
                        .getScaler(sname);
                if (scaler != null) {
                    final int fileValue = data.getInteger(i, 2).intValue();
                    if (mode == FileOpenMode.ADD) {
                        scaler.setValue(scaler.getValue() + fileValue);
                    } else {
                        scaler.setValue(fileValue);
                    }
                }
            }
        }
        return numScalers;
    }

    /*
     * non-javadoc: retrieve the parameters from the file
     * 
     * @param mode whether to open or reload @throws HDFException if an error
     * occurs reading the parameters
     */
    private int getParameters(FileOpenMode mode) throws HDFException {
        int numParams = 0;
        final VdataDescription vdd = VdataDescription.ofName(DataObject
                .ofType(DataObject.DFTAG_VH), PARAMETERS);
        /* only the "parameters" VH (only one element) in the file */
        if (vdd != null) {
            /* Get corresponding VS for this VH */
            final Vdata data = (Vdata) (DataObject.getObject(
                    DataObject.DFTAG_VS, vdd.getRef()));
            numParams = vdd.getNumRows();
            for (int i = 0; i < numParams; i++) {
                final String pname = data.getString(i, 0);
                /* make if OPEN, retrieve if RELOAD */
                final DataParameter param = mode.isOpenMode() ? new DataParameter(
                        pname)
                        : DataParameter.getParameter(pname);
                if (param != null) {
                    param.setValue(data.getFloat(i, 1).floatValue());
                }
            }
        }
        return numParams;
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


    private void outln(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                msgHandler.messageOutln(msg);
            }
        });
    }

    /**
     * Class to hold histogram properties while we decide if we should load
     * them.
     */
    public class HistogramAttributes {

        String name;

        String title;

        int number;

        int sizeX;

        int sizeY;

        int histDim;

        byte histNumType;

        Object dataArray; //generic data array

        Object errorArray;

        private HistogramAttributes() {
            super();
        }

        /**
         * 
         * @return the name of the histogram
         */
        public String getName() {
            return name;
        }
    }
}