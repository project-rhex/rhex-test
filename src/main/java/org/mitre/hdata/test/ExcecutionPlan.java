package org.mitre.hdata.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import org.mitre.hdata.test.TestUnit.StatusEnumType;

/**
 * Creates Execution plan and orders tests depending which tests are depending
 * on each other. Prerequisite tests ordered first and executed before the
 * tests that require them.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 11:04 AM
 */
public class ExcecutionPlan {

	private static final Logger log = LoggerFactory.getLogger(ExcecutionPlan.class);

	private final LinkedList<TestUnit> list = new LinkedList<TestUnit>();
	private final Set<Class<? extends TestUnit>> visited = new HashSet<Class<? extends TestUnit>>();
	private final Loader loader = Loader.getInstance();
	private long startTime;

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

	public long getStartTime() {
		return startTime;
	}

	private int add(TestUnit test) {
		final Class<? extends TestUnit> aClass = test.getClass();
		System.out.println("Check: " + aClass.getName());
		if (!visited.add(aClass)) {
			log.trace("Test already visited: " + aClass.getName());
			return indexOf(test);
		}
		List<Class<? extends TestUnit>> depends = test.getDependencyClasses();
		assert(depends != null);

		// validate deferred properties are only on declared dependent test classes
		for(Tuple prop : test.getProperties()) {
			if (!depends.contains(prop.testClass)) {
				log.warn("Test {} sets property {} on non-dependent class <{}>",
					new Object[]{ aClass.getName(), prop.key, prop.testClass.getName()});
				test.setStatus(StatusEnumType.SKIPPED, "Cannot set property on non-dependent class <" + prop.testClass.getName() + ">");
				return -1;
			}
		}

		if (depends.isEmpty()) {
			// 1) add independent test to tail of list
			log.trace("Add last: " + aClass.getName());
			// if test has dependent properties but no dependencies then warn an issue
			//if (!test.getProperties().isEmpty()) {
				//log.warn("Test " + aClass.getName() + " has dependent properties but no dependencies");
			//}
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
				test.setStatus(StatusEnumType.SKIPPED, "Dependency class <" + dependClass.getName() + "> not loaded");
				return -1;
			}
			test.addDependency(other);
			int idx = add(other);
			if (idx == -1) {
				log.error("Failed to add dependency class: " + dependClass.getName());
				test.setStatus(StatusEnumType.SKIPPED, "Failed to add dependency class: " + dependClass.getName());
				return -1;
			}
			// TODO: review if dependency (Prerequsite) same as require the test's input results to remain
			// other.setKeepContent(true); // set flag to keep content in case dependency needs to use it
			for (Tuple prop : test.getProperties()) {
				// this is verified earlier that can only set properties that test is dependent on
				// so should not to be tested again.
				assert(prop.testClass == other.getClass());
				try {
					other.setProperty(prop.key, prop.value);
				} catch(IllegalArgumentException e) {
					// setting property on one class and dependent on another - invalid test
					final String msg = "Failed to set property on dependent test: " + prop.testClass;
					System.out.println("ERROR: " + msg);
					log.debug("", e);
					test.setStatus(StatusEnumType.SKIPPED, msg);
					return -1;
				}
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
				test.setStatus(StatusEnumType.SKIPPED, "Dependency class <" + dependClass.getName() + "> not loaded");
				return -1;
			}
			test.addDependency(other);
			int index = add(other);
			if (index == -1) {
				final String msg = "Failed to add dependency class: " + dependClass.getName();
				log.error(msg);
				test.setStatus(StatusEnumType.SKIPPED, msg);
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
				test.setStatus(StatusEnumType.SKIPPED, msg);
				return -1;
			}
		} else if (idx == -1) {
			final String msg = "Failed to add test with respect to its dependencies";
			System.out.println("ERROR: " + msg);
			test.setStatus(StatusEnumType.SKIPPED, msg);
			return -1; // ???
		}

		// System.out.println("XXX: props = " + test.getProperties());
		for (Tuple prop : test.getProperties()) {
			TestUnit other = loader.getTest(prop.testClass);
			//boolean setFlag = false;
			if (other != null)
				try {
					other.setProperty(prop.key, prop.value);
					//setFlag = true;
					continue;
				} catch(IllegalArgumentException e) {
					log.debug("", e);
				}
			//if (!setFlag) {
			final String msg = "Failed to set property on dependent test: " + prop.testClass;
			System.out.println("ERROR: " + msg);
			test.setStatus(StatusEnumType.SKIPPED, msg);
			return -1;
			//}
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
		//SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        final Context context = Loader.getInstance().getContext();
        startTime = System.currentTimeMillis();
		System.out.printf("\nExec Tests (%d) on %s%n", list.size(), new Date(startTime));
		OUTER: for(TestUnit test : list) {
			System.out.printf("%nRun test: %s [%s]%n", test.getClass().getName(), test.getId());
			// assert status == null for all new tests
			final StatusEnumType status = test.getStatus();
			if (status != null) log.info("XXX: expected status to be null at start but was: " + status); // assertion
			// by the method of ordering tests by this ExecutionPlan all prerequisite tests are guaranteed
			// to be run first so we need to first check if any prerequisite test failed in which case we
			// cancel running this test and flag it PREREQ FAILED.
			for (TestUnit aTest: test.getDependencies()) {
				final StatusEnumType aTestStatus = aTest.getStatus();
				if (aTestStatus == StatusEnumType.FAILED || aTestStatus ==  StatusEnumType.PREREQ_FAILED) {
                    String msg = "Prerequisite test " + aTest.getId() + " failed";
                    System.out.println(msg);
					test.setStatus(StatusEnumType.PREREQ_FAILED, msg);
					// skip test because one of its prerequisite test failed
					continue OUTER;
				}
                if (aTestStatus == StatusEnumType.SKIPPED) {
                    String msg = "Prerequisite test " + aTest.getId() + " skipped";
                    System.out.println(msg);
                    test.setStatus(StatusEnumType.SKIPPED, msg);
                    // skip test because one of its prerequisite test was skipped
                    continue OUTER;
                }
				if (aTestStatus != StatusEnumType.SUCCESS) {
					log.error("XXX: wasn't expecting this situation: status=" + aTestStatus);
					// TODO: if status other than SUCCESS (i.e. SKIPPED) should test execute ??
					continue OUTER;
				}
			}
            String contextUser = context.getUser();
			try {
				test.execute();
				if (test.getStatus() == StatusEnumType.SUCCESS)
					System.out.println("Test: OK");
			} catch (TestException e) {
				test.setStatus(StatusEnumType.FAILED, e.getMessage());
				log.error("", e);
			} catch (RuntimeException e) {
				test.setStatus(StatusEnumType.FAILED, "Unexpected exception: " + e.toString());
				log.error("", e);
			} finally {
				final StatusEnumType testStatus = test.getStatus();
				if (testStatus == StatusEnumType.FAILED) {
					System.out.println("XXX: Test *failed*");
					String desc = test.getStatusDescription();
					if (desc != null) System.out.println("Reason: " + desc); // debug
				} else if (testStatus == null) {
					// assert status != null after execute() called without throwing an exception
					log.error("status for test " + test.getId() + " is undefined after execution");
					test.setStatus(StatusEnumType.SKIPPED, "Unknown status after execution");
				}
				test.cleanup();
                if (contextUser != null && !contextUser.equals(context.getUser())) {
                    log.info("restore user context={}", contextUser);
                    context.setUser(contextUser);
                }
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
