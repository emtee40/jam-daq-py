/*
 * Created on Oct 27, 2004
 */
package jam.sort;

import jam.RunControl;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:dale@visser.name">Dale W Visser</a>
 */
public class NetDaemonTest extends TestCase {

	private RunControl rc;
	private NetDaemon nd;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		rc=RunControl.getSingletonInstance();
		nd=new NetDaemon(null,null,null,"localhost",8080);
		rc.setupOn("Test",null,null,null,nd,null);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSetEmptyBefore(){
		nd.setEmptyBefore(true);
	}
}