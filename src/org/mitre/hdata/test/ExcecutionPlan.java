package org.mitre.hdata.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Creates Execution plan and orders tests depending which tests are depending
 * on each other. Prerequisite tests ordered first and executed before the
 * tests that require them.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 11:04 AM
 */
public class ExcecutionPlan {

	private final static Log log = LogFactory.getLog(ExcecutionPlan.class);

	private final LinkedList<TestUnit> list = new LinkedList<TestUnit>();
	private final Set<Class<? extends TestUnit>> visited = new HashSet<Class<? extends TestUnit>>();
	private final Loader loader = Loader.getInstance();

	public ExcecutionPlan(Iterator<TestUnit> it) {
		while (it.hasNext()) {
			TestUnit test = it.next();
			add(test);
		}
	}

	/**
	 * Get tests in execution order
	 * @return read-only list
	 */
	public List<TestUnit> getList() {
		return Collections.unmodifiableList(list);
	}

	private int add(TestUnit test) {
		final Class<? extends TestUnit> aClass = test.getClass();
		System.out.println("Check: " + aClass.getName());
		if (!visited.add(aClass)) {
			log.trace("Test already visited: " + aClass.getName());
			return indexOf(test);
		}
		List<Class<? extends TestUnit>> depends = test.getDependencyClasses();

		if (depends.isEmpty()) {
			// 1) add independent test to tail of list
			log.trace("Add last: " + aClass.getName());
			list.addLast(test);
			// System.out.printf("Add[%d]: %s%n", list.size() - 1, aClass.getName()); // debug
			return list.size() - 1; // return index in list
		}

		if (depends.size() == 1) {
			// 2) only one dependency to check
			Class<? extends TestUnit> dependClass = depends.get(0);
			TestUnit other = loader.getTest(dependClass);
			if (other == null) {
				log.error("Dependency class <" + dependClass.getName() + "> not loaded. Skip test "+ aClass.getName());
				test.setStatus(TestUnit.StatusEnumType.SKIPPED, "Dependency class <" + dependClass.getName() + "> not loaded");
				return -1;
			}
			test.addDependency(other);
			int idx = add(other);
			if (idx == -1) {
				log.error("Failed to add dependency class: " + dependClass.getName());
				test.setStatus(TestUnit.StatusEnumType.SKIPPED, "Failed to add dependency class: " + dependClass.getName());
				return -1;
			}
			// TODO: review if dependency (Prerequsite) same as require the test's input results to remain
			// other.setKeepContent(true); // set flag to keep content in case dependency needs to use it
			for (Tuple prop : test.getProperties()) {
				if (prop.testClass == other.getClass())
					other.setProperty(prop.key, prop.value);
				else log.error("Failed to set property on dependent test: " + prop.testClass); // assertion failed
			}
			list.add(++idx, test);
			System.out.printf("Add[%d]: %s%n", idx, aClass.getName());
			return idx; // added successfully
		}

		// 3) multiple dependencies
		// add test after all dependencies
		int idx = -1;
		boolean updateIndex = false;
		TestUnit dependsOnTest = null;
		// List<TestUnit> otherTests = new ArrayList<TestUnit>(depends.size());
		for (Class<? extends TestUnit> dependClass : depends) {
			TestUnit other = loader.getTest(dependClass);
			if (other == null) {
				log.error("Dependency class <" + dependClass.getName() + "> not loaded. Skip test "+ aClass.getName());
				test.setStatus(TestUnit.StatusEnumType.SKIPPED, "Dependency class <" + dependClass.getName() + "> not loaded");
				return -1;
			}
			test.addDependency(other);
			int index = add(other);
			if (index == -1) {
				final String msg = "Failed to add dependency class: " + dependClass.getName();
				log.error(msg);
				test.setStatus(TestUnit.StatusEnumType.SKIPPED, msg);
				return -1;
			}
			if (index > idx) {
				idx = index; // get largest index in list
				dependsOnTest = other;
			} else {
				updateIndex = true; // index of last element may have moved due to add()
			}
			// otherTests.add(other);
		}

		// get index of last dependency -- index could have changed due to side-effects of recursive adds
		if (dependsOnTest != null && updateIndex) {
			idx = indexOf(dependsOnTest);
			if (idx == -1) {
				// should never happen
				final String msg = "Failed to add in order with respect to dependency class " + dependsOnTest.getClass().getName();
				System.out.println("ERROR: " + msg);
				test.setStatus(TestUnit.StatusEnumType.SKIPPED, msg);
				return -1;
			}
		} else if (idx == -1) {
			final String msg = "Failed to add test with respect to its dependencies";
			System.out.println("ERROR: " + msg);
			test.setStatus(TestUnit.StatusEnumType.SKIPPED, msg);
			return -1; // ???
		}

		// System.out.println("XXX: props = " + test.getProperties());
		for (Tuple prop : test.getProperties()) {
			TestUnit other = loader.getTest(prop.testClass);
			if (other != null) other.setProperty(prop.key, prop.value);
			else log.error("Failed to set property on dependent test: " + prop.testClass);
		}

		list.add(++idx, test);
		System.out.printf("Add[%d]: %s%n", idx, aClass.getName());

		return idx;
	}

