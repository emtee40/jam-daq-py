package jam.ui;

import jam.JamPrefs;
import jam.commands.CommandManager;
import jam.data.Histogram;
import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.CommandNames;
import jam.global.JamStatus;
import jam.global.SortMode;
import jam.plot.PlotPrefs;
import jam.plot.View;
import jam.plot.color.ColorPrefs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
/**
 * 
 * Jam's menu bar. Separated from JamMain to reduce its size and separate
 * responsibilities.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser </a>
 * @version 1.4
 * @since 30 Dec 2003
 */
public final class MainMenuBar implements Observer, CommandNames {

	final transient private JamStatus status = JamStatus.instance();

	final transient private JMenuItem impHist = new JMenu("Import");

	/** Fit menu needed as members so we can add a fit */
	final transient private JMenu fitting = new JMenu("Fitting");

	/** Fit menu needed as members so we can add a fit */
	final transient private JMenu view = new JMenu("View");

	final transient private JMenuBar menubar=new JMenuBar();

	final transient private JMenu calHist = new JMenu("Calibrate");

	final transient private CommandManager commands = CommandManager.getInstance();
	
	private static final MainMenuBar INSTANCE=new MainMenuBar();

	/**
	 * Jam's menu bar. It has the following menus:
	 * <ul>
	 * <li>File</li>
	 * <li>Setup</li>
	 * <li>Control</li>
	 * <li>Histogram</li>
	 * <li>Gate</li>
	 * <li>Scalers</li>
	 * <li>Preferencs</li>
	 * <li>Fitting</li>
	 * <li>Help</li>
	 * </ul>
	 * 
	 * @author Dale Visser
	 * @author Ken Swartz
	 */
	private MainMenuBar() {
		super();
		Broadcaster.getSingletonInstance().addObserver(this);
		menubar.add(getFileMenu());
		menubar.add(getSetupMenu());
		menubar.add(getControlMenu());
		menubar.add(getHistogramMenu());
		menubar.add(getGateMenu());
		menubar.add(getScalerMenu());
		menubar.add(getViewMenu());
		menubar.add(getPreferencesMenu());
		menubar.add(getFitMenu());
		menubar.add(getHelp());
	}

	private JMenu getFileMenu() {

		final JMenu file = new JMenu("File");

		file.add(getMenuItem(CLEAR));
		file.add(getMenuItem(OPEN_HDF));
		file.add(getMenuItem(RELOAD_HDF));
		file.add(getMenuItem(ADD_HDF));
		file.add(getMenuItem(SAVE_HDF));
		file.add(getMenuItem(SAVE_AS_HDF));

		final JMenuItem special = new JMenu("Special");
		special.add(getMenuItem(OPEN_SELECTED));
		special.add(getMenuItem(SAVE_GATES));
		file.add(special);
		file.addSeparator();

		final JMenuItem utilities = new JMenu("Utilities");
		file.add(utilities);
		utilities.add(getMenuItem(OPEN_SCALERS_YALE_CAEN));
		utilities.add(getMenuItem(SHOW_SCALER_SCAN));
		file.addSeparator();

		file.add(impHist);
		impHist.add(getMenuItem(IMPORT_TEXT));
		impHist.add(getMenuItem(IMPORT_SPE));
		impHist.add(getMenuItem(IMPORT_DAMM));
		impHist.add(getMenuItem(IMPORT_XSYS));
		impHist.add(getMenuItem(IMPORT_BAN));

		final JMenu expHist = new JMenu("Export");
		file.add(expHist);
		expHist.add(getMenuItem(EXPORT_TEXT));
		expHist.add(getMenuItem(EXPORT_SPE));
		expHist.add(getMenuItem(EXPORT_DAMM));
		expHist.add(getMenuItem(SHOW_BATCH_EXPORT));

		file.addSeparator();
		file.add(getMenuItem(PRINT));
		file.add(getMenuItem(PAGE_SETUP));
		file.addSeparator();
		file.add(getMenuItem(EXIT));

		return file;
	}

	private JMenu getSetupMenu() {
		final JMenu setup = new JMenu("Setup");
		setup.add(getMenuItem(SHOW_SETUP_ONLINE));
		setup.add(getMenuItem(SHOW_SETUP_OFFLINE));
		setup.add(getMenuItem(SHOW_SETUP_REMOTE));
		return setup;
	}

	private JMenu getControlMenu() {
		final JMenu mcontrol = new JMenu("Control");
		mcontrol.add(getMenuItem(START));
		mcontrol.add(getMenuItem(STOP));
		mcontrol.add(getMenuItem(FLUSH));
		mcontrol.addSeparator();
		mcontrol.add(getMenuItem(SHOW_RUN_CONTROL));
		mcontrol.add(getMenuItem(SHOW_SORT_CONTROL));
		mcontrol.add(getMenuItem(PARAMETERS));
		mcontrol.add(getMenuItem(SHOW_BUFFER_COUNT));
		return mcontrol;
	}

