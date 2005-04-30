package jam.commands;

import jam.global.BroadcastEvent;

/**
 *  Command for scalers
 * 
 * @author Ken Swartz
 */
public final class ScalersCmd extends AbstractCommand {

	private static final int READ =1;
	private static final int ZERO =2;

	protected void execute(Object[] cmdParams) {
		final int param =((Integer)cmdParams[0]).intValue();
		if (param==READ) {
			readScalers();
		}else if (param==ZERO) {
			zeroScalers();
		} else {
			msghdlr.errorOutln(
			"Incomplete command: need 'scaler zero' or 'scaler read'.");	
		}
	}

	protected void executeParse(String[] cmdTokens) {
		final Object [] params = new Object[1];		
		if (cmdTokens[0].equals("read")) {			
			params[0]= new Integer(READ);			
		}else if (cmdTokens[0].equals("zero")) {
			params[0]= new Integer(ZERO);
		}		 	
		execute(params);	
	}
	
	/**
	 * Does the scaler zeroing.
	 */
	public void zeroScalers() {
		if (STATUS.isOnline()) {
			BROADCASTER.broadcast(BroadcastEvent.Command.SCALERS_CLEAR);
			BROADCASTER.broadcast(BroadcastEvent.Command.SCALERS_READ);
			
		}else {
			msghdlr.errorOutln("Can only Zero Scalers when in Online mode.");			
			//FIXME KBS remove
			//throw new IllegalStateException("Can't Zero Scalers when not in Online mode.");
			
		}

	}
	
	/**
	 * Does the scaler reading.
	 */
	private void readScalers() {
		if (STATUS.isOnline()){
			BROADCASTER.broadcast(BroadcastEvent.Command.SCALERS_READ);
		} 		
	}
}
