package org.mitre.test;

import java.io.IOException;

/**
 * Reporter interface. Report generation takes place through concrete
 * implementations of this interface.
 * <p/>
 * Individual tests should not need to know the details of any given Reporter
 * implementation. Tests can simply write output to System.out or using Logger.
 * Special handling and formatting for output should be hidden in implementation
 * of the Reporter instance.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/28/12 8:46 AM
 */
public interface Reporter {

    /**
     * Sets up the reporter.
     * This method is called before any other methods are called with the exception of {@link #setOutputFile}.
     */
    void setup();

    /**
     * Start execution
     */
    void executeStart();

    /**
     * Stop execution
     */
    void executeStop();

    /**
     * Generate summary report after all tests are run.
     *
     * @return error status 0 = all passed, 1 = some failed
     */
    int generateSummary();

    /**
     * Set output file.
     *
     * @param outFile
     * @throws IOException  if an I/O error occurs.
     */
    void setOutputFile(String outFile) throws IOException;

    /**
     * Start a new group or section in the report
     * @param title  Title of group, value can be null if not relevant
     */
    void startGroup(String title);

    /**
     * End last group or section in the report
     */
    void endGroup();

    /**
     * Start a new test
     * @param test
     */
    void startTest(TestUnit test);

    /**
     * Called after test is executed
     * @param test
     */
    void stopTest(TestUnit test);

    /**
     * Closes this reporter and releases any system resources
     * associated with it.
     */
    void close();

}
