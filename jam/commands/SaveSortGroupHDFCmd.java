package jam.commands;

import jam.data.Group;
import jam.global.BroadcastEvent;
import jam.global.SortMode;
import jam.io.hdf.HDFIO;
import jam.io.hdf.HDFileFilter;

import java.awt.Frame;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFileChooser;

/**
 * Command to save the sort group of histograms.
 * 
 * @author Ken Swartz
 *
 */
final class SaveSortGroupHDFCmd extends AbstractCommand implements Observer {

	public void initCommand() {
		putValue(NAME, "Save sort group as\u2026");
	}
	
	/* (non-Javadoc)
	 * @see jam.commands.AbstractCommand#execute(java.lang.Object[])
	 */
	protected void execute(Object[] cmdParams) {
		final Frame frame =STATUS.getFrame();
		final HDFIO hdfio = new HDFIO(frame, msghdlr);
		final SortMode mode =STATUS.getSortMode();
		if (mode == SortMode.ONLINE_DISK ||
			mode == SortMode.ON_NO_DISK ||
		  	mode == SortMode.OFFLINE ) {
			/* find sort group */
			final Group sortGroup=Group.getSortGroup();
			if (sortGroup!=null) {
				if (cmdParams == null || cmdParams.length==0) { //No file given		
			        final JFileChooser jfile = new JFileChooser(HDFIO.getLastValidFile());
			        jfile.setFileFilter(new HDFileFilter(true));
			        final int option = jfile.showSaveDialog(frame);
			        /* don't do anything if it was cancel */
			        if (option == JFileChooser.APPROVE_OPTION
			                && jfile.getSelectedFile() != null) {
			            final File file = jfile.getSelectedFile();
			            hdfio.writeFile(file, sortGroup.getHistogramList());
			        }
				}else {
					final File file=(File)cmdParams[0];
					hdfio.writeFile(file, sortGroup.getHistogramList());
				}
			}
		} else {//No sort group
			throw new IllegalStateException("Need to be in a sort mode to save sort group.");
		}
	}

	/* (non-Javadoc)
	 * @see jam.commands.AbstractCommand#executeParse(java.lang.String[])
	 */
	protected void executeParse(String[] cmdTokens) {
	    execute(null);
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
		setEnabled(mode==SortMode.OFFLINE 
				|| mode==SortMode.ONLINE_DISK
				|| mode==SortMode.ON_NO_DISK);		
	}
}
