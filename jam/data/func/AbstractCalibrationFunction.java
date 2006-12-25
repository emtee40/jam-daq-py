package jam.data.func;

import jam.data.DataException;

import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 * A function that can be use to calibrate a histogram. Most often used to
 * define energy calibrations of spectra. But could also do time of flight an
 * rho for a magnetic spectometer.
 * 
 * @author Ken Swartz
 * @version 1.0
 */
public abstract class AbstractCalibrationFunction implements Function {

	private static final Map<String, Class<? extends AbstractCalibrationFunction>> FUNCTIONS = new HashMap<String, Class<? extends AbstractCalibrationFunction>>();

	private static final List<String> NAMES = new ArrayList<String>();

	private static final Map<String, ImageIcon> ICONS = new HashMap<String, ImageIcon>();

	private static final AbstractCalibrationFunction noFunc = new NoFunction();

	static {
		clearAll();
		addFunction(noFunc.getName(), noFunc.getClass());
		AbstractCalibrationFunction linearFunc = new LinearFunction();
		addFunction(linearFunc.getName(), linearFunc.getClass());
		AbstractCalibrationFunction quadFunc = new QuadraticFunction();
		addFunction(quadFunc.getName(), quadFunc.getClass());
		AbstractCalibrationFunction cubicFunc = new CubicFunction();
		addFunction(cubicFunc.getName(), cubicFunc.getClass());
		AbstractCalibrationFunction sqrtEFunc = new SqrtEnergyFunction();
		addFunction(sqrtEFunc.getName(), sqrtEFunc.getClass());
	}

	static public AbstractCalibrationFunction getNoCalibration() {
		return noFunc;
	}

	public boolean isCalibrated() {
		return true;
	}

	/**
	 * Maximum number of terms assigned by default to <code>POLYNOMIAL</code>
	 * type.
	 */
	public final static int MAX_TERMS = 5;

	/**
	 * Term labels.
	 */
	protected transient String[] labels;

	/**
	 * Name of calibration function.
	 */
	protected transient String name;

	/**
	 * This functions class
	 */
	protected transient Class funcClass;

	/**
	 * Title of calibration function.
	 */
	protected transient String title;

	/**
	 * Whether fit points were used for calibration.
	 */
	protected transient boolean fitPoints = true;

	/**
	 * Fit channels
	 */
	protected transient double[] ptsChannel;

	/**
	 * Fit energy
	 */
	protected transient double[] ptsEnergy = new double[0];

	/**
	 * Coeffiecient values.
	 */
	protected double[] coeff;

	/**
	 * Length histogram
	 */
	protected transient int sizeHistogram;

	/**
	 * The formula for the function.
	 */
	protected transient StringBuffer formula = new StringBuffer();

	/**
	 * for subclasses to use
	 * 
	 */
	protected AbstractCalibrationFunction() {
		super();
	}

	/**
	 * Returns the list of function names.
	 * 
	 * @return the list of function names
	 */
	public static List<String> getListNames() {
		return Collections.unmodifiableList(NAMES);
	}

	/**
	 * Returns the map of function names to functions.
	 * 
	 * @return the map of function names to functions
	 */
	public static Map<String, Class<? extends AbstractCalibrationFunction>> getMapFunctions() {
		return Collections.unmodifiableMap(FUNCTIONS);
	}

	/**
	 * Clear the collections.
	 */
	private static void clearAll() {
		FUNCTIONS.clear();
		ICONS.clear();
		NAMES.clear();
	}

	private static void addFunction(final String name,
			final Class<? extends AbstractCalibrationFunction> funcClass) {
		/* Only add once. */
		if (!FUNCTIONS.containsKey(name)) {
			FUNCTIONS.put(name, funcClass);
			NAMES.add(name);
		}
	}

	/**
	 * Sets an icon for the given function name.
	 * 
	 * @param name
	 *            of the function
	 * @param icon
	 *            for the function
	 */
	public static void setIcon(final String name, final ImageIcon icon) {
		ICONS.put(name, icon);
	}

	public static ImageIcon getIcon(final String name) {
		return ICONS.get(name);
	}

