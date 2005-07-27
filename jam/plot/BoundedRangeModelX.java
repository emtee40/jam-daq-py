package jam.plot;


/**
 * <code>Scroller</code> contains instance of this, and it is be handed to the
 * horizontal <code>JScrollBar</code>.<code>setFields()</code> is to be
 * called whenever the displayed <code>Histogram</code> changes.
 * 
 * @author Dale Visser
 * @version 1.2
 */
final class BoundedRangeModelX extends AbstractScrollBarRangeModel {

	BoundedRangeModelX(PlotContainer container) {
		super(container);
	}

	void scrollBarMoved() {
		final int maxX = getValue() + getExtent();
		final int minX = getValue();
		if (lim != null) {
			lim.setLimitsX(minX, maxX);
		}
	}

	/**
	 * Set model using values in Limits object.
	 */
	protected void setDisplayLimits() {
		int min, max, extent, value;
		min = 0;
		max = plot.getSizeX() - 1;
		if (lim == null) {
			extent = max - min + 1;
			value = 0;
		} else {
			extent = lim.getMaximumX() - lim.getMinimumX();
			value = lim.getMinimumX();
		} 
		/*
		 * BoundedRangeModel method, throws appropriate event up to scroll bar.
		 */
		setRangeProperties(value, extent, min, max, true);
	}
}