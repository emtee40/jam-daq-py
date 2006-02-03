package jam;

import jam.commands.CommandManager;
import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.CommandNames;
import jam.global.JamStatus;
import jam.global.SortMode;
import jam.io.hdf.HDFPrefs;
import jam.plot.PlotPrefs;
import jam.plot.View;
import jam.plot.color.ColorPrefs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
final class MenuBar implements Observer, CommandNames {

	final transient private JamStatus status = JamStatus.getSingletonInstance();

	final transient private JMenuItem impHist = new JMenu("Import");

	/** Fit menu needed as members so we can add a fit */
	final transient private JMenu fitting = new JMenu("Fitting");

	/** Fit menu needed as members so we can add a fit */
	final transient private JMenu view = new JMenu("View");

	final transient private JMenuBar menus = new JMenuBar();

	private static final MenuBar INSTANCE = new MenuBar();

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
	private MenuBar() {
		super();
		Broadcaster.getSingletonInstance().addObserver(this);
		menus.add(createFileMenu());
		menus.add(createSetupMenu());
		menus.add(createControlMenu());
		menus.add(createHistogramMenu());
		menus.add(createGateMenu());
		menus.add(createScalerMenu());
		menus.add(createViewMenu());
		menus.add(createPreferencesMenu());
		menus.add(createFitMenu());
		menus.add(createHelp());
	}

