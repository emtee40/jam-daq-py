package jam.io.hdf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Class to represent an HDF <em>Data identifier annotation</em> data object.  
 * An annotation is lenghtier than a label, and can hold a descriptive text block.
 
 * @version	0.5 December 98
 * @author 	Dale Visser
 * @since       JDK1.1
 * @see		DataIDLabel
 */
final class DataIDAnnotation extends DataObject {

	/**
	 * Object being annotated.
	 */
	private DataObject object;

	/**
	 * Text of annotation.
	 */
	private String note;

	/**
	 * Annotate an existing <code>DataObject</code> with specified annotation text.
	 *
	 * @param obj   item to be annotated
	 * @param note  text of annotation
	 * @exception  HDFException thrown on unrecoverable error 
	 */
	DataIDAnnotation(DataObject obj, String note) throws HDFException {
		super(obj.getFile(), DFTAG_DIA); //sets tag
		try {
			this.object = obj;
			this.note = note;
			int byteLength = 4 + note.length();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(byteLength);
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(object.getTag());
			dos.writeShort(object.getRef());
			dos.writeBytes(note);
			bytes = baos.toByteArray();
		} catch (IOException e) {
			throw new HDFException("Problem creating DIA.",e);
		}
	}

	DataIDAnnotation(HDFile hdf, byte[] data, short t, short reference) {
		super(hdf, data, t,reference);
	}

	/**
	 * Implementation of <code>DataObject</code> abstract method.
	 *
	 * @exception HDFException thrown if there is a problem interpreting the bytes
	 */
	protected void interpretBytes() throws HDFException {
		short tag;
		short ref;
		byte[] temp;
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		DataInputStream dis = new DataInputStream(bais);

		try {
			tag = dis.readShort();
			ref = dis.readShort();
			temp = new byte[bytes.length - 4];
			dis.read(temp);
			note = new String(temp);
			object = file.getObject(tag, ref);
		} catch (IOException e) {
			throw new HDFException(
				"Problem interpreting DIA.", e);
		}
	}

	String getNote() {
		return note;
	}

	private DataObject getObject() {
		return object;
	}

	/**
	 * 
	 * @param labels list of <code>DataIDAnnotation</code>'s
	 * @param tag to look for
	 * @param ref to look for
	 * @return annotation object that refers to the object witht the given
	 * tag and ref
	 */
	static DataIDAnnotation withTagRef(
		List labels,
		int tag,
		int ref) {
		DataIDAnnotation output=null;
		for (Iterator temp = labels.iterator(); temp.hasNext();) {
			DataIDAnnotation dia = (DataIDAnnotation) (temp.next());
			if ((dia.getObject().getTag() == tag)
				&& (dia.getObject().getRef() == ref)) {
				output = dia;
			}
		}
		return output;
	}
}
