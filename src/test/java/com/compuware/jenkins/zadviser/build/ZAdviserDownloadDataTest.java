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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.compuware.jenkins.zadviser.common.configuration.ZAdviserGlobalConfiguration;

import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;

/**
 * Test cases for {@link ZAdviserDownloadData}.
 */
@SuppressWarnings("nls")
public class ZAdviserDownloadDataTest {
	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_JCL = "some jcl";
	private static final String EXPECTED_ENCRYPTED_DATA_FILE = "/test/encrypted.csv";
	private static final String EXPECTED_UNENCRYPTED_DATA_FILE = "/test/unencrypted.csv";

	private static final String EXPECTED_ACCESS_KEY_VALUE = "accessKeyValue";
	private static final String EXPECTED_ENCRYPTION_KEY_VALUE = "encryptionKeyValue";

	private static final String CW01 = "cw01";
	private static final String CW02 = "cw02";
	/* @formatter:on */

	@ClassRule
	public static final JenkinsRule jenkinsRule = new JenkinsRule();

	@InjectMocks
	private ZAdviserGlobalConfiguration zAdviserGlobalConfig;
	private ZAdviserDownloadData.DescriptorImpl descriptor;

	@Mock
	private StaplerRequest request;

	@Before
	public void setUp() throws Exception {
		zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();
		descriptor = new ZAdviserDownloadData.DescriptorImpl();
		request = mock(StaplerRequest.class);
	}

	@Test
	public void testNullJcl() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl(null).kind);
	}

	@Test
	public void testEmptyJcl() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidJcl() {
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckJcl(EXPECTED_JCL).kind);
	}

	@Test
	public void testNullEncryptedCsvFilePath() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(null).kind);
	}

	@Test
	public void testEmptyEncryptedCsvFilePath() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidEncryptedCsvFilePath() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(EXPECTED_ENCRYPTION_KEY_VALUE));

		assertEquals(FormValidation.Kind.OK, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testValidEncryptedCsvFilePathWithoutAccessKey() {
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(EXPECTED_ENCRYPTION_KEY_VALUE));

		zAdviserGlobalConfig.setAccessKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testValidEncryptedCsvFilePathWithoutEncryptionKey() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));

		zAdviserGlobalConfig.setEncryptionKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testNullUnencryptedCsvFilePath() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(null).kind);
	}

	@Test
	public void testEmptyUnencryptedCsvFilePath() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidUnencryptedCsvFilePath() {
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUnencryptedDataFile(EXPECTED_UNENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testLoadDefaultJclNotNull() {
		assertNotNull("Expected default JCL to not be null.", descriptor.getDefaultJcl());
	}

	@Test
	public void testLoadDefaultJclNotEmpty() {
		assertNotEquals("Expected default JCL to not be empty.", "", descriptor.getDefaultJcl());
	}

	@Test
	public void testValidDefaultJcl() {
		assertTrue(descriptor.getDefaultJcl().contains("//ZADVISER JOB"));
	}

	@Test
	public void testLastExecutionTimeSingleHost() {
		long lastExecutionTimeCw01 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime(CW01, lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime("cw01"));
		
		lastExecutionTimeCw01 = 1L;
		zAdviserGlobalConfig.updateLastExecutionTime(CW01, lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime("cw01"));
	}

	@Test
	public void testLastExecutionTimeMultipleHosts() {
		long lastExecutionTimeCw01 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime(CW01, lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		
		long lastExecutionTimeCw02 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime(CW02, lastExecutionTimeCw02);
		zAdviserGlobalConfig.configure(request, new JSONObject());

		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime(CW01));
		assertEquals(Long.toString(lastExecutionTimeCw02), zAdviserGlobalConfig.getLastExecutionTime(CW02));
	}

	@Test
	public void testUploadData() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUploadData(Boolean.TRUE).kind);
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUploadData(Boolean.FALSE).kind);

		zAdviserGlobalConfig.setAccessKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUploadData(Boolean.TRUE).kind);
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUploadData(Boolean.FALSE).kind);

		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUploadData(Boolean.TRUE).kind);
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUploadData(Boolean.FALSE).kind);

		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUploadData(Boolean.TRUE).kind);
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUploadData(Boolean.FALSE).kind);
	}
}
