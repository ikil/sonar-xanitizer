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
 * Created on October 2, 2015
 */
package com.rigsit.xanitizer.sqplugin;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

import com.rigsit.xanitizer.sqplugin.batch.XanitizerSensor;
import com.rigsit.xanitizer.sqplugin.metrics.XanMetrics;
import com.rigsit.xanitizer.sqplugin.ui.XanWidget;

/**
 * @author rust
 * 
 */
@Properties({ @Property(key = XanSonarQubePlugin.XAN_XML_REPORT_FILE,

		name = "Xanitizer XML Report File",

		// HTML format.
		description = "The Xanitizer XML report file from which to read the findings."
				+ " Generated by Xanitizer, either from the GUI "
				+ "'Reporting > Generate Findings List Report...', and choosing the XML output format,"
				+ " or by running headless, for example using ANT.",

		defaultValue = "",

		// This is a project-level property.
		global = false, project = true

		) })
public class XanSonarQubePlugin extends SonarPlugin {

	public static final String XAN_XML_REPORT_FILE = "xanitizer.xmlReportFile";

	@Override
	public List<Class<?>> getExtensions() {
		return (List) Arrays.asList(

				XanRulesDefinition.class

		, XanitizerSensor.class

		, XanMetrics.class

		, XanQualityProfile.class

		, XanWidget.class

		);
	}
}
