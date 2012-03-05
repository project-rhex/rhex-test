package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.http.HttpResponse;

import java.util.*;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 11:56 AM
 */
public abstract class BaseTest implements TestUnit {

	private StatusEnumType status;
	private String description;
	private Set<String> warnings;
	protected HttpResponse response;
	private boolean keepResponse;

	private final Set<TestUnit> depends = new TreeSet<TestUnit>();

	// property
	public static final String PROP_KEEP_RESPONSE_BOOL = "keepResponse";
	
	private List<Tuple> properties;

	/**
	 * Associate a prerequisite that this test is dependent upon such that if any of its
	 * prerequisites fail then this test is not executed and assumed to fail. The classes
	 * associated with the prerequisites must be contained in those returned by calling
	 * {@link #getDependencyClasses}.
	 *
	 * @param aTest a required prerequisite test that this test is dependent upon
	 * @throws IllegalArgumentException if attempt to add dependency on itself
	 */
	public void addDependency(TestUnit aTest) {
		if (aTest == this) throw new IllegalArgumentException("Test cannot depend on itself");
		depends.add(aTest);
		// assume that any TestUnit object added here has its class contained in the list returned by getDependencyClasses()
		// since that is the source for invoking this method.
	}

	/**
	 * Get list of prerequisite or dependent tests associated with this test
	 * @return non-null list, empty if no prerequisites are applicable
	 */
	@NonNull
	public Set<? extends TestUnit> getDependencies() {
		return depends;
	}

	@CheckForNull
	public TestUnit getDependency(Class<? extends TestUnit> testClass) {
		for (TestUnit test : depends) {
			if (testClass == test.getClass()) return test;
		}
		// if returns null then error in test
		return null;
	}

	public boolean equals(Object other) {
		return other instanceof TestUnit && this.equals((TestUnit) other);
	}

	public boolean equals(TestUnit other) {
		return other != null && this.getId().equals(other.getId());
	}

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 * @param other   the <code>TestUnit</code> to be compared.
	 * @return  the value <code>0</code> if the argument other is equal to
	 *          this TestUnit by its id; a value less than <code>0</code> if the id
	 *          for this Test is lexicographically less than the other argument; and a
	 *          value greater than <code>0</code> if id for this test is
	 *          lexicographically greater than the other's id.
	 * @exception NullPointerException if other is null
	 */
	public int compareTo(TestUnit other) {
		// if (other == null) return 1;
		return getId().compareTo(other.getId());
	}

	/**
	 * Returns a hash code for this TestUnit based on its unique identifier.
	 *
	 * @return  a hash code value for this object.
	 */
	public int hashCode() {
		return getId().hashCode();
	}

	public void addWarning(String msg) {
		if (msg != null)
			getWarnings().add(msg);
	}

	public Set<String> getWarnings() {
		if (warnings == null) warnings = new LinkedHashSet<String>();
		return warnings;
	}

	public void setStatus(StatusEnumType status) {
		this.status = status;
		this.description = null;
	}

	public void setStatus(StatusEnumType status, String description) {
		this.status = status;
		this.description = description;
	}

	public StatusEnumType getStatus() {
		return status;
	}

	public String getStatusDescription() {
		return description;
	}

	/*
	public boolean isKeepContent() {
		return keepContent;
	}

	public void setKeepContent(boolean keepContent) {
		this.keepContent = keepContent;
	}
	*/

	public void execute() throws TestException {
		status = StatusEnumType.SKIPPED;
		// must implement in sub-class
	}

	@NonNull
	public String getName() {
		return "default " + getId();
	}

	@Nullable
	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * Set deferred property on this class that will be set on the target testClass
	 * instance after the instance is created and added to the execution plan list as
	 * well as any other dependencies for this test are also loaded. It is required for
	 * the <tt>testClass</tt> argument called here to be contained in the list
	 * returned by calling the TestUnit's {@link #getDependencyClasses} method.
	 *
	 * @param testClass
	 * @param key
	 * @param value
	 */
	protected void setProperty(Class<? extends TestUnit> testClass, String key, Object value) {
		if (properties == null) {
			properties = new ArrayList<Tuple>();
		}
		properties.add(new Tuple(testClass, key, value));
	}

	/**
	 * Set property on this test.
	 *
	 * @param key
	 * @param value
	 * @exception ClassCastException if target type does not match expected type typically indicated
	 * as ending of the property name constant (e.g. PROP_KEEP_RESPONSE_BOOL)
	 */
	public void setProperty(String key, Object value) {
		// System.out.printf("XXX: setProperty %s: %s%n", key, value);
		if (PROP_KEEP_RESPONSE_BOOL.equals(key)) {
			// setKeepContent((Boolean)value);
			this.keepResponse = (Boolean)value;
		} else {
			// must be implemented by sub-classes if properties apply otherwise assertion error
			throw new IllegalArgumentException("setProperty " + key + " + not implemented");
		}
	}

	@NonNull
	public List<Tuple> getProperties() {
		return properties == null ?  Collections.<Tuple>emptyList() : properties;
	}
	
	public void cleanup() {
		if (!keepResponse) {
			response = null; // clear response - no longer needed
		} // else System.out.println("XXX: keep HTTP results"); // debug
	}

	// start of "junit-like methods

	protected void assertEquals(String expected, String actual) throws TestException {
		if (expected == null) {
			if (actual == null) return;
			fail("Expected null but was: <" + actual + ">");
		} else if (!expected.equals(actual)) {
			fail("Expected <"+expected+"> but was: <" + actual + ">");
		}
	}

	protected void assertEquals(int expected, int actual) throws TestException {
		if (expected != actual) {
			fail("Expected <"+expected+"> but was: <" + actual + ">");
		}
	}

	protected void assertTrue(boolean cond, String msg) throws TestException {
		if (!cond) fail(msg);
	}


	protected void assertFalse(boolean cond, String msg) throws TestException {
		if (cond) fail(msg);
	}

	protected void fail(String s) throws TestException {
		throw new TestException(s);
	}

	// end of "junit-like methods

}
