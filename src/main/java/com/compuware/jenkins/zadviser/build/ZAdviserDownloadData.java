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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
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
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
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
 * Captures the configuration information for zAdviser download/optional encrypt/optional upload build step.
 */
public class ZAdviserDownloadData extends Builder implements SimpleBuildStep {
	// Member Variables
	private String connectionId;
	private String credentialsId;
	private String jcl;
	private String unencryptedCsvFilePath;
	private String encryptedCsvFilePath;
	private boolean encryptData = false;
	private boolean uploadData = true;

	private FilePath jclFile;

	/**
	 * Constructor.
	 *
	 * @param connectionId
	 *            a unique host connection identifier
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param jcl
	 *            job card used to instruct zAdviser to collect
	 * @param encryptedCsvFilePath
	 *            encrypted CSV file path
	 * @param unencryptedCsvFilePath
	 *            unencrypted CSV file path
	 */
	@DataBoundConstructor
	public ZAdviserDownloadData(String connectionId, String credentialsId, String jcl, String encryptedCsvFilePath,
			String unencryptedCsvFilePath) {
		this.connectionId = StringUtils.trimToEmpty(connectionId);
		this.credentialsId = StringUtils.trimToEmpty(credentialsId);
		this.jcl = StringUtils.trimToEmpty(jcl);
		this.encryptedCsvFilePath = StringUtils.trimToEmpty(encryptedCsvFilePath);
		this.unencryptedCsvFilePath = StringUtils.trimToEmpty(unencryptedCsvFilePath);
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
	 * Gets the value of the credentialsId attribute.
	 *
	 * @return <code>String</code> value of credentialsId
	 */
	public String getCredentialsId() {
		return credentialsId;
	}

	/**
	 * Gets the value of the jcl attribute.
	 *
	 * @return <code>String</code> value of jcl
	 */
	public String getJcl() {
		return jcl;
	}

	/**
	 * Gets the value of the encryptedCsvFilePath attribute.
	 *
	 * @return <code>String</code> value of encryptedCsvFilePath
	 */
	public String getEncryptedCsvFilePath() {
		return encryptedCsvFilePath;
	}

	/**
	 * Gets the value of the unencryptedCsvFilePath attribute.
	 *
	 * @return <code>String</code> value of unencryptedCsvFilePath
	 */
	public String getUnencryptedCsvFilePath() {
		return unencryptedCsvFilePath;
	}

	/**
	 * Returns the value of the encryptData attribute. Used for databinding.
	 *
	 * @return the value of the encryptData attribute
	 */
	public boolean isEncryptData() {
		return encryptData;
	}

	/**
	 * Sets the encryptData attribute.
	 *
	 * @param encryptData
	 *            the flag to encrypt data
	 */
	@DataBoundSetter
	public void setEncryptData(boolean encryptData) {
		this.encryptData = encryptData;
	}

	/**
	 * Returns the value of the uploadData attribute. Used for databinding.
	 *
	 * @return the value of the uploadData attribute
	 */
	public boolean isUploadData() {
		return uploadData;
	}

	/**
	 * Sets the uploadData attribute.
	 *
	 * @param uploadData
	 *            the flag to upload data
	 */
	@DataBoundSetter
	public void setUploadData(boolean uploadData) {
		this.uploadData = uploadData;
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
	 * DescriptorImpl is used to create instances of <code>ZAdviserDownloadData</code>. It also contains the global configuration options as
	 * fields, just like the <code>ZAdviserDownloadData</code> contains the configuration options for a job
	 */
	@Symbol("zAdviserDownload")
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

		/*
		 * (non-Javadoc)
		 *
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.zAdviserDownloadDataDescriptorDisplayName();
		}

		/**
		 * Validator for the 'Host connection' field.
		 *
		 * @param connectionId
		 *            unique identifier for the host connection passed from the config.jelly "connectionId" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckConnectionId(@QueryParameter String connectionId) {
			if (StringUtils.isBlank(connectionId)) {
				return FormValidation.error(Messages.checkHostConnectionError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Login credentials' field.
		 *
		 * @param credentialsId
		 *            login credentials passed from the config.jelly "credentialsId" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) {
			if (StringUtils.isBlank(credentialsId)) {
				return FormValidation.error(Messages.checkLoginCredentialsError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'JCL' field.
		 *
		 * @param jcl
		 *            the jcl passed from the config.jelly "jcl" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckJcl(@QueryParameter String jcl) {
			if (StringUtils.isBlank(jcl)) {
				return FormValidation.error(Messages.checkJclError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Encrypted CSV File Path' field.
		 *
		 * @param encryptedCsvFilePath
		 *            the encrypted CSV file path passed from the config.jelly "encryptedCsvFilePath" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckEncryptedCsvFilePath(@QueryParameter String encryptedCsvFilePath) {
			if (StringUtils.isBlank(encryptedCsvFilePath)) {
				return FormValidation.error(Messages.checkEncryptedCsvFilePathError());
			} else {
				ZAdviserGlobalConfiguration zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();

				Secret accessKey = zAdviserGlobalConfig.getAccessKey();
				if (accessKey == null) {
					return FormValidation.error(Messages.checkMissingAccessKeyError());
				} else {
					Secret encryptionKey = zAdviserGlobalConfig.getEncryptionKey();
					if (encryptionKey == null) {
						return FormValidation.error(Messages.checkMissingEncryptionKeyError());
					}
				}
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Unencrypted CSV File Path' field.
		 *
		 * @param unencryptedCsvFilePath
		 *            the unencrypted CSV file path passed from the config.jelly "unencryptedCsvFilePath" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckUnencryptedCsvFilePath(@QueryParameter String unencryptedCsvFilePath) {
			if (StringUtils.isBlank(unencryptedCsvFilePath)) {
				return FormValidation.error(Messages.checkUnencryptedCsvFilePathError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Upload Data' field.
		 *
		 * @param uploadData
		 *            the upload data flag passed from the config.jelly "uploadData" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckUploadData(@QueryParameter Boolean uploadData) {
			if (uploadData != null && uploadData.booleanValue()) {
				ZAdviserGlobalConfiguration zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();

				Secret accessKey = zAdviserGlobalConfig.getAccessKey();
				if (accessKey == null) {
					return FormValidation.error(Messages.checkMissingAccessKeyError());
				}
			}
				
			return FormValidation.ok();
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
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project) {
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

		/**
		 * Fills in the Login Credentials selection box with applicable connections.
		 *
		 * @param context
		 *            filter for login credentials
		 * @param credentialsId
		 *            existing login credentials; can be null
		 * @param project
		 *            the Jenkins project
		 *
		 * @return login credentials selection
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project) {
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardUsernamePasswordCredentials c : creds) {
				boolean isSelected = false;
				if (credentialsId != null) {
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ')' : StringUtils.EMPTY), //$NON-NLS-1$
						c.getId(), isSelected));
			}

			return model;
		}

		/**
		 * Find the requested file using the classloader and return an input stream.
		 *
		 * @param fileName file name to open for input
		 *
		 * @return InputStream the file requested
		 */
		private InputStream readResource(String fileName) {
			return ZAdviserDownloadData.class.getClassLoader().getResourceAsStream(fileName);
		}

		/**
		 * Get the default JCL.
		 *
		 * @return default JCL
		 */
		public String getDefaultJcl() {
			StringBuilder builder = new StringBuilder();
			try (InputStream stream = readResource("defaultJcl.jcl"); //$NON-NLS-1$
					Scanner scanner = new Scanner(stream, "UTF-8")) { //$NON-NLS-1$
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					builder.append(line);
					builder.append('\n');
				}
			} catch (IOException e) {
				// We should not get here, but just in case return empty string in this case; use can always copy from the help example.
			}

			return builder.toString();
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
		try {
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

			// Get host configuration
			HostConnection connection = globalConfig.getHostConnection(getConnectionId());
			StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(), getCredentialsId());

			String host = ArgumentUtils.escapeForScript(connection.getHost());
			args.add(CommonConstants.HOST_PARM, host);

			String port = ArgumentUtils.escapeForScript(connection.getPort());
			args.add(CommonConstants.PORT_PARM, port);

			String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
			args.add(CommonConstants.USERID_PARM, userId);

			String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
			args.add(CommonConstants.PW_PARM);
			args.add(password, true);

			String protocol = connection.getProtocol();
			if (StringUtils.isNotBlank(protocol) && !StringUtils.equalsIgnoreCase(protocol, "none")) { //$NON-NLS-1$
				CLIVersionUtils.checkProtocolSupported(cliVersion);
				args.add(CommonConstants.PROTOCOL_PARM, protocol);
			}

			String codePage = connection.getCodePage();
			args.add(CommonConstants.CODE_PAGE_PARM, codePage);

			String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
			args.add(CommonConstants.TIMEOUT_PARM, timeout);

			// Get workspace configuration
			String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE
					+ UUID.randomUUID().toString();
			logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
			args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

			// Get download configuration
			args.add(ZAdviserUtilitiesConstants.BUILD_STEP_PARAM, ZAdviserUtilitiesConstants.DOWNLOAD_STEP);
			ZAdviserGlobalConfiguration zAdviserGlobalConfiguration = ZAdviserGlobalConfiguration.get();

			jclFile = workspace.createTextTempFile("jcl", ".txt", getJcl()); //$NON-NLS-1$ //$NON-NLS-2$
			String escapedJclFileName = ArgumentUtils.escapeForScript(jclFile.getRemote());
			logger.println("JCL file path: " + escapedJclFileName); //$NON-NLS-1$
			String unencryptedCsvFilePathStr = ArgumentUtils.escapeForScript(getUnencryptedCsvFilePath());
			String lastExecutionTime = zAdviserGlobalConfiguration.getLastExecutionTime(host);
			String initialDateRangeStr = zAdviserGlobalConfiguration.getInitialDateRange();

			args.add(ZAdviserUtilitiesConstants.JCL_FILE_PATH_PARM, escapedJclFileName);
			args.add(ZAdviserUtilitiesConstants.UNENCRYPTED_CSV_FILE_PATH_PARM, unencryptedCsvFilePathStr);
			if (lastExecutionTime != null) {
				args.add(ZAdviserUtilitiesConstants.LAST_DATE_RUN_PARM, lastExecutionTime);
			} else {
				args.add(ZAdviserUtilitiesConstants.INITIAL_DATE_RANGE_PARM, initialDateRangeStr);
			}

			if (isEncryptData() || isUploadData()) {
				// we need the access key for encryption in order to obtain the security rules
				// we need the access key for upload in order to send data via SFTP
				Secret accessKey = zAdviserGlobalConfiguration.getAccessKey();
				if (accessKey != null) {
					args.add(ZAdviserUtilitiesConstants.ACCESS_KEY_PARM);
					args.add(accessKey.getPlainText(), true);
				}
			}

			if (isEncryptData()) {
				Secret encryptionKey = zAdviserGlobalConfiguration.getEncryptionKey();
				if (encryptionKey != null) {
					args.add(ZAdviserUtilitiesConstants.ENCRYPTION_KEY_PARM);
					args.add(encryptionKey.getPlainText(), true);
				}

				String encryptedCsvFilePathStr = ArgumentUtils.escapeForScript(getEncryptedCsvFilePath());
				args.add(ZAdviserUtilitiesConstants.ENCRYPTED_CSV_FILE_PATH_PARM, encryptedCsvFilePathStr);
			}

			if (isUploadData()) {
				String csvFilePathStr;
				if (isEncryptData()) {
					csvFilePathStr = ArgumentUtils.escapeForScript(getEncryptedCsvFilePath());
				} else {
					csvFilePathStr = ArgumentUtils.escapeForScript(getUnencryptedCsvFilePath());
				}
				args.add(ZAdviserUtilitiesConstants.CSV_FILE_PATH_PARM, csvFilePathStr);
			}

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

				zAdviserGlobalConfiguration.updateLastExecutionTime(host, System.currentTimeMillis());
				zAdviserGlobalConfiguration.save();
			}
		} finally {
			cleanUp();
		}
	}

