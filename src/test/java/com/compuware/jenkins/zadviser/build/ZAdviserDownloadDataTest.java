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
	private static final String EXPECTED_CUSTOMER_ID_VALUE = "customerIdValue";
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
	public void testNullEncryptedDataFile() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(null).kind);
	}

	@Test
	public void testEmptyEncryptedDataFile() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidEncryptedDataFile() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(EXPECTED_ENCRYPTION_KEY_VALUE));
		zAdviserGlobalConfig.setCustomerId(EXPECTED_CUSTOMER_ID_VALUE);

		assertEquals(FormValidation.Kind.OK, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testValidEncryptedDataFileWithoutAccessKey() {
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(EXPECTED_ENCRYPTION_KEY_VALUE));
		zAdviserGlobalConfig.setCustomerId(EXPECTED_CUSTOMER_ID_VALUE);

		zAdviserGlobalConfig.setAccessKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testValidEncryptedDataFileWithoutEncryptionKey() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		zAdviserGlobalConfig.setCustomerId(EXPECTED_CUSTOMER_ID_VALUE);

		zAdviserGlobalConfig.setEncryptionKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
		zAdviserGlobalConfig.setEncryptionKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckEncryptedDataFile(EXPECTED_ENCRYPTED_DATA_FILE).kind);
	}

	@Test
	public void testNullUnencryptedDataFile() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(null).kind);
	}

	@Test
	public void testEmptyUnencryptedDataFile() {
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUnencryptedDataFile(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidUnencryptedDataFile() {
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
}
