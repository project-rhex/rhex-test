package org.mitre.hdata.test;

/**
 * Simple Tuple holder for properties set from tests on prerequisite or dependent tests
 * only after the prerequisite and caller test are correctly loaded. When a test is added
 * then these properties are set on the prerequisite object to change behavior when it executes
 * or to prevent cleanup.
 *
 * User: MATHEWS
 * Created: 2/28/12 3:16 PM
 */
public class Tuple {

	Class<? extends TestUnit> testClass;
	String key;
	Object value;

	public Tuple(Class<? extends TestUnit> testClass, String key, Object value) {
		this.testClass = testClass;
		this.key = key;
		this.value = value;
	}
}
