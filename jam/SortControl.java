package jam;
import jam.global.GlobalException;
import jam.global.GoodThread;
import jam.global.JamProperties;
import jam.global.MessageHandler;
import jam.global.RunInfo;
import jam.io.ExtensionFileFilter;
import jam.sort.Controller;
import jam.sort.SortDaemon;
import jam.sort.SortException;
import jam.sort.StorageDaemon;
import jam.sort.stream.EventException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Class to control the offline sort process
 * Allows you to enter the list of files to sort
 * an the output pre-sorted file if you have one
 *
 * @author Dale Visser and Ken Swartz
 * @version 1.0
 */
class SortControl implements Controller, ActionListener, ItemListener {

	private final JamMain jamMain;
	private final MessageHandler msgHandler;

	/* daemon threads */
	private StorageDaemon dataInpDaemon;
	private StorageDaemon dataOutDaemon;
	private SortDaemon sortDaemon;

	private File lastFile; //last file referred to in a JFileChooser
	private File fileOut; //file name for output
	private File outDirectory; //directory we last output files to.

	private boolean setupLock;
	private boolean writeEvents;

	/**
	 *  Dialog box widget
	 */
	private JDialog d;

	/**
	 * Text field for output file
	 */
	private JTextField textOutFile;

	/** check box for writing out events */
	private JCheckBox cout;

	private JPanel pdiskfiles;

	private JList listEventFiles;
	private DefaultListModel eventFileModel;

	private JButton addfile,
		addDir,
		loadlist,
		remove,
		removeAll,
		savelist;

	/** 
	  * button to get file brower 
	  */
	private JButton bbrowse;

	/** 
	  *control button for begin sort 
	  */
	private JButton bbegin;

	/** 
	  * control button for end sort 
	  */
	private JButton bend;

	String defaultEvents;
	String defaultOutputFile;