	static void loadIcon(final AbstractCalibrationFunction calFunc, final String iconFile) {
		final ClassLoader loader = ClassLoader.getSystemClassLoader();

		URL urlIcon = loader.getResource(iconFile);
		if (urlIcon == null) {
			JOptionPane.showMessageDialog(null,
					"Can't load resource for calibration function icon "
							+ iconFile);
		} else {
			AbstractCalibrationFunction.setIcon(calFunc.getName(),
					new ImageIcon(urlIcon));
		}

	}

	/**
	 * Creates a new <code>CalibrationFunction</code> object.
	 * 
	 * @param inClass
	 *            function to use
	 * @param name
	 *            name of function
	 * @param numberTerms
	 *            number of terms in function
	 */
	AbstractCalibrationFunction(Class inClass, String name, int numberTerms) {
		super();
		this.funcClass = inClass;
		this.name = name;
		if (numberTerms < MAX_TERMS) {
			coeff = new double[numberTerms];
			labels = new String[numberTerms];
		} else {
			throw new IllegalArgumentException(getClass().getName()
					+ "--Maximum terms: " + MAX_TERMS + ", asked for: "
					+ numberTerms);
		}
	}

	/**
	 * @return Number of terms
	 */
	public int getNumberTerms() {
		return coeff.length;
	}

	/**
	 * Given a type of <code>CalibrationFunction</code>, returns an array of
	 * parameter labels.
	 * 
	 * @return an array of parameter labels
	 */
	public String[] getLabels() {
		final int len = labels.length;
		final String[] rval = new String[len];
		System.arraycopy(labels, 0, rval, 0, len);
		return rval;
	}

	/**
	 * Gets the calibration coefficients.
	 * 
	 * @return array containing the calibration coefficients
	 */
	public double[] getCoeff() {
		final int len = coeff.length;
		final double[] rval = new double[len];
		System.arraycopy(coeff, 0, rval, 0, len);
		return rval;
	}

	/**
	 * 
	 * @return name of the calibration function
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return title of the calibration function
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns whether coeffecients are result of a fit.
	 * 
	 * @return whether coeffecients are result of a fit
	 */
	public boolean isFitPoints() {
		synchronized (this) {
			return fitPoints;
		}
	}

	/**
	 * Sets whether coefficients are result of a fit.
	 * 
	 * @param isFitIn
	 *            whether coefficients are result of a fit
	 */
	private void setIsFitPoints(final boolean isFitIn) {
		synchronized (this) {
			fitPoints = isFitIn;
		}
	}

	/**
	 * 
	 * @return the function formula
	 */
	public String getFormula(final NumberFormat numFormat) {
		updateFormula(numFormat);
		return formula.toString();
	}

	/**
	 * Set histogram size, used to convert from energy to channel
	 */
	public void setSizeHistogram(final int size) {
		sizeHistogram = size;
	}

	/**
	 * Called by setCoeff() to update the formula.
	 * 
	 */
	protected abstract void updateFormula(NumberFormat numFormat);

	/**
	 * Set the calibration points used for fitting.
	 * 
	 * @param ptsChannelIn
	 *            the channels
	 * @param ptsEnergyIn
	 *            the "energies"
	 */
	public void setPoints(final double[] ptsChannelIn,
			final double[] ptsEnergyIn) {
		setIsFitPoints(true);
		ptsChannel = ptsChannelIn.clone();
		ptsEnergy = ptsEnergyIn.clone();
	}

	/**
	 * Get the input point channels.
	 * 
	 * @return the input point channels
	 */
	public double[] getPtsChannel() {
		final double[] rval;
		if (ptsChannel == null) {
			rval = new double[0];
		} else {
			final int len = ptsChannel.length;
			rval = new double[len];
			System.arraycopy(ptsChannel, 0, rval, 0, len);
		}
		return rval;
	}

	/**
	 * Get the input point energies.
	 * 
	 * @return the input point energies
	 */
	public double[] getPtsEnergy() {
		return ptsEnergy.clone();
	}

	/**
	 * Set the coefficients of the calibration function using the contents of
	 * the passed <code>Array</code>. If passed a larger than necessary
	 * array, the first elements of the array will be used.
	 * 
	 * @param aIn
	 *            array of coefficients which should be at least as large as the
	 *            number of coefficients
	 */
	public void setCoeff(final double aIn[]) {
		setIsFitPoints(false);
		if (aIn.length <= coeff.length) {
			Arrays.fill(coeff, 0.0);
			System.arraycopy(aIn, 0, coeff, 0, aIn.length);
		} else {
			throw new IndexOutOfBoundsException(getClass().getName()
					+ ".setCoeff(double [" + aIn.length + "]): too many terms.");
		}
	}

