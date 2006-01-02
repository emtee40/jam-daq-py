package jam.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Contains utilities for manipulating <code>String</code> objects.
 * 
 * @author Dale Visser
 * @version 0.5
 * @see java.lang.String
 */
public final class StringUtilities {

	private static final StringUtilities INSTANCE = new StringUtilities();

	private static final Charset ASCII = Charset.availableCharsets().get(
			"US-ASCII");
	
	public static String ELEMENT_SEPARATOR ="/"; 

	private StringUtilities() {
		super();
	}

	/**
	 * Get the only instance of this class.
	 * 
	 * @return the only instance of this class
	 */
	public static StringUtilities getInstance() {
		return INSTANCE;
	}

	/**
	 * Make the full path to a data element
	 * @param parentName name of parent element
	 * @param name name of element 
	 * @return fullName
	 */
	public String makeFullName(String parentName, String name)
	{
		return parentName+ELEMENT_SEPARATOR+name;
	}

	/**
	 * Make a unique name out of the given name that differs from names in the
	 * given set.
	 * 
	 * @param name
	 *            name to make unique
	 * @param nameSet
	 *            contains the existing names
	 * @return unique name
	 */
	public String makeUniqueName(final String name, final Set<String> nameSet) {
		String nameTemp = name.trim();
		boolean isUnique = false;
		int prime = 1;
		boolean copyFound;
		/* find a name that does not conflict with existing names */
		while (!isUnique) {
			copyFound = false;
			final Iterator<String> nameIter = nameSet.iterator();
			while (nameIter.hasNext()) {
				final String nameNext = nameIter.next();
				if (nameTemp.compareTo(nameNext) == 0) {
					copyFound = true;
					break;
				}
			}
			if (copyFound) {
				final String nameAddition = "[" + prime + "]";
				nameTemp = name + nameAddition;
				prime++;
			} else {
				isUnique = true;
			}
		}
		return nameTemp;
	}

	/**
	 * Make a unique name out of the given name that differs from names in the
	 * given set.
	 * 
	 * @param name
	 *            name to make unique
	 * @param nameSet
	 *            contains the existing names
	 * @param nameLength
	 *            target length of name
	 * @return unique name
	 */
	public String makeUniqueName(final String name, final Set<String> nameSet,
			final int nameLength) {
		String nameTemp = makeLength(name, nameLength);
		boolean warn = name.length() > nameTemp.length();
		boolean isUnique = false;
		int prime = 1;
		boolean copyFound;
		/* find a name that does not conflict with existing names */
		while (!isUnique) {
			copyFound = false;
			final Iterator<String> nameIter = nameSet.iterator();
			while (nameIter.hasNext()) {
				final String nameNext = nameIter.next();
				if (nameTemp.compareTo(nameNext) == 0) {
					copyFound = true;
					break;
				}
			}
			if (copyFound) {
				final String nameAddition = "[" + prime + "]";
				nameTemp = makeLength(nameTemp, nameLength
						- nameAddition.length());
				warn |= name.length() > nameTemp.length();
				nameTemp += nameAddition;
				prime++;
			} else {
				isUnique = true;
			}
		}

		if (warn) {
			System.err.println("\"" + name
					+ "\" truncated to produce new name \"" + nameTemp + "\".");
		}
		return nameTemp;
	}

	/**
	 * Truncates a <code>String</code> or pads the end with spaces to make it
	 * a certain length.
	 * 
	 * @param input
	 *            <code>String</code> to modify
	 * @param length
	 *            desired number of characters in the <code>String</code>
	 * @return <code>String</code> with <code>length</code> characters
	 */
	public String makeLength(final String input, final int length) {
		final StringBuffer temp = new StringBuffer(input);
		for (int i = input.length(); i < length; i++) {
			temp.append(' ');
		}
		return temp.substring(0, length);
	}

	/**
	 * Creates a <code>String</code> from the given US-ASCII byte array.
	 * 
	 * @param input
	 *            US-ASCII characters as bytes
	 * @return representation of the given array
	 */
	public String getASCIIstring(final byte[] input) {
		final ByteBuffer buffer = ByteBuffer.wrap(input);
		final CharBuffer charBuffer = ASCII.decode(buffer);
		return charBuffer.toString();
	}

	/**
	 * 
	 * @param input
	 *            a string object
	 * @return array of ASCII bytes representing the input string
	 */
	public byte[] getASCIIarray(final String input) {
		final ByteBuffer buffer = ASCII.encode(input);
		return buffer.array();
	}
}
