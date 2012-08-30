package org.mitre.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract Base Test implementation of TestUnit.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 11:56 AM
 */
public abstract class BaseTest implements TestUnit {

	protected final Logger log;

	private StatusEnumType status;
	private String description;
	private final Set<String> warnings = new LinkedHashSet<String>();

	private HttpResponse response;
	private boolean keepResponse;

	private final Set<TestUnit> depends = new TreeSet<TestUnit>();

    /**
     * keepResponse property if true flags the test component to keep the
     * HTTP response object so dependent tests can fetch it.
     */
	public static final String PROP_KEEP_RESPONSE_BOOL = "keepResponse";
	
	private List<Tuple> properties;

	public BaseTest() {
		log = LoggerFactory.getLogger(getClass());
	}

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

	/**
	 * Add warning message to the test results
	 *
	 * @param msg Warning message, ignored if null
	 * @return <tt>true</tt> if the warning set did not already contain the specified
	 *         message (i.e. the warning has not been seen for this test)
	 */
	public boolean addWarning(String msg) {
		return msg != null && getWarnings().add(msg);
	}

	/**
	 * Add warning message to the test results and log
	 * if message has not already been logged in this test.
	 * @param msg Warning message, ignored if null
	 * @return <tt>true</tt> if this warning set did not already contain the specified
	 *         message (i.e. the warning has not been seen for this test)
	 */
	public boolean addLogWarning(String msg) {
		boolean ret = addWarning(msg);
		if (ret) log.warn(msg);
		return ret;
	}

	/**
	 * Get list of warnings, empty if no warnings were generated
	 *
	 * @return ist, never null
	 */
	@NonNull
	public Set<String> getWarnings() {
		return warnings;
	}

	/**
	 * Set status on the test
	 *
	 * @param status Status code, preferably non-null
	 */
	public void setStatus(StatusEnumType status) {
		this.status = status;
		this.description = null;
	}

	/**
	 * Set status on the test with an optional description (or reason).
	 *
	 * @param status Status code, preferably non-null
	 * @param description Description for why status is what it is or <tt>null</tt> otherwise
	 */
	public void setStatus(StatusEnumType status, String description) {
		this.status = status;
		this.description = description;
	}

	/**
	 * Get final execution status of the test. After execution this should be non-null
	 * even if an exception is thrown in which it would have a <em>FAILED</em> status.
	 * @return status
	 */
	public StatusEnumType getStatus() {
		return status;
	}

	/**
	 * Get status description associated with the final disposition of
	 * executing or not executing this test.
	 * @return description, null if not defined
	 */
	@Nullable
	public String getStatusDescription() {
		return description;
	}

	/**
	 * Get name (or short description) of the test
	 * @return name
	 */
	@NonNull
	public String getName() {
		return getId();
	}

	protected boolean isKeepResponse() {
		return keepResponse;
	}

	@Nullable
	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}

	protected void dumpResponse(HttpRequestBase req, HttpResponse response) {
        ClientHelper.dumpResponse(req, response, false);
	}

	protected void dumpResponse(HttpRequestBase req, HttpResponse response, boolean dumpEntity) {
        ClientHelper.dumpResponse(req, response, dumpEntity);
	}

	/**
	 * Set deferred property on this class that will be set on the target <em>testClass</em>
	 * instance after the instance is created and added to the execution plan list as
	 * well as any other dependencies for this test are also loaded. It is required
	 * for the <tt>testClass</tt> argument called here to be contained in the list
	 * returned by calling the TestUnit's {@link #getDependencyClasses} method
	 * otherwise test will not be executed.
	 *
	 * @param testClass, never null
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 */
	protected void setProperty(Class<? extends TestUnit> testClass, String key, Object value) {
		/*
		// checking properties on only declared dependent classes is enforced in ExecutionPlan.add()
		if (!getDependencyClasses().contains(testClass)) {
			throw new IllegalStateException("Test add dependency to class=" + testClass.getName());
		}
		*/
		if (properties == null) {
			properties = new ArrayList<Tuple>();
		}
		properties.add(new Tuple(testClass, key, value));
	}

	/**
	 * Set property on this test.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @throws IllegalArgumentException if key not handled by class
	 * @exception ClassCastException if target type does not match expected type typically indicated
	 * as ending of the property name constant (e.g. PROP_KEEP_RESPONSE_BOOL)
	 */
	public void setProperty(String key, Object value) {
		// System.out.printf("XXX: setProperty %s: %s%n", key, value);
		if (PROP_KEEP_RESPONSE_BOOL.equals(key)) {
			// setKeepContent((Boolean)value);
			this.keepResponse = (Boolean)value;
		} else {
			// must be implemented by sub-classes if properties apply otherwise IllegalArgumentException
			throw new IllegalArgumentException("setProperty " + key + " + not implemented");
		}
	}

	/**
	 * Get list of deferred properties for this test that will be set on the
	 * named classes only if all prerequisite tests for this test are also loaded.
	 * @return list of properties, empty if not set
	 */
	@NonNull
	public List<Tuple> getProperties() {
		return properties == null ?  Collections.<Tuple>emptyList() : properties;
	}

	/**
	 * Cleanup after test is executed. Some tests may need to keep its state
	 * (e.g., HTTP response or created DOM instance) for other tests that are
	 * dependent on its results.
	 */
	public void cleanup() {
		if (!keepResponse || status != StatusEnumType.SUCCESS) {
			response = null; // clear response - no longer needed
		} // else System.out.println("XXX: keep HTTP results"); // debug
	}

	// start of "junit"-like methods

	protected void assertEquals(String expected, String actual) throws TestException {
		if (expected == null) {
			if (actual == null) return;
			fail("Expected null but was: <" + actual + ">");
		} else if (!expected.equals(actual)) {
			fail("Expected <" + expected + "> but was: <" + actual + ">");
		}
	}

	protected void assertEquals(int expected, int actual) throws TestException {
		if (expected != actual) {
			fail("Expected <" + expected + "> but was: <" + actual + ">");
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

	// end of "junit"-like methods
}
