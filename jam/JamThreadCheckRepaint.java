/*
 * Created on Nov 18, 2004
 */
package jam;

import jam.util.ThreadCheckingRepaintManager;

import javax.swing.RepaintManager;

/**
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public final class JamThreadCheckRepaint {

	private JamThreadCheckRepaint() {
		super();
	}

	/**
	 * Try running Jam with thread checking repaint manager.
	 * 
	 * @param args
	 *            passed to Jam's main()
	 */
	public static void main(final String[] args) {
		RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager());
		JamMain.main(args);
	}

}
