/*
 * Created on Nov 26, 2004
 */
package jam.data;

import java.util.Arrays;



/**
 * @author <a href="mailto:dale@visser.name">Dale W Visser </a>
 */
public final class HistInt1D extends AbstractHist1D {
	private int counts[]; // array to hold counts for 1d int

	/**
	 * Create a new 1-d <code>Histogram</code> with the counts known and with
	 * axes labeled.
	 * 
	 * @param name
	 *            unique name of histogram, should be limited to
	 *            <code>NAME_LENGTH</code> characters, used in both .jhf and
	 *            .hdf files as the unique identifier for reloading the
	 *            histogram
	 * @param title
	 *            lengthier title of histogram, displayed on plot
	 * @param axisLabelX
	 *            label displayed for x-axis on plot
	 * @param axisLabelY
	 *            label displayed for y-axis on plot
	 * @param countsIn
	 *            array of counts to initialize with
	 */
	HistInt1D(String name, String title, String axisLabelX, String axisLabelY,
			int[] countsIn) {
		super(name, Type.ONE_DIM_INT, countsIn.length, title, axisLabelX,
				axisLabelY);
		initCounts(countsIn);
	}

	/**
	 * Given an array of counts, create a new 1-d <code>Histogram</code> and
	 * give it a number.
	 * 
	 * @param name
	 *            unique name of histogram, should be limited to
	 *            <code>NAME_LENGTH</code> characters, used in both .jhf and
	 *            .hdf files as the unique identifier for reloading the
	 *            histogram
	 * @param title
	 *            lengthier title of histogram, displayed on plot
	 * @param countsIn
	 *            array of counts to initialize with
	 */
	HistInt1D(String name, String title, int[] countsIn) {
		super(name, Type.ONE_DIM_INT, countsIn.length, title);
		initCounts(countsIn);
	}
	
	private void initCounts(int [] countsIn){
		counts=new int[sizeX];
		System.arraycopy(countsIn, 0, counts, 0, countsIn.length);		
	}

	public synchronized Object getCounts() {
		return counts;
	}

	public synchronized double getCounts(int channel) {
		return counts[channel];
	}

	public synchronized void setZero() {
		Arrays.fill(counts,0);
		unsetErrors();
	}

	public synchronized void setCounts(Object countsIn) {
		if (Type.getArrayType(countsIn)!=getType()){
			throw new IllegalArgumentException("Expected array for type "+getType());
		}
		final int inLength = ((int[]) countsIn).length;
		System.arraycopy(countsIn, 0, counts, 0, Math.min(inLength, sizeX));
	}

	public synchronized void setCounts(int channel, double count) {
		counts[channel] = (int) Math.round(count);
	}

	private synchronized void addCounts(int[] countsIn) {
		final int max = Math.min(countsIn.length, sizeX) - 1;
		for (int i = max; i >= 0; i--) {
			counts[i] += countsIn[i];
		}
	}


	/**
	 * Increments the counts by one in the given channel. Must be a histogram of
	 * type <code>ONE_DIM_INT</code>.
	 * 
	 * @param dataWord
	 *            the channel to be incremented
	 * @exception UnsupportedOperationException
	 *                thrown if method called for inappropriate type of
	 *                histogram
	 */
	public void inc(int dataWord) {
		int incCh = dataWord;
		/* check for overflow */
		if (incCh >= sizeX) {
			incCh = sizeX - 1;
		} else if (dataWord < 0) {
			incCh = 0;
		}
		synchronized (this) {
			counts[incCh]++;
		}
	}

	public synchronized double[] getErrors() {
		final int length = counts.length;
		if (errors == null) {
			errors = new double[length];
			for (int i = 0; i < length; i++) {
				if (counts[i] == 0) {
					/* set errors according to Poisson with error = 1 */
					errors[i] = 1.0;
				} else {
					errors[i] = Math.sqrt((double) counts[i]);
				}
			}
		}
		return errors;
	}

	public synchronized double getArea() {
		double sum = 0.0;
		for (int i = 0; i < sizeX; i++) {
			sum += counts[i];
		}
		return sum;
	}
	
	public synchronized void addCounts(Object add){
		if (Type.getArrayType(add)!=getType()){
			throw new IllegalArgumentException("Expected array for type "+getType());
		}
		addCounts((int[]) add);
	}
	
	synchronized void clearCounts(){
		counts=null;
		unsetErrors();
	}

}