package jam.sort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public final class GainCalibrationTest {//NOPMD

	private transient GainCalibration gain;
	
	@Before
	public void setUp() {
		gain=new GainCalibration(this);
	}

	@Test
	public void testGainFile() {
		try {
			gain.gainFile("jam/sort/GainCalibrationTest.txt", true);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		assertEquals("Gain adjusted value should be 4.1", 4.1, gain.adjustExact(0, 1));
		assertEquals("Gain adjusted value should be 3.0", 3.0, gain.adjustExact(1, 1));
		assertEquals("Gain adjusted value should be 0.9", 0.9 , gain.adjustExact(2, 1));
	}
}
