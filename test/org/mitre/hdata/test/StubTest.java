package org.mitre.hdata.test;

import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

/**
 * Simple Test
 * @author Jason Mathews, MITRE Corp.
 * Date: 4/23/12 2:48 PM
*/
public abstract class StubTest extends BaseTest {

	public static final String PROP_KEEP_STATE_BOOL = "keepState";

	StatusEnumType testStatus = StatusEnumType.SKIPPED;
	boolean keepState;
	boolean execState;

	@Override
	public void execute() throws TestException {
		// System.out.println("exec: " + getId() + " " + testStatus);
		setStatus(testStatus);
		execState = keepState;
		if (isKeepResponse()) {
			setResponse(new BasicHttpResponse(
					new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "Ok")));
		}
	}

	public void setTestStatus(StatusEnumType testStatus) {
		this.testStatus = testStatus;
	}

	public void cleanup() {
		if (!keepState || getStatus() == StatusEnumType.FAILED) {
			execState = false;
		}
	}

	public void setProperty(String key, Object value) {
		if (PROP_KEEP_STATE_BOOL.equals(key)) {
			this.keepState = (Boolean)value;
		} else {
			super.setProperty(key, value);
		}
	}
}
