/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018, 2019 Compuware Corporation
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.utils.CLIVersionUtils;
import com.compuware.jenkins.zadviser.common.configuration.ZAdviserGlobalConfiguration;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Matchers.any;
/**
 * Test cases for {@link ZAdviserDownloadData}, {@link ZAdviserUploadData} integration.
 */
@SuppressWarnings("nls")
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({CLIVersionUtils.class, ZAdviserDownloadData.class, Launcher.class, ProcStarter.class})
public class ZAdviserBuildStepIntegrationTest {
	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CONNECTION_ID = "12345";
	private static final String EXPECTED_CREDENTIALS_ID = "67890";
	private static final String EXPECTED_JCL = "some jcl";
	private static final String EXPECTED_ENCRYPTED_CSV_FILE_PATH = "/test/encrypted.csv";
	private static final String EXPECTED_UNENCRYPTED_CSV_FILE_PATH = "/test/unencrypted.csv";
	private static final String EXPECTED_CSV_FILE_PATH = "/test/encrypted.csv";
	private static final String EXPECTED_HOST = "cw01";
	private static final String EXPECTED_PORT = "30947";
	private static final String EXPECTED_CES_URL = "https://expectedcesurl/";
	private static final String EXPECTED_CODE_PAGE = "1047";
	private static final String EXPECTED_PROTOCOL = "TLSv1.2";
	private static final String EXPECTED_TIMEOUT = "123";
	private static final String EXPECTED_USER_ID = "xdevreg";
	private static final String EXPECTED_PASSWORD = "********";
	/* @formatter:on */

	// Member variables
	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();
	private CpwrGlobalConfiguration globalConfig;
//	private ZAdviserGlobalConfiguration zAdviserGlobalConfig;

