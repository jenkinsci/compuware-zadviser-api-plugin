/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 - 2019 Compuware Corporation
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.util.FormValidation;
import hudson.util.Secret;
import net.sf.json.JSONObject;

/**
 * Class for testing the zAdviser global configuration.
 */
@RunWith(MockitoJUnitRunner.class)
public class ZAdviserGlobalConfigurationTest {

	private static final String EXPECTED_ACCESS_KEY = "123foobar321";
	private static final String EXPECTED_ENCRYPTION_KEY = "blahblah";
	private static final String EXPECTED_INITIAL_DATE_RANGE = "1";

	@ClassRule
	public static final JenkinsRule jenkinsRule = new JenkinsRule();
    private final JSONObject formData = new JSONObject();

    @InjectMocks
    ZAdviserGlobalConfiguration globalConfig;

    @Mock
    private StaplerRequest request;

	@Before
	public void setUp() throws Exception {
		globalConfig = new ZAdviserGlobalConfiguration();
	}

    @Test
    public void testNullInitialDateRange() {
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange(null).kind);
    }

    @Test
    public void testEmptyInitialDateRange() {
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange("").kind);
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange(" ").kind);
    }

    @Test
    public void testNonNumericInitialDateRange() {
        assertEquals(FormValidation.Kind.ERROR, globalConfig.doCheckInitialDateRange("A").kind);
    }

    @Test
    public void testNegativeInitialDateRange() {
        assertEquals(FormValidation.Kind.ERROR, globalConfig.doCheckInitialDateRange("-1").kind);
    }

    @Test
    public void testZeroInitialDateRange() {
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange("0").kind);
    }

    @Test
    public void testSignedInitialDateRange() {
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange("+1").kind);
    }

    @Test
    public void testValidInitialDateRange() {
        assertEquals(FormValidation.Kind.OK, globalConfig.doCheckInitialDateRange(EXPECTED_INITIAL_DATE_RANGE).kind);
    }

    @Test
    public void testConfigureValid() {
    	ZAdviserGlobalConfiguration testConfig = new ZAdviserGlobalConfiguration();
    	formData.put("accessKey", Secret.fromString(EXPECTED_ACCESS_KEY));
    	formData.put("encryptionKey", Secret.fromString(EXPECTED_ENCRYPTION_KEY));
    	formData.put("initialDateRange", EXPECTED_INITIAL_DATE_RANGE);
        assertTrue(testConfig.configure(request, formData));
        verify(request).bindJSON(testConfig, formData);
    }
}