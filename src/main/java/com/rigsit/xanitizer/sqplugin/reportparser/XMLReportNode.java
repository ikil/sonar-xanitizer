/**
 * SonarQube Xanitizer Plugin
 * Copyright 2012-2016 by RIGS IT GmbH, Switzerland, www.rigs-it.ch.
 * mailto: info@rigs-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Created on May 31, 2016
 */
package com.rigsit.xanitizer.sqplugin.reportparser;

/**
 * @author rust
 *
 */
public class XMLReportNode {

	private final String classFQNOrNull;
	private final int lineNoOrMinus1;
	private final String relativePathOrNull;
	private final String absolutePathOrNull;

	/**
	 * The representation of a single node of a taint path
	 * 
	 * @param classFQNOrEmpty
	 * @param lineNoOrMinus1
	 * @param urlOrEmpty
	 */
	public XMLReportNode(final String classFQNOrNull, final int lineNoOrMinus1,
			final String relativePathOrNull, final String absolutePathOrNull) {
		this.classFQNOrNull = classFQNOrNull != null && classFQNOrNull.isEmpty() ? null
				: classFQNOrNull;
		this.lineNoOrMinus1 = lineNoOrMinus1;
		this.relativePathOrNull = relativePathOrNull != null && relativePathOrNull.isEmpty() ? null : relativePathOrNull;
		this.absolutePathOrNull = absolutePathOrNull != null && absolutePathOrNull.isEmpty() ? null : absolutePathOrNull;
	}

	public String getClassFQNOrNull() {
		return classFQNOrNull;
	}

	public int getLineNoOrMinus1() {
		return lineNoOrMinus1;
	}

	public String getRelativePathOrNull() {
		return relativePathOrNull;
	}
	
	public String getAbsolutePathOrNull() {
		return absolutePathOrNull;
	}

}
