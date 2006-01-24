package jam.sort.control;

import jam.JamException;
import jam.data.DataBase;
import jam.global.BroadcastEvent;
import jam.global.GoodThread;
import jam.global.JamProperties;
import jam.global.RTSI;
import jam.global.SortMode;
import jam.sort.DiskDaemon;
import jam.sort.SortDaemon;
import jam.sort.SortException;
import jam.sort.stream.AbstractEventInputStream;
import jam.sort.stream.AbstractEventOutputStream;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

;

/**
 * Class to setup the offline sort process.
 * 
 * @author Dale Visser
 * @author Ken Swartz
 * @version 1.1
 */
public final class SetupSortOff extends AbstractSetup {

	private final class ApplyActionListener implements ActionListener {

		ApplyActionListener() {
			super();
		}

		/**
		 * Perform setup tasks when OK or APPLY is clicked.
		 * 
		 * @param event
		 *            the event created by clicking OK or APPLY
		 */
		public void actionPerformed(final ActionEvent event) {
			doApply(bok.equals(event.getSource()));
		}
	}

	private final static String APPLY = "Apply";

	private final static String CANCEL = "Cancel";

	private static SetupSortOff instance = null;

	private final static String OK_TEXT = "OK";

	private final static String SETUP_LOCKED = "Setup Locked";

	/**
	 * Returns the only instance of this class.
	 * 
	 * @return the only instance of this class
	 */
	public static SetupSortOff getInstance() {
		if (instance == null) {
			instance = new SetupSortOff();
		}
		return instance;
	}

	private transient final JButton bok, bapply;

	/* dialog box widgets */
	private transient final JCheckBox checkLock;

	final transient private DisplayCounters dispCount;

	/** Input stream, how tells how to read an event */
	private transient AbstractEventInputStream eventInput;

	/** Output stream, tells how to write an event */
	private transient AbstractEventOutputStream eventOutput;

	private transient final JComboBox inChooser, outChooser;

	// final transient private MessageHandler msgHandler;

	/* handles we need */
	final transient private SortControl sortControl;

	private transient SortDaemon sortDaemon;