	/**
	 * Get the calibration value at a specified channel.
	 * 
	 * @param channel
	 *            value at which to get calibration
	 * @return calibration value of the channel
	 */
	public abstract double getValue(double channel);

	/**
	 * Gets the channel for the given energy. Don't always have a inverse
	 * function so by default search for the best channel.
	 * 
	 * @param energy
	 *            to get channel for
	 * @return channel for the given energy
	 */
	public double getChannel(final double energy) {

		double ch = 0;
		double bestDiff = Math.abs(getValue(ch) - energy);
		double diff;

		for (int i = 0; i < sizeHistogram; i++) {
			diff = Math.abs(getValue(i) - energy);
			if (diff < bestDiff) {
				ch = i;
			}

		}

		return ch;
	}

	/**
	 * Do a calibration fit.
	 * 
	 * @throws DataException
	 *             if the fit fails
	 */
	public abstract void fit() throws DataException;

	// TODO the rest of the methods should be moved to jam.fit

	/**
	 * do a linear regression of data points y=a+bx returns to fit a, and b
	 * values in an array
	 * 
	 * @param xVal
	 *            array of x values
	 * @param yVal
	 *            array of y values
	 * @throws DataException
	 *             if regression fails
	 * @return array where first element is constant, second is slope
	 */
	protected double[] linearRegression(final double[] xVal, final double[] yVal)
			throws DataException {
		double[] rval = new double[2];
		double sum = 0.0;
		double sumx = 0.0;
		double sumy = 0.0;
		double sumxx = 0.0;
		double sumxy = 0.0;
		double sumyy = 0.0;
		final double weight = 1.0;
		double delta = 0.0;
		double aEst, bEst;
		final int numberPoints = xVal.length;
		for (int i = 0; i < numberPoints; i++) {
			sum = sum + weight;
			sumx = sumx + weight * xVal[i];
			sumy = sumy + yVal[i];
			sumxx = sumxx + weight * xVal[i] * xVal[i];
			sumxy = sumxy + weight * xVal[i] * yVal[i];
			sumyy = sumyy + weight * yVal[i] * yVal[i];
		}
		delta = sum * sumxx - sumx * sumx;
		if (delta == 0.0) {
			rval[0] = 0.0;
			rval[1] = 0.0;
			throw new DataException(
					"Linear regression failed [CalibrationFunction]");
		}
		aEst = (sumxx * sumy - sumx * sumxy) / delta;
		bEst = (sumxy * sum - sumx * sumy) / delta;
		rval[0] = aEst;
		rval[1] = bEst;
		return rval;
	}

	/**
	 * do a least squares fit of data points y=a+bx returns to fit a, and b
	 * values in an array
	 * 
	 * @param xVal
	 *            array of x values
	 * @param yVal
	 *            array of y values
	 * @throws DataException
	 *             if regression fails
	 * @return with polynomial coefficents
	 */
	protected double[] polynomialFit(final double[] xVal, final double[] yVal,
			int order) throws DataException {

		double[] xNorm = new double[xVal.length];
		double matrixA[][] = null;
		double vectorB[] = null;
		double gaussMatrixB[][];
		double gaussCoeffs[][];
		double polyCoeffs[];
		int numTerms;
		double sum;
		double xMean;

		numTerms = order + 1;

		// Check data
		if (xVal.length < numTerms){
			throw new DataException(
					"Need more positions than order for polynomial fit");
		}
		if (xVal.length != yVal.length){
			throw new DataException(
					"Need same number of x and y points for polynomial fit");
		}
		// Find mean x to shift fit around mean
		sum = 0.0;
		for (int k = 0; k < xVal.length; k++) {
			sum += xVal[k];
		}
		xMean = sum / xVal.length;

		// Shift x data around mean
		for (int k = 0; k < xVal.length; k++) {
			xNorm[k] = xVal[k] - xMean;
		}

		matrixA = new double[numTerms][numTerms];
		vectorB = new double[numTerms];

		buildPolyMatrix(xVal, yVal, numTerms, matrixA, vectorB);

		// Copy vector b into a column matrix
		gaussMatrixB = new double[vectorB.length][1];
		for (int i = 0; i < vectorB.length; i++) {
			gaussMatrixB[i][0] = vectorB[i];
		}

		// Do gaussian elimination
		gaussCoeffs = gaussj(matrixA, gaussMatrixB);

		// Copy vector b into a column matrix
		polyCoeffs = new double[vectorB.length];
		for (int i = 0; i < polyCoeffs.length; i++) {
			polyCoeffs[i] = gaussCoeffs[i][0];
		}

		return polyCoeffs;
	}

