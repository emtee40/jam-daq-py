package jam.io.hdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract class to represent a generic HDF data object.
 *
 * @version	0.5 November 98
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @since       JDK1.1
 */
public abstract class DataObject {

	/**
	 * List of Data Objects in the file.
	 */
	private static List objectList=Collections.synchronizedList(new ArrayList());
	/** Map of Data Objects in file with tag/ref as key */
	private static Map tagRefMap=Collections.synchronizedMap(new HashMap());
	/** Reference count, starts at 1 */
	static short refCount;	

	/**
	 * Tag for machine type.  
	 *
	 * @see JavaMachineType
	 */
	public final static short DFTAG_MT = 107;

	/**
	 * HDF tag for Data identifier annotation
	 *
	 * @see DataIDAnnotation
	 */
	public final static short DFTAG_DIA = 105;

	/**
	 * HDF tag for Data identifier label.
	 *
	 * @see DataIDLabel
	 */
	public final static short DFTAG_DIL = 104;

	/**
	 * HDF tag for File Identifier.
	 *
	 * @see FileIdentifier
	 */
	public final static short DFTAG_FID = 100;

	/**
	 * HDF tag for File Description.
	 *
	 * @see FileDescription
	 */
	public final static short DFTAG_FD = 101;

	/**
	 * HDF tag for number type.
	 *
	 * @see NumberType
	 */
	public final static short DFTAG_NT = 0x006a;

	/**
	 * HDF tag for Library version number
	 *
	 * @see LibVersion
	 */
	public final static short DFTAG_VERSION = 30;

	/**
	 * HDF tag for Numerical Data Group
	 *
	 * @see NumericalDataGroup
	 */
	public final static short DFTAG_NDG = 720;

	/**
	 * HDF tag for Scientific Data
	 *
	 * @see ScientificData
	 */
	public final static short DFTAG_SD = 702;

	/**
	 * HDF tag for Scientific data dimension records
	 *
	 * @see ScientificDataDimension
	 */
	public final static short DFTAG_SDD = 701;

	/**
	 * HDF tag for Scientific data labels
	 *
	 * @see ScientificDataLabel
	 */
	public final static short DFTAG_SDL = 704;

	/**
	 * HDF tag for Scientific data scales
	 */
	public final static short DFTAG_SDS = 703;

	/**
	 * HDF tag for Vgroup
	 *
	 * @see VirtualGroup
	 */
	public final static short DFTAG_VG = 1965;

	/**
	 * HDF tag for Vdata description
	 *
	 * @see VdataDescription
	 */
	public final static short DFTAG_VH = 1962;

	/**
	 * HDF tag for Vdata
	 *
	 * @see Vdata
	 */
	public final static short DFTAG_VS = 1963;
	
	//Instance members
	
	/**
	 * contains the 2-byte tag for the data type
	 */
	protected short tag;
		
	/**
	  * Unique reference number, in case several data elements with the same tag exist. Only makes sense in 
	  * the context of an HDF file.
	  */
	protected short ref;	
	/**
	 * Offset from start of file.
	 */
	protected int offset;
	/**
	 * Before bytes is created, length of bytes.
	 */
	protected int length;
	/**
	 * Actual bytes stored in HDF file.
	 */
	protected byte[] bytes;

	/**
	 * Set to false once the ref number is defined.
	 */
	protected boolean haveNotSetRef=true;
	
	/**
	 * Get the list of all data objects
	 * @return
	 */
	static List getDataObjectList() {
		return objectList;
	}
	/**
	 * Clear the lists of all data objects
	 * @return
	 */	
	static void clear() {
	
		for (Iterator it=objectList.iterator(); it.hasNext();){
			DataObject ob=(DataObject)it.next();
			ob.bytes=null;
		}

		objectList.clear();
		tagRefMap.clear();
		refCount =0;

	}
	/**
	 * <p>Adds the data object to the list of objects.
	 * --- FIXME KBS remove --  
	 * The reference number is 
	 * implicitly assigned at this time as the index number in the 
	 * internal <code>Vector objectList</code>.  A typical call should 
	 * look like:</p>
	 * <blockquote><code>hdf = new hdfFile(outfile, "rw");<br>
	 * hdf.addDataObject(new DataObject(this));</code></blockquote>
	 * <p>Each call causes setOffsets to be called.</p>
	 * ---
	 *
	 * @param data data object
	 * @see	#setOffsets()
	 */
	static void addDataObjectToList(DataObject data) {
		final Integer key =data.getKey();
		if (!tagRefMap.containsKey(key)){
			tagRefMap.put(key, data);
			objectList.add(data);
		}
	}
	/**
	 * @return object in file with the matching tag and ref
	 * @param t tag of HDF object
	 * @param r <em>unique</em> reference number in file
	 */
	public static DataObject getObject(short t, short r) {
		DataObject match = null;
		final Integer key =calculateKey(t, r);		
		if (tagRefMap.containsKey(key)){
			match=(DataObject)tagRefMap.get(key);
		}
		return match;
	}
	
	
	/**
	 * 
	 * The HDF standard only requires that for a particular tag type, each instance have a
	 * unique ref.
	 * ----------------NO TRUE ANY LONGER-----------------  
	 * Since our files are not expected to contain more than
	 * several dozen objects, 
	 * I take the simplest approach of simply
	 * assigning the index number + 1 from the objectList.
	 * ----------------NO TRUE ANY LONGER-----------------
	 * 
	 * Just adds one to ref Count
	 * 
	 * @return a reference number for the given HDF object
	 * @param refs the map for a given tag type
	 */
	static short getUniqueRef() {
		//Just add 1, set to 0 every time DataObject.clear is called 
		return ++refCount;
	}

