package org.mitre.test.impl;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.mitre.test.Loader;
import org.mitre.test.TestUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text output report writer.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/28/12 8:50 AM
 */
public class TextReporter extends AbstractReporter {

    private static final Logger log = LoggerFactory.getLogger(TextReporter.class);

    /**
     * Sets up the reporter.
     * This method is called before any other methods are called with the exception of {@link #setOutputFile}.
     */
    @Override
    public void setup() {
        // nothing
    }

    /**
     * Start execution
     */
    @Override
    public void executeStart() {
        startTime = System.currentTimeMillis();
        final Loader loader = Loader.getInstance();
        System.out.println("\nExecution:");
        System.out.printf("\nExec Tests (%d) on %s%n", loader.getCount(), new Date(startTime));
    }

    /**
     * Stop execution
     */
    @Override
    public void executeStop() {
        elapsedTime = System.currentTimeMillis() - startTime;
    }

    /**
     * Start a new test
     * @param test
     */
    @Override
    public void startTest(TestUnit test) {
        System.out.printf("%nRun test: %s [%s]%n", test.getClass().getName(), test.getId());
    }

    /**
     * Called after test is executed
     * @param test
     */
    @Override
    public void stopTest(TestUnit test) {
        TestUnit.StatusEnumType status = test.getStatus();
        if (status == TestUnit.StatusEnumType.SUCCESS)
            System.out.println("Test: OK");
        else if (status == TestUnit.StatusEnumType.FAILED) {
            System.out.println("XXX: Test *failed*");
            String desc = test.getStatusDescription();
            if (desc != null) System.out.println("Reason: " + desc); // debug
        } else if (status == TestUnit.StatusEnumType.SKIPPED) {
            System.out.println("Test: Skipped");
            String desc = test.getStatusDescription();
            if (desc != null) System.out.println("Reason: " + desc); // debug
        }
        System.out.println();
    }

    /**
     * Generate summary report after all tests are run
     * @return error status 0 = all passed, 1 = some failed
     */
    @Override
    public int generateSummary() {
        System.out.println("\n------------------------------------------------------------------------------------");
        System.out.println("\nConformance Test Report:\n");
        // sortedSet.addAll(loader.list.values());
        int failed = 0;
        int testsRun = 0;
        int warningCount = 0 ;
        final Loader loader = Loader.getInstance();
        for (TestUnit test : loader.getSortedSet()) {
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
                         * the test assertion indicated that it was 'recommended', not 'required'.
                         * This type of failure will not affect the overall conformance result.
                         */
                }
            } else if (status == TestUnit.StatusEnumType.SUCCESS) {
                if (warnings.isEmpty())
                    outStatus = "Passed";
                else
                    outStatus = "Passed with warnings";
            } else {
                outStatus = status.toString(); // SKIPPED
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
                warningCount++; // count failed recommendation as a warning
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
                int count = 0;
                for(TestUnit aTest : dependencies) {
                    if (count++ != 0) System.out.print(',');
                    System.out.printf(" %s", aTest.getId());
                }
                System.out.println();
            }

            System.out.println();
        }

        System.out.printf("Tests run: %d, Failures: %d, Warnings: %d, Time elapsed: %.1f sec%n",
                testsRun, failed, warningCount, elapsedTime / 1000.0);

        if (outputStream != null) {
            System.setOut(origSysOut);
            outputStream.close();
        }
        return failed;
    }

    /**
     * Start a new group or section in the report
     * @param title  Title of group, value can be null if not relevant
     */
    @Override
    public void startGroup(String title) {
        if (title != null) System.out.println(title + ":");
    }

    /**
     * End last group or section in the report
     */
    @Override
    public void endGroup() {
        // nothing
    }

}
