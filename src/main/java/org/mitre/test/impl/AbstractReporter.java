package org.mitre.test.impl;

import org.mitre.test.Reporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Base report writer.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/28/12 3:32 PM
 */
public abstract class AbstractReporter implements Reporter {

    protected long startTime;
    protected long elapsedTime;

    protected PrintStream outputStream, origSysOut;

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    @Override
    public void setOutputFile(String outFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outFile);
        outputStream = createPrintStream(fos);
        if (origSysOut == null) {
            // save original
            origSysOut = System.out;
            // if call setOutputFile multiple times without first calling close()
            // would lose original System.out so only change if value unset.
        }
        System.setOut(outputStream);
    }

    /**
     * Create PrintStream. Allow subclasses to implement custom PrintStream classes
     *
     * @param  out        The output stream to which values and objects will be
     *                    printed
     * @return PrintStream
     */
    protected PrintStream createPrintStream(OutputStream out) {
        return new PrintStream(out);
    }

    public void close() {
        if (origSysOut != null) {
            // restore original System.out
            System.setOut(origSysOut);
            origSysOut = null;
        }
        if (outputStream != null) {
            outputStream.close();
	        outputStream = null;
        }
    }

}