	/**
	 * Build a matrix for a polynomial fit
	 * 
	 * @param xVal
	 *            x values
	 * @param yVal
	 *            y values
	 * @param order
	 *            order of polynomial
	 * @return matrixA the matrix to do gaussj on.
	 */
	protected void buildPolyMatrix(double xVal[], double yVal[], int order,
			double[][] matrixA, double[] vectorB) {
		double sum;

		// Alpha matrix part
		for (int row = 0; row < order; row++) {
			for (int col = 0; col < order; col++) {
				// Sum of x^n
				sum = 0.0;
				for (int k = 0; k < order; k++) {
					sum += Math.pow(xVal[k], row + col);
				}
				matrixA[row][col] = sum;
			}
		}

		// Alpha matrix part
		for (int row = 0; row < order; row++) {
			// Beta vector
			sum = 0.0;
			for (int k = 0; k < order; k++) {
				sum += Math.pow(xVal[k], row) * yVal[k];
			}
			vectorB[row] = sum;
		}

	}

	/**
	 * gauss jordon reduction from numerical recipes
	 * 
	 * @param alpha
	 *            alpha matrix
	 * @param beta
	 *            beta matrix
	 * @return fit coeffients
	 */
	protected double[][] gaussj(double[][] alpha, double[][] beta)
			throws DataException {

		int j, k, l, ll;
		int icol, irow;
		int[] indxc, indxr, ipiv;
		double big, dum, pivinv, temp;

		int n = alpha.length;
		int m = beta[1].length;
		icol = 0;
		irow = 0;
		indxc = new int[n];
		indxr = new int[n];
		ipiv = new int[n];

		for (j = 0; j < n; j++)
			ipiv[j] = 0;

		// loop over cols
		for (int i = 0; i < n; i++) {
			// search for pivot
			big = 0.0;
			for (j = 0; j < n; j++)
				if (ipiv[j] != 1)
					for (k = 0; k < n; k++) {
						if (ipiv[k] == 0) {
							if (Math.abs(alpha[j][k]) >= big) {
								big = Math.abs(alpha[j][k]);
								irow = j;
								icol = k;
							}
						}
					}
			++(ipiv[icol]);
			if (irow != icol) {
				for (l = 0; l < n; l++) {
					temp = alpha[irow][l];
					alpha[irow][l] = alpha[icol][l];
					alpha[icol][l] = temp;
				}
				for (l = 0; l < m; l++) {
					temp = beta[irow][l];
					beta[irow][l] = beta[icol][l];
					beta[icol][l] = temp;
				}
			}
			indxr[i] = irow;
			indxc[i] = icol;

			if (alpha[icol][icol] == 0.0)
				throw new DataException("gaussj: Singular Matrix");

			pivinv = 1.0 / alpha[icol][icol];
			alpha[icol][icol] = 1.0;
			for (l = 0; l < n; l++)
				alpha[icol][l] *= pivinv;
			for (l = 0; l < m; l++)
				beta[icol][l] *= pivinv;
			for (ll = 0; ll < n; ll++)
				if (ll != icol) {
					dum = alpha[ll][icol];
					alpha[ll][icol] = 0.0;
					for (l = 0; l < n; l++)
						alpha[ll][l] -= alpha[icol][l] * dum;
					for (l = 0; l < m; l++)
						beta[ll][l] -= beta[icol][l] * dum;
				}
		}

		for (l = n - 1; l >= 0; l--) {
			if (indxr[l] != indxc[l]) {
				for (k = 0; k < n; k++) {
					temp = alpha[k][indxr[l]];
					alpha[k][indxr[l]] = alpha[k][indxc[l]];
					alpha[k][indxc[l]] = temp;
				}
			}
		}

		return beta;
	}
}
