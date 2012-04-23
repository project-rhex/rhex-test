package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.NonNull;
import junit.framework.TestCase;

import java.util.*;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 4/23/12 1:11 PM
 */
public class TestExcecutionPlan extends TestCase {

	private final static Loader loader = Loader.getInstance();
	private final static List<StubTest> tests = new ArrayList<StubTest>(4);
	private final static Test1 test1 = new Test1();
	private final static Test2 test2 = new Test2();
	private final static Test3 test3 = new Test3();
	private final static Test4 test4 = new Test4();

	static {
		tests.add(test1);
		tests.add(test2);
		tests.add(test3);
		tests.add(test4);
		for(TestUnit test : tests) {
			// pre-load these tests
			loader.load(test);
		}
	}

	public void testOrder() {
		// add in excecution order
		// test1 [pre=none]
		// test2 [pre=test1]
		// test3 [pre=test2]
		// test4 [pre=test2, test3]
		Set<TestUnit> set = new LinkedHashSet<TestUnit>(4);
		set.add(test1);
		set.add(test2);
		set.add(test3);
		set.add(test4);
		realTest(set.iterator());
	}

	public void testReverseOrder() {
		Set<TestUnit> set = new LinkedHashSet<TestUnit>(4);
		// add in reverse order
		// test4 [pre=test2, test3]
		// test3 [pre=test2]
		// test2 [pre=test1]
		// test1 [pre=none]
		set.add(test4);
		set.add(test3);
		set.add(test2);
		set.add(test1);
		realTest(set.iterator());
	}

	private void realTest(Iterator<TestUnit> iterator) {
		resetTests(TestUnit.StatusEnumType.SUCCESS);

		ExcecutionPlan exec = new ExcecutionPlan(iterator);
		exec.execute();
		List<TestUnit> list = exec.getList();
		assertEquals(4, list.size());

		for(TestUnit test : list) {
			assertEquals(TestUnit.StatusEnumType.SUCCESS, test.getStatus());
		}

		// tests should be in expected order
		assertEquals(test1, list.get(0));
		assertEquals(test2, list.get(1));
		assertEquals(test3, list.get(2));
		assertEquals(test4, list.get(3));

		// if test2 is loaded then test1 keeps its response when
		// executed
		assertNotNull(test1.getResponse());

		// test2 + test3 should have its execState -> true
		assertTrue(test2.execState);
		assertTrue(test3.execState);
	}

	public void testFailedTest() {
		// add in excecution order
		// test1 [pre=none]
		// test2 [pre=test1]
		// test3 [pre=test2]
		// test4 [pre=test2, test3]
		Set<TestUnit> set = new LinkedHashSet<TestUnit>(4);
		set.add(test1);
		set.add(test2);
		set.add(test3);
		set.add(test4);

		resetTests(TestUnit.StatusEnumType.FAILED);
		test1.testStatus = TestUnit.StatusEnumType.SUCCESS;

		ExcecutionPlan exec = new ExcecutionPlan(set.iterator());
		exec.execute();
		List<TestUnit> list = exec.getList();
		assertEquals(4, list.size());

		// test1 passed, test2 fails, others won't exec
		assertEquals(TestUnit.StatusEnumType.SUCCESS, test1.getStatus());
		assertEquals(TestUnit.StatusEnumType.FAILED, test2.getStatus());
		assertEquals(TestUnit.StatusEnumType.PREREQ_FAILED, test3.getStatus());
		assertEquals(TestUnit.StatusEnumType.PREREQ_FAILED, test4.getStatus());

		assertNotNull(test1.getResponse());

		// test2 + test3 should have its execState -> false
		assertFalse(test2.execState);
		assertFalse(test3.execState);
	}

	private void resetTests(TestUnit.StatusEnumType testStatus) {
		for(StubTest test : tests) {
			test.setStatus(null, null);
			test.execState = false;
			test.getWarnings().clear();
			test.setTestStatus(testStatus);
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

}
