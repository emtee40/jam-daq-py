package jam;

import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.JamStatus;
import jam.plot.View;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.border.EmptyBorder;

/**
 * Delete a vieew
 * 
 * @author Ken Swartz
 *  
 */
public class ViewDelete extends JDialog {

	private final JComboBox comboNames;

	private static final JamStatus status = JamStatus.instance();

	private static final String CHOOSE_NAME = "Choose Name";

	public ViewDelete() {
		super.setTitle("Delete View");
		super.setModal(true);

		final Container cdnew = getContentPane();
		setResizable(false);
		cdnew.setLayout(new BorderLayout(5, 5));
		setLocation(20, 50);

		final JPanel pNames = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 20, 20));
		pNames.setBorder(new EmptyBorder(0, 10, 0, 10));
		cdnew.add(pNames, BorderLayout.CENTER);

		final JLabel ln = new JLabel("Name", JLabel.RIGHT);
		pNames.add(ln);
		// Name combo
		comboNames = new JComboBox();
		Dimension dim = comboNames.getPreferredSize();
		dim.width = 200;
		comboNames.setPreferredSize(dim);
		pNames.add(comboNames);

		/* panel for buttons */
		final JPanel pbutton = new JPanel(new FlowLayout(FlowLayout.CENTER));
		cdnew.add(pbutton, BorderLayout.SOUTH);
		final JPanel pbnew = new JPanel();
		pbnew.setLayout(new GridLayout(1, 0, 5, 5));
		pbutton.add(pbnew, BorderLayout.SOUTH);
		final JButton bok = new JButton("OK");
		bok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				deleteView();
				close();

			}
		});
		pbnew.add(bok);
		final JButton bapply = new JButton("Apply");
		bapply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				deleteView();
			}
		});
		pbnew.add(bapply);
		final JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				close();
			}
		});
		pbnew.add(bcancel);
		pack();
	}

	public void showView() {
		updateViewNames();
		this.show();
	}

	private void updateViewNames() {
		final List namesList = new ArrayList();
		namesList.add(CHOOSE_NAME);
		namesList.addAll(View.getNameList());
		comboNames.removeAllItems();
		final Iterator iter=namesList.iterator();
		while (iter.hasNext()){
			comboNames.addItem(iter.next());
		}
	}

	/**
	 * Make a new view.
	 */
	private void deleteView() {
		final String name = (String) comboNames.getSelectedItem();
		if (!(name.equals(CHOOSE_NAME) || name.equals(View.SINGLE.getName()))) {
			View.removeView(name);
		}
		Broadcaster.getSingletonInstance().broadcast(
				BroadcastEvent.Command.VIEW_NEW);
		status.getDisplay().setView(View.SINGLE);
	}

	private void close() {
		dispose();
	}
}

