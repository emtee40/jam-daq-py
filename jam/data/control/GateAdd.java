package jam.data.control;

import jam.data.DataException;
import jam.data.Gate;
import jam.data.Histogram;
import jam.global.BroadcastEvent;
import jam.global.MessageHandler;
import jam.ui.GateComboBoxModel;
import jam.ui.GateListCellRenderer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * A dialog for adding existing gates to histograms.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Jun 4, 2004
 */
public final class GateAdd extends AbstractControl {
	
	private Gate currentGateAdd;
	private final JComboBox cadd;
	private final MessageHandler messageHandler;
	
	/**
	 * Create a new "add gate" dialog.
	 * @param mh where to print messages
	 */
	public GateAdd(MessageHandler mh){
		super("Add Gate",false);
		messageHandler=mh;
		final Container cdadd=getContentPane();
		setResizable(false);
		setLocation(20,50);
		/* panel with chooser */
		final JPanel ptadd =new JPanel();
		ptadd.setLayout(new FlowLayout(FlowLayout.CENTER,20,20));
		cdadd.add(ptadd,BorderLayout.CENTER);
		cadd=new JComboBox(new GateComboBoxModel(GateComboBoxModel.Mode.ALL));
		cadd.setRenderer(new GateListCellRenderer());
		cadd.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				final Object item=cadd.getSelectedItem();
				if (item instanceof Gate){
					selectGateAdd((Gate)item);
				}
			}
		});
		Dimension dimadd = cadd.getPreferredSize();
		dimadd.width=200;
		cadd.setPreferredSize(dimadd);
		ptadd.add(cadd);
		/* panel for buttons */
		final JPanel pbuttonAdd = new JPanel(new FlowLayout(FlowLayout.CENTER));
		cdadd.add(pbuttonAdd,BorderLayout.SOUTH);
		final JPanel pbadd= new JPanel();
		pbadd.setLayout(new GridLayout(1,0,5,5));
		pbuttonAdd.add(pbadd);
		final JButton bokadd  =   new JButton("OK");
		bokadd.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				addGate();
				dispose();
			}
		});
		pbadd.add(bokadd);
		final JButton bapplyadd = new JButton("Apply");
		bapplyadd.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				addGate();
			}
		});
		pbadd.add(bapplyadd);
		final JButton bcanceladd =new JButton("Cancel");
		bcanceladd.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				dispose();
			}
		});	
		pbadd.add(bcanceladd);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				dispose();
			}
		});
		pack();
	}

	/**
	 * Add a gate.
	 *
	 * @throws DataException if there's a problem
	 * @throws GlobalException if there's a problem
	 */
	private void addGate() {
		if (currentGateAdd!=null) {
			final Histogram hist=STATUS.getCurrentHistogram();
			hist.addGate(currentGateAdd);
			BROADCASTER.broadcast(BroadcastEvent.Command.GATE_ADD);
			messageHandler.messageOutln("Added gate '"+
			currentGateAdd.getName().trim()+"' to histogram '"+hist.getName()+"'");
		} else {
			messageHandler.errorOutln("Need to choose a gate to add ");
		}
	}

	void selectGateAdd(Gate gate) {
		synchronized (this) {
			currentGateAdd=gate;
		}
	}

	/**
	 * @see jam.data.control.AbstractControl#doSetup()
	 */
	public void doSetup() {
		cadd.setSelectedIndex(0);
	}

}
