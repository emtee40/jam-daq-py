package jam.commands;

import jam.data.control.AbstractControl;
import jam.global.BroadcastEvent;
import jam.global.SortMode;
import jam.io.FileOpenMode;
import jam.io.hdf.HDFIO;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.KeyStroke;

/**
 * Open a hdf file
 *  
 * @author Ken Swartz
 *
 */
final class OpenHDFCmd extends AbstractCommand implements Observer {
	
	OpenHDFCmd(){
		putValue(NAME,"Open\u2026");
		putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_O, CTRL_MASK));
	}

	/* 
	 * @see jam.commands.AbstractCommand#execute(java.lang.Object[])
	 */
	protected void execute(final Object[] cmdParams) {
		final Runnable r=new Runnable(){
			public void run(){
				final boolean isFileRead; 
				final HDFIO	hdfio = new HDFIO(status.getFrame(), msghdlr);		
				if (cmdParams==null) {//No file given									
					isFileRead=hdfio.readFile(FileOpenMode.OPEN);//opens file dialog
				} else {
					isFileRead=hdfio.readFile(FileOpenMode.OPEN, (File)cmdParams[0]);
				}	
				if (isFileRead){//File was read in	
					notifyApp(HDFIO.getLastValidFile());
				}						
			}
		};
		final Thread t=new Thread(r);
		t.run();
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
		status.setSortMode(file);
		AbstractControl.setupAll();
		broadcaster.broadcast(BroadcastEvent.Command.HISTOGRAM_ADD);
		status.getFrame().repaint();
	}			
	
	public void update(Observable observe, Object obj){
		final BroadcastEvent be=(BroadcastEvent)obj;
		final BroadcastEvent.Command command=be.getCommand();
		if (command==BroadcastEvent.Command.SORT_MODE_CHANGED){
			enable();
		}
	}
	
	protected void enable(){
		final SortMode mode=status.getSortMode();
		setEnabled(mode==SortMode.FILE || mode==SortMode.NO_SORT);		
	}
}
