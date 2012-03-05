package org.mitre.hdata.test;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.mitre.hdata.test.tests.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main Loader loads test units, executes them, and generates output report
 *
 * @author Jason Mathews, MITRE Corp.
 */
public final class Loader {

	private static final Logger log = LoggerFactory.getLogger(Loader.class);

	private Context context = new Context();
	private final Map<Class<? extends TestUnit>, TestUnit> list =
			new HashMap<Class<? extends TestUnit>, TestUnit>();

	private final Set<TestUnit> sortedSet = new TreeSet<TestUnit>();

	/*(new Comparator<TestUnit>() {
		public int compare(TestUnit a, TestUnit b) {
			return a.getId().compareTo(b.getId());
		}
	});
	*/

	private final Set<String> idSet = new HashSet<String>();

	private static final Loader loader = new Loader();
	private String configFile = "config.xml";

	private Loader() {
		try {
			XMLConfiguration config = new XMLConfiguration();
			// load from cmd-line parameter. default=config.xml
			config.setFileName(configFile);
			config.load();
			context.load(config);

			// load instances of all possible tests in any order
			load(new BaseUrlNotFoundTest()); 		// 6.2.1.1
			load(new BaseUrlGetNoAcceptTest());		// 6.2.1.2
			load(new BaseUrlGetStarAcceptTest());	// 6.2.1.3
			load(new BaseUrlGetTest());		 		// 6.2.1.4
			load(new BaseUrlGetHtmlAcceptTest()); 	// 6.2.1.5

			load(new BaseUrlRootXml());				// 6.3.1
//			load(new BaseUrlRootXmlPost());			// 6.3.2.1
//			load(new BaseUrlRootXmlPut());			// 6.3.2.2
//			load(new BaseUrlRootXmlDeleteTest());	// 6.3.2.3
			load(new BaseUrlOptions());				// 6.2.5.1
			load(new BaseUrlOptionsSecurityHeader()); // 6.2.5.2
			load(new BaseUrlOptionsHcpHeader());	// 6.2.5.3
			load(new BaseUrlOptionsExtHeader());	// 6.2.5.4
			load(new BaseUrlOptionNoBody());		// 6.2.5.6

			load(new BaseSectionFromRootXml());		// 6.4.1.1
			load(new SectionNotFound());			// 6.4.1.2

			load(new DocumentTest());				// 6.5.1.1

			//load(new Test3());
			//load(new Test2());
			//load(new Test5());
			//load(new Test6());
			//load(new Test7());
		} catch (ConfigurationException e) {
			log.error("", e);
		} catch (IllegalArgumentException e) {
			log.error("", e);
		}
	}

	public void execute() {
		ExcecutionPlan exec = new ExcecutionPlan(sortedSet.iterator()); // list.values());
		exec.execute();
	}

	public static Loader getInstance() {
		return loader;
	}

	private void load(TestUnit test) throws IllegalArgumentException {
		if (!idSet.add(test.getId())) {
			// assertion failed
			throw new IllegalArgumentException("Duplicate id [" + test.getId() + "] found with test " + test.getClass().getName());
		}
		final Class<? extends TestUnit> aClass = test.getClass();
		for(Class<? extends TestUnit> depend : test.getDependencyClasses()) {
			if (aClass == depend) {
				// assertion failed
				log.error("dependency cannot include self: " + aClass.getName());
				return;
			}
		}
		// add to hash of test units by class to order tests by dependencies
		list.put(aClass, test);
		sortedSet.add(test);
	}

	public Context getContext() {
		return context;
	}

	public void setConfigFile(String config) {
		configFile = config;
	}

	public TestUnit getTest(Class<? extends TestUnit> aClass) {
		return list.get(aClass);
	}

	public static void main(String[] args) {
		String config = null;
		final Loader loader = Loader.getInstance();
		for (int i=0; i <args.length; i++) {
			if ("-config".equals(args[i]))
				config = args[++i];
			//else if ("-xml".equals(args[i]))
				//xmlOutput = true;
		}
		if (config != null) {
			loader.setConfigFile(config);
		}
		System.out.println("\nExecution:");
		long start = System.currentTimeMillis();
		loader.execute();
		long elapsed = System.currentTimeMillis() - start;

		/*
		Set<TestUnit> sortedSet = new TreeSet<TestUnit>(new Comparator<TestUnit>() {
			public int compare(TestUnit a, TestUnit b) {
				return a.getId().compareTo(b.getId());
			}
		});
		*/
		System.out.println("\n------------------------------------------------------------------------------------");
		System.out.println("\nConformance Test Report:\n");
		// sortedSet.addAll(loader.list.values());
		int failed = 0;
		int testsRun = 0;
		int warningCount = 0 ;
		for (TestUnit test : loader.sortedSet) {
			TestUnit.StatusEnumType status = test.getStatus();
			if (status == null) {
				log.warn("Null status for test " + test.getClass().getName() + " id=" + test.getId());
				// REVIEW: assume skipped, error condition or warning (assertion failed)
				status = TestUnit.StatusEnumType.SKIPPED;
			}
			String outStatus;
			final Set<String> warnings = test.getWarnings();
			if (status != TestUnit.StatusEnumType.SKIPPED && status != TestUnit.StatusEnumType.PREREQ_FAILED) {
				testsRun++;
			}
			if (status == TestUnit.StatusEnumType.PREREQ_FAILED) {
				outStatus = "Prerequsite Failed";
				failed++; // ??
			} else if (status == TestUnit.StatusEnumType.FAILED) {
				if (test.isRequired()) {
					outStatus = "*FAILED*";
					failed++;
				} else {
					outStatus = "warning";
					/*
					* Failed to meet recommended element of the specification (SHOULD, RECOMMENDED, etc.)
					* Treated as a warning such that a test assertion failed, but the type attribute for
					* the test assertion indicated that it was “recommended”, not “required”.
					* This type of failure will not affect the overall conformance result.
					*/
				}
			} else if (status == TestUnit.StatusEnumType.SUCCESS && !warnings.isEmpty()) {
				outStatus = "Success with warnings";
			} else {
				outStatus = status.toString(); // SUCCESS + SKIPPED
			}
			System.out.printf("%s: %s%n", test.getId(), outStatus);
			String name = test.getName();
			if (!name.startsWith("default"))
				System.out.println(name);

			if (!warnings.isEmpty()) {
				warningCount += warnings.size();
				System.out.println("Warnings:");
				for (String s : warnings) {
					System.out.println("\t" + s);
				}
			} else if (status == TestUnit.StatusEnumType.FAILED && !test.isRequired()) {
				warningCount++; // count as a warning
			}
			
			String desc = test.getStatusDescription();
			if (StringUtils.isNotBlank(desc)) {
				System.out.println("Reason: " + desc);
			}
			final Set<? extends TestUnit> dependencies = test.getDependencies();
			System.out.print("Prerequisites:");
			if (dependencies.isEmpty()) {
				System.out.println(" None");
			} else {
				for(TestUnit aTest : dependencies) {
					System.out.println(" " + aTest.getId());
				}
			}

			System.out.println();
		}

		System.out.printf("Tests run: %d, Failures: %d, Warnings: %d, Time elapsed: %.1f sec%n",
				testsRun, failed, warningCount, elapsed / 1000.0);
		System.exit(failed == 0 ? 0 : 1);
	}

}
