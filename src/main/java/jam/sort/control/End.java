package jam.sort.control;

import injection.GuiceInjector;
import jam.ui.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

class End extends AbstractAction {

    private transient final RunController runControl;

    End(final RunController runControl) {
        super();
        this.runControl = runControl;
        putValue(Action.NAME, "End Run");
        putValue(Action.SHORT_DESCRIPTION, "Ends the current run.");
        putValue(Action.SMALL_ICON,
                GuiceInjector.getObjectInstance(Icons.class).END);
        setEnabled(false);
    }

    public void actionPerformed(final ActionEvent event) {
        runControl.endRun();
    }

}