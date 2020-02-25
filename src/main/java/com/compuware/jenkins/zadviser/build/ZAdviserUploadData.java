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
package com.compuware.jenkins.zadviser.build;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CLIVersionUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import com.compuware.jenkins.zadviser.Messages;
import com.compuware.jenkins.zadviser.build.utils.ZAdviserUtilitiesConstants;
import com.compuware.jenkins.zadviser.common.configuration.ZAdviserGlobalConfiguration;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for the zAdviser upload build step.
 */
public class ZAdviserUploadData extends Builder implements SimpleBuildStep {
	// Member Variables
	private String csvFilePath;

	/**
	 * Constructor.
	 *
	 * @param csvFilePath
	 *            CSV file path
	 */
	@DataBoundConstructor
	public ZAdviserUploadData(String csvFilePath) {
		this.csvFilePath = StringUtils.trimToEmpty(csvFilePath);
	}

	/**
	 * Gets the value of the csvFilePath attribute.
	 *
	 * @return <code>String</code> value of csvFilePath
	 */
	public String getCsvFilePath() {
		return csvFilePath;
	}

	/**
	 * Sets the value of the csvFilePath attribute.
	 *
	 * @param csvFilePath
	 *            the path to the CSV file to upload
	 */
	public void setCsvFilePath(String csvFilePath) {
		this.csvFilePath = csvFilePath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see hudson.tasks.Builder#getDescriptor()
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * DescriptorImpl is used to create instances of <code>ZAdviserUploadData</code>. It also contains the global configuration options as
	 * fields, just like the <code>ZAdviserUploadData</code> contains the configuration options for a job
	 */
	@Symbol("zAdviserUpload")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		/**
		 * Constructor.
		 * <p>
		 * In order to load the persisted global configuration, you have to call load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		/**
		 * Validator for the 'CSV File path' field.
		 * <p>
		 * If a valid CSV file pathe exists, then the access key will be validated for existence.
		 *
		 * @param csvFilePath
		 *            the CSV file path passed from the config.jelly "csvFilePath" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckCsvFilePath(@QueryParameter String csvFilePath) {
			if (StringUtils.isBlank(csvFilePath)) {
				return FormValidation.error(Messages.checkCsvFilePathError());
			} else {
				ZAdviserGlobalConfiguration zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();

				Secret accessKey = zAdviserGlobalConfig.getAccessKey();
				if (accessKey == null || StringUtils.isBlank(accessKey.getPlainText())) {
					return FormValidation.error(Messages.checkMissingAccessKeyError());
				}
			}

			return FormValidation.ok();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.zAdviserUploadDataDescriptorDisplayName();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.compuware.jenkins.build.SubmitJclBaseBuilder#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher,
	 * hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException {
		// obtain argument values to pass to the CLI
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		VirtualChannel vChannel = launcher.getChannel();

		// Check CLI compatibility
		FilePath cliDirectory = new FilePath(vChannel, globalConfig.getTopazCLILocation(launcher));
		String cliVersion = CLIVersionUtils.getCLIVersion(cliDirectory, ZAdviserUtilitiesConstants.ZADVISER_MINIMUM_CLI_VERSION);
		CLIVersionUtils.checkCLICompatibility(cliVersion, ZAdviserUtilitiesConstants.ZADVISER_MINIMUM_CLI_VERSION);

		ArgumentListBuilder args = new ArgumentListBuilder();

		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? ZAdviserUtilitiesConstants.ZADVISER_CLI_SH : ZAdviserUtilitiesConstants.ZADVISER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		args.add(cliScriptFileRemote);

		// Get workspace configuration
		String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE
				+ UUID.randomUUID().toString();
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

		// Get upload configuration
		args.add(ZAdviserUtilitiesConstants.BUILD_STEP_PARAM, ZAdviserUtilitiesConstants.UPLOAD_STEP);
		ZAdviserGlobalConfiguration zAdviserGlobalConfiguration = ZAdviserGlobalConfiguration.get();

		Secret accessKey = zAdviserGlobalConfiguration.getAccessKey();
		if (accessKey != null) {
			args.add(ZAdviserUtilitiesConstants.ACCESS_KEY_PARM);
			args.add(accessKey.getPlainText(), true);
		}

		String customerId = zAdviserGlobalConfiguration.getCustomerId();
		if (StringUtils.isNotEmpty(customerId)) {
			args.add(ZAdviserUtilitiesConstants.CUSTOMER_ID_PARM, customerId);
		}

		String csvFilePathStr = ArgumentUtils.escapeForScript(getCsvFilePath());
		args.add(ZAdviserUtilitiesConstants.CSV_FILE_PATH_PARM, csvFilePathStr);

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = run.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0) {
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
			FilePath topazDataDir = new FilePath(vChannel, topazCliWorkspace);
			topazDataDir.deleteRecursive();
		}
	}
}
