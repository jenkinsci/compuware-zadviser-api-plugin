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
package com.compuware.jenkins.zadviser.build.utils;

/**
 * Constants for use by the zAdviser API plugin.
 */
@SuppressWarnings("nls")
public class ZAdviserUtilitiesConstants {
	/**
	 * Private constructor.
	 * <p>
	 * All constants should be accessed statically.
	 */
	private ZAdviserUtilitiesConstants() {
		// Do not instantiate
	}

	// Constants
	public static final String ZADVISER_MINIMUM_CLI_VERSION = "20.2.1";

	public static final String ZADVISER_CLI_BAT = "ZAdviserCLI.bat";
	public static final String ZADVISER_CLI_SH = "ZAdviserCLI.sh";

	// Common / global configuration
	public static final String ACCESS_KEY_PARM = "-accessKey";
	public static final String CUSTOMER_ID_PARM = "-customerId";
	public static final String ENCRYPTION_KEY_PARM = "-encryptionKey";
	public static final String INITIAL_DATE_RANGE_PARM = "-initialDateRange";

	// Data collection/upload steps
	public static final String JCL_FILE_PATH_PARM = "-jclFilePath";
	public static final String UNENCRYPTED_DATA_FILE_PARM = "-unencryptedDataFile";
	public static final String ENCRYPTED_DATA_FILE_PARM = "-encryptedDataFile";
	public static final String LAST_DATE_RUN_PARM = "-lastDateRun";
	public static final String ZADVISER_LAST_RUN_FILE = "/zAdviserLastRun.properties";
	public static final String UPLOAD_DATA_FILE_PARM = "-uploadDataFile";

	// Build steps
	public static final String BUILD_STEP_PARAM = "-buildStep";
	public static final String DOWNLOAD_STEP = "D";
	public static final String UPLOAD_STEP = "U";

	public static final String PERSIST_DATA_PARM = "-persistData";
}