	private JMenu createFileMenu() {

		final JMenu file = new JMenu("File");

		file.add(getMenuItem(CLEAR));
		file.add(getMenuItem(OPEN_HDF));

		final JMenuItem openSpecial = new JMenu("Open Special");
		file.add(openSpecial);
		openSpecial.add(getMenuItem(OPEN_MULTIPLE_HDF));
		openSpecial.add(getMenuItem(OPEN_ADD_HDF));
		openSpecial.add(getMenuItem(OPEN_SELECTED));

		file.add(getMenuItem(RELOAD_HDF));
		file.add(getMenuItem(ADD_HDF));
		file.add(getMenuItem(SAVE_HDF));
		file.add(getMenuItem(SAVE_AS_HDF));

		final JMenuItem saveSpecial = new JMenu("Save Special");
		saveSpecial.add(getMenuItem(SAVE_SORT));
		saveSpecial.add(getMenuItem(SAVE_GROUP));
		saveSpecial.add(getMenuItem(SAVE_HISTOGRAMS));
		saveSpecial.add(getMenuItem(SAVE_GATES));

		file.add(saveSpecial);
		file.addSeparator();

		final JMenuItem utilities = new JMenu("Scaler Utilities");
		file.add(utilities);
		utilities.add(getMenuItem(OPEN_SCALERS));
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
		expHist.add(getMenuItem(EXPORT_TABLE));
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

	private JMenu createSetupMenu() {
		final JMenu setup = new JMenu("Setup");
		setup.add(getMenuItem(SHOW_SETUP_ONLINE));
		setup.add(getMenuItem(SHOW_SETUP_OFF));
		setup.add(getMenuItem(SHOW_SETUP_REMOTE));
		setup.add(getMenuItem(SHOW_CONFIG));
		return setup;
	}

	private JMenu createControlMenu() {
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

	private JMenu createHistogramMenu() {
		final JMenu histogram = new JMenu("Histogram");
		final JMenuItem group = new JMenu("Group");
		histogram.add(group);
		group.add(getMenuItem(SHOW_NEW_GROUP));
		group.add(getMenuItem(SHOW_RENAME_GROUP));
		group.add(getMenuItem(DELETE_GROUP));

		histogram.add(getMenuItem(SHOW_NEW_HIST));
		histogram.add(getMenuItem(SHOW_HIST_ZERO));
		histogram.add(getMenuItem(DELETE_HISTOGRAM));
		histogram.add(getMenuItem(SHOW_HIST_FIT));
		histogram.add(getMenuItem(SHOW_HIST_PROJECT));
		histogram.add(getMenuItem(SHOW_HIST_COMBINE));
		histogram.add(getMenuItem(SHOW_GAIN_SHIFT));

		return histogram;
	}

	private JMenu createGateMenu() {

		final JMenu gate = new JMenu("Gate");
		menus.add(gate);
		gate.add(getMenuItem(SHOW_NEW_GATE));
		gate.add(getMenuItem(SHOW_ADD_GATE));
		gate.add(getMenuItem(SHOW_SET_GATE));
		return gate;
	}

	private JMenu createViewMenu() {

		updateViews();
		return view;

	}

	private JMenu createScalerMenu() {
		final JMenu scalers = new JMenu("Scaler");
		menus.add(scalers);
		scalers.add(getMenuItem(DISPLAY_SCALERS));
		scalers.add(getMenuItem(SHOW_ZERO_SCALERS));
		scalers.addSeparator();
		scalers.add(getMenuItem(DISPLAY_MONITORS));
		scalers.add(getMenuItem(DISPLAY_MON_CFG));
		return scalers;
	}

	private JMenu createFitMenu() {
		fitting.add(getMenuItem(SHOW_FIT_NEW));
		fitting.addSeparator();
		return fitting;
	}

	private JMenu createHelp() {

		final JMenu helpMenu = new JMenu("Help");
		menus.add(helpMenu);
		helpMenu.add(getMenuItem(HELP_ABOUT));
		helpMenu.add(getMenuItem(USER_GUIDE));
		helpMenu.add(getMenuItem(HELP_LICENSE));
		return helpMenu;
	}

	private JMenu createPreferencesMenu() {
		final JMenu mPrefer = new JMenu("Preferences");
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_IGNORE_ZERO));
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_IGNORE_FULL));
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_ON_EXPAND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(PlotPrefs.HIGHLIGHT_GATE_CHANNELS));
		mPrefer.add(getMenuItem(ColorPrefs.SMOOTH_SCALE));
		mPrefer.add(getMenuItem(SHOW_GRADIENT));
		mPrefer.add(getMenuItem(PlotPrefs.ENABLE_SCROLLING_TILED));
		mPrefer.add(getMenuItem(PlotPrefs.DISPLAY_AXIS_LABELS));
		mPrefer.add(getMenuItem(PlotPrefs.BLACK_BACKGROUND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(PlotPrefs.AUTO_PEAK_FIND));
		mPrefer.add(getMenuItem(SHOW_PEAK_FIND));
		mPrefer.addSeparator();
		mPrefer.add(getMenuItem(HDFPrefs.SUPPRESS_WRITE_EMPTY));
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
	private JMenuItem getMenuItem(final String name) {
		return new JMenuItem(CommandManager.getInstance().getAction(name));
	}

	/**
	 * @see Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(final Observable observe, final Object obj) {
		final BroadcastEvent event = (BroadcastEvent) obj;
		final BroadcastEvent.Command command = event.getCommand();
		if (command == BroadcastEvent.Command.SORT_MODE_CHANGED) {
			sortModeChanged();
		} else if (command == BroadcastEvent.Command.FIT_NEW) {
			final Action fitAction = (Action) (event.getContent());
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

	private void updateViews() {
		view.removeAll();
		view.add(getMenuItem(SHOW_VIEW_NEW));
		view.add(getMenuItem(SHOW_VIEW_DELETE));
		view.addSeparator();
		for (final String name : View.getNameList()) {
			view.add(namedMenuItem(name));
		}
	}
	
	private JMenuItem namedMenuItem(final String name) {
		final JMenuItem rval = new JMenuItem(name);
		rval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				status.getDisplay().setView(View.getView(name));
			}
		});
		return rval;
	}

	/**
	 * @return the only menubar created by this class
	 */
	static JMenuBar getMenuBar() {
		return INSTANCE.menus;
	}
}