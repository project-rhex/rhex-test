package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple Base Test Unit suitable for JUnit tests
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 4/23/12 2:48 PM
*/
public abstract class StubTest extends BaseTest {

	public static final String PROP_KEEP_STATE_BOOL = "keepState";

	StatusEnumType testStatus = StatusEnumType.SKIPPED;
	boolean keepState;
	boolean execState;

	@Override
	public void execute() throws TestException {
		// System.out.println("exec: " + getId() + " " + testStatus);
		setStatus(testStatus);
		execState = keepState;
		if (isKeepResponse()) {
			setResponse(new BasicHttpResponse(
					new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "Ok")));
		}
	}

	public void setTestStatus(StatusEnumType testStatus) {
		this.testStatus = testStatus;
	}

	public void cleanup() {
		if (!keepState || getStatus() == StatusEnumType.FAILED) {
			execState = false;
		}
	}

	public void setProperty(String key, Object value) {
		if (PROP_KEEP_STATE_BOOL.equals(key)) {
			this.keepState = (Boolean)value;
		} else {
			super.setProperty(key, value);
		}
	}

	static class Test1 extends StubTest {

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.emptyList(); // no dependencies
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.1";
		}
	}

	static class TestDup1 extends StubTest {

		@Override
		public boolean isRequired() {
			return false;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.emptyList();
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.1";
		}
	}

	static class Test2 extends StubTest {

		public Test2() {
			setProperty(Test1.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return false;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.<Class<? extends TestUnit>> singletonList(Test1.class); // 1.0.1
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.2";
		}
	}

	static class Test3 extends StubTest {

		@Override
		public boolean isRequired() {
			return false;
		}

		@Override
		public void execute() throws TestException {
			super.execute();
			addWarning("this is a warning");
			assertFalse(getStatus() == null," always false");
			assertTrue(testStatus.equals(getStatus()), "status not matching test status");
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.<Class<? extends TestUnit>> singletonList(Test2.class); // 1.0.2
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.3";
		}
	}

	static class Test4 extends StubTest {

		public Test4() {
			setProperty(Test2.class, PROP_KEEP_STATE_BOOL, Boolean.TRUE);
			setProperty(Test3.class, PROP_KEEP_STATE_BOOL, Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			List<Class<? extends TestUnit>> depends = new ArrayList<Class<? extends TestUnit>>(2);
			depends.add(Test2.class); // 1.0.2
			depends.add(Test3.class); // 1.0.3
			return depends;
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.4";
		}
	}

	static class Test5 extends StubTest {

		public Test5() {
			// tries to set an invalid property -- will fail to load
			setProperty(Test1.class, "foo", Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.<Class<? extends TestUnit>> singletonList(Test1.class); // 1.0.1
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.5";
		}
	}

	static class Test6 extends StubTest {

		public Test6() {
			// tries to set a property on dependent class that it did not declare a dependency on
			setProperty(Test1.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.emptyList();
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.6";
		}
	}

	static class Test7 extends StubTest {

		public Test7() {
			// mismatch dependency
			// tries to set property on one class and depends on another
			setProperty(Test2.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.<Class<? extends TestUnit>> singletonList(Test1.class); // 1.0.1
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.7";
		}
	}

	static class Test8 extends StubTest {

		public Test8() {
			setProperty(Test7.class, PROP_KEEP_RESPONSE_BOOL, Boolean.TRUE);
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@NonNull
		@Override
		public List<Class<? extends TestUnit>> getDependencyClasses() {
			return Collections.<Class<? extends TestUnit>> singletonList(Test7.class); // 1.0.7
		}

		@NonNull
		@Override
		public String getId() {
			return "1.0.8";
		}
	}

}
