package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:55 AM
 */
public class Context {

	private static final Logger log = LoggerFactory.getLogger(Context.class);

	private SAXBuilder builder, validatingBuilder;

	/** Validation feature id */
	protected static final String VALIDATION_FEATURE =
		"http://xml.org/sax/features/validation";

	/** Schema validation feature id */
	protected static final String SCHEMA_VALIDATION_FEATURE =
		"http://apache.org/xml/features/validation/schema";

	/** Schema full checking feature id */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE =
		"http://apache.org/xml/features/validation/schema-full-checking";

	protected static final String LOAD_DTD_GRAMMAR =
		"http://apache.org/xml/features/nonvalidating/load-dtd-grammar";  // [TRUE]

	protected static final String LOAD_EXTERNAL_DTD =
		"http://apache.org/xml/features/nonvalidating/load-external-dtd"; // [TRUE]

	protected static final String CONTINUE_AFTER_FATAL_FEATURE =
		"http://apache.org/xml/features/continue-after-fatal-error"; // [FALSE]

	@NonNull
	private URI baseURL;

	private HttpHost proxy;

	// security info
	// root.xml contents ?

	private XMLConfiguration config;

	/**
	 * server specific implementation of HttpRequestChecker to pre-test HTTP requests
	 * such as handling authentication
	 */
	private HttpRequestChecker httpRequestChecker;

	private String baseUrlString; // cached copy of baseURL.toASCIIString()

	@NonNull
	public URI getBaseURL() {
		return baseURL;
	}

	@NonNull
	public URI getBaseURL(String relativePath) throws URISyntaxException {
		if (StringUtils.isBlank(relativePath)) {
			return baseURL;
		}
		String uri = baseUrlString; // baseURL.toASCIIString();
		if (relativePath.startsWith("/")) {
			// relative paths are relative to the baseURL and server-relative URLs
			// will be assumed to be relative to baseURL not the server root
			// if (uri.endsWith("/")) // always true
			// relativePath = relativePath.substring(1);
			uri += relativePath.substring(1); // strip off the leading '/'
		} else {
			// relative path is correctly relative so append to base URL
			uri += relativePath;
			//if (uri.endsWith("/")) uri += relativePath; // always true
			//else uri += "/" + relativePath;
		}
		return new URI(uri);
	}

