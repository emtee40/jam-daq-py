package jam.io;

import jam.data.Histogram;
import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.global.RTSI;
import jam.util.CollectionsUtil;
import jam.util.FileUtilities;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

/**
 * Dialog for exporting lists of histograms. Searches <code>jam.io</code> for
 * all classes extending <code>jam.io.ImpExp</code>.
 * 
 * @author <a href=mailto:dale@visser.name>Dale Visser </a>
 */
public class BatchExport extends JDialog implements ActionListener, Observer {

	private final Map exportMap = Collections.synchronizedMap(new HashMap());

	private final JTextField txtDirectory = new JTextField(System.getProperty("user.home"), 40);

	private final JComboBox cbHist = new JComboBox();

	private final JList lstHists = new JList(new DefaultListModel());

	private final JButton bExport = new JButton("Export");

	private final MessageHandler console;

	private File lastListFile = null;

	/**
	 * Constructs a new batch histogram exporter.
	 * 
	 * @param msgHandler console to print messages to
	 */
	public BatchExport(MessageHandler msgHandler) {
		super(JamStatus.getSingletonInstance().getFrame(), "Batch Histogram Export");
		console = msgHandler;
		Broadcaster broadcaster = Broadcaster.getSingletonInstance();
		broadcaster.addObserver(this);
		buildGUI();
		setup();
	}

