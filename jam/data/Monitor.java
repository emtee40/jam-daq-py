package jam.data;

import jam.global.Sorter;

import java.applet.AudioClip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is for monitoring the status of data acquisition. Monitors can
 * show the status of things like event rates, beam current, rate of growth in a
 * histogram, etc.
 * 
 * @author Ken Swartz
 * @author Dale Visser
 * @version 0.5,0.9
 * @since JDK 1.1
 */
public final class Monitor {

	/**
	 * Lookup table for all monitors.
	 */
	public static Map monitorTable = Collections.synchronizedMap(new HashMap());

	/**
	 * List of all monitors.
	 */
	public static List monitorList = Collections
			.synchronizedList(new ArrayList());

	/** The update interval */
	private static int interval;

	private transient final String name; //name

	private transient final Object source;

	private double threshold;

	private double maximum;

	private boolean alarm;

	private java.applet.AudioClip audioClip;

	private transient double valueNew; //the newest value set

	private transient double valueOld; //the previous value set

	private double value; //value for testing

	/**
	 * Constructs an monitor object which delegates to a given
	 * <code>Sorter</code> for the caluclation of its current
	 * value.
	 * 
	 * @param monitorName
	 *            name of the monitor for display in dialog
	 * @param sort
	 *            the sort routine which produces the monitor values
	 */
	public Monitor(String monitorName, Sorter sort) {
		name = monitorName;
		source = sort;
		if (source==null){
			throw new IllegalArgumentException("Monitor \""+monitorName+"\": source must be non-null.");
		}
		addToCollections();
	}

	private final void addToCollections() {
		monitorTable.put(name, this);
		monitorList.add(this);
	}

	/**
	 * Constructs an object which monitors rate of increase
	 * in the given <code>Scaler</code>.
	 * 
	 * @param monitorName
	 *            name of the monitor for display in dialog
	 * @param scaler
	 *            the scaler which is monitored
	 */
	public Monitor(String monitorName, Scaler scaler) {
		name = monitorName;
		source = scaler;
		if (source==null){
			throw new IllegalArgumentException("Monitor \""+monitorName+"\": source must be non-null.");
		}
		addToCollections();
	}

	/**
	 * Constructs an object which monitors the rate of counts
	 * in a particular <code>Gate</code>.
	 * 
	 * @param monitorName
	 *            name of the monitor for display in dialog
	 * @param gate
	 *            the gate whose area is monitored
	 */
	public Monitor(String monitorName, Gate gate) {
		name = monitorName;
		source = gate;
		if (source==null){
			throw new IllegalArgumentException("Monitor \""+monitorName+"\": source must be non-null.");
		}
		addToCollections();
	}

	/**
	 * Set the interval in seconds at which updates occur.
	 * 
	 * @param intervalIn
	 *            interval in seconds
	 */
	public synchronized static void setInterval(int intervalIn) {
		interval = intervalIn;
	}

	/**
	 * Gets the interval in seconds at which updates occur.
	 * 
	 * @return interval in seconds
	 */
	public synchronized static int getInterval() {
		return interval;
	}

	/**
	 * Returns the list of monitors.
	 * 
	 * @return the list of monitors
	 */
	public static List getMonitorList() {
		return Collections.unmodifiableList(monitorList);
	}

	/**
	 * Sets the list of monitor objects.
	 * 
	 * @param inMonList
	 *            must contain all <code>Monitor</code> objects
	 */
	public static void setMonitorList(List inMonList) {
		clearList();
		for (final Iterator allMonitors = inMonList.iterator(); allMonitors
				.hasNext();) {
			final Monitor monitor = (Monitor) allMonitors.next();
			final String name = monitor.getName();
			monitorTable.put(name, monitor);
			monitorList.add(monitor);
		}
	}

	/**
	 * Clears the list of monitors.
	 */
	public static void clearList() {
		monitorTable.clear();
		monitorList.clear();
		/* run garbage collector */
		System.gc();
	}

