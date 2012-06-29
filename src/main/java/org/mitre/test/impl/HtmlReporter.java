package org.mitre.test.impl;

import org.mitre.test.Loader;
import org.mitre.test.TestUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import java.util.Date;
import java.util.Set;

/**
 * HTML output report writer.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/28/12 8:47 AM
 */
public class HtmlReporter extends AbstractReporter {

    private static final Logger log = LoggerFactory.getLogger(HtmlReporter.class);

    private long startTime, elapsed;

    /**
     * Sets up the reporter.
     * This method is called before any other methods are called with the exception of {@link #setOutputFile}.
     */
    @Override
    public void setup() {
        System.out.println("<html>");
        System.out.println("<head><title>Interoperability Conformance Test Report</title></head>");
        System.out.println("<body>");
        System.out.println("<h1>Interoperability Conformance Test Report</h1>");
        System.out.println("<h2><a name='toc'>Table of contents</a></h2>");
        System.out.println("<ol>");
        System.out.println("<li><B><a href='#configuration'>Configuration</a></B>");
        System.out.println("<li><B><a href='#build'>Build Execution Plan</a></B>");
        System.out.println("<li><B><a href='#exec'>Execution</a></B>");
        System.out.println("<li><B><a href='#summary'>Conformance Test Report</a></B>");
        System.out.println("</ol>"); // <HR>

        // TODO: set custom handler on logger
    }

    /**
     * Start a new group or section in the report
     * @param title  Title of group, value can be null if not relevant
     */
    @Override
    public void startGroup(String title) {
        System.out.println("<hr>");
        if (title != null) {
            int ind = title.indexOf(' ');
            String name = ind < 1 ? title : title.substring(0,ind);
            System.out.printf("<h2><a name='%s'>%s</a></h2>%n", name.toLowerCase(), title);
        }
        System.out.println("<pre>");
    }

    /**
     * End last group or section in the report
     */
    @Override
    public void endGroup() {
        System.out.println("</pre>");
    }

    /**
     * start execution
     */
    @Override
    public void executeStart() {
        startTime = System.currentTimeMillis();
        final Loader loader = Loader.getInstance();
        System.out.println("<hr><h2><a name='exec'>Execution</a></h2>");
        System.out.printf("<h2>Exec Tests (%d) on %s</h2>%n", loader.getCount(), new Date(startTime));
        //System.out.println("<pre>");
        System.out.println("<ol>");
    }

    /**
     * stop execution
     */
    @Override
    public void executeStop() {
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("</ol>");
        //System.out.println("</pre>");
    }

    @Override
    public void startTest(TestUnit test) {
        // System.out.println("<hr width='50%'>");
        System.out.printf("<li><b><a name='%s'>Run test: %s [<a href='#s%s'>%s</a>]</a></b>%n",
                test.getId(), test.getClass().getName(), test.getId(), test.getId());
        System.out.print("<pre>");
    }

    @Override
    public void stopTest(TestUnit test) {
        System.out.println("</pre>");
        TestUnit.StatusEnumType status = test.getStatus();
        String color = null;
        String statusDesc = null;
        if (status == TestUnit.StatusEnumType.SUCCESS) {
            color = "green";
            statusDesc = "Test: OK";
        } else if (status == TestUnit.StatusEnumType.FAILED) {
            color = test.isRequired() ? "red" : "orange";
            statusDesc = "<B>XXX: Test *failed*</B>";
            if (!test.isRequired() && test.getWarnings().isEmpty()) {
                test.addWarning("Recommended feature not implemented");
                System.out.println("WARN: Recommended feature not implemented");
            }
        } else if (status == TestUnit.StatusEnumType.PREREQ_FAILED) {
            color = "purple";
            statusDesc = "<B>Test: prereq failed</B>";
        } else if (status == TestUnit.StatusEnumType.SKIPPED) {
            color = "blue";
            statusDesc = "<B>Test: Skipped</B>";
        } else {
            log.warn("unexpected status: " + status);
        }
        if (statusDesc != null) {
            System.out.printf("<div style='background-color: %s; width: 600px'>%n", color);
            System.out.println(statusDesc);
            System.out.println("</div>");
        }

        String desc = test.getStatusDescription();
        if (desc != null) {
            // check if "test xxx" appears in reason text and make hyperlink
            // e.g. Prerequisite test 6.3.1.1 failed
            int ind = desc.indexOf("test ");
            //System.out.println("<BR>XXX: " + ind);
            if (ind > 0) {
                int endp = desc.indexOf(' ', ind+5);
                //System.out.println("<BR>XXX: " + endp);
                if (endp > ind) {
                    String name = desc.substring(ind+5, endp);
                    if (Loader.getInstance().getIdSet().contains(name)) {
                        desc = String.format("%s <a href='#%s'>%s</a>%s",
                                desc.substring(0,ind+5), name, name,
                                desc.substring(endp));
                    }
                    //System.out.printf("<BR>[%s]%n",name);
                }
            }
            System.out.println("<BR>Reason: " + desc); // debug
        }
        System.out.println("<P>");
    }

    /**
     * Write String to output report. HTML escape string if needed.
     * @param s  The <code>String</code> to be printed.
     */
    @Override
    public void println(String s) {
        System.out.println(escapeHtml(s));
    }

