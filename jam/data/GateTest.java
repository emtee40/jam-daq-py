package jam.data;

import static org.junit.Assert.assertTrue;

import java.awt.Polygon;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit tests for <code>jam.data.Gate</code>.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser </a>
 * @see Gate
 */
public class GateTest {//NOPMD

    private transient Gate gate1, gate2; //1d and 2d, respectively
    private transient Group group;
    private transient Histogram hist1, hist2;

    /**
     * Test for boolean inGate(int).
     * 
     * @see Gate#inGate(int)
     */
    @Test
    public void inGateI() {
        final boolean assertion1 = gate1.inGate(20);
        final boolean assertion2 = !gate1.inGate(5);
        final boolean assertion3 = !gate1.inGate(60);
        assertTrue("20 not in gate.", assertion1);
        assertTrue("5 in gate.", assertion2);
        assertTrue("60 in gate.", assertion3);
    }

    /**
     * Test for boolean inGate(int, int).
     * 
     * @see Gate#inGate(int,int)
     */
    @Test
    public void inGateII() {
        final boolean assertion1 = gate2.inGate(20, 20);
        final boolean assertion2 = !gate2.inGate(5, 20);
        final boolean assertion3 = !gate2.inGate(60, 20);
        final boolean assertion4 = !gate2.inGate(20, 5);
        final boolean assertion5 = !gate2.inGate(5, 5);
        final boolean assertion6 = !gate2.inGate(60, 60);
        final boolean assertion7 = !gate2.inGate(20, 60);
        assertTrue("20,20 not in gate.", assertion1);
        assertTrue("5,20 in gate.", assertion2);
        assertTrue("60,20 in gate.", assertion3);
        assertTrue("20,5 in gate.", assertion4);
        assertTrue("5,5 in gate.", assertion5);
        assertTrue("60,60 in gate.", assertion6);
        assertTrue("20,60 in gate.", assertion7);
    }

    /**
     * Initialize local variables for the tests.
     * 
     * @see TestCase#setUp()
     */
    @Before
    public void setUp() {
        group = Group.createGroup("TestGateGroup", Group.Type.FILE);
        hist1 = Histogram.createHistogram(group, new int[100], "h1");
        hist2 = Histogram.createHistogram(group, new int[100][100], "h2");
        gate1 = new Gate("g1", hist1);
        gate1.setLimits(10, 50);
        gate2 = new Gate("g2", hist2);
        final int[] xpoints = { 10, 50, 50, 10 };
        final int[] ypoints = { 10, 10, 50, 50 };
        final Polygon box = new Polygon(xpoints, ypoints, 4);
        gate2.setLimits(box);
    }

    @After
    public void tearDown(){
    	DataBase.getInstance().clearAllLists();
    	group=null;//NOPMD
    	hist1=null;//NOPMD
    	hist2=null;//NOPMD
    	gate1=null;//NOPMD
    	gate2=null;//NOPMD
    }   
}
