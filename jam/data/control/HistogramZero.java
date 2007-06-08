package jam.data.control;

import jam.data.Histogram;
import jam.global.BroadcastEvent;
import jam.ui.SelectionTree;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Zero histograms dialog
 */
public class HistogramZero extends AbstractControl {

	/**
	 * Construct a new "zero histograms" dialog.
	 * 
	 * @param mh
	 *            where to print messages
	 */
	public HistogramZero() {
		super("Zero Histograms", false);
		/* zero histogram dialog box */
		final Container dzc = getContentPane();
		setResizable(false);
		setLocation(20, 50);
		dzc.setLayout(new FlowLayout(FlowLayout.CENTER));
		final JPanel pButton = new JPanel(new GridLayout(1, 0, 5, 10));
		final Border border = new EmptyBorder(10, 10, 10, 10);
		pButton.setBorder(border);

		final JButton one = new JButton("Displayed");
		one.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				final Histogram currentHistogram = (Histogram) SelectionTree
						.getCurrentHistogram();
				currentHistogram.setZero();
				BROADCASTER.broadcast(BroadcastEvent.Command.REFRESH);
				LOGGER.info("Zero Histogram: " + currentHistogram.getTitle());
				dispose();
			}
		});
		pButton.add(one);
		final JButton all = new JButton("   All   ");
		all.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				zeroAll();
				dispose();
			}
		});
		pButton.add(all);
		final JButton cancel = new JButton(" Cancel ");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				dispose();
			}
		});
		pButton.add(cancel);
		dzc.add(pButton);
		pack();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent event) {
				dispose();
			}
		});
	}

	/**
	 * Zero all the histograms.
	 */
	public void zeroAll() {
		final String classname = getClass().getName();
		final String methname = "zeroAll";
		LOGGER.entering(classname, methname);
		LOGGER.info("Zero All");
		final List<Histogram> allHistograms = Histogram.getHistogramList();
		for (Histogram hist : allHistograms) {
			hist.setZero();
		}
		BROADCASTER.broadcast(BroadcastEvent.Command.REFRESH);
		LOGGER.exiting(classname, methname);
	}

	public void doSetup() {
		/* nothing to do */
	}
}
