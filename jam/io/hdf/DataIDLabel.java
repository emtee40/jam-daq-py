package jam.io.hdf;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * Class to represent an HDF <em>Data identifier label</em> data object. The
 * label is meant to be a short probably one or 2 word <em>label</em>.
 * 
 * @version 0.5 December 98
 * @author Dale Visser
 * @since JDK1.1
 */
final class DataIDLabel extends AbstractHData {


    static DataIDLabel withTagRef(int tag, int ref) {    	
    	DataIDLabel dil =null;
		final List objectList = getDataObjectList();
		final Iterator iter = objectList.iterator();
		while(iter.hasNext()) {
			final AbstractHData dataObject=(AbstractHData)iter.next();
			if ( dataObject.getTag() ==AbstractHData.DFTAG_DIL)  {
				dil =  (DataIDLabel)dataObject;
			    if ( (dil.getObject().getTag() == tag) &&
                     (dil.getObject().getRef() == ref) ){			    		
			    	break;
			    }
			}
		}
		return dil;
	}
    		
    static DataIDLabel withTagRef(List labels, int tag, int ref) {
        DataIDLabel output = null;
        for (final Iterator temp = labels.iterator(); temp.hasNext();) {
            final DataIDLabel dil = (DataIDLabel) (temp.next());
            if ((dil.getObject().getTag() == tag)
                    && (dil.getObject().getRef() == ref)) {
                output = dil;
            }
        }
        return output;
    }
    
    /**
     * Object being labelled.
     */
    private AbstractHData object;

    private String label;

    DataIDLabel(AbstractHData obj, String label) {
        super(DFTAG_DIL); //sets tag
        object = obj;
        this.label = label;
        int byteLength = 4 + label.length();
        bytes = ByteBuffer.allocate(byteLength);
        bytes.putShort(object.getTag());
        bytes.putShort(object.getRef());
        putString(label);
    }

    DataIDLabel() {
        super();
    }

    /**
     * Implementation of <code>DataObject</code> abstract method.
     * 
     * @exception HDFException
     *                thrown if there is a problem interpreting the bytes
     */
    protected void interpretBytes() throws HDFException {
        bytes.position(0);
        final short tag = bytes.getShort();
        final short ref = bytes.getShort();
        label = getString(bytes.remaining());
        object = getObject(tag, ref);
    }

    /**
     * @return the text contained.
     */
    String getLabel() {
        return label;
    }
    
    public String toString(){
        return label;
    }

    /**
     * 
     * @return the object referred to
     */
    private AbstractHData getObject() {
        return object;
    }

}