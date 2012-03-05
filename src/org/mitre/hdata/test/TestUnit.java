package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jdom.Namespace;

import java.util.List;
import java.util.Set;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:44 AM
 */
public interface TestUnit extends Comparable<TestUnit> {

	static final String MIME_APPLICATION_ATOM_XML = "application/atom+xml";
	static final String MIME_APPLICATION_XHTML = "application/xhtml+xml";
	static final String MIME_APPLICATION_XML = "application/xml";
	static final String MIME_TEXT_XML = "text/xml";
	static final String MIME_TEXT_HTML = "text/html";

	static final String NAMESPACE_W3_ATOM_2005 = "http://www.w3.org/2005/Atom";
	static final String NAMESPACE_W3_XMLSchema_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
	static final String NAMESPACE_HDATA_SCHEMAS_2009_06_CORE = "http://projecthdata.org/hdata/schemas/2009/06/core";

	//static final Namespace xsiNamespace = Namespace.getNamespace("xsi", NAMESPACE_W3_XMLSchema_INSTANCE);

	public enum StatusEnumType {

		/**
		 * Test passed and was successful.
		 */
		SUCCESS,

		/**
		 * Test was skipped and not executed either because its inputs were missing
		 * (i.e. the input required to process the test assertion was missing) or
		 * one of its prerequisite tests was not found or not loaded.
		 */
		SKIPPED,

		/**
		 * The test assertion was not processed because a prerequisite (dependent) test assertion failed.
		 */
		PREREQ_FAILED,

		/**
		 * Test failed to meet some element of the specification.
		 * If isRequired() is true then the test fails to meet a mandatory (MUST, SHALL, etc.) element
		 * of the hData specification, which indicates that the target system fails to conform.
		 * <P>
		 * Otherwise if isRequired() is <tt>false</tt> then the test fails to meet a recommended
		 * (e.g. SHOULD, RECOMMENDED, etc.) element of the specification. This is treated as a warning
		 * such that a test assertion failed, but the type attribute for the test assertion indicated
		 * that it was “recommended”, not “required”. This type of failure will not affect the overall
		 * conformance result.
		 */
		FAILED
	}

	/**
	 * Return true if test is a mandatory and required element of the specification whose type attribute is SHALL, MUST, etc.
	 * Otherwise if <tt>false</tt> then test is not required and either recommended or optional element of the specification.
	 *
	 * @return true if test is required to pass for overall conformance to pass
	 */
	boolean isRequired();

	// boolean isKeepContent();

	/*
	 * Indicate that test should keep its content after it has executed to be used
	 * by other tests in the future.
	 *
	 * @param keepContent True if need to keep test results after it has executed
	 */
	// void setKeepContent(boolean keepContent);

	/**
	 * Cleanup after test is executed. Some tests may need to keep its state( aka HTTP response)
	 * for other tests that are dependent on its results.
	 */
	void cleanup();

	/**
	 * Associate a prerequisite that this test is dependent upon such that if any of its
	 * prerequisites fail then this test is not executed and assumed to fail. The classes
	 * associated with the prerequisites must be contained in those returned by calling
	 * {@link #getDependencyClasses}.
	 *
	 * @param aTest a required prerequisite test that this test is dependent upon
	 */
	void addDependency(TestUnit aTest);

	/**
	 * Get list of prerequisite or dependent tests associated with this test
	 * @return non-null list, empty if no prerequisites are applicable
	 */
	@NonNull
	Set<? extends TestUnit> getDependencies();

	//@CheckForNull
	//TestUnit getDependency(Class<? extends TestUnit> testClass);

	/**
	 * Execute test
	 */
	void execute() throws TestException;

	/**
	 * Add warning message to the test results
	 *
	 * @param msg Warning message, ignored if null
	 */
	void addWarning(String msg);

	/**
	 * Get list of warnings, empty if no warnings were generated
	 *
	 * @return ist, never null
	 */
	Set<String> getWarnings();

	void setProperty(String key, Object value);

	@NonNull
	List<Tuple> getProperties();

	/**
	 * Get list of dependency test classes for this test to run. If create dependency cycle then tests
	 * creating the cycle will be skipped.
	 *
	 * @return list, never null.
	 */
	@NonNull
	List<Class<? extends TestUnit>> getDependencyClasses();

	void setStatus(StatusEnumType status, String description);

	StatusEnumType getStatus();

	String getStatusDescription();

	@NonNull
	public String getName();

	/**
	 * Unique identifier for this test. Any duplicate ids will fail to load.
	 * @return non-null/non-empty unique identifier.
	 */
	@NonNull
	String getId();

}
