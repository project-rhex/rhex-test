package org.mitre.test.impl;

import org.mitre.test.Reporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Base report writer.
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/28/12 3:32 PM
 */
public abstract class AbstractReporter implements Reporter {

    protected PrintStream outputStream, origSysOut;

    @Override
    public void setOutputFile(String outFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outFile);
        outputStream = new PrintStream(fos);
        origSysOut = System.out;
        System.setOut(outputStream);
    }

}
