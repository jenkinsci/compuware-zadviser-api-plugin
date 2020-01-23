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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;

import com.compuware.jenkins.zadviser.common.configuration.ZAdviserGlobalConfiguration;

import hudson.util.FormValidation;
import net.sf.json.JSONObject;

/**
 * Test cases for {@link ZAdviserDownloadData}.
 */
public class ZAdviserDownloadDataTest {

	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_JCL = "some jcl";
	private static final String EXPECTED_ENCRYPTED_CSV_FILE_PATH = "/test/encrypted.csv";
	private static final String EXPECTED_UNENCRYPTED_CSV_FILE_PATH = "/test/unencrypted.csv";
	/* @formatter:on */

	@ClassRule
	public static final JenkinsRule jenkinsRule = new JenkinsRule();

	@InjectMocks
	ZAdviserGlobalConfiguration zAdviserGlobalConfig;

	@Mock
	private StaplerRequest request;

	@Before
	public void setUp() throws Exception {
		zAdviserGlobalConfig = new ZAdviserGlobalConfiguration();
		request = mock(StaplerRequest.class);
	}

	@Test
	public void testNullJcl() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl(null).kind);
	}

	@Test
	public void testEmptyJcl() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl("").kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckJcl(" ").kind);
	}

	@Test
	public void testValidJcl() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckJcl(EXPECTED_JCL).kind);
	}

	@Test
	public void testNullEncryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedCsvFilePath(null).kind);
	}

	@Test
	public void testEmptyEncryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedCsvFilePath("").kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedCsvFilePath(" ").kind);
	}

	@Test
	public void testValidEncryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckEncryptedCsvFilePath(EXPECTED_ENCRYPTED_CSV_FILE_PATH).kind);
	}

	@Test
	public void testNullUnencryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedCsvFilePath(null).kind);
	}

	@Test
	public void testEmptyUnencryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedCsvFilePath("").kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedCsvFilePath(" ").kind);
	}

	@Test
	public void testValidUnencryptedCsvFilePath() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUnencryptedCsvFilePath(EXPECTED_UNENCRYPTED_CSV_FILE_PATH).kind);
	}

	@Test
	public void testLoadDefaultJclNotNull() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertNotNull("Expected default JCL to not be null.", descriptor.getDefaultJcl());
	}

	@Test
	public void testLoadDefaultJclNotEmpty() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertNotEquals("Expected default JCL to not be empty.", "", descriptor.getDefaultJcl());
	}

	@Test
	public void testValidDefaultJcl() {
		ZAdviserDownloadData.DescriptorImpl descriptor = new ZAdviserDownloadData.DescriptorImpl();
		assertTrue(descriptor.getDefaultJcl().contains("//ZADVISER JOB"));
	}

	@Test
	public void testLastExecutionTimeSingleHost() {
		long lastExecutionTimeCw01 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime("cw01", lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime("cw01"));
		
		lastExecutionTimeCw01 = 1L;
		zAdviserGlobalConfig.updateLastExecutionTime("cw01", lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime("cw01"));
	}

	@Test
	public void testLastExecutionTimeMultipleHosts() {
		long lastExecutionTimeCw01 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime("cw01", lastExecutionTimeCw01);
		zAdviserGlobalConfig.configure(request, new JSONObject());
		
		long lastExecutionTimeCw02 = System.currentTimeMillis();
		zAdviserGlobalConfig.updateLastExecutionTime("cw02", lastExecutionTimeCw02);
		zAdviserGlobalConfig.configure(request, new JSONObject());

		zAdviserGlobalConfig.load();
		assertEquals(Long.toString(lastExecutionTimeCw01), zAdviserGlobalConfig.getLastExecutionTime("cw01"));
		assertEquals(Long.toString(lastExecutionTimeCw02), zAdviserGlobalConfig.getLastExecutionTime("cw02"));
	}
}