	/**
	 * Create a unique key given then tag an ref numbers
	 */
	static Integer calculateKey(short tag, short ref){
		int key= (((int)tag)<<16)+(int)ref;		
		return new Integer(key);
	}
	
	/**
	 * Creates a new HDF DataObject, belonging to the specified <code>HDFile</code>.  My approach is to have a 
	 * separate HDFile object for each physical HDF file on disk.  Each <code>HDFile</code> object
	 * handles the bookkeeping of the HDF File Header (see NCSA HDF: Specifications and Developer's Guide v3.2).
	 *
	 * @param	file	The file to contain the new object.
	 * @param	tag	The hdf tag of the new object.
	 */
	DataObject(short tag) {
		setTag(tag);
		setRef(getUniqueRef());
		addDataObjectToList(this); //ref gets set in this call
	}
	
	/* non-javadoc:
	 * Creates a new <code>DataObject</code> with the specified byte array as the data which will (or does already) 
	 * physically
	 * reside in the file.
	 *
	 * @param	f	    The file to contain the new object.
	 * @param	data	    The byte representation of the data.
	 * @param	r   The unique value specifying the type of data object.
	 */
	DataObject(byte[] data, short t, short r) {
		setTag(t);
		setRef(r);
		this.bytes = data;
		addDataObjectToList(this);
	}

	/* non-javadoc:
	 * Creates a new <code>DataObject</code> pointing to the offset in the file where its data reside.  This
	 * option is for when you don't want to hog memory with a large byte array.
	 *
	 * @param	file	    The file to contain the new object.
	 * @param	offset	    The location in <code>file</code>
	 * @param	reference   The unique value specifying the type of data object.
	 */
	DataObject(int offset, int length, short t, short reference) {
		this.tag=t;
		setRef(reference);
		this.offset = offset;
		this.length = length;
		addDataObjectToList(this);
	}
	
	private final void setTag(short t){
		tag=t;
	}

	/* non-javadoc:
	 * Returns a 4-byte representation of the tag, usually defined in the HDF standard, of this item.  
	 * The tag is a 2-byte integer
	 * denoting a unique data object type.
	 */
	final short getTag() {
		return tag;
	}
	/**
	 * Set the reference which is 2 bytes
	 * 
	 * @param newref
	 */
	final void setRef(short newref) {
		if (haveNotSetRef) {
			ref = newref;
		} else {
			if (ref!=newref){
				//Change key, 
				//remove and add with new key
				Integer key = calculateKey(tag, ref);
				if (tagRefMap.containsKey(key)) {
					tagRefMap.remove(key);
					//Add
					ref = newref;
					Integer keyNew = calculateKey(tag, ref);
					tagRefMap.put(keyNew, this);
				 }
			}			
			haveNotSetRef=false;
		}
	}
	
	/**
	 * Returns a 2-byte representation of the reference number, which is unique for any given
	 * tag type in an HDF file.  In my code, it is unique, period, but the HDF standard does not 
	 * expect or require this.
	 * 
	 * @return reference number
	 */
	public final short getRef() {
		return ref;
	}
	/* non-javadoc:
	 * Called back by <code>HDFile</code> to set the offset information.
	 */
	void setOffset(int off) {
		offset = off;
	}

	int getOffset() {
		return offset;
	}
	/**
	 * Returns the length of the byte array in the file for this data element.
	 * 
	 * @return he length of the byte array in the file for this data element
	 */
	protected int getLength() {
		return bytes.length;
	}

	/* non-javadoc:
	 * Returns the byte representation to be written at <code>offset</code> in the file.
	 */
	byte[] getBytes()  {
		return bytes;
	}

	protected Integer getKey(){
		return calculateKey(tag, ref);
	}
	
	/**
	 * When bytes are read from file, sets the internal fields of the data object.
	 *
	 * @exception   HDFException	    thrown if unrecoverable error occurs
	 */
	protected abstract void interpretBytes() throws HDFException;


}