    /**
     * Generate summary report after all tests are run
     * @return error status 0 = all passed, 1 = some failed
     */
    @Override
    public int generateSummary() {
        int failed = 0;
        int testsRun = 0;
        int successCount = 0;
        int warningCount = 0 ;
        System.out.println("<HR>");
        System.out.println("<h2><a name='#summary'>Conformance Test Report</a></h2>");
        System.out.println("<table>");
        final Loader loader = Loader.getInstance();
        Set<TestUnit> tests = loader.getSortedSet();
        for (TestUnit test : tests) {
            TestUnit.StatusEnumType status = test.getStatus();
            System.out.println("<TR>");
            String warning = null;
            if (status == null) {
                warning = "Null status for test " + test.getClass().getName() + " id=" + test.getId();
                // REVIEW: assume skipped, error condition or warning (assertion failed)
                status = TestUnit.StatusEnumType.SKIPPED;
            } else if (status != TestUnit.StatusEnumType.SKIPPED && status != TestUnit.StatusEnumType.PREREQ_FAILED) {
                // only count PASSED and FAILED tests
                testsRun++;
            }
            final Set<String> warnings = test.getWarnings();
            String outStatus;
            String color;
            if (status == TestUnit.StatusEnumType.PREREQ_FAILED) {
                outStatus = "Prerequsite<BR>Failed";
                color = "purple";
                // failed++; // ??
            } else if (status == TestUnit.StatusEnumType.FAILED) {
                if (test.isRequired()) {
                    outStatus = "*FAILED*";
                    color = "red";
                    failed++;
                } else {
                    outStatus = "Failed<BR>recommendation";
                    color = "orange";
                    /*
                     * Failed to meet recommended element of the specification (SHOULD, RECOMMENDED, etc.)
                     * Treated as a "warning" such that a test assertion failed, but the type attribute for
                     * the test assertion indicated that it was 'recommended', not 'required'.
                     * This type of failure will not affect the overall conformance result.
                     */
                }
            } else if (status == TestUnit.StatusEnumType.SUCCESS) {
                successCount++;
                if (warnings.isEmpty()) {
                    outStatus = "Passed";
                    color = "green";
                } else {
                    outStatus = "Passed<BR>with warnings";
                    color = "yellow";
                }
            } else {
                assert(status == TestUnit.StatusEnumType.SKIPPED);
                outStatus = status.toString(); // SKIPPED
                color = "blue";
            }

            System.out.printf("<TD valign='top'><a name='#s%s'/><a href='#%s'>%s</a>",
                    test.getId(), test.getId(), test.getId());
            System.out.printf("<TD bgcolor='%s'>%s%n", color, outStatus);
            String name = test.getName();
            System.out.println("<TD valign='top'>" + name);
            System.out.println("<TR><TD colspan='3'>");

            final Set<? extends TestUnit> dependencies = test.getDependencies();
            System.out.print("<P>Prerequisites:");
            if (dependencies.isEmpty()) {
                System.out.println(" None");
            } else {
                int count = 0;
                for(TestUnit aTest : dependencies) {
                    if (count++ != 0) System.out.print(',');
                    System.out.printf(" <a href='#%s'>%s</a>", aTest.getId(), aTest.getId());
                }
                System.out.println("");
            }

            String desc = test.getStatusDescription();
            if (StringUtils.isNotBlank(desc)) {
                System.out.printf("<P><b>Reason:</b> %s%n", desc);
            }

            if (!warnings.isEmpty()) {
                warningCount += warnings.size();
                System.out.println("<p><b>Warnings</b><ul>");
                for (String s : warnings) {
                    System.out.println("<li>" + s);
                }
                System.out.println("</ul>");
            }
            // else if (status == TestUnit.StatusEnumType.FAILED && !test.isRequired()) {
            //warningCount++; // treat failed recommendation as a warning
            //}
            if (warning != null) log.warn(warning);

            System.out.println("<P/>");
            System.out.println("</tr>");
        }
        System.out.println("</table>");

        System.out.println("<h3>Summary</h3>\n<blockquote><table><tr><td>Tests run:<td>");
        if (tests.size() != testsRun) {
            System.out.printf("%d (%d)%n", testsRun, tests.size());
        } else {
            System.out.printf("%d%n", testsRun);
        }
        System.out.printf("<tr><td>Passed:<td>%d%n", successCount);
        System.out.printf("<tr><td>Failures:<td>%d<tr><td>Warnings:<td>%d", failed, warningCount);
        System.out.printf("<tr><td>Time elapsed:<td>%.1f sec%n",elapsed / 1000.0);
        System.out.println("</table></blockquote><P>Return to <a href='#toc'>Table of Contents</a>");

        System.out.println("</body>");
        System.out.println("</html>");

        if (outputStream != null) {
            System.setOut(origSysOut); // restore original System.out
            outputStream.close();
        }
        return failed;
    }

    private static String escapeHtml(String s) {
        StringBuilder buf = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<')
                buf.append("&lt;");
            else if (c == '>')
                buf.append("&gt;");
            else if (c == '&')
                buf.append("&amp;");
            else
                buf.append(c);
        }
        return buf.toString();
    }

}
