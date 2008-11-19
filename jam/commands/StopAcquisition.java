/*
 * Created on Jun 7, 2004
 */
package jam.commands;

import injection.GuiceInjector;
import jam.global.QuerySortMode;
import jam.global.SortMode;
import jam.sort.control.RunControl;

import java.util.Observable;
import java.util.Observer;

import javax.swing.Action;
import javax.swing.Icon;

import com.google.inject.Inject;

/**
 * Stop data acquisition.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version June 7, 2004
 */
final class StopAcquisition extends AbstractCommand implements Observer {

	private transient final RunControl control;

	@Inject
	StopAcquisition(final RunControl control) {
		super("stop");
		this.control = control;
		final Icon iPause = loadToolbarIcon("jam/ui/Pause.png");
		putValue(Action.SMALL_ICON, iPause);
		putValue(Action.SHORT_DESCRIPTION, "Pause data acquisition.");
		enable();
	}

	@Override
	protected void execute(final Object[] cmdParams) {
		this.control.stopAcq();
	}

	@Override
	protected void executeParse(final String[] cmdTokens) {
		execute(null);
	}

	public void update(final Observable obs, final Object arg) {
		enable();
	}

	private void enable() {
		final QuerySortMode mode = GuiceInjector.getJamStatus().getSortMode();
		setEnabled(mode == SortMode.ONLINE_DISK || mode == SortMode.ON_NO_DISK);
	}
}
