package jam.commands;

import java.io.File;

import jam.data.Histogram;
import jam.io.ImpExp;
import jam.io.ImpExpException;
import jam.io.ImpExpASCII;
import jam.global.JamStatus;
import jam.global.CommandListenerException;

/**
 * Export data to file
 * @author Ken Swartz
 *
 */
final class ExportTextFileCmd extends AbstractCommand {

	/* (non-Javadoc)
	 * @see jam.commands.AbstractCommand#execute(java.lang.Object[])
	 */
	protected void execute(Object[] cmdParams)throws CommandException {
		if ( cmdParams==null) {//No file given		
			throw new CommandException("No file given");
		} else {//File given
			try {
				final ImpExp impexp = new ImpExpASCII();	
				File file = (File)cmdParams[0]; 
				impexp.saveFile(file, Histogram.getHistogram(
				JamStatus.instance().getCurrentHistogramName()));
			} catch (ImpExpException iee) {
				throw new CommandException(iee.getMessage());
			}
		}	
	}

	/* (non-Javadoc)
	 * @see jam.commands.AbstractCommand#executeParse(java.lang.String[])
	 */
	protected void executeParse(String[] cmdTokens) throws CommandListenerException {
		
		try { 
			Object [] cmdParams = new Object[1]; 
			if (cmdTokens.length==0) {				
				execute(null);
			} else {
				File file = new File(cmdTokens[0]); 
				cmdParams[0]=file;
				execute(cmdParams);
			}		
		} catch (CommandException ce) {
			throw new CommandListenerException(ce.getMessage());
		}		
	}

}