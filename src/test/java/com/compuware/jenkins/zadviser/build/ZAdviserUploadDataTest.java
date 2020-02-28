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
 * Test cases for {@link ZAdviserUploadData}.
 */
@SuppressWarnings("nls")
public class ZAdviserUploadDataTest {
	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_CSV_FILE_PATH = "/test/encrypted.csv";
	private static final String EXPECTED_ACCESS_KEY_VALUE = "accessKeyValue";
	/* @formatter:on */

	@ClassRule
	public static final JenkinsRule jenkinsRule = new JenkinsRule();

	@InjectMocks
	private ZAdviserGlobalConfiguration zAdviserGlobalConfig;
	private ZAdviserUploadData.DescriptorImpl descriptor;

	@Mock
	private StaplerRequest request;

	@Before
	public void setUp() throws Exception {
		zAdviserGlobalConfig = ZAdviserGlobalConfiguration.get();
		descriptor = new ZAdviserUploadData.DescriptorImpl();
		request = mock(StaplerRequest.class);
	}

	@Test
	public void testNullCsvFilePath() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(null).kind);
	}

	@Test
	public void testEmptyCsvFilePath() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(StringUtils.EMPTY).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(StringUtils.SPACE).kind);
	}

	@Test
	public void testValidCsvFilePath() {
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(EXPECTED_ACCESS_KEY_VALUE));
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH).kind);
	}

	@Test
	public void testValidCsvFilePathWithoutAccessKey() {
		zAdviserGlobalConfig.setAccessKey(null);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.EMPTY));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH).kind);
		zAdviserGlobalConfig.setAccessKey(Secret.fromString(StringUtils.SPACE));
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH).kind);
	}
}