// TODO: Keep code until last execution time storage is decided upon
//
//	/**
//	 * Get the last execution time for the specified host.
//	 *
//	 * @param host the host
//	 * @param run the run
//	 * @param listener the job listener
//	 *
//	 * @return last execution time; can be null
//	 */
//	protected String getLastExecutionTimeForHost(String host, Run<?, ?> run, TaskListener listener) {
//		String lastExecutionTime = null;
//		PrintStream logger = listener.getLogger();
//
//		try {
//			EnvVars env = run.getEnvironment(listener);
//			String jenkinsHome = env.get("JENKINS_HOME");
//			File cfgFile = new File(jenkinsHome + ZAdviserUtilitiesConstants.ZADVISER_LAST_RUN_FILE);
//			if (cfgFile.exists()) {
//				Properties props = new Properties();
//				FilePath cfgFilePath = new FilePath(cfgFile);
//				props.load(cfgFilePath.read());
//
//				lastExecutionTime = props.getProperty(host);
//			}
//		} catch (InterruptedException | IOException e) {
//			logger.println("An error occurred attempting to get the last execution time for host '" + host + "'. " + e.getMessage());
//		}
//
//		return lastExecutionTime;
//	}
//
//	/**
//	 * Update the last execution time for the specified host.
//	 *
//	 * @param host the host
//	 * @param run the run
//	 * @param listener the job listener
//	 */
//	@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification = "createNewFile either creates the file or not if already exists")
//	protected void updateLastExecutionForHost(String host, Run<?, ?> run, TaskListener listener) {
//		PrintStream logger = listener.getLogger();
//
//		try {
//			EnvVars env = run.getEnvironment(listener);
//			String jenkinsHome = env.get("JENKINS_HOME");
//			File cfgFile = new File(jenkinsHome + ZAdviserUtilitiesConstants.ZADVISER_LAST_RUN_FILE);
//
//			cfgFile.createNewFile();
//
//			Properties props = new Properties();
//			FilePath cfgFilePath = new FilePath(cfgFile);
//			props.load(cfgFilePath.read());
//
//			props.setProperty(host, Long.toString(System.currentTimeMillis()));
//
//			FileOutputStream fos = new FileOutputStream(cfgFile);
//			FileLock fLock = fos.getChannel().lock();
//			try {
//				props.store(fos, null);
//			} finally {
//				fLock.release();
//			}
//		} catch (InterruptedException | IOException | SecurityException e) {
//			logger.println("An error occurred attempting to update the last execution time for host '" + host + "'. " + e.getMessage());
//		}
//	}

	/**
	 * Handle clean up when finished builder execution.
	 * <p>
	 * A temporary JCL file is created during builder execution that is consumed by the CLI and is deleted when execution is finished.
	 *
	 * @throws IOException
	 *             if unable to delete temporary JCL file
	 * @throws InterruptedException
	 *             if unable to delete temporary JCL file
	 */
	protected void cleanUp() throws IOException, InterruptedException {
		if (jclFile != null) {
			jclFile.delete();
		}
	}
}
