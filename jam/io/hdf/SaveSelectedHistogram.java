package jam.io.hdf;


import jam.data.Group;
import jam.data.Histogram;
import jam.io.hdf.HDFIO;
import jam.global.BroadcastEvent;
import jam.global.Broadcaster;
import jam.global.MessageHandler;
import jam.global.JamStatus;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;


/**
 * Save a selection of histograms
 * 
 * @author Ken Swartz
 *
 */
public class SaveSelectedHistogram {

	
	private Frame frame;
	
	private JDialog dialog;
	
	private JList listHist;
	
	private JButton bSave;

	private JButton bCancel;
	
	private final String SAVE = "Save";

	private final String CANCEL = "Cancel";
	
	/** Messages output */
	private MessageHandler console;
	/** Broadcaster */
	private Broadcaster broadcaster;
	
	/**
	 * Constructs a dialog to save a selection of histograms out of an
	 * HDF file.
	 *  
	 * @param f parent frame
	 * @param c where to print messages
	 */
	public SaveSelectedHistogram(Frame f, MessageHandler c) {
		frame = f;
		console = c;
		broadcaster= Broadcaster.getSingletonInstance();
		
		dialog = new JDialog(f, "Save Selected Histograms", false);
		dialog.setLocation(f.getLocation().x + 50, f.getLocation().y + 50);
		final Container container = dialog.getContentPane();
		container.setLayout(new BorderLayout(10, 10));
		/* Selection list */
		DefaultListModel listModel = new DefaultListModel();
		listHist = new JList(listModel);
		listHist
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		listHist.setSelectedIndex(0);
		listHist.setVisibleRowCount(10);
		JScrollPane listScrollPane = new JScrollPane(listHist);
		listScrollPane.setBorder(new EmptyBorder(10, 10, 0, 10));
		container.add(listScrollPane, BorderLayout.CENTER);
		/* Lower panel with buttons */
		final JPanel pLower = new JPanel();
		container.add(pLower, BorderLayout.SOUTH);
		final JPanel pButtons = new JPanel(new GridLayout(1, 0, 5, 5));
		pLower.add(pButtons);
		bSave = new JButton(SAVE);
		bSave.setActionCommand(SAVE);
		bSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				doSave();
			}
		});
		pButtons.add(bSave);
		bCancel = new JButton(CANCEL);
		bCancel.setActionCommand(CANCEL);
		bCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				doCancel();
			}
		});
		pButtons.add(bCancel);
		dialog.setResizable(false);
		dialog.pack();
		
	}
	/**
	 * 
	 *
	 */
	public void show(){
		loadHistogramList();
		dialog.show();
	}
	/**
	 * Save Button
	 *  
	 */
	private void doSave() {
		saveHistListToFile();
		dialog.dispose();
		//broadcaster.broadcast(BroadcastEvent.Command.HISTOGRAM_ADD);
		//JamStatus.getSingletonInstance().setCurrentHistogram(firstHist);
		//broadcaster.broadcast(BroadcastEvent.Command.HISTOGRAM_SELECT, firstHist);
	}

	/**
	 * Cancel Button
	 *  
	 */
	private void doCancel() {

		//Clear memory
		dialog.dispose();
	}
	
	public void loadHistogramList(){
		int i;
		List histList= Histogram.getHistogramList();
		
		String [] histNames = new String[histList.size()];
		
		Iterator histIter=histList.iterator();
		i=0;
		while(histIter.hasNext()) {
			histNames[i]=((Histogram)histIter.next()).getFullName();
			i++;
		}
		
		listHist.setListData(histNames);
	
	}
	/**
	 * Save list to file
	 *
	 */
	private void saveHistListToFile(){
		
		HDFIO hdfio= new HDFIO(frame, console);
		List listSelected = new ArrayList();
		File file=null;
		//Add selected histgrams to a list
		Object[] selected = listHist.getSelectedValues();		
		for (int  i=0;i<selected.length;i++) {
			listSelected.add(Histogram.getHistogram((String)selected[i]));			
		}

		//Select file
        final JFileChooser chooser = new JFileChooser(HDFIO.getLastValidFile());
        chooser.setFileFilter(new HDFileFilter(true));
        final int option = chooser.showSaveDialog(frame);
        // dont do anything if it was cancel
        if (option == JFileChooser.APPROVE_OPTION
                && chooser.getSelectedFile() != null) {
        	file = chooser.getSelectedFile();
        	//write out histograms
        	hdfio.writeFile(file, listSelected);
        }
	}
}
