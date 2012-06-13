package org.mitre.test;

import org.mitre.test.StubTest.*;

import junit.framework.TestCase;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 4/25/12 11:39 AM
 */
public class TestBaseTestUnit extends TestCase {

	public void testEquals() {
		Test1 test1 = new Test1();
		Test2 test2 = new Test2();
		TestDup1 test3 = new TestDup1();
		assertFalse(test1.equals(test2));
		assertFalse(test1.equals(this));
		assertTrue(test1.equals(test3));
	}

	public void testCompare() {
		Test1 test1 = new Test1();
		Test2 test2 = new Test2();
		assertTrue(test1.compareTo(test2) < 0);
	}

	public void testXml() throws TestException {
		TestXml test = new TestXml();
		test.execute();
		assertNotNull(test.getDocument());
		assertEquals(TestUnit.StatusEnumType.SUCCESS, test.getStatus());
	}

}
