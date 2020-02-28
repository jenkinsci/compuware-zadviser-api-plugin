/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Compuware Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.zadviser.common.configuration;

import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.compuware.jenkins.zadviser.Messages;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * Class to handle Compuware global zAdviser configuration settings.
 */
@Extension
public class ZAdviserGlobalConfiguration extends GlobalConfiguration {
	private static final Logger logger = Logger.getLogger("hudson.ZAdviserGlobalConfiguration"); //$NON-NLS-1$

	// Member Variables
	private Secret accessKey;
	private String customerId;
	private Secret encryptionKey;
	private String initialDateRange;
	private Properties lastExecutionTimes = new Properties();

	// Used to indicate if the configuration needs saving; used only in the context of migration.
	protected transient boolean needsSaving = false;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the Jenkins managed singleton for the configuration object
	 */
	public static ZAdviserGlobalConfiguration get() {
		return GlobalConfiguration.all().get(ZAdviserGlobalConfiguration.class);
	}

	/**
	 * Constructor.
	 * <p>
	 * Clients should not call this - use {@link #get()} instead.
	 */
	public ZAdviserGlobalConfiguration() {
		load();
	}

	/**
	 * Return TRUE if the configuration needs saving.
	 *
	 * @return TRUE if the configuration needs saving.
	 */
	public boolean needsSaving() {
		return needsSaving;
	}

	/**
	 * Perform initialization after all jobs have been loaded.
	 */
	@Initializer(after = InitMilestone.JOB_LOADED)
	public static void jobLoaded() {
		ZAdviserGlobalConfiguration globalConfig = ZAdviserGlobalConfiguration.get();
		if (globalConfig.needsSaving()) {
			globalConfig.save();

			logger.info("Compuware global zAdviser configuration has been saved."); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
	 */
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) {
		req.bindJSON(this, json);
		save();

		return true;
	}

	/**
	 * Returns the value of the accessKey attribute. Used for databinding.
	 *
	 * @return the value of the accessKey attribute
	 */
	public Secret getAccessKey() {
		return accessKey;
	}

	/**
	 * Sets the accesskey attribute.
	 *
	 * @param accessKey
	 *            the access key
	 */
	public void setAccessKey(Secret accessKey) {
		this.accessKey = handleEmpty(accessKey);
	}

	/**
	 * Returns the value of the customerId attribute. Used for databinding.
	 *
	 * @return the value of the customerId attribute
	 */
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * Sets the customerId attribute.
	 *
	 * @param customerId
	 *            the customer id
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * Returns the value of the encryptionKey attribute. Used for databinding.
	 *
	 * @return the value of the encryptionKey attribute
	 */
	public Secret getEncryptionKey() {
		return encryptionKey;
	}

	/**
	 * Sets the encryptionKey attribute.
	 *
	 * @param encryptionKey
	 *            the Encryption key
	 */
	public void setEncryptionKey(Secret encryptionKey) {
		this.encryptionKey = handleEmpty(encryptionKey);
	}

	/**
	 * Returns the value of the initialDateRange attribute. Used for databinding.
	 *
	 * @return the value of the initialDateRange attribute
	 */
	public String getInitialDateRange() {
		return initialDateRange;
	}

	/**
	 * Sets the value of the initialDateRange attribute.
	 *
	 * @param initialDateRange
	 *            the initial date range
	 */
	public void setInitialDateRange(String initialDateRange) {
		this.initialDateRange = initialDateRange;
	}

	/**
	 * Returns the value of the lastExecutionTimes attribute. Used for dataabinding.
	 *
	 * @return the value of the lastExecutionTimes attribute
	 */
	public Properties getLastExecutionTimes() {
		return lastExecutionTimes;
	}

	/**
	 * Validation for the initial date range text field.
	 *
	 * @param value
	 *            value passed from the config.jelly "Initial date range" field
	 *
	 * @return validation message
	 */
	public FormValidation doCheckInitialDateRange(@QueryParameter String value) {
		if (StringUtils.isNotBlank(value)) {
			try {
				Integer.parseUnsignedInt(StringUtils.trim(value));
			} catch (NumberFormatException e) {
				return FormValidation.error(Messages.checkInitialDateRangeError());
			}
		}

		return FormValidation.ok();
	}

	/**
	 * Returns the last execution time for the given host.
	 *
	 * @param host the host
	 *
	 * @return the last execution time; can be null
	 */
	public String getLastExecutionTime(String host) {
		return lastExecutionTimes.getProperty(host);
	}

	/**
	 * Sets the last execution time for the given host.
	 *
	 * @param host the host
	 * @param lastExecutionTime the current time in milliseconds
	 */
	public void updateLastExecutionTime(String host, long lastExecutionTime) {
		lastExecutionTimes.put(host, Long.toString(lastExecutionTime));
	}

	/**
	 * Handles an empty Secret so it does not appear masked.
	 *
	 * @param secret
	 *            the Secret to analyze
	 *
	 * @return updated Secret
	 */
    private static Secret handleEmpty(Secret secret) {
		return (secret == null || StringUtils.isBlank(secret.getPlainText())) ? null : secret;
    }
}
