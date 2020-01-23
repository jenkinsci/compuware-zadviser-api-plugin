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

import org.apache.commons.lang.StringUtils;
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
	// Constants
	private static Logger m_logger = Logger.getLogger("hudson.ZAdviserGlobalConfiguration"); //$NON-NLS-1$

	// Member Variables
	private Secret awsAccessKey;
	private Secret encryptionKey;
	private String initialDateRange;
	private boolean shouldEncrypt = true;
	private Properties lastExecutionTimes = new Properties();

	// Used to indicate if the configuration needs saving; used only in the context of migration.
	protected transient boolean m_needsSaving = false;

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
		return m_needsSaving;
	}

	/**
	 * Perform initialization after all jobs have been loaded.
	 */
	@Initializer(after = InitMilestone.JOB_LOADED)
	public static void jobLoaded() {
		ZAdviserGlobalConfiguration globalConfig = ZAdviserGlobalConfiguration.get();
		if (globalConfig.needsSaving()) {
			globalConfig.save();

			m_logger.info("Compuware global zAdviser configuration has been saved."); //$NON-NLS-1$
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
	 * Returns the AWS Access Key. Used for databinding.
	 * 
	 * @return the AWS Access Key
	 */
	public Secret getAwsAccessKey() {
		return awsAccessKey;
	}

	/**
	 * Sets the AWS Access Key.
	 * 
	 * @param awsAccessKey
	 *            the AWS Access Key
	 */
	public void setAwsAccessKey(Secret awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}

	/**
	 * Returns the Encryption Key. Used for databinding.
	 * 
	 * @return the Encryption Key
	 */
	public Secret getEncryptionKey() {
		return encryptionKey;
	}

	/**
	 * Sets the Encryption Key.
	 * 
	 * @param encryptionKey
	 *            the Encryption Key
	 */
	public void setEncryptionKey(Secret encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	/**
	 * Returns the Initial Date Range. Used for databinding.
	 * 
	 * @return the initialDateRange
	 */
	public String getInitialDateRange() {
		return initialDateRange;
	}

	/**
	 * Sets the Initial Date Range.
	 * 
	 * @param initialDateRange
	 *            the Initial Date Range
	 */
	public void setInitialDateRange(String initialDateRange) {
		this.initialDateRange = initialDateRange;
	}
	
	/**
	 * Returns the Should Encrypt. Used for databinding.
	 * 
	 * @return the shouldEncrypt
	 */
	public boolean getShouldEncrypt() {
		return shouldEncrypt;
	}

	/**
	 * Returns the Should Encrypt. Used for databinding.
	 * 
	 * @return the shouldEncrypt
	 */
	public boolean shouldEncrypt() {
		return shouldEncrypt;
	}

	/**
	 * Sets the Should Encrypt.
	 * 
	 * @param shouldEncrypt
	 *            the Should Encrypt
	 */
	public void setShouldEncrypt(boolean shouldEncrypt) {
		this.shouldEncrypt = shouldEncrypt;		
	}

	/**
	 * Returns the lastExecutionTimes. Used for dataabinding.
	 * 
	 * @return the lastExecutionTimes
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
		String tempValue = StringUtils.trimToEmpty(value);
		if (StringUtils.isNotEmpty(tempValue)) {
			try {
				Integer.parseUnsignedInt(tempValue);
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
}