	private int indexOf(TestUnit test) {
		int idx = 0;
		for (TestUnit aTest : list) {
			if (aTest == test) return idx;
			idx++;
		}
		return -1; // not found
	}

	public void execute() {
		System.out.println("\nExec Tests");
		OUTER: for(TestUnit test : list) {
			System.out.printf("%nRun test: %s [%s]%n", test.getClass().getName(), test.getId());
			// assert status == null for all new tests
			final TestUnit.StatusEnumType status = test.getStatus();
			if (status != null) log.info("XXX: expected status to be null at start but was: " + status); // assertion
			// by the method of ordering tests by this ExecutionPlan all prerequisite tests are guaranteed
			// to be run first so we need to first check if any prerequisite test failed in which case we
			// cancel running this test and flag it PREREQ FAILED.
			for (TestUnit aTest: test.getDependencies()) {
				final TestUnit.StatusEnumType aTestStatus = aTest.getStatus();
				if (aTestStatus == TestUnit.StatusEnumType.FAILED || aTestStatus ==  TestUnit.StatusEnumType.PREREQ_FAILED) {
					System.out.println("Prerequisite test " + aTest.getId() + " failed");
					test.setStatus(TestUnit.StatusEnumType.PREREQ_FAILED, "Prerequisite test " + aTest.getId() + " failed");
					// skip test because one of its prerequisite test failed
					continue OUTER;
				}
				if (aTestStatus != TestUnit.StatusEnumType.SUCCESS) {
					log.error("XXX: wasn't expecting this situation: status=" + aTestStatus);
					// TODO: if status other than SUCCESS (i.e. SKIPPED) should test execute ??
				}
			}
			try {
				test.execute();
				if (test.getStatus() == TestUnit.StatusEnumType.SUCCESS)
					System.out.println("Test: OK");
			} catch (TestException e) {
				test.setStatus(TestUnit.StatusEnumType.FAILED, e.getMessage());
				log.error("", e);
			} catch (RuntimeException e) {
				test.setStatus(TestUnit.StatusEnumType.FAILED, "Unexpected exception: " + e.toString());
				log.error("", e);
			} finally {
				final TestUnit.StatusEnumType testStatus = test.getStatus();
				if (testStatus == TestUnit.StatusEnumType.FAILED) {
					System.out.println("XXX: Test *failed*");
					String desc = test.getStatusDescription();
					if (desc != null) System.out.println("Reason: " + desc); // debug
				} else if (testStatus == null) {
					// assert status != null after execute() called without throwing an exception
					log.error("status for test " + test.getId() + " is not defined after execution");
					test.setStatus(TestUnit.StatusEnumType.SKIPPED, "Unknown status after execution");
				}
				test.cleanup();
			}
			/*
			final List<String> warnings = test.getWarnings();
			if (!warnings.isEmpty()) {
				for (String s : warnings) {
					System.out.println("\t" + s);
				}
			}
			*/
		}
	}
}
