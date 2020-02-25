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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.compuware.jenkins.zadviser.common.configuration.ZAdviserGlobalConfiguration;

import hudson.util.FormValidation;

/**
 * Test cases for {@link ZAdviserUploadData}.
 */
public class ZAdviserUploadDataTest {

	// Builder expected values
	/* @formatter:off */
	private static final String EXPECTED_ACCESS_KEY = "accesskey";
	private static final String EXPECTED_CSV_FILE_PATH = "/test/encrypted.csv";
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
	public void testNullCsvFilePath() {
		ZAdviserUploadData.DescriptorImpl descriptor = new ZAdviserUploadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(null, null).kind);
	}

	@Test
	public void testEmptyCsvFilePath() {
		ZAdviserUploadData.DescriptorImpl descriptor = new ZAdviserUploadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath("", null).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(" ", null).kind);
	}

	@Test
	public void testValidCsvFilePath() {
		ZAdviserUploadData.DescriptorImpl descriptor = new ZAdviserUploadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH, EXPECTED_ACCESS_KEY).kind);
	}

	@Test
	public void testMissingAccessKey() {
		ZAdviserUploadData.DescriptorImpl descriptor = new ZAdviserUploadData.DescriptorImpl();
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH, null).kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCsvFilePath(EXPECTED_CSV_FILE_PATH, "").kind);
	}
}