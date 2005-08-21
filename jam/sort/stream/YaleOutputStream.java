package jam.sort.stream;

import java.io.IOException;

/**
 * This class knows how to handle Oak Ridge tape format. It extends
 * EventOutputStream, adding methods for reading events and returning them as
 * int arrays which the sorter can handle.
 * 
 * @version 0.5 April 98
 * @author Dale Visser, Ken Swartz
 * @see AbstractEventOutputStream
 * @since JDK1.1
 */
public final class YaleOutputStream extends AbstractL002HeaderWriter {

	/**
	 * Default constructor.
	 */
	public YaleOutputStream() {
		super();
	}

	/**
	 * @see AbstractEventOutputStream#AbstractEventOutputStream(int)
	 */
	public YaleOutputStream(int eventSize) {
		super(eventSize);
	}

	/**
	 * Implemented <code>EventOutputStream</code> abstract method.
	 * 
	 * @exception EventException
	 *                thrown for unrecoverable errors
	 */
	public void writeEvent(final int[] input) throws EventException {
		try {
			for (short i = 0; i < eventSize; i++) {
				if (input[i] != 0) {
					writeParameter(i, (short) input[i]);
				}
			}
			dataOutput.writeShort(EVENT_END_MARKER);
		} catch (IOException ie) {
			throw new EventException("Can't write event: " + ie.toString());
		}
	}

	 /**
		 * Writes out a event in the L002 format Implemented
		 * <code>EventOutputStream</code> abstract method.
		 * 
		 * @exception EventException
		 *                thrown for unrecoverable errors
		 */
	public void writeEvent(final short[] input) throws EventException {
		try {
			for (short i = 0; i < eventSize; i++) {
				if (input[i] != 0) {
					writeParameter(i, input[i]);
				}
			}
			dataOutput.writeShort(EVENT_END_MARKER);
		} catch (IOException ie) {
			throw new EventException("Can't write event: " + ie.toString());
		}
	}

	/*
	 * non-javadoc: NOT CURRENTLY IMPLEMENTED. Writes a single parameter. would
	 * be called by writeEvent @exception EventException thrown for errors in
	 * the event stream
	 */
	private void writeParameter(final short param, final short value)
			throws EventException {
		try {
			if (isValidParameterNumber(param)) {
				dataOutput.writeShort(parameterMarker(param));
				dataOutput.writeShort(value);
			} else {
				throw new EventException("Parameter number out of range: "
						+ param);
			}
		} catch (IOException ioe) {
			throw new EventException(ioe.toString());
		}
	}

	/**
	 * Check for end of run word
	 */
	public boolean isEndRun(short dataWord) {
		return (dataWord == RUN_END_MARKER);
	}

	/**
	 * Write the character that signifies the end of the run data.
	 */
	public void writeEndRun() throws EventException {
		try {
			dataOutput.writeShort(RUN_END_MARKER);
		} catch (IOException ioe) {
			throw new EventException(ioe.toString());
		}
	}

	/*
	 * non-javadoc: Checks whether a valid parameter number (should be 1 to 512
	 * according to ORNL documentation).
	 */
	private boolean isValidParameterNumber(short number) {
		return ((number >= 0) && (number < 2048));
	}

	/*
	 * non-javadoc: Converts a short to a valid parameter marker for the stream.
	 */
	private short parameterMarker(short number) {
		return (short) ((EVENT_PARAMETER | number) & 0xFFFF);
	}
}