	/**
	 * Construct the GUI
	 */
	private void buildGUI() {
		final Container contents = getContentPane();
		contents.setLayout(new BorderLayout());
		final int CHOOSER_SIZE = 200;
		/* Top Panel with chooser */
		final JPanel pTop = new JPanel(new GridLayout(0, 1, 5, 5));
		pTop.setBorder(new EmptyBorder(10, 10, 0, 10));
		contents.add(pTop, BorderLayout.NORTH);
		final JPanel pChooser = new JPanel(new FlowLayout(FlowLayout.CENTER,
				10, 10));
		pTop.add(pChooser);
		pChooser.add(new JLabel("Add Histogram"));
		cbHist.setActionCommand("select");
		final Dimension dim = cbHist.getPreferredSize();
		dim.width = CHOOSER_SIZE;
		cbHist.setPreferredSize(dim);
		pChooser.add(cbHist);
		/* Button panel on left */
		final JPanel pButtons = new JPanel(new GridLayout(0, 1, 5, 2));
		pButtons.setBorder(new EmptyBorder(10, 10, 10, 10));
		contents.add(pButtons, BorderLayout.WEST);
		final JButton bAddAllHist = new JButton("Add All");
		bAddAllHist.setToolTipText("Adds all 1 dimension histograms.");
		bAddAllHist.setActionCommand("addall");
		bAddAllHist.addActionListener(this);
		pButtons.add(bAddAllHist);
		final JButton bRemoveName = new JButton("Remove Selected");
		bRemoveName.setToolTipText("Removes selected histograms");
		bRemoveName.setActionCommand("removeselect");
		bRemoveName.addActionListener(this);
		pButtons.add(bRemoveName);
		final JButton bRemoveAll = new JButton("Remove All");
		bRemoveAll.setToolTipText("Remove all histograms.");
		bRemoveAll.setActionCommand("removeall");
		bRemoveAll.addActionListener(this);
		pButtons.add(bRemoveAll);
		final JButton bLoadList = new JButton("Load List");
		bLoadList.setToolTipText("Load list of histograms from file.");
		bLoadList.setActionCommand("loadlist");
		bLoadList.addActionListener(this);
		pButtons.add(bLoadList);
		final JButton bSaveList = new JButton("Save List");
		bSaveList.setToolTipText("Save list of histograms to file.");
		bSaveList.setActionCommand("savelist");
		bSaveList.addActionListener(this);
		pButtons.add(bSaveList);
		/* List of histograms */
		final JPanel pList = new JPanel(new GridLayout(1, 1));
		pList.setBorder(new EmptyBorder(10, 0, 10, 10));
		pList.setToolTipText("List of histograms to export.");
		lstHists
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		pList.add(new JScrollPane(lstHists));
		contents.add(pList, BorderLayout.CENTER);
		/* Lower button panel */
		final JPanel pBottom = new JPanel(new GridLayout(0, 1, 5, 5));
		contents.add(pBottom, BorderLayout.SOUTH);
		/* Options panel */
		final JPanel pOptions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		pBottom.add(pOptions);
		final JPanel pBtnOpn = new JPanel(new GridLayout(1, 0, 10, 0));
		pOptions.add(pBtnOpn);
		final ButtonGroup options = new ButtonGroup();
		final Iterator iter=getClasses().iterator();
		while (iter.hasNext()) {
			final AbstractImpExp impExp = (AbstractImpExp) iter.next();
			final String desc = impExp.getFormatDescription();
			final JRadioButton exportChoice = new JRadioButton(desc);
			exportChoice.setToolTipText("Select to export in " + desc
					+ " format.");
			exportChoice.addActionListener(this);
			options.add(exportChoice);
			pBtnOpn.add(exportChoice);
			exportMap.put(exportChoice, impExp);
		}
		/* Directory panel */
		final JPanel pDirectory = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		pDirectory.add(new JLabel("Directory"));
		txtDirectory
				.setToolTipText("Directory to write exported histogram files.");
		pDirectory.add(txtDirectory);
		final JButton bBrowse = new JButton("Browse\u2026");
		bBrowse.setActionCommand("browse");
		bBrowse.addActionListener(this);
		pDirectory.add(bBrowse);
		pBottom.add(pDirectory);
		/* Button panel */
		final JPanel pButton = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		bExport.setToolTipText("Export selected histograms.");
		bExport.setEnabled(false);
		bExport.setActionCommand("export");
		bExport.addActionListener(this);
		pButton.add(bExport);
		final JButton bCancel = new JButton("Cancel");
		bCancel.setToolTipText("Close this dialog.");
		bCancel.setActionCommand("cancel");
		bCancel.addActionListener(this);
		pButton.add(bCancel);
		pBottom.add(pButton);
		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				setup();
			}
		});
		pack();
		setResizable(false);
	}

	private List getClasses() {
		final List rval=new ArrayList();
		final String here = getClass().getName() + ".getClasses(): ";
		final Set set = RTSI.find("jam.io", AbstractImpExp.class, false);
		set.remove(AbstractImpExp.class);
		for (final Iterator it = set.iterator(); it.hasNext();) {
			final Class temp = (Class) it.next();
			try {
				final AbstractImpExp impExp = (AbstractImpExp) temp.newInstance();
				if (impExp.batchExportAllowed()) {
					rval.add(impExp);
				}
			} catch (InstantiationException e) {
				console.errorOutln(here + e.getMessage());
			} catch (IllegalAccessException e) {
				console.errorOutln(here + e.getMessage());
			}
		}
		return rval;
	}
	
	public void actionPerformed(ActionEvent actionEvent) {
		final String command = actionEvent.getActionCommand();
		if (command.equals("select")) {
			addSelectedHist();
		} else if (command.equals("addall")) {
			addAllHists();
		} else if (command.equals("removeselect")) {
			removeSelectedHist();
		} else if (command.equals("removeall")) {
			removeAllHists();
		} else if (command.equals("browse")) {
			browseForDir();
		} else if (command.equals("loadlist")) {
			loadList();
		} else if (command.equals("savelist")) {
			saveList();
		} else if (command.equals("cancel")) {
			dispose();
		} else if (command.equals("export")) {
			export();
		}
		setExportEnable();
	}

	/**
	 * Enable export button
	 *  
	 */
	private void setExportEnable() {
		boolean selected = false;
		final Iterator iter = exportMap.keySet().iterator();
		while (iter.hasNext()) {
			selected |= ((AbstractButton) iter.next()).isSelected();
		}
		selected &= lstHists.getModel().getSize() > 0;
		bExport.setEnabled(selected);
	}

	/**
	 * Add a selected histogram
	 *  
	 */
	private void addSelectedHist() {
		final String name = cbHist.getSelectedItem().toString();
		final HashSet histSet = new HashSet();
		/* now combine this with stuff already in list. */
		final ListModel model = lstHists.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			histSet.add(model.getElementAt(i));
		}
		histSet.add(name);
		lstHists.setListData(histSet.toArray());
	}

	/**
	 * Add all 1 d histograms to the list
	 *  
	 */
	private void addAllHists() {
        final HashSet histSet = new HashSet();
        CollectionsUtil.instance().addConditional(Histogram.getHistogramList(),
                histSet, HIST_COND_1D);
        lstHists.setListData(histSet.toArray());
    }
	
	private static final CollectionsUtil.Condition HIST_COND_1D = new CollectionsUtil.Condition() {
        public boolean accept(Object object) {
            final Histogram hist = (Histogram) object;
            return hist.getDimensionality() == 1;
        }
    };

	/**
	 * remove a histogram from the list
	 */
	private void removeSelectedHist() {
		final Object[] removeList = lstHists.getSelectedValues();
		final ListModel model = lstHists.getModel();
		final List list = new ArrayList();
		for (int i = 0; i < model.getSize(); i++) {
			list.add(model.getElementAt(i));
		}
		list.removeAll(Arrays.asList(removeList));
		lstHists.setListData(list.toArray());
	}

	/**
	 * remove all items from sort list
	 *  
	 */
	private void removeAllHists() {
		lstHists.setListData(new Vector());
	}

	/**
	 * Load a list of histograms to export from a file.
	 *  
	 */
	private void loadList() {
		Object listItem;
		final JFileChooser chooser = new JFileChooser(lastListFile);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new ExtensionFileFilter(new String[] { "lst" },
				"List Files (*.lst)"));
		final int option = chooser.showOpenDialog(this);
		//save current values
		if (option == JFileChooser.APPROVE_OPTION
				&& chooser.getSelectedFile() != null) {
			lastListFile = chooser.getSelectedFile(); //save current directory
			final List list = new Vector();
			try {
				final BufferedReader reader = new BufferedReader(new FileReader(
						lastListFile));
				do {
					listItem = Histogram.getHistogram(reader.readLine());
					if (listItem != null) {
						list.add(listItem);
					}
				} while (listItem != null);
				reader.close();
			} catch (IOException ioe) {
				console.errorOutln(ioe.getMessage());
			}
			lstHists.setListData(list.toArray());
		}
	}

	/**
	 * Save list of items to export.
	 */
	private void saveList() {
		final JFileChooser chooser = new JFileChooser(lastListFile);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new ExtensionFileFilter(new String[] { "lst" },
				"List Files (*.lst)"));
		final int option = chooser.showSaveDialog(this);
		/* save current values */
		if (option == JFileChooser.APPROVE_OPTION
				&& chooser.getSelectedFile() != null) {
			lastListFile = chooser.getSelectedFile(); //save current directory
		}
		try {
			final ListModel model = lstHists.getModel();
			final FileWriter saveStream = new FileWriter(chooser.getSelectedFile());
			for (int i = 0; i < model.getSize(); i++) {
				saveStream.write(model.getElementAt(i) + "\n");
			}
			saveStream.close();
		} catch (IOException ioe) {
			console.errorOutln(ioe.getMessage());
		}
	}

	/**
	 * Export the histograms
	 */
	private void export() {
		/* select the format */
		AbstractImpExp out = null;
		final Iterator iter = exportMap.keySet().iterator();
		while (iter.hasNext()) {
			final AbstractButton button = (AbstractButton) iter.next();
			if (button.isSelected()) {
				out = (AbstractImpExp) exportMap.get(button);
			}
		}
		final File dir = new File(txtDirectory.getText().trim());
		if (dir.exists()) {
			if (dir.isDirectory()) {
				//look for any files that might be overwritten
				final ListModel model = lstHists.getModel();
				File[] files = new File[model.getSize()];
				Histogram[] hist = new Histogram[model.getSize()];
				boolean already = false;
				for (int i = 0; i < files.length; i++) {
					hist[i] = Histogram.getHistogram((String) model
							.getElementAt(i));
					files[i] = new File(dir, FileUtilities.setExtension(hist[i]
							.getFullName().trim(), out.getDefaultExtension(),
							FileUtilities.APPEND_ONLY));
					already |= files[i].exists();
				}
				if (already) {
					console
							.errorOutln("At least one file to export already exists. Delete or try a"
									+ " different directory.");
				} else { //go ahead and write
					console.messageOut("Exporting to " + dir.getPath() + ": ",
							MessageHandler.NEW);
					for (int i = 0; i < files.length; i++) {
						console.messageOut(files[i].getName(),
								MessageHandler.CONTINUE);
						if (i < files.length - 1){
							console.messageOut(", ", MessageHandler.CONTINUE);
						}
						try {
							out.saveFile(files[i], hist[i]);
						} catch (ImpExpException e) {
							console
									.errorOutln("Error while trying to write files: "
											+ e.getMessage());
						}
					}
					console.messageOut(".", MessageHandler.END);
				}
			} else { //not a directory
				console
						.errorOutln("The specified directory is not really a directory.");
			}
		} else { //directory doesn't exist
			console.errorOutln("The specified directory does not exist.");
		}
	}

	/**
	 * add all files in a directory to sort
	 *  
	 */
	private void browseForDir() {
		final JFileChooser chooser = new JFileChooser(new File(txtDirectory.getText()
				.trim()));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final int option = chooser.showOpenDialog(this);
		/* save current values */
		if (option == JFileChooser.APPROVE_OPTION
				&& chooser.getSelectedFile() != null) {
			txtDirectory.setText(chooser.getSelectedFile().getPath());
		}
	}

	/**
	 * Setup histogram chooser.
	 */
	private void setup() {
		cbHist.removeActionListener(this);
		cbHist.removeAllItems();
		final Iterator iterator = Histogram.getHistogramList().iterator();
		while (iterator.hasNext()) {
			final Histogram hist = (Histogram) iterator.next();
			if (hist.getDimensionality() == 1) {
				cbHist.addItem(hist.getFullName());
			}
		}
		cbHist.addActionListener(this);
	}

	/**
	 * Implementation of Observable interface listeners for broadcast events.
	 * broadcast events where there are new histograms or histograms added.
	 */
	public void update(Observable observable, Object object) {
		final BroadcastEvent event = (BroadcastEvent) object;
		final BroadcastEvent.Command command = event.getCommand();
		if (command == BroadcastEvent.Command.HISTOGRAM_NEW
				|| command == BroadcastEvent.Command.HISTOGRAM_ADD) {
			setup();
		}
	}
}