package jam.plot;

import jam.data.Histogram;
import jam.global.Nameable;
import jam.util.StringUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the number and arrangements of plots to show within the
 * <code>Display</code>.
 * 
 * @author Ken Swartz
 * @version 2004-11-03
 * @see PlotDisplay
 */
public final class View {

	private final static List<String> NAME_LIST;

	private final static Map<String, View> MAP;

	static {
		NAME_LIST = new ArrayList<String>();
		MAP = new TreeMap<String, View>();
	}

	/**
	 * Default view--one plot in the window.
	 */
	public static final View SINGLE = new View("Single", 1, 1);

	private static final int NAME_LENGTH = 20;

	private transient final String name;

	private transient final int nRows;

	private transient final int nCols;

	private transient final String[] histogramNames;

	/**
	 * Constructs a new view.
	 * 
	 * @param viewName
	 *            the name to associate with the view.
	 * @param rows
	 *            number of rows of plots
	 * @param cols
	 *            number of columns of plots
	 */
	public View(String viewName, int rows, int cols) {
		super();
		if (rows < 1) {
			throw new IllegalArgumentException("Can't have a view with " + rows
					+ " rows.");
		}
		if (cols < 1) {
			throw new IllegalArgumentException("Can't have a view with " + cols
					+ " columns.");
		}
		String tempName = viewName;
		nRows = rows;
		nCols = cols;
		int prime;
		final int numHists = rows * cols;
		histogramNames = new String[numHists];
		prime = 1;
		while (MAP.containsKey(tempName)) {
			final String addition = "[" + prime + "]";
			tempName = StringUtilities.getInstance().makeLength(tempName,
					NAME_LENGTH - addition.length())
					+ addition;
			prime++;
		}
		name = tempName;
		addView(name, this);
	}

	private static void addView(final String name, final View view) {
		synchronized (View.class) {
			MAP.put(name, view);
			NAME_LIST.add(name);
		}
	}

	/**
	 * Gets the list of view names.
	 * 
	 * @return list of the names of the existing views
	 */
	public static List<String> getNameList() {
		return Collections.unmodifiableList(NAME_LIST);
	}

	/**
	 * Get the view with the given name.
	 * 
	 * @param name
	 *            of the view
	 * @return the view
	 */
	public static View getView(final String name) {
		synchronized (View.class) {
			return MAP.get(name);
		}
	}

	/**
	 * Remove the view with the given name.
	 * 
	 * @param name
	 *            of view to delete
	 */
	public static void removeView(final String name) {
		synchronized (View.class) {
			MAP.remove(name);
			NAME_LIST.remove(name);
		}
	}

	/**
	 * Get the number of rows
	 * 
	 * @return rows
	 */
	int getRows() {
		return nRows;
	}

	/**
	 * Get the number of columns
	 * 
	 * @return columns
	 */
	int getColumns() {
		return nCols;
	}

	/**
	 * Get the number of histogram plots.
	 * 
	 * @return the number of plots
	 */
	int getNumberHists() {
		return histogramNames.length;
	}

	/**
	 * Returns the histogram associatied with the given plot.
	 * 
	 * @param num
	 *            which plot
	 * @return histogram for the given plot
	 */
	Histogram getHistogram(final int num) {
		return Histogram.getHistogram(histogramNames[num]);
	}

	/**
	 * Associates the given histogram with the given plot.
	 * 
	 * @param num
	 *            which plot
	 * @param histIn
	 *            the Histogram
	 */
	void setHistogram(final int num, final Nameable histIn) {
		if (histIn == null) {
			histogramNames[num] = "";
		} else {
			histogramNames[num] = histIn instanceof Histogram ? ((Histogram) histIn)
					.getFullName()
					: histIn.getName();
		}
	}

	/**
	 * Gets the name of this view.
	 * 
	 * @return the name of this view
	 */
	public String getName() {
		return name;
	}
}