	public SortControl(JamMain jamMain, MessageHandler msgHandler) {
		this.jamMain = jamMain;
		this.msgHandler = msgHandler;
		defaultEvents =
			JamProperties.getPropString(JamProperties.EVENT_OUTPATH);
		defaultOutputFile =
			JamProperties.getPropString(JamProperties.EVENT_OUTFILE);
		d = new JDialog(jamMain, "Sorting ", false);
		d.setResizable(true);
		d.setLocation(20, 50);
		Container cd = d.getContentPane();
		cd.setLayout(new BorderLayout(10, 10));
		JPanel ptop = new JPanel();
		ptop.setLayout(new GridLayout(0, 1, 5, 5));
		cd.add(ptop, BorderLayout.NORTH);
		pdiskfiles = new JPanel();
		pdiskfiles.setLayout(new BorderLayout());
		eventFileModel = new DefaultListModel();
		listEventFiles = new JList(eventFileModel);
		listEventFiles.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		pdiskfiles.add(new JLabel("Event Files to Sort",JLabel.RIGHT), 
		BorderLayout.NORTH);
		pdiskfiles.add(new JScrollPane(listEventFiles), BorderLayout.CENTER);
		cd.add(pdiskfiles, BorderLayout.CENTER);

		JPanel ef = new JPanel(new GridLayout(0, 1));

		addfile = new JButton("Add File");
		addfile.setActionCommand("addfile");
		addfile.addActionListener(this);
		ef.add(addfile);

		addDir = new JButton("Add Directory");
		addDir.setActionCommand("addDir");
		addDir.addActionListener(this);
		ef.add(addDir);

		remove = new JButton("Remove File");
		remove.setActionCommand("remove");
		remove.addActionListener(this);
		ef.add(remove);

		removeAll = new JButton("Remove All");
		removeAll.setActionCommand("removeall");
		removeAll.addActionListener(this);
		ef.add(removeAll);

		loadlist = new JButton("Load List");
		loadlist.setActionCommand("loadlist");
		loadlist.addActionListener(this);
		ef.add(loadlist);

		savelist = new JButton("Save List");
		savelist.setActionCommand("savelist");
		savelist.addActionListener(this);
		ef.add(savelist);

		cd.add(ef, BorderLayout.WEST);

		JPanel ple = new JPanel();
		ple.setLayout(new GridLayout(0, 1, 5, 5));
		cd.add(ple, BorderLayout.EAST);

		JPanel pbottom = new JPanel();
		pbottom.setLayout(new GridLayout(0, 1, 5, 2));
		cd.add(pbottom, BorderLayout.SOUTH);

		// panel for output file
		JPanel pout = new JPanel(new BorderLayout());
		pbottom.add(pout);
		cout = new JCheckBox("Output Events to File:", false);
		cout.addItemListener(this);
		pout.add(cout,BorderLayout.WEST);

		textOutFile =
			new JTextField(defaultEvents + File.separator + defaultOutputFile);
		textOutFile.setColumns(28);
		textOutFile.setEnabled(false);
		pout.add(textOutFile,BorderLayout.CENTER);

		bbrowse = new JButton("Browse");
		bbrowse.setActionCommand("bfileout");
		bbrowse.addActionListener(this);
		bbrowse.setEnabled(false);
		pout.add(bbrowse,BorderLayout.EAST);

		//panel with begin and end bottoms	
		JPanel pb = new JPanel();
		pb.setLayout(new GridLayout(1,0));

		bbegin = new JButton("Begin");
		bbegin.setActionCommand("begin");
		bbegin.setBackground(Color.green);
		bbegin.setEnabled(false);
		bbegin.addActionListener(this);
		pb.add(bbegin);

		bend = new JButton("End/Cancel");
		bend.setActionCommand("end");
		bend.setBackground(Color.red);
		bend.addActionListener(this);
		pb.add(bend);

		pbottom.add(pb);

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.dispose();
			}
		});
		lastFile = new File(defaultEvents); //default directory
		//call to private version to avoid breaking if subclassed
		//setDevice(DISK); //initial mode is from disk
		writeEvents = false; //don't write out events
		d.pack();
	}

	/**
	 * method to show dialog box
	 */
	public void show() {
		d.show();
	}

	/**
	 * action performed on widget in dialog box
	 *
	 */
	public void actionPerformed(ActionEvent ae) {
		final String command = ae.getActionCommand();
		try {
			if (command == "addfile") {
				addEventFile();
			} else if (command == "addDir") {
				addDirectory();
			} else if (command == "remove") {
				removeItem();
			} else if (command == "removeall") {
				removeAllItems();
			} else if (command == "savelist") {
				Savelist();
			} else if (command == "loadlist") {
				Loadlist();
			} else if (command == "begin") {
				loadNames();
				lockFields(true);
				beginSort();
			} else if (command == "end") {
				endSort();
				msgHandler.warningOutln(
					"Ended offline sorting before reading all events.");
				//lockFields(false);
			} else if (command == "bfileout") {
				textOutFile.setText(getOutFile().getPath());
			}
		} catch (SortException se) {
			msgHandler.errorOutln(se.getMessage());
		} catch (EventException ee) {
			msgHandler.errorOutln(ee.getMessage());
		} catch (JamException je) {
			msgHandler.errorOutln(je.getMessage());
		} catch (GlobalException ge) {
			msgHandler.errorOutln(ge.getMessage());
		}
	}

	/**
	 * Recieves events from the check box.
	 *
	 */
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getItemSelectable() == cout) {
			setWriteEvents(cout.isSelected());
		}
	}
	
	void setWriteEvents(boolean state){
		textOutFile.setEnabled(state);
		bbrowse.setEnabled(state);
		writeEvents = state;
	}

	/**
	 * Setup up called by SetupSortOff  
	 *
	 */
	public void setup(
		SortDaemon sortDaemon,
		StorageDaemon fromDaemon,
		StorageDaemon toDaemon) {
		this.sortDaemon = sortDaemon;
		this.dataInpDaemon = fromDaemon;
		this.dataOutDaemon = toDaemon;
		bbegin.setEnabled(true);
	}

	/**
	 * Load the name of objects entered in dialog box
	 * give the list to storage deamon
	 */
	private void loadNames() throws JamException, SortException {
		final List fileList = new Vector(eventFileModel.getSize());
		for (int count = 0; count < eventFileModel.getSize(); count++) {
			final File f=(File)eventFileModel.get(count);
			fileList.add(f);	
		}
		/* tell storage daemon list of files */
		dataInpDaemon.setEventInputList(fileList);
		/* save output file */
		fileOut = new File(textOutFile.getText().trim());
		msgHandler.messageOutln("Loaded list of sort files");
	}

	/**
	 * start sorting offline
	 */
	public void beginSort()
		throws SortException, EventException, GlobalException {
		RunInfo.runNumber = 999;
		RunInfo.runTitle = "Pre-sorted data";
		RunInfo.runStartTime = new java.util.Date();
		if (writeEvents) {
			sortDaemon.setWriteEnabled(true);
			dataOutDaemon.openEventOutputFile(fileOut);
			dataOutDaemon.writeHeader();
		} else {
			sortDaemon.setWriteEnabled(false);
		}
		msgHandler.messageOutln("Starting sorting from Disk");
		bbegin.setEnabled(false);
		sortDaemon.setState(GoodThread.RUN);
		jamMain.setRunState(RunState.ACQ_ON);
	}

	/**
	 * stop offline sorting
	 *
	 */
	private void endSort()
		throws JamException, SortException, GlobalException {
		sortDaemon.cancelOfflineSorting();
		if (!dataInpDaemon.closeEventInputListFile()) {
			msgHandler.errorOutln(
				"Closing sort input event file: "
					+ dataInpDaemon.getEventInputFileName());
		}
		if (writeEvents) {
			dataOutDaemon.closeEventOutputFile();
			msgHandler.messageOutln(
				"Closed pre-sorted file: " + fileOut.getPath());
		}
		msgHandler.warningOutln(
				"Stopped sorting from disk before all events were read.");
		jamMain.setRunState(RunState.ACQ_OFF);
	}

	/**
	 * Called at the start of a new sort thread by
	 * the sort thread. All it does is suspend the
	 * <code>SortDaemon</code> thread, to make the
	 * offline sorting loop wait at its beginning for
	 * the thread to be resumed when the user requests
	 * the sort to begin.
	 */
	public void atSortStart() throws GlobalException {
		sortDaemon.setState(GoodThread.SUSPEND);
	}

	/**
	 * Called by sorter after startup looking to see if there is a next file
	 * to sort. If there is a nex file we tell <code>StorageDaemon</code> to open it
	 * if <code>storageDaemon</code> can open it we return true.
	 *
	 * @return <code>true</code> if there is a next event file to sort
	 */
	public boolean isSortNext() {
		boolean sortNext = false;
		if (!dataInpDaemon.closeEventInputListFile()) {
			msgHandler.errorOutln(
				"Could not close file: "
					+ dataInpDaemon.getEventInputFileName());
		}
		if (dataInpDaemon.hasMoreFiles()) {
			if (dataInpDaemon.openEventInputListFile()) {
				msgHandler.messageOutln(
					"Sorting next file: "
						+ dataInpDaemon.getEventInputFileName());
				msgHandler.messageOutln(
					"  Run number: "
						+ RunInfo.runNumber
						+ " title: "
						+ RunInfo.runTitle);
				sortNext = true;
			} else {
				msgHandler.errorOutln(
					"Could not open file: "
						+ dataInpDaemon.getEventInputFileName());
				sortNext = true; //try next file anyway
			}
		}
		return sortNext;
	}

	/**
	 * Called back by sorter when sort encounters a end-run-marker.
	 * Tell StorageDaemon to close file. 
	 * Tells user sorting is done and unlocks fields so that 
	 * new files can be input to sort.
	 * 
	 */
	public void atSortEnd() {
		try {
			msgHandler.messageOutln("Sorting all done");
			jamMain.setRunState(RunState.ACQ_OFF);
			if (!dataInpDaemon.closeEventInputListFile()) {
				msgHandler.errorOutln("Couldn't close file [SortControl]");
			}
			if (writeEvents) {
				dataOutDaemon.closeEventOutputFile();
				msgHandler.messageOutln(
					"Closed pre-sorted file: " + fileOut.getPath());
			}
			bbegin.setEnabled(true);
			//Toolkit.getDefaultToolkit().beep();
			lockFields(false);
		} catch (SortException se) {
			msgHandler.errorOutln(
				"Unable to close event output file [SortControl]");
		}
	}

	/**
	 * Method not implement for SortControl,
	 * called by sorting when at end of writing file.
	 * implemented in RunControl
	 */
	public void atWriteEnd() {
		/* does nothing  */
	}

	/**
	 * browse for event files
	 */
	private void addEventFile() {
		JFileChooser fd = new JFileChooser(lastFile);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "evn" },
				"Event Files (*.evn)"));
		int option = fd.showOpenDialog(jamMain);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			lastFile = fd.getSelectedFile(); //save current directory
			eventFileModel.addElement(fd.getSelectedFile());
		}
	}

	/**
	 * add all files in a directory to sort
	 *
	 */
	private void addDirectory() {
		final JFileChooser fd = new JFileChooser(lastFile);
		fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final int option = fd.showOpenDialog(jamMain);
		/* save current values */
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			lastFile = fd.getSelectedFile(); //save current directory
			addEventFile(lastFile);
		}
	}
	
	void addEventFile(File f){
		if (f != null && f.exists()){
			final ExtensionFileFilter ff =
				new ExtensionFileFilter(
					new String[] { "evn" },
					"Event Files (*.evn)");
			if (f.isFile() && ff.accept(f)){
				eventFileModel.addElement(f);
			}
			if (f.isDirectory()){
				File[] dirArray = f.listFiles();
				for (int i = 0; i < dirArray.length; i++) {
					if (ff.accept(dirArray[i]))
						eventFileModel.addElement(dirArray[i]);
				}
			}
		}
	}

	/**
	 * remove a item from sort list
	 */
	private void removeItem() {
		Object[] removeList = listEventFiles.getSelectedValues();
		for (int i = 0; i < removeList.length; i++) {
			eventFileModel.removeElement(removeList[i]);
		}
	}

	/**
	 * remove all items from sort list 
	 *
	 */
	private void removeAllItems() {
		eventFileModel.removeAllElements();
	}

	/**
	 * save list of items to sort
	 */
	private void Savelist() throws JamException {
		JFileChooser fd = new JFileChooser(lastFile);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "lst" },
				"List Files (*.lst)"));
		int option = fd.showSaveDialog(jamMain);
		/* save current values */
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			lastFile = fd.getSelectedFile(); //save current directory
		}
		try {
				FileWriter saveStream = new FileWriter(fd.getSelectedFile());
				for (int i = 0; i < eventFileModel.size(); i++) {
					final File f=(File)eventFileModel.elementAt(i);
					saveStream.write(f.getAbsolutePath());
					saveStream.write("\n");
				}
				saveStream.close();
		} catch (IOException ioe) {
			throw new JamException("Unable to save list to file, open file [SortControl]");
		}
	}

	/**
	 * load a list of items to sort from a file
	 *
	 */
	private void Loadlist() throws JamException {
		JFileChooser fd = new JFileChooser(lastFile);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "lst" },
				"List Files (*.lst)"));
		int option = fd.showOpenDialog(jamMain);
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			readList(fd.getSelectedFile());
		}
	}
	
	void readList(File f) throws JamException {
		lastFile=f;
		try {
			BufferedReader br =
				new BufferedReader(new FileReader(lastFile));
			String listItem;
			do {
				listItem = br.readLine();
				if (listItem != null) {
					final File fEvn=new File(listItem);
					eventFileModel.addElement(fEvn);
				}
			} while (listItem != null);
			br.close();
		} catch (IOException ioe) {
			throw new JamException("Unable to load list, open file [SortControl]");
		}
	}

	/**
	 * Is the Browse for the File Output Name 
	 *
	 */
	private File getOutFile() {
		File rval = null; //default return value
		JFileChooser fd = new JFileChooser(outDirectory);
		fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd.setFileFilter(
			new ExtensionFileFilter(
				new String[] { "evn" },
				"Event Files (*.evn)"));
		int option = fd.showOpenDialog(jamMain);
		/* save current values */
		if (option == JFileChooser.APPROVE_OPTION
			&& fd.getSelectedFile() != null) {
			outDirectory = fd.getSelectedFile(); //save current directory
			rval = outDirectory;
		}
		return rval;
	}
	
	void setEventOutput(File f){
		outDirectory=f;
		textOutFile.setText(f.getAbsolutePath());
		setWriteEvents(true);
	}

	/** 
	 * Lock the file and record input list while sorting
	 * This method is called when sorting is actived to lock fields
	 * again when done to unlock fields
	 */
	private void lockFields(boolean lock) {
		setupLock = lock;
		final boolean notLock = !lock;
		addfile.setEnabled(notLock);
		addDir.setEnabled(notLock);
		remove.setEnabled(notLock);
		loadlist.setEnabled(notLock);
		savelist.setEnabled(notLock);
		removeAll.setEnabled(notLock);
		textOutFile.setEditable(notLock);
		cout.setEnabled(notLock);
	}
}
