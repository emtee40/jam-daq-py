package jam.commands;

import jam.data.DataBase;
import jam.data.Group;
import jam.data.Histogram;
import jam.data.control.AbstractControl;
import jam.global.BroadcastEvent;
import jam.global.SortMode;
import jam.io.FileOpenMode;
import jam.io.hdf.HDFIO;
import jam.io.hdf.HDFileFilter;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

/**
 * Open a hdf file
 *  
 * @author Ken Swartz
 *
 */
final class OpenHDFCmd extends AbstractCommand implements Observer, HDFIO.AsyncListener {
	
	private File openFile=null;
	private final HDFIO hdfio;
	
	OpenHDFCmd(){
		putValue(NAME,"Open\u2026");
		putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_O, CTRL_MASK));
		Frame frame= STATUS.getFrame();
		hdfio = new HDFIO(frame, msghdlr);		
		
	}

	/* 
	 * @see jam.commands.AbstractCommand#execute(java.lang.Object[])
	 */
	protected void execute(final Object[] cmdParams) {
		File file=null;
		if (cmdParams!=null) {
			if (cmdParams.length>0)
				file =(File)cmdParams[0];			
		}		
		readHDFFile(file);
	}
	/**
	 * Read in a HDF file
	 * @param cmdParams
	 */ 
	private void readHDFFile(File file) {

		final boolean isFileReading;		

		if (file==null) {//No file given				
	        final JFileChooser jfile = new JFileChooser(HDFIO.getLastValidFile());
	        jfile.setFileFilter(new HDFileFilter(true));
	        final int option = jfile.showOpenDialog(STATUS.getFrame());
	        // dont do anything if it was cancel
	        if (option == JFileChooser.APPROVE_OPTION
	                && jfile.getSelectedFile() != null) {
	        	file = jfile.getSelectedFile();
	        	openFile=file;	//Save for callback
	        	DataBase.getInstance().clearAllLists();
	    		hdfio.setListener(this);
				isFileReading=hdfio.readFile(FileOpenMode.OPEN, file);	        	
	        } else {
	        	isFileReading=false;
	        }	
		} else {
			isFileReading=hdfio.readFile(FileOpenMode.OPEN, file);
		}
		//File was not read in so no call back do notify here		
		if (!isFileReading){	
			hdfio.removeListener();
			notifyApp(null);
		}								
	}
	
	/* (non-Javadoc)
	 * @see jam.commands.AbstractCommand#executeParse(java.lang.String[])
	 */
	protected void executeParse(String[] cmdTokens) {		
		Object [] cmdParams = new Object[1]; 
		if (cmdTokens.length==0) {
			execute(null);
		} else {
			File file = new File(cmdTokens[0]); 
			cmdParams[0]=file;
			execute(cmdParams);
		}
	}

	private void notifyApp(File file) {
		
		Histogram firstHist;
 
		//Set general status 
		STATUS.setOpenFile(file);
		AbstractControl.setupAll();
		BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_ADD);
		
		//Set selection of group and histogram
		
		//Set to first group
		if (Group.getGroupList().size()>0 ) {
			Group.setCurrentGroup((Group)Group.getGroupList().get(0));
		}
		//Set the current histogram to the first opened histogram
		if (Group.getCurrentGroup().getHistogramList().size()>0 ) {
			firstHist = (Histogram)Group.getCurrentGroup().getHistogramList().get(0);
		}else{
			firstHist=null;
		}			
		STATUS.setCurrentHistogram(firstHist);
		BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_SELECT, firstHist);
	}			
	
	public void update(Observable observe, Object obj){
		final BroadcastEvent be=(BroadcastEvent)obj;
		final BroadcastEvent.Command command=be.getCommand();
		if (command==BroadcastEvent.Command.SORT_MODE_CHANGED){
			enable();
		}
	}
	
	private void enable(){
		final SortMode mode=STATUS.getSortMode();
		setEnabled(mode==SortMode.FILE || mode==SortMode.NO_SORT);		
	}
	/**
	 * Called by HDFIO when asynchronized IO is completed  
	 */
	public void CompletedIO(String message, String errorMessage) {
		hdfio.removeListener();
		notifyApp(openFile);
		openFile=null;
	}
}
