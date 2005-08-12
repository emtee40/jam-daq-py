package jam.sort.stream;
import jam.global.MessageHandler;

import java.io.EOFException;
import java.io.IOException;

/**
 * This class knows how to handle Oak Ridge tape format.
 *
 * @version	0.5 April 98
 * @author 	Dale Visser, Ken Swartz
 * @see         AbstractEventInputStream
 * @since       JDK1.1
 */
public class LdfInputStream extends AbstractEventInputStream implements L002Parameters {

    private EventInputStatus status;
    private int parameter;

    //private final long DATA=0x4441544100200000L;
    private final short DA=0x4441;
    private final short TA=0x5441;
    private final int REST = 0x00200000;
    
    //make sure to issue a setConsole() after using this constructor
    //It is here to satisfy the requirements of Class.newInstance()
    /** Called by Jam to create an instance of this input stream.
     */
    public LdfInputStream(){
        super();
    }

    /** Default constructor.
     * @param console object where messages to the user are printed
     */
    public LdfInputStream(MessageHandler console) {
        super(console);
    }

    /** Creates the input stream given an event size.
     * @param eventSize number of parameters per event.
     * @param console object where messages to the user are printed
     */
    public LdfInputStream(MessageHandler console,int eventSize) {
        super(console, eventSize);
    }

    private boolean skip=true;
    
    /** Reads an event from the input stream
     * Expects the stream position to be the beginning of an event.
     * It is up to the user to ensure this.
     * @param input source of event data
     * @exception EventException thrown for errors in the event stream
     * @return status after attempt to read an event
     */
    public synchronized EventInputStatus readEvent(int[] input) throws  EventException {
        boolean gotParameter=false;
        try {
            if (skip) {
                boolean stop = false;
                do {
                    stop = dataInput.readShort() == DA;
                    if (stop){
                        stop=dataInput.readShort() == TA;
                    }
                    if (stop){
                        stop = dataInput.readInt() == REST;
                    }
                } while (!stop);
                skip = false;
            }
            while(isParameter(readVaxShort())){//could be event or scaler parameter
                gotParameter=true;
                if (status == EventInputStatus.PARTIAL_EVENT) {
                    if (parameter >= eventSize) {//skip, since array index would be too great for event array
                        dataInput.readShort();
                    } else {//read into array
                        input[parameter]= readVaxShort();	//read event word
                    }
                } else if (status == EventInputStatus.SCALER_VALUE) {
                    dataInput.readInt();//throw away scaler value
                }
            }
        } catch (EOFException eofe) {// we got to the end of a file or stream
            status=EventInputStatus.END_FILE;
        } catch (Exception e){
            status=EventInputStatus.UNKNOWN_WORD;
            throw new EventException(getClass().getName()+".readEvent() parameter = "+parameter,e);
        }
        if (!gotParameter && status == EventInputStatus.EVENT) {
            status = EventInputStatus.IGNORE;
        }
        return status ;
    }

    /* non-javadoc:
     * Read an event parameter.
     */
    private boolean isParameter(short paramWord) {
        boolean parameterSuccess;
        //check special types parameter
        if (paramWord==EVENT_END_MARKER){
            parameterSuccess=false;
            status=EventInputStatus.EVENT;
        } else if  (paramWord==BUFFER_END_MARKER){
            parameterSuccess=false;
            status=EventInputStatus.END_BUFFER;
        } else if (paramWord==RUN_END_MARKER){
            parameterSuccess=false;
            status=EventInputStatus.END_RUN;
            //get parameter value if not special type
        } else if ((paramWord & EVENT_PARAMETER_MARKER) != 0) {
            int paramNumber = paramWord & EVENT_PARAMETER_MASK;
            if (paramNumber < 2048) {
                parameter=paramNumber;//parameter number used in array
                parameterSuccess=true;
                status=EventInputStatus.PARTIAL_EVENT;
            } else {// 2048-4095 assumed
                parameterSuccess=true;
                status = EventInputStatus.SCALER_VALUE;
            }
        } else {//unknown word
            parameterSuccess=false;
            skip=true;
            status=EventInputStatus.IGNORE;
        }
        return parameterSuccess;
    }

    /** Check for end of run word
     * @param dataWord smallest atomic unit in data stream
     * @return whether the data word was an end-of-run word
     */
    public synchronized boolean isEndRun(short dataWord){
        return (dataWord==RUN_END_MARKER);
    }
    
    public boolean readHeader(){
        return true;
    }
    
    /* non-javadoc:
	 * reads a little endian short (2 bytes)
	 * but return a 4 byte integer
	 */
	private short readVaxShort() throws IOException {
		int ch1 = dataInput.read();
		int ch2 = dataInput.read();
		if ((ch1 | ch2) < 0) {
			return -1;
		}
		return (short) ((ch2 << 8) + (ch1 << 0));
	}
}