	/**
	 * Returns this monitor's name. The name is used in display and to retrieve
	 * the monitor.
	 * 
	 * @return this montor's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns this monitor's current value.
	 * 
	 * @return this monitor's current value
	 */
	public synchronized double getValue() {
		return value;
	}

	/**
	 * Sets this monitor's latest value.
	 * 
	 * @param valueIn
	 *            the new value
	 */
	public synchronized void setValue(int valueIn) {
		valueNew = valueIn;
	}

	/**
	 * Sets this monitor's value to zero.
	 */
	public synchronized void reset() {
		value = 0;
	}

	/**
	 * Updates this monitor, calculating the latest monitor values. Keeps the
	 * most recent value, too, for rate determination.
	 */
	public void update() {
		if (source instanceof Scaler) {
			valueNew = ((Scaler)source).getValue();
			value = (valueNew - valueOld) / interval;
			valueOld = valueNew;
		} else if (source instanceof Gate) {
			valueNew = ((Gate)source).getArea();
			value = (valueNew - valueOld) / interval;
			valueOld = valueNew;
		} else if (source instanceof Sorter) {
			value = ((Sorter)source).monitor(name);
		}
	}

	/**
	 * Sets the threshold value, which is the minimum value for a monitor to
	 * have without <code>MonitorControl</code> issuing a warning beep.
	 * 
	 * @param inThreshold
	 *            the new minimum
	 * @see jam.data.control.MonitorControl
	 */
	public synchronized void setThreshold(double inThreshold) {
		threshold = inThreshold;
	}

	/**
	 * Returns the threshold value for this monitor.
	 * 
	 * @return the threshold value
	 */
	public synchronized double getThreshold() {
		return threshold;
	}

	/**
	 * Sets the maximum value, which is the maximum value for a monitor to have
	 * without <code>MonitorControl</code> issuing a warning beep.
	 * 
	 * @param inMaximum
	 *            the new maximum
	 * @see jam.data.control.MonitorControl
	 */
	public synchronized void setMaximum(double inMaximum) {
		maximum = inMaximum;
	}

	/**
	 * Returns the maximum value for this monitor.
	 * 
	 * @return the maximum value for this monitor
	 */
	public synchronized double getMaximum() {
		return maximum;
	}

	/**
	 * Sets whether the alarm is activated. If the alarm is not activated,
	 * <code>MonitorControl</code> simply turns the indicator bar red when the
	 * value is below threshold or above the maximum. If it is activated, an
	 * alarm sound is issued too.
	 * 
	 * @param inAlarm
	 *            <code>true</code> if an audible alarm is desired,
	 *            <code>false</code> if not
	 */
	public synchronized void setAlarm(boolean inAlarm) {
		alarm = inAlarm;
	}

	/**
	 * Returns whether alarm is activated or not.
	 * 
	 * @return <code>true</code> if an audible alarm is desired,
	 *         <code>false</code> if not
	 */
	public synchronized boolean getAlarm() {
		return alarm;
	}

	/**
	 * NOT YET IMPLEMENTED, Sets an <code>AudioClip</code> object to be played
	 * for alarms if the alarm is enabled. Currently, the plan is to fully
	 * implement this when the JDK 1.2 <code>javax.media</code> packeage is
	 * available.
	 */
	public synchronized void setAudioClip(AudioClip clip) {
		audioClip = clip;
	}

	/**
	 * NOT YET IMPLEMENTED, Gets the current <code>AudioClip</code> object to
	 * be played for alarms if the alarm is enabled. Currently, the plan is to
	 * fully implement this when the JDK 1.2 <code>javax.media</code> packeage
	 * is available.
	 * 
	 * @return the sound clip for this monitor's alarm, <code>null</code>
	 *         indicates that a default system beep is desired
	 */
	public synchronized AudioClip getAudioClip() {
		return audioClip;
	}

	public synchronized boolean isAcceptable() {
		return value > maximum || value < threshold;
	}
}