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
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.GET;
import org.kohsuke.stapler.verb.POST;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
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
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for the zAdviser upload build step.
 */
public class ZAdviserUploadData extends Builder implements SimpleBuildStep {
	// Member Variables
	private String connectionId;
	private String uploadDataFile;

	/**
	 * Constructor.
	 *
	 * @param connectionId
	 *            a unique host connection identifier
	 * @param uploadDataFile
	 *            the data file to upload
	 */
	@DataBoundConstructor
	public ZAdviserUploadData(String connectionId, String uploadDataFile) {
		this.connectionId = StringUtils.trimToEmpty(connectionId);
		this.uploadDataFile = StringUtils.trimToEmpty(uploadDataFile);
	}

	/**
	 * Gets the value of the connectionId attribute.
	 *
	 * @return <code>String</code> value of connectionId
	 */
	public String getConnectionId() {
		return connectionId;
	}

	/**
	 * Gets the value of the uploadDataFile attribute.
	 *
	 * @return <code>String</code> value of uploadDataFile
	 */
	public String getUploadDataFile() {
		return uploadDataFile;
	}

	/**
	 * Sets the value of the uploadDataFile attribute.
	 *
	 * @param uploadDataFile
	 *            the data file to upload
	 */
	public void setUploadDataFile(String uploadDataFile) {
		this.uploadDataFile = uploadDataFile;
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
		 * Validator for the 'Host connection' field.
		 *
		 * @param connectionId
		 *            unique identifier for the host connection passed from the config.jelly "connectionId" field
		 *
		 * @return validation message
		 */
		@GET
		public FormValidation doCheckConnectionId(@QueryParameter String connectionId) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			if (StringUtils.isBlank(connectionId)) {
				return FormValidation.error(Messages.checkHostConnectionError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Upload Data File' field.
		 * <p>
		 * If a valid data file exists, then the access key will be validated for existence.
		 *
		 * @param uploadDataFile
		 *            the upload data file passed from the config.jelly "uploadDataFile" field
		 *
		 * @return validation message
		 */
		@POST
		public FormValidation doCheckUploadDataFile(@QueryParameter String uploadDataFile) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			if (StringUtils.isBlank(uploadDataFile)) {
				return FormValidation.error(Messages.checkUploadDataFileError());
			} else {
				ZAdviserGlobalConfiguration zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();

				Secret accessKey = zAdviserGlobalConfig.getAccessKey();
				if (accessKey == null || StringUtils.isBlank(accessKey.getPlainText())) {
					return FormValidation.error(Messages.checkMissingAccessKeyError());
				}

				String customerId = zAdviserGlobalConfig.getCustomerId();
				if (StringUtils.isBlank(customerId)) {
					return FormValidation.error(Messages.checkMissingCustomerIdError());
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

		/**
		 * Fills in the Host Connection selection box with applicable connections.
		 *
		 * @param context
		 *            filter for host connections
		 * @param connectionId
		 *            an existing host connection identifier; can be null
		 * @param project
		 *            the Jenkins project
		 *
		 * @return host connection selections
		 */
		@POST
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project) {
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection connection : hostConnections) {
				boolean isSelected = false;
				if (connectionId != null) {
					isSelected = connectionId.matches(connection.getConnectionId());
				}

				model.add(new Option(connection.getDescription() + " [" + connection.getHostPort() + ']', //$NON-NLS-1$
						connection.getConnectionId(), isSelected));
			}

			return model;
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

		assert launcher != null;
		VirtualChannel vChannel = launcher.getChannel();

		// Check CLI compatibility
		FilePath cliDirectory = new FilePath(vChannel, globalConfig.getTopazCLILocation(launcher));
		String cliVersion = CLIVersionUtils.getCLIVersion(cliDirectory, ZAdviserUtilitiesConstants.ZADVISER_MINIMUM_CLI_VERSION);
		CLIVersionUtils.checkCLICompatibility(cliVersion, ZAdviserUtilitiesConstants.ZADVISER_MINIMUM_CLI_VERSION);

		ArgumentListBuilder args = new ArgumentListBuilder();

		assert vChannel != null;
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? ZAdviserUtilitiesConstants.ZADVISER_CLI_SH : ZAdviserUtilitiesConstants.ZADVISER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		args.add(cliScriptFileRemote);

		// Get host configuration
		HostConnection connection = globalConfig.getHostConnection(getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		args.add(CommonConstants.HOST_PARM, host);

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

		String uploadDataFileStr = ArgumentUtils.escapeForScript(getUploadDataFile());
		args.add(ZAdviserUtilitiesConstants.UPLOAD_DATA_FILE_PARM, uploadDataFileStr);

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = run.getEnvironment(listener);
		args.add(ZAdviserUtilitiesConstants.PERSIST_DATA_PARM, ArgumentUtils.escapeForScript(env.get("JENKINS_HOME"))); //$NON-NLS-1$
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
