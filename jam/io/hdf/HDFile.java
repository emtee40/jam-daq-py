package jam.io.hdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

/**
 * Class which reads and writes DataObjects to and from
 * HDF files on disk.
 *
 * @author 	Dale Visser
 * @author Ken Swartz
 * @since       JDK1.1
 */
public final class HDFile extends RandomAccessFile implements HDFconstants {

	/**
	 * Check if a file is an HDF file
	 * @param file
	 * @return true if file is an hdf file
	 */
	static boolean isHDFFile(File file) {
		final HDFileFilter filter=new HDFileFilter(false);
		return filter.accept(file);
	}
	
	private final AsyncProgressMonitor monitor;
	
	private final int stepsToTake;
	
	private boolean lazyLoadData =true;
	/** Number of data objects to lazy load */
	private int lazyLoadNum;
	
	/** Count number of data object that have been lazy loaded */
	private int lazyCount;
	
	/**
	 * variable for marking position in file
	 */
	private transient long mark = 0;

	/**
	 * Constructor called with a <code>File</code> object, and an access
	 * mode.
	 *
	 * @param file file to be accessed
	 * @param mode "r" or "rw"
	 * @param progMon progress monitor
	 * @param steps to take to completion
	 * @exception FileNotFoundException if given file not found
	 */
	HDFile(File file, String mode, AsyncProgressMonitor progMon,
            int steps) throws FileNotFoundException {
        super(file, mode);
        monitor = progMon;
        stepsToTake = steps;
    }
	
	/**
	 * Constructor called with a <code>File</code> object, and an access
	 * mode.
	 *
	 * @param file file to be accessed
	 * @param mode "r" or "rw"
	 * @exception FileNotFoundException if given file not found
	 */
	public HDFile(File file, String mode) throws FileNotFoundException {
		this(file, mode, null, 0);//ignores progress
	}
	
	void setLazyLoadData(boolean lazy) {
		lazyLoadData=lazy;
	}
	
	boolean isLazyLoadData() {
		return lazyLoadData;
	}
	/* non-javadoc:
	 * Write a hdf file from all DataObjects
	 * 
	 * @throws HDFException
	 */
	void writeFile() throws HDFException {
		updateBytesOffsets();            
        writeMagicWord();	      
        writeDataDescriptorBlock();
        writeAllObjects();
	}
	
	/**
	 * Looks at the internal index of data elements and sets the offset
	 * fields of the <code>DataObject</code>'s. To be run when all data
	 * elements have been defined.
	 */
	private synchronized void updateBytesOffsets() {
		//final int DDblockSize = 2 + 4 + 12 * objectList.size();
		final int initOffset = sizeDataDescriptorBlock() + 4; //add in HDF file header
		int counter = initOffset;
		final Iterator temp = DataObject.getDataObjectList().iterator();
		while (temp.hasNext()) {
			final DataObject dataObject = (DataObject) (temp.next());
			dataObject.refreshBytes();
			dataObject.setOffset(counter);
			counter += dataObject.getBytes().capacity();
		}
	}
	
	private final int sizeDataDescriptorBlock(){
	    /* The size of the DD block. */
		/* numDD's + offset to next (always 0 here) + size*12 for
		 * tag/ref/offset/length info */
		final int size = DataObject.getDataObjectList().size();
	    return 2 + 4 + 12 * size;
	}


	/**
	 * Writes the unique 4-byte pattern at the head of the file denoting
	 * that it is an HDF file.
	 *
	 * @throws HDFException error with writing hdf file
	 */
	private void writeMagicWord() throws HDFException {
		try {
			seek(0);
			writeInt(HDF_HEADER);
		} catch (IOException e) {
			throw new HDFException("Problem writing HDF header.",e);
		}
	}
	
	/**
	 *
	 * @exception HDFException unrecoverable errror
	 */
	private synchronized void writeDataDescriptorBlock() throws HDFException {
		final List objectList = DataObject.getDataObjectList();
		try {
			seek(4); //skip header
			writeShort(objectList.size()); //number of DD's
			writeInt(0); //no additional descriptor block
			final Iterator temp = objectList.iterator();
			while (temp.hasNext()) {
				final DataObject dataObject = (DataObject) (temp.next());
				writeShort(dataObject.getTag());
				writeShort(dataObject.getRef());
				writeInt(dataObject.getOffset());
				writeInt(dataObject.getBytes().capacity());
			}
		} catch (IOException e) {
			throw new HDFException(
				"Problem writing DD block.",e);
		}
	}

	/* non-javadoc:
	 * Called after all <code>DataObject</code> objects have been 
	 * created.
	 *
	 * @exception HDFException thrown if err occurs during file write
	 */
	private void writeAllObjects() throws HDFException {
		final List objectList = DataObject.getDataObjectList();
		int countObjct=0;
		final int numObjSteps = getNumberObjctProgressStep(objectList.size());
		final Iterator temp = objectList.iterator();
		boolean foundEmpty = false;
		writeLoop: while (temp.hasNext()) {
			if (countObjct%numObjSteps==0) {
				monitor.increment();
			}
			final DataObject dataObject = (DataObject) (temp.next());
			if (dataObject.getBytes().capacity() == 0){
			    foundEmpty=true;
			    break writeLoop;
			}
			writeDataObject(dataObject);
			countObjct++;
		}
		if (foundEmpty){
			throw new HDFException("DataObject with no length encountered, halted writing HDF File.");
		}
	}
		
	/**
	 * Given a data object, writes out the appropriate bytes to the file
	 * on disk.
	 *
	 * @param	data	HDF data element
	 * @exception   HDFException	    thrown if unrecoverable error occurs
	 */
	private void writeDataObject(DataObject data) throws HDFException {
		try {
			seek(data.getOffset());
			write(data.getBytes().array());
		} catch (IOException e) {
			throw new HDFException(
				"Problem writing HDF data object.",e);
		}
	}
	
	/**
	 * Reads file into set of DataObject's and sets their internal
	 * variables.
	 * 
	 *  @exception HDFException unrecoverable error
	 */
	public void readFile() throws HDFException {
		lazyLoadNum=0;
		lazyCount=0;	
		try {
			if (!checkMagicWord()) {
				throw new HDFException("Not an hdf file");
			}
			seek(HDF_HEADER_NBYTES);
			boolean doAgain = true;
			do {
				final int numDD = readShort(); //number of DD's
				final int nextBlock = readInt();
				//Just one dd block for now
				final int numObjSteps=getNumberObjctProgressStep(numDD);
				int countObjct=0;
				for (int i = 1; i <= numDD; i++) {
					final short tag = readShort();
					final short ref = readShort();
					final int offset = readInt();
					final int length = readInt();
					
					//Load scientific data as last moment needed
					if ( lazyLoadData &&
						 tag==DataObject.DFTAG_SD)
					{
						DataObject.create(tag,ref,offset,length);
						lazyLoadNum++;
					} else {
						final byte [] bytes = readBytes(offset,length);
						DataObject.create(bytes,tag,ref);
					}
					countObjct++;
					//Update progress bar
					if (countObjct%numObjSteps==0 && monitor != null) {
						monitor.increment();
					}

				}
				if (nextBlock == 0) {
					doAgain = false;
				} else {
					seek(nextBlock);
				}
			} while (doAgain);
		} catch (IOException e) {
			throw new HDFException(
				"Problem reading HDF file objects. ",e);
		}
	}
	
	private byte [] readBytes(int offset, int length) throws IOException {
		final byte [] rval = new byte[length];
		mark();
		seek(offset);
		read(rval);
		reset();
		return rval;
	}
	
	/* non-javadoc:
	 * Checks whether this file contains the HDF magic word 
	 * at the beginning.
	 *
	 * @return <code>true</code> if this file has a valid HDF magic word
	 */
	private boolean checkMagicWord() throws IOException {
		seek(0);
		final int magicInt =readInt();
		return (magicInt == HDF_HEADER);
	}
	
	/**
	 * Helper for reading objects
	 *  @exception IOException unrecoverable error
	 */
	private synchronized void mark() throws IOException {
            mark = getFilePointer();
    }
	
	/**
	 * Helper for reading objects
	 *  @exception IOException unrecoverable error
	 */
	private synchronized void reset() throws IOException {
		seek(mark);
	}

	/**
	 * First, calls <code>super.close()</code>, then clears collections of temporary objects used
	 * to build the file, and then sets their references to
	 * to <code>null</code>. 
	 * 
	 * @see java.io.RandomAccessFile#close()
	 */
	public void close() throws IOException{
		super.close();
	}

	/* non-javadoc:
	 * Lazy load the bytes for an object
	 */
	byte [] lazyReadData(DataObject dataObject) throws HDFException {
        final int numObjSteps=getNumberObjctProgressStep(lazyLoadNum);
		final byte [] localBytes = new byte[dataObject.getLength()];	
		try {
	        seek(dataObject.getOffset());
	        read(localBytes);
			if (lazyCount%numObjSteps==0) {
				if (monitor!=null) {	//FIXME KBS cleanup
					monitor.increment();
				}
			}
			lazyCount++;
		} catch (IOException e) {
			throw new HDFException(
				"Problem lazy reading data objects. ",e);
		}		
		return localBytes;
	}
	
	private int getNumberObjctProgressStep(int numObjects) {
		int rval;
		if (stepsToTake>0) {
			rval =numObjects/stepsToTake;
			if (lazyLoadData){	//half the steps if lazy load (redo to take care of round off)
				rval=2*numObjects/stepsToTake;
			}
			if (rval <=0) {
				rval=1;
			}
		} else {
			rval =1;
		}
		return rval;
	}

}
