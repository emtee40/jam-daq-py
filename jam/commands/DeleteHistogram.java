package jam.commands;

import jam.data.Group;
import jam.data.Histogram;
import jam.global.BroadcastEvent;

import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 *  Command for file menu new also clears
 * 
 * @author Ken Swartz
 *
 */
final class DeleteHistogram extends AbstractCommand {
	
	DeleteHistogram(){
		super();
		putValue(NAME,"Delete\u2026");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, 
		CTRL_MASK));
	}

	/**
	 * Excecute command
	 * 
	 * @see jam.commands.AbstractCommand#execute(java.lang.Object[])
	 */
	protected void execute(Object[] cmdParams) {
		final JFrame frame =STATUS.getFrame();
		final Histogram hist=STATUS.getCurrentHistogram();
		final String name =hist.getFullName().trim();
		final Group.Type type=hist.getGroup().getType();	
		/* Cannot delete sort histograms */
		if (type == Group.Type.SORT) {
			msghdlr.errorOutln("Cannot delete '"+name+"', it is sort histogram.");
		} else {
			if (JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(frame,
					"Delete "+name+"?","Delete histogram",JOptionPane.YES_NO_OPTION)){
				Histogram.deleteHistogram(hist.getUniqueFullName());
				BROADCASTER.broadcast(BroadcastEvent.Command.HISTOGRAM_ADD);
			}
		}
	}
	
	protected void executeParse(String[] cmdTokens) {
		execute(null);		
	}
}