	/**
	 * Load configuration
	 *
	 * @param config
	 * @throws IllegalArgumentException if any required configuration element is invalid or missing
	 * @exception NumberFormatException if any required string property does not contain a
	 *               parsable integer.
	 */
	public void load(XMLConfiguration config) {
		this.config = config;
		final String url = config.getString("baseURL");
		// see http://commons.apache.org/configuration/userguide/howto_xml.html
		if (StringUtils.isBlank(url)) {
			// TODO: if any tests don't require baseURL then may this optional and have tests check and mark status = SKIPPED
			throw new IllegalArgumentException("baseURL property must be defined");
		} else {
			try {
				baseURL = new URI(url);
				if (baseURL.getQuery() != null) {
					log.error("6.1.1 baseURL MUST NOT contain a query component, baseURL=" + baseURL);
				}
				baseUrlString = baseURL.toASCIIString();
				final String baseRawString = baseURL.toString();
				if (!baseUrlString.equals(baseRawString)) {
					log.warn("baseURL appears to have non-ASCII characters and comparisons using URL may have problems");
					log.debug("baseURL ASCII String=" + baseUrlString);
					log.debug("baseURL raw String=" + baseRawString);
				}
				if (!baseUrlString.endsWith("/")) baseUrlString += '/'; // end the baseURL with slash
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}

		// setup HTTP proxy if required
		String proxyHost = config.getString("proxy.host");
		String proxyPort = config.getString("proxy.port");
		if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
			proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort), "http");
		}

		// load optional HttpRequestChecker for HTTP request handling
		final String httpRequestCheckerClass = config.getString("HttpRequestChecker");
		if (StringUtils.isNotBlank(httpRequestCheckerClass)) {
			try {
				Class httpClass = Class.forName(httpRequestCheckerClass);
				httpRequestChecker = (HttpRequestChecker) httpClass.newInstance();
				httpRequestChecker.setup(this);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			} catch (InstantiationException e) {
				throw new IllegalArgumentException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Get a string associated with the given configuration key.
	 * @param key The configuration key
	 * @return The associated string value if key is found otherwise null
	 */
	@CheckForNull
	public String getString(String key) {
		return config != null ? config.getString(key) : null;
	}

	/**
	 * Get named property as File from config.xml
	 * @param key The configuration key
	 * @return File or null if property does not found or file does not exist
	 */
	@CheckForNull
	public File getPropertyAsFile(String key) {
		String fileProp = getString(key);
		if (StringUtils.isBlank(fileProp)) {
			log.debug("property {} not found or contains empty string", key);
			return null;
		}
		log.trace(fileProp);
		File file = new File(fileProp);
		if (!file.isFile()) {
			log.info("file {} does not exist or isn't regular file", file);
			return null;
		}
		return file;
	}

	/**
	 * Get named property as URI from config.xml
	 * @param key The configuration key
	 * @return URI or null if property does not found or not valid URI
	 */
	@CheckForNull
	public URI getPropertyAsURI(String key) {
		String value = getString(key);
		if (StringUtils.isBlank(value)) {
			log.debug("property {} not found or contains empty string", key);
			return null;
		}
		log.trace(value);
		try {
			return new URI(value);
		} catch (URISyntaxException e) {
			log.warn("", e);
			return null;
		}
	}

	public SAXBuilder getBuilder(ErrorHandler errorHandler) {
		if (builder == null) {
			builder = new SAXBuilder(false);
			builder.setFeature(VALIDATION_FEATURE, false); // [false]
			builder.setFeature(SCHEMA_FULL_CHECKING_FEATURE, false); // [false]
			builder.setFeature(SCHEMA_VALIDATION_FEATURE, false); // [false]
			builder.setFeature(LOAD_DTD_GRAMMAR, false); // [true]
			builder.setFeature(LOAD_EXTERNAL_DTD, false); // [true]
			builder.setFeature("http://xml.org/sax/features/external-general-entities", false); // TRUE
			builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // TRUE
			// http://xml.org/sax/features/namespace-prefixes [false]
			// builder.setFeature("http://xml.org/sax/features/namespaces", true); [true]
		}
		builder.setErrorHandler(errorHandler);
		return builder;
	}

	public SAXBuilder getValidatingBuilder(ErrorHandler errorHandler) {
		if (validatingBuilder == null) {
			validatingBuilder = new SAXBuilder(true);
			validatingBuilder.setFeature(VALIDATION_FEATURE, true);
			validatingBuilder.setFeature(SCHEMA_FULL_CHECKING_FEATURE, true);
			validatingBuilder.setFeature(SCHEMA_VALIDATION_FEATURE, true);
			validatingBuilder.setFeature(LOAD_DTD_GRAMMAR, false);
			validatingBuilder.setFeature(LOAD_EXTERNAL_DTD, false);
		}
		validatingBuilder.setErrorHandler(errorHandler);
		return validatingBuilder;
	}

	//private HttpClient client;
	public HttpClient getHttpClient() {
		HttpClient client = new DefaultHttpClient();
		if (proxy != null) {
			// System.out.println("XXX: use HTTP proxy");
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
		return client;
	}

	/**
	 * Wrap <tt>HttpClient.execute()</tt> to pre/post-test HTTP requests for any
	 * server specific implementation handling such as authentication.
	 *
	 * @param client   the HttpClient, must never be null
	 * @param request   the request to execute, must never be null
	 *
	 * @return  the response to the request.
	 * @throws IOException in case of a problem or the connection was aborted
	 * @throws ClientProtocolException in case of an http protocol error
	 */
	public HttpResponse executeRequest(HttpClient client, HttpRequestBase request)
			throws IOException
	{
		if (httpRequestChecker != null) {
			return httpRequestChecker.executeRequest(this, client, request);
		} else {
			return client.execute(request);
		}
	}
}