	private JMenu getHistogramMenu() {
		final JMenu histogram = new JMenu("Histogram");
		histogram.add(getMenuItem(SHOW_NEW_HIST));
		histogram.add(getMenuItem(SHOW_HIST_ZERO));
		histogram.add(getMenuItem(DELETE_HISTOGRAM));
		histogram.add(calHist);
		calHist.add(getMenuItem(SHOW_HIST_FIT));
		calHist.add(getMenuItem(SHOW_HIST_DISPLAY_FIT));
		histogram.add(getMenuItem(SHOW_HIST_PROJECT));
		histogram.add(getMenuItem(SHOW_HIST_COMBINE));
		histogram.add(getMenuItem(SHOW_HIST_GAIN_SHIFT));
		return histogram;
	}

	private JMenu getGateMenu() {

		final JMenu gate = new JMenu("Gate");
		menubar.add(gate);
		gate.add(getMenuItem(SHOW_NEW_GATE));
		gate.add(getMenuItem(SHOW_ADD_GATE));
		gate.add(getMenuItem(SHOW_SET_GATE));
		return gate;
	}

	private JMenu getViewMenu() {
		
		updateViews();
		return view;

	}

	private JMenu getScalerMenu() {
		final JMenu scalers = new JMenu("Scaler");
		menubar.add(scalers);
		scalers.add(getMenuItem(DISPLAY_SCALERS));
		scalers.add(getMenuItem(SHOW_ZERO_SCALERS));
		scalers.addSeparator();
		scalers.add(getMenuItem(DISPLAY_MONITORS));
		scalers.add(getMenuItem(DISPLAY_MON_CONFIG));
		return scalers;
	}

	private JMenu getFitMenu() {
		fitting.add(getMenuItem(SHOW_FIT_NEW));
		fitting.addSeparator();
		return fitting;
	}

	private JMenu getHelp() {

		final JMenu helpMenu = new JMenu("Help");
		menubar.add(helpMenu);
		helpMenu.add(getMenuItem(HELP_ABOUT));
		helpMenu.add(getMenuItem(USER_GUIDE));
		helpMenu.add(getMenuItem(HELP_LICENSE));
		return helpMenu;
	}

	private JMenu getPreferencesMenu() {
		final JMenu mPrefer = new JMenu("Preferences");
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_IGNORE_ZERO));
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_IGNORE_FULL));
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_ON_EXPAND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(PlotPrefs.HIGHLIGHT_GATE_CHANNELS));
		mPrefer.add(getMenuItem(ColorPrefs.SMOOTH_COLOR_SCALE));
		mPrefer.add(getMenuItem(SHOW_GRADIENT_SETTINGS));
		mPrefer.add(getMenuItem(PlotPrefs.ENABLE_SCROLLING));
		mPrefer.add(getMenuItem(PlotPrefs.DISPLAY_AXIS_LABELS));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_PEAK_FIND));
		mPrefer.add(getMenuItem(SHOW_PEAK_FIND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(PlotPrefs.BLACK_BACKGROUND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(JamPrefs.VERBOSE));
		mPrefer.add(getMenuItem(JamPrefs.DEBUG));
		return mPrefer;
	}

	/**
	 * Produce a menu item that invokes the action given by the lookup table in
	 * <code>jam.commands.CommandManager</code>
	 * 
	 * @param name
	 *            name of the command
	 * @return JMenuItem that invokes the associated action
	 */
	private final JMenuItem getMenuItem(String name) {
		return new JMenuItem(commands.getAction(name));
	}

	/**
	 * @see Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable observe, Object obj) {
		final BroadcastEvent event = (BroadcastEvent) obj;
		final BroadcastEvent.Command command = event.getCommand();
		if (command == BroadcastEvent.Command.SORT_MODE_CHANGED) {
			sortModeChanged();
		} else if (command == BroadcastEvent.Command.HISTOGRAM_SELECT) {
			final Object content = event.getContent();
			final Histogram hist = content == null ? status.getCurrentHistogram()
					: (Histogram) content;
			adjustHistogramItems(hist);
		} else if (command == BroadcastEvent.Command.FIT_NEW) {
			Action fitAction = (Action) (event.getContent());
			fitting.add(new JMenuItem(fitAction));
		} else if (command == BroadcastEvent.Command.VIEW_NEW) {
			updateViews();
		}
	}

	private void sortModeChanged() {
		final SortMode mode = status.getSortMode();
		final boolean file = mode == SortMode.FILE || mode == SortMode.NO_SORT;
		impHist.setEnabled(file);
	}

	private void adjustHistogramItems(Histogram hist) {
		final boolean hExists = hist != null;
		final boolean oneDops = hExists && hist.getDimensionality() == 1;
		calHist.setEnabled(oneDops);
	}
	
	private void updateViews(){
		view.removeAll();	
		view.add(getMenuItem(SHOW_VIEW_NEW));
		view.add(getMenuItem(SHOW_VIEW_DELETE));
		view.addSeparator();		
		final Iterator viewNames =View.getNameList().iterator(); 
		while(viewNames.hasNext()){
			final String name=(String)viewNames.next();
			final JMenuItem viewItem = new JMenuItem(name);
			view.add(viewItem);
			viewItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					status.getDisplay().setView(View.getView(name));
				}
			});
		}
	}	
	
	/**
	 * @return the only menubar created by this class
	 */
	static public JMenuBar getMenuBar(){
	    return INSTANCE.menubar;
	}
}