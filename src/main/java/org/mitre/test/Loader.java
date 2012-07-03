package org.mitre.test;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;

import org.mitre.rhex.*;
import org.mitre.test.impl.HtmlReporter;
import org.mitre.test.impl.TextReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main Loader loads test units, executes them, and generates output report
 *
 * @author Jason Mathews, MITRE Corp.
 */
public final class Loader {

	private static final Logger log = LoggerFactory.getLogger(Loader.class);

	private final Context context = new Context();

    /**
     * Hash of TestUnit instances indexed by class to allow for dependency checking
     */
	private final Map<Class<? extends TestUnit>, TestUnit> list =
			new HashMap<Class<? extends TestUnit>, TestUnit>();

    /**
     * Sorted set of loaded tests
     */
	private final Set<TestUnit> sortedSet = new TreeSet<TestUnit>();

	private final Set<String> idSet = new HashSet<String>();

    /**
     * Singleton Loader instance
     */
	private static final Loader loader = new Loader();

    private static final AtomicBoolean initialized = new AtomicBoolean();

    private Loader() {
        // private constructor
	}

    /**
     * Get singleton Loader instance.
     *
     * @param initCheck  Flag to check if Loader needs one-time initialization at run-time.
     *              If <code>false</code> then bypassed the initCheck check.
     * @return Loader
     */
    public static Loader getInstance(boolean initCheck) {
        if (initCheck) {
            loader.init();
        }
        return loader;
    }

    public static Loader getInstance() {
        loader.init();
        return loader;
    }

    private void init() {
        if (initialized.getAndSet(true)) {
            return;
        }
        log.debug("XXX: init Loader");
        final Reporter reporter = getContext().getReporter();
        reporter.startGroup("Configuration");
        try {
            XMLConfiguration config = new XMLConfiguration();
            // load from cmd-line parameter. default=config.xml
            String configName = System.getProperty("configFile");
            if (StringUtils.isBlank(configName)) configName = "config.xml"; // default
            File configFile = new File(configName);
            config.setFile(configFile);
            config.load();

            // dump the config file to output
            System.out.println("Config: " + configFile);
            FileInputStream reader = null;
            try {
                // strip comments, passwords, and blank lines
                reader = new FileInputStream(configFile);
                final String content = IOUtils.toString(reader, "UTF-8")
                        .replaceAll("(?s)<!--.*?-->\r?\n?", "")
                        .replaceAll("(?s)<password>[^>]+</password>", "")
                        .replaceAll("(?s)\\s*[\r\n]+", "\n");
                System.out.println(content);
            } catch(IOException e) {
                log.debug("", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
            System.out.println("---------------------------------------------------------------------");

            context.load(config);

            String profile = context.getString("profileDocumentFile");
            if (StringUtils.isNotBlank(profile)) {
                loadProfile(profile);
            } else {
                loadDefaultTests();
            }
        } catch (ConfigurationException e) {
            log.error("", e);
        } catch (IllegalArgumentException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("Failed to load assertions from profile document", e);
        } finally {
            reporter.endGroup();
        }
    }

	private void loadProfile(String profile) throws IOException {
        System.out.println("Profile document=" + profile);
        File file = new File(profile);
		if (!file.isFile()) {
			throw new IllegalArgumentException("file file " + file +
					" does not exist or isn't regular file");
		}
		try {
			Document doc = context.getBuilder(null).build(file);
			for(Object child : doc.getRootElement().getChildren("testAssertion")) {
				if (!(child instanceof Element)) continue;
				Element e = (Element)child;
				String className = StringUtils.trimToEmpty(e.getAttributeValue("class"));
				if (className == null) continue;
				System.out.println(className);
				Class objClass = Class.forName(className);
				TestUnit testUnit = (TestUnit) objClass.newInstance();
				load(testUnit);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void loadDefaultTests() throws IllegalArgumentException {
		// load instances of all possible tests in any order
		// prerequisites will be ordered in the execution plan such that
		// they're executed before those tests that require them

		load(new BaseUrlNotFoundTest()); 		// 6.2.1.1
		load(new BaseUrlGetNoAcceptTest());		// 6.2.1.2
		load(new BaseUrlGetStarAcceptTest());		// 6.2.1.3
		load(new BaseUrlGetTest());		 	// 6.2.1.4
		load(new BaseUrlGetHtmlAcceptTest()); 		// 6.2.1.5

		load(new CreateDuplicateSection());		// 6.2.2.3 [req=6.3.1.1]

		load(new BaseUrlOptions());			// 6.2.5.1
		load(new BaseUrlOptionsSecurityHeader());	// 6.2.5.2 [req=6.2.5.1]
		load(new BaseUrlOptionsHcpHeader());		// 6.2.5.3 [req=6.2.5.1]
		load(new BaseUrlOptionsExtHeader());		// 6.2.5.4 [req=6.2.5.1]
		load(new BaseUrlOptionNoBody());		// 6.2.5.6 [req=6.2.5.1]
		load(new BaseUrlOptionsNotFound());		// 6.2.5.7

		load(new BaseUrlRootXml());			// 6.3.1.1
		load(new BaseUrlRootXmlNotFound());		// 6.3.1.2

		load(new BaseUrlRootXmlPost());			// 6.3.2.1
		load(new BaseUrlRootXmlPut());			// 6.3.2.2
		load(new BaseUrlRootXmlDeleteTest());		// 6.3.2.3

		load(new BaseSectionFromRootXml());		// 6.4.1.1 [req=6.3.1.1]
		load(new SectionNotFound());			// 6.4.1.2

		load(new DocumentCreate());			// 6.4.2.2 [req=6.3.1.1]
		load(new DocumentCreateCheck());		// 6.4.2.3 [req=6.4.2.2]
		load(new DocumentBadCreate());			// 6.4.2.4 [req=6.3.1.1]

		load(new SectionPut());             // 6.4.3 [req=6.4.1.1]


		load(new DocumentTest());			// 6.5.1.2 [req=6.4.1.1]
		load(new DocumentNotFound());			// 6.5.1.3 [req=6.4.1.1]

		load(new DocumentUpdate());			// 6.5.2.1
		load(new DocumentPut());			// 6.5.2.3 [req=6.4.1.1]
	}

	/**
	 * Creates and executes an execution plan for all test loaded tests
     *
	 * @return timestamp when execution started
	 */
	public void execute() {
        Reporter reporter = getContext().getReporter();
        reporter.startGroup("Build Execution Plan");
        ExcecutionPlan exec = new ExcecutionPlan(sortedSet.iterator());
        reporter.endGroup();
		exec.execute();
	}

	public void load(TestUnit test) throws IllegalArgumentException {
		if (!idSet.add(test.getId())) {
			// assertion failed
			log.error("Duplicate id [" + test.getId() + "] found with test " + test.getClass().getName());
			return;
		}
		final Class<? extends TestUnit> aClass = test.getClass();
		for(Class<? extends TestUnit> depend : test.getDependencyClasses()) {
			if (aClass == depend) {
				// assertion failed
				log.error("dependency cannot include self: " + aClass.getName());
				return;
			}
		}
		// add to hash of test units by class
		list.put(aClass, test);
		sortedSet.add(test);
	}

	public Context getContext() {
		return context;
	}

	public TestUnit getTest(Class<? extends TestUnit> aClass) {
		return list.get(aClass);
	}

    public int getCount() {
        return list.size();
    }

    public Set<TestUnit> getSortedSet() {
        return Collections.unmodifiableSet(sortedSet);
    }

    public Set<String> getIdSet() {
        return Collections.unmodifiableSet(idSet);
    }

	public static void main(String[] args) {

        Reporter reporter = null;
        String outFile = null;
        for (String arg : args) {
            if ("-html".equals(arg))
                reporter = new HtmlReporter();
            else if (arg.startsWith("-out=")) {
                outFile = arg.substring(5);
            }
        }
        if (reporter == null) reporter = new TextReporter();
        if (outFile != null)
            try {
                reporter.setOutputFile(outFile);
            } catch (IOException e) {
                log.error("", e);
                System.exit(1);
            }

        // must setup report before initializing the Loader
        // and must get Loader with false argument to bypass init check
        final Loader loader = Loader.getInstance(false);
        Context context = loader.getContext();
        context.setReporter(reporter);
        reporter.setup();

        loader.init();

		loader.execute();

		int failed = reporter.generateSummary();

		System.exit(failed == 0 ? 0 : 1);
	}

}