	@ClassRule
	public static BuildWatcher bw = new BuildWatcher();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setup() {
		try {
			JSONObject hostConnection = new JSONObject();
			hostConnection.put("description", "TestConnection");
			hostConnection.put("hostPort", EXPECTED_HOST + ':' + EXPECTED_PORT);
			hostConnection.put("protocol", EXPECTED_PROTOCOL);
			hostConnection.put("codePage", EXPECTED_CODE_PAGE);
			hostConnection.put("timeout", EXPECTED_TIMEOUT);
			hostConnection.put("connectionId", EXPECTED_CONNECTION_ID);
			hostConnection.put("cesUrl", EXPECTED_CES_URL);

			JSONArray hostConnections = new JSONArray();
			hostConnections.add(hostConnection);

			JSONObject json = new JSONObject();
			json.put("hostConn", hostConnections);
			json.put("topazCLILocationLinux", "/opt/Compuware/TopazCLI");
			json.put("topazCLILocationWindows", "C:\\Program Files\\Compuware\\Topaz Workbench CLI");

			globalConfig = CpwrGlobalConfiguration.get();
			globalConfig.configure(Stapler.getCurrentRequest(), json);

//			zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();
//			zAdviserGlobalConfig.setAccessKey(Secret.fromString("foo"));
//			zAdviserGlobalConfig.setEncryptionKey(Secret.fromString("foobar"));
//			zAdviserGlobalConfig.setInitialDateRange("30");
//			zAdviserGlobalConfig.save();

			SystemCredentialsProvider.getInstance().getCredentials().add(new UsernamePasswordCredentialsImpl(CredentialsScope.USER,
					EXPECTED_CREDENTIALS_ID, null, EXPECTED_USER_ID, EXPECTED_PASSWORD));
			SystemCredentialsProvider.getInstance().save();
		} catch (Exception e) {
			// Add the print of the stack trace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
		ZAdviserDownloadData beforeZAdviserCollectData = new ZAdviserDownloadData(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_JCL,
				EXPECTED_ENCRYPTED_CSV_FILE_PATH, EXPECTED_UNENCRYPTED_CSV_FILE_PATH);
		project.getBuildersList().add(beforeZAdviserCollectData);

		ZAdviserUploadData beforeZAdviserUploadData = new ZAdviserUploadData(EXPECTED_CSV_FILE_PATH);
		project.getBuildersList().add(beforeZAdviserUploadData);

		project = jenkinsRule.configRoundtrip(project);
		jenkinsRule.assertEqualDataBoundBeans(beforeZAdviserCollectData, project.getBuildersList().get(ZAdviserDownloadData.class));
		jenkinsRule.assertEqualDataBoundBeans(beforeZAdviserUploadData, project.getBuildersList().get(ZAdviserUploadData.class));
	}

//	@Test
//	public void testDownloadSuccess() throws Exception {
//		mockStatic(CLIVersionUtils.class);
//		ProcStarter procStarter = mock(ProcStarter.class);
//		PowerMockito.doReturn("19.4.1").when(CLIVersionUtils.class, "getCLIVersion", any(String.class), any(String.class));
//		when(procStarter.join()).thenReturn(0);
//
//		FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
//		ZAdviserDownloadData beforeZAdviserCollectData = new ZAdviserDownloadData(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_JCL,
//				EXPECTED_ENCRYPTED_CSV_FILE_PATH, EXPECTED_UNENCRYPTED_CSV_FILE_PATH);
//		project.getBuildersList().add(beforeZAdviserCollectData);
//
//		Cause cause = Cause.UserCause.class.newInstance();
//		FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
//
//		assertEquals(Result.SUCCESS, build.getResult());
//	}
//
//	@Test
//	public void testDownloadFailure() throws Exception {
//		mockStatic(CLIVersionUtils.class);
//		mock(ProcStarter.class);
//		PowerMockito.doReturn("19.3.1").when(CLIVersionUtils.class, "getCLIVersion", any(String.class), any(String.class));
//		PowerMockito.doReturn(Result.FAILURE).when(ProcStarter.class, "join");
//
//		FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
//		ZAdviserDownloadData beforeZAdviserCollectData = new ZAdviserDownloadData(EXPECTED_CONNECTION_ID, EXPECTED_CREDENTIALS_ID, EXPECTED_JCL,
//				EXPECTED_ENCRYPTED_CSV_FILE_PATH, EXPECTED_UNENCRYPTED_CSV_FILE_PATH);
//		project.getBuildersList().add(beforeZAdviserCollectData);
//
//		Cause cause = Cause.UserCause.class.newInstance();
//		FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
//
//		assertEquals(Result.FAILURE, build.getResult());
//	}
//
//	@Test
//	public void testUploadSuccess() throws Exception {
//		mockStatic(CLIVersionUtils.class);
//		mock(ProcStarter.class);
//		PowerMockito.doReturn("19.4.1").when(CLIVersionUtils.class, "getCLIVersion", any(String.class), any(String.class));
//		PowerMockito.doReturn(Result.SUCCESS).when(ProcStarter.class, "join");
//
//		FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
//		ZAdviserUploadData beforeZAdviserCollectData = new ZAdviserUploadData(EXPECTED_CSV_FILE_PATH);
//		project.getBuildersList().add(beforeZAdviserCollectData);
//
//		Cause cause = Cause.UserCause.class.newInstance();
//		FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
//
//		assertEquals(Result.SUCCESS, build.getResult());
//	}
//
//	@Test
//	public void testUploadFailure() throws Exception {
//		mockStatic(CLIVersionUtils.class);
//		mock(ProcStarter.class);
//		PowerMockito.doReturn("19.3.1").when(CLIVersionUtils.class, "getCLIVersion", any(String.class), any(String.class));
//		PowerMockito.doReturn(Result.FAILURE).when(ProcStarter.class, "join");
//
//		FreeStyleProject project = jenkinsRule.createFreeStyleProject("TestProject");
//		ZAdviserUploadData beforeZAdviserCollectData = new ZAdviserUploadData(EXPECTED_CSV_FILE_PATH);
//		project.getBuildersList().add(beforeZAdviserCollectData);
//
//		Cause cause = Cause.UserCause.class.newInstance();
//		FreeStyleBuild build = project.scheduleBuild2(0, cause).get();
//
//		assertEquals(Result.FAILURE, build.getResult());
//	}
}