	private SetupSortOff() {
		super("Setup Offline");
		final String defSortRout = JamProperties
				.getPropString(JamProperties.SORT_ROUTINE);
		final String defSortPath = JamProperties
				.getPropString(JamProperties.SORT_CLASSPATH);
		final String defInStream = JamProperties
				.getPropString(JamProperties.EVENT_INSTREAM);
		final String defOutStream = JamProperties
				.getPropString(JamProperties.EVENT_OUTSTREAM);
		final boolean useDefault = (defSortPath
				.equals(JamProperties.DEFAULT_SORT_CLASSPATH));
		sortControl = SortControl.getInstance();
		dispCount = DisplayCounters.getSingletonInstance();
		final Container contents = dialog.getContentPane();
		dialog.setResizable(false);
		final int posx = 20;
		final int posy = 50;
		dialog.setLocation(posx, posy);
		contents.setLayout(new BorderLayout(5, 5));
		final int space = 5;
		final JPanel pNorth = new JPanel(new GridLayout(0, 1, space, space));
		contents.add(pNorth, BorderLayout.NORTH);
		final JPanel pradio = new JPanel(new FlowLayout(FlowLayout.CENTER,
				space, space));
		final ButtonGroup pathType = new ButtonGroup();
		pathType.add(defaultPath);
		pathType.add(specify);
		pradio.add(defaultPath);
		pradio.add(specify);
		pNorth.add(pradio);
		/* Labels */
		final JPanel pLabels = new JPanel(new GridLayout(0, 1, space, space));
		pLabels.setBorder(new EmptyBorder(2, 10, 0, 0)); // down so browse
		// button lines up
		contents.add(pLabels, BorderLayout.WEST);
		pLabels.add(new JLabel("Sort classpath", SwingConstants.RIGHT));
		pLabels.add(new JLabel("Sort Routine", SwingConstants.RIGHT));
		final JLabel lis = new JLabel("Event input stream",
				SwingConstants.RIGHT);
		pLabels.add(lis);
		final JLabel los = new JLabel("Event output stream",
				SwingConstants.RIGHT);
		pLabels.add(los);
		/* Entry fields */
		final JPanel pEntry = new JPanel(new GridLayout(0, 1, space, space));
		pEntry.setBorder(new EmptyBorder(2, 0, 0, 0));// down so browse button
		// lines up
		contents.add(pEntry, BorderLayout.CENTER);
		/* Path */
		pEntry.add(textSortPath);
		/* Sort class */
		selectSortRoutine(defSortRout, useDefault);
		pEntry.add(sortChoice);
		/* Input stream */
		final RTSI rtsi = RTSI.getSingletonInstance();
		Set<Class<?>> lhs = new LinkedHashSet<Class<?>>(rtsi.find(
				"jam.sort.stream", AbstractEventInputStream.class, false));
		lhs.remove(AbstractEventInputStream.class);
		inChooser = new JComboBox(lhs.toArray());
		inChooser.setToolTipText("Select input event data format.");
		selectName(inChooser, lhs, defInStream);
		pEntry.add(inChooser);
		// Output stream
		lhs = new LinkedHashSet<Class<?>>(rtsi.find("jam.sort.stream",
				AbstractEventOutputStream.class, false));
		lhs.remove(AbstractEventOutputStream.class);
		outChooser = new JComboBox(lhs.toArray());
		outChooser.setToolTipText("Select output event format.");
		selectName(outChooser, lhs, defOutStream);
		pEntry.add(outChooser);
		final JPanel pBrowse = new JPanel(new GridLayout(4, 1, 0, 0));
		pBrowse.setBorder(new EmptyBorder(0, 0, 0, 10));
		contents.add(pBrowse, BorderLayout.EAST);
		pBrowse.add(bbrowsef);
		/* Button Panel */
		final JPanel pbutton = new JPanel(new FlowLayout(FlowLayout.CENTER));
		final JPanel panelB = new JPanel();
		panelB.setLayout(new GridLayout(1, 0, space, space));
		pbutton.add(panelB);
		contents.add(pbutton, BorderLayout.SOUTH);
		bok = new JButton(OK_TEXT);
		panelB.add(bok);
		ApplyActionListener aal = new ApplyActionListener();
		bok.addActionListener(aal);
		bapply = new JButton(APPLY);
		panelB.add(bapply);
		bapply.addActionListener(aal);
		final JButton bcancel = new JButton(new AbstractAction(CANCEL) {
			public void actionPerformed(final ActionEvent event) {
				dialog.dispose();
			}
		});
		panelB.add(bcancel);
		checkLock = new JCheckBox(SETUP_LOCKED, false);
		checkLock.setEnabled(false);
		checkLock.addItemListener(new ItemListener() {
			public void itemStateChanged(final ItemEvent event) {
				if (!checkLock.isSelected()) {
					try {
						resetSort();
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		});
		panelB.add(checkLock);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.pack();
	}

	protected void doApply(final boolean dispose) {
		try {
			if (STATUS.canSetup()) {
				resetSort();// clear current data areas and kill daemons
				loadSorter();
				loadEventInput();
				loadEventOutput();
				LOGGER.info("Loaded sort class '"
						+ sortRoutine.getClass().getName()
						+ "', event instream class '"
						+ eventInput.getClass().getName()
						+ "', and event outstream class '"
						+ eventOutput.getClass().getName() + "'");
				if (sortRoutine != null) {
					setupSort(); // create data areas and daemons
					LOGGER.info("Daemons and dialogs initialized.");
				}
				STATUS.selectFirstSortHistogram();
				if (dispose) {
					dialog.dispose();
				}
			} else {
				final JamException exception = new JamException(
						"Can't set up sorting, mode locked.");
				LOGGER.throwing("SetupSortOff", "doApply", exception);
				throw exception;
			}
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}

	private void loadEventInput() throws JamException {
		try {// create new event input stream class
			synchronized (this) {
				eventInput = (AbstractEventInputStream) ((Class) inChooser
						.getSelectedItem()).newInstance();
			}
			eventInput.setConsoleExists(true);
		} catch (InstantiationException ie) {
			final String msg = classname
					+ "Cannot instantize event input stream: "
					+ inChooser.getSelectedItem();
			LOGGER.log(Level.SEVERE, msg, ie);
			throw new JamException(msg, ie);
		} catch (IllegalAccessException iae) {
			final String msg = classname + "Cannot access event input stream: "
					+ inChooser.getSelectedItem();
			LOGGER.log(Level.SEVERE, msg, iae);
			throw new JamException(msg, iae);
		}
	}

	private void loadEventOutput() throws JamException {
		try {// create new event output stream class
			synchronized (this) {
				eventOutput = (AbstractEventOutputStream) ((Class) outChooser
						.getSelectedItem()).newInstance();
			}
		} catch (InstantiationException ie) {
			throw new JamException(classname
					+ "Cannot instantize event output stream: "
					+ eventOutput.getClass().getName());
		} catch (IllegalAccessException iae) {
			throw new JamException(classname
					+ "Cannot access event output stream: "
					+ eventOutput.getClass().getName());
		}

	}

	protected void lockMode(final boolean lock) {
		final boolean notLock = !lock;
		checkLock.setEnabled(lock);
		checkLock.setSelected(lock);
		textSortPath.setEnabled(notLock);
		inChooser.setEnabled(notLock);
		outChooser.setEnabled(notLock);
		bok.setEnabled(notLock);
		bapply.setEnabled(notLock);
		specify.setEnabled(notLock);
		defaultPath.setEnabled(notLock);
		sortChoice.setEnabled(notLock);
		if (lock) {
			STATUS.setSortMode(SortMode.OFFLINE, sortRoutine.getClass()
					.getName());
			bbrowsef.setEnabled(false);
		} else {
			STATUS.setSortMode(SortMode.NO_SORT, "No Sort");
			bbrowsef.setEnabled(specify.isSelected());
		}
	}

	/**
	 * Resets offline data aquisition. Kills sort daemon. Clears all data areas:
	 * histograms, gates, scalers and monitors.
	 */
	private void resetSort() {
		if (sortDaemon != null) {
			sortDaemon.setState(GoodThread.State.STOP);
			sortDaemon.setSorter(null);
		}
		sortRoutine = null;
		DataBase.getInstance().clearAllLists();
		BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_NEW);
		lockMode(false);
	}

	private void selectSortRoutine(final String srName, final boolean useDefault) {
		final java.util.List sortList = setChooserDefault(useDefault);
		final Iterator iter = sortList.iterator();
		while (iter.hasNext()) {
			final Class clazz = (Class) iter.next();
			final String name = clazz.getName();
			if (name.equals(srName)) {
				sortChoice.setSelectedItem(clazz);
				break;
			}
		}
	}

	protected void setupSort() throws SortException, JamException {
		initializeSorter();
		/* setup sorting */
		synchronized (this) {
			sortDaemon = new SortDaemon(sortControl);
		}
		sortDaemon.setup(eventInput, sortRoutine.getEventSize());
		sortDaemon.setSorter(sortRoutine);
		/* eventInputStream to use get event size from sorting routine */
		eventInput.setEventSize(sortRoutine.getEventSize());
		eventInput.setBufferSize(sortRoutine.getBufferSize());
		/* give sortroutine output stream */
		eventOutput.setEventSize(sortRoutine.getEventSize());
		eventOutput.setBufferSize(sortRoutine.getBufferSize());
		sortRoutine.setEventOutputStream(eventOutput);
		/* always setup diskDaemon */
		final DiskDaemon diskDaemon = new DiskDaemon(sortControl);
		diskDaemon.setupOff(eventInput, eventOutput);
		/* tell run control about all, disk always to device */
		sortControl.setup(sortDaemon, diskDaemon, diskDaemon);
		/* tell status to setup */
		dispCount.setupOff(sortDaemon, diskDaemon);
		/*
		 * start sortDaemon which is then suspended by Sort control until files
		 * entered
		 */
		sortDaemon.start();
		/* lock setup */
		lockMode(true);
	}

	/**
	 * Provided so setup offline sort can be scriptable.
	 * 
	 * @param classPath path to sort routine classpath base
	 * @param sortName name of sort routine class
	 * @param inStream event input stream class
	 * @param outStream event output stream class
	 */
	public void setupSort(final File classPath, final String sortName,
			final Class inStream, final Class outStream) {
		setSortClassPath(classPath);
		selectSortRoutine(sortName, false);
		inChooser.setSelectedItem(inStream);
		outChooser.setSelectedItem(outStream);
		doApply(false);
	}
}