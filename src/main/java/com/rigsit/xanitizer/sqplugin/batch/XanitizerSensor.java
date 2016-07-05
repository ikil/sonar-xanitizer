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
 * Created on October 10, 2015
 */
package com.rigsit.xanitizer.sqplugin.batch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.JavaResourceLocator;

import com.rigsit.xanitizer.sqplugin.XanRulesDefinition;
import com.rigsit.xanitizer.sqplugin.XanRulesDefinition.XanRule;
import com.rigsit.xanitizer.sqplugin.XanSonarQubePlugin;
import com.rigsit.xanitizer.sqplugin.metrics.Util;
import com.rigsit.xanitizer.sqplugin.metrics.XanMetrics;

/**
 * @author rust
 * 
 */
public class XanitizerSensor implements Sensor {
	private static Logger LOG = Loggers.get(XanitizerSensor.class);

	private final JavaResourceLocator m_JavaResourceLocator;
	private final File m_ReportFile;

	private final String m_RepositoryKey = XanRulesDefinition.getRepositoryKey();

	private final Set<String> m_ActiveXanRuleNames = new HashSet<>();

	public XanitizerSensor(final JavaResourceLocator javaResourceLocator, final Settings settings,
			final ActiveRules activeRules) {
		m_JavaResourceLocator = javaResourceLocator;

		final String reportFileStringOrNull = settings
				.getString(XanSonarQubePlugin.XAN_XML_REPORT_FILE);
		if (reportFileStringOrNull == null) {
			m_ReportFile = null;
		} else {
			m_ReportFile = new File(reportFileStringOrNull);
		}

		for (final ActiveRule activeRule : activeRules.findAll()) {
			if (activeRule.ruleKey().repository().equals(XanRulesDefinition.getRepositoryKey())) {
				final String ruleAsString = activeRule.ruleKey().rule();
				m_ActiveXanRuleNames.add(ruleAsString);
			}
		}
	}

	@Override
	public boolean shouldExecuteOnProject(final Project project) {
		return true;
	}

	@Override
	public void analyse(final Project project, final SensorContext sensorContext) {
		if (m_ReportFile == null) {
			LOG.error("Xanitizer parameter " + XanSonarQubePlugin.XAN_XML_REPORT_FILE
					+ " not specified; skipping");
			return;
		}

		if (!m_ReportFile.isFile()) {
			LOG.error("Xanitizer XML report file '" + m_ReportFile + "' not found; skipping");
			return;
		}

		LOG.info("Reading Xanitizer findings from '" + m_ReportFile + "' for project '"
				+ project.name() + "'");

		final XMLReportParser parser = new XMLReportParser();
		final XMLReportContent content;
		try {
			content = parser.parse(m_ReportFile, true);
		} catch (final Exception ex) {
			LOG.error("Exception caught while parsing Xanitizer XML report file; skipping", ex);
			return;
		}

		final String toolVersionShortOrNull = content.getToolVersionShortOrNull();
		if (toolVersionShortOrNull == null) {
			LOG.error("No attribute xanitizerVersionShort found in XML report; skipping");
			return;
		}

		{
			final String errMsgOrNull;
			if (null != (errMsgOrNull = checkVersion(toolVersionShortOrNull, 2, 3, -1))) {
				LOG.error("Could not parse xanitizerVersionShort attribute: " + errMsgOrNull
						+ "; skipping");
				return;
			}
		}

		final Map<Metric, Map<Resource, Integer>> metricValues = new LinkedHashMap<>();

		{
			final long analysisEndDate = content.getAnalysisEndDate();
			if (analysisEndDate == 0) {
				LOG.error(
						"No Xanitizer analysis results found - Check if Xanitizer analysis has been executed");
				return;
			}

			final String analysisDatePresentation = convertToDateWithTimeString(
					new Date(analysisEndDate));

			LOG.info("Processing Xanitizer analysis results of " + analysisDatePresentation
					+ "; findings: " + content.getXMLReportFindings().size());

			// Generate issues for findings.
			for (final XMLReportFinding f : content.getXMLReportFindings()) {
				generateIssuesForFinding(f, metricValues, analysisDatePresentation, project,
						sensorContext);
			}
		}

		// Metrics: Counts of different findings.
		for (final Map.Entry<Metric, Map<Resource, Integer>> e : metricValues.entrySet()) {
			final Metric metric = e.getKey();
			for (final Map.Entry<Resource, Integer> e1 : e.getValue().entrySet()) {
				final Resource resource = e1.getKey();
				final Integer value = e1.getValue();

				if (value != 0) {
					final Measure measure = new Measure(metric, value.doubleValue());
					LOG.debug("Creating measure for metric " + measure.getMetricKey()
							+ ": adding value = " + value + " to resource " + resource.getName());
					sensorContext.saveMeasure(resource, measure);
				}
			}
		}
	}

	private final static Pattern TOOL_VERSION_PATTERN = Pattern
			.compile("([0-9]+)[.]([0-9]+)(?:[.]([0-9]+))?");

	private static String checkVersion(final String shortToolVersion, final int majorNeeded,
			final int minorNeeded, final int patchNeeded) {
		final Matcher m = TOOL_VERSION_PATTERN.matcher(shortToolVersion);
		if (!m.matches()) {
			return "XML report file version does not match <number>.<number>[,<number>]: '"
					+ shortToolVersion + "'";
		}
		final int majorNo = Integer.parseInt(m.group(1));
		final int minorNo = Integer.parseInt(m.group(2));

		final int patchNo;
		final String patchNoString = m.group(3);
		if (patchNoString == null) {
			// Does not exist.
			patchNo = -1;
		} else {
			patchNo = Integer.parseInt(patchNoString);
		}

		if (majorNo < majorNeeded) {
			return "XML report file version: major version number must be at least " + majorNeeded
					+ ": '" + shortToolVersion + "'";
		}

		if (majorNo == majorNeeded) {
			if (minorNo < minorNeeded) {
				return "XML report file version: when the major version is " + majorNo
						+ ", the minor version number must be at least " + minorNeeded + ": '"
						+ shortToolVersion + "'";
			}

			if (minorNo == minorNeeded) {
				if (patchNo < patchNeeded) {
					return "XML report file version: when the major and minor version is " + majorNo
							+ "." + minorNo + ", the patch number must be at least " + patchNeeded
							+ ": '" + shortToolVersion + "'";
				}
			}
		}

		// Fine.
		return null;
	}

	private static final DateFormat DATE_WITH_TIME_FORMATTER = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private static String convertToDateWithTimeString(Date date) {
		return DATE_WITH_TIME_FORMATTER.format(date);
	}

	private void generateIssuesForFinding(final XMLReportFinding finding,
			final Map<Metric, Map<Resource, Integer>> metricValuesAccu,
			final String analysisDatePresentation, final Project project,
			final SensorContext sensorContext) {
		InputFile inputFileOrNull;

		switch (finding.getFindingKind()) {
		case PATH: {
			final XMLReportNode startNode = finding.getStartNodeOfPathOrNull();
			final XMLReportNode endNode = finding.getEndNodeOfPathOrNull();

			if (startNode == null || endNode == null) {
				LOG.error("start or end node missing for path finding " + finding.getFindingID()
						+ "; skipping");
				return;
			}

			inputFileOrNull = mkInputFileOrNull(endNode, sensorContext);
			generateIssueOnInputFileOrProject(inputFileOrNull, project,
					normalizeLineNo(endNode.getLineNoOrMinus1()),
					XanRule.mkForFindingOrNull(finding),
					"Taint path for problem type '"
							+ Util.mkPresentationNameForBugTypeId(finding.getProblemTypeId())
							+ "', path starting at " + mkLocationString(startNode, sensorContext)
							+ " and ending at " + mkLocationString(endNode, sensorContext),
					finding, analysisDatePresentation, metricValuesAccu, sensorContext);
		}
			break;

		case SPECIAL: {
			inputFileOrNull = mkInputFileOrNull(finding.getClassFQNOrNull(), sensorContext);
			final XMLReportNode node = finding.getNodeOrNull();

			if (node == null) {
				LOG.error("node missing for special finding " + finding.getFindingID()
						+ "; skipping");
				return;
			}

			generateIssueOnInputFileOrProject(inputFileOrNull, project,
					normalizeLineNo(finding.getLineNoOrMinus1()),
					XanRule.mkForFindingOrNull(finding),
					"Special code location for problem type '"
							+ Util.mkPresentationNameForBugTypeId(finding.getProblemTypeId()) + "'"
							+ mkDescriptionSuffixForLocation(inputFileOrNull,
									mkLocationString(node, sensorContext)),
					finding, analysisDatePresentation, metricValuesAccu, sensorContext);
			break;
		}

		case USER:
			inputFileOrNull = mkInputFileOrNull(finding.getClassFQNOrNull(), sensorContext);
			generateIssueOnInputFileOrProject(inputFileOrNull, project,
					normalizeLineNo(finding.getLineNoOrMinus1()),
					XanRule.mkForFindingOrNull(finding),
					"User-defined finding for problem type '"
							+ Util.mkPresentationNameForBugTypeId(finding.getProblemTypeId()) + "'"
							+ finding.getDescriptionOrNull()
							+ mkDescriptionSuffixForLocation(inputFileOrNull,
									mkLocationForUserFindingString(finding)),
					finding, analysisDatePresentation, metricValuesAccu, sensorContext);
			break;

		case GENERIC:
			/*
			 * If this is an OWASP Dependency Check finding, create an issue on
			 * the root project.
			 */
			if (finding.getFindingProducer().toLowerCase().contains("owaspdependencycheck")) {

				// No line number.
				final int lineNumber = -1;
				generateIssueOnInputFileOrProject(null, project, lineNumber,
						XanRule.mkForFindingOrNull(finding),
						mkOWASPDepCheckFindingDescription(finding), finding,
						analysisDatePresentation, metricValuesAccu, sensorContext);
			} else {
				inputFileOrNull = mkInputFileOrNull(finding.getClassFQNOrNull(), sensorContext);

				generateIssueOnInputFileOrProject(inputFileOrNull, project,
						normalizeLineNo(finding.getLineNoOrMinus1()),
						XanRule.mkForFindingOrNull(finding), mkGenericFindingDescription(finding),
						finding, analysisDatePresentation, metricValuesAccu, sensorContext);
			}
			break;

		default:
			// Nothing to do.
			LOG.error("Unexpected Xanitizer finding kind:" + finding.getFindingKind());
			break;
		}
	}

	private String mkLocationForUserFindingString(final XMLReportFinding finding) {
		final String persistenceStringOrNull = finding.getPersistenceStringOrNull();
		if (persistenceStringOrNull != null) {
			return persistenceStringOrNull;
		}
		return "<no file found>";
	}

	private String mkDescriptionSuffixForLocation(final InputComponent inputComponentOrNull,
			final String locationStringOrNull) {
		if (inputComponentOrNull != null) {
			/*
			 * No need to generate an extra description for the location - the
			 * issue will be registered with the given resource.
			 */
			return "";
		}
		if (locationStringOrNull == null) {
			// No location description.
			return "";
		}
		return " - " + locationStringOrNull;
	}

	/*
	 * In SonarQube, line numbers must be strioctly positive.
	 */
	private static int normalizeLineNo(final int lineNo) {
		if (lineNo <= 0) {
			return 1;
		}
		return lineNo;
	}

	private static String mkOWASPDepCheckFindingDescription(final XMLReportFinding finding) {
		final String origAbsFileOrNull = finding.getOriginalAbsFileOrNull();

		final String descOrNull = finding.getDescriptionOrNull();
		final String extraDescOrNull = finding.getExtraDescriptionOrNull();

		return "OWASP Dependency Check"
				+ (origAbsFileOrNull == null ? "" : "with " + origAbsFileOrNull) + ":\n"

				+ (descOrNull == null && extraDescOrNull == null ? "- no description available -"
						: "")

				+ (descOrNull == null ? "" : descOrNull)

				+ (descOrNull != null && extraDescOrNull != null ? "\n" : "")

				+ (extraDescOrNull == null ? "" : extraDescOrNull);
	}

	private static String mkGenericFindingDescription(final XMLReportFinding finding) {
		final String origAbsFileOrNull = finding.getOriginalAbsFileOrNull();

		String descOrNull = finding.getDescriptionOrNull();
		if (descOrNull != null && descOrNull.isEmpty()) {
			descOrNull = null;
		}
		final String extraDescOrNull = finding.getExtraDescriptionOrNull();

		final String prefix;
		if (finding.getFindingProducer().toLowerCase().contains("findbugs")) {
			prefix = "Findbugs";
		} else {
			prefix = "Generic";
		}

		return prefix + " finding" + (origAbsFileOrNull == null ? "" : " with " + origAbsFileOrNull)
				+ ":\n"

				+ (descOrNull == null && extraDescOrNull == null ? "- no description available -"
						: "")

				+ (descOrNull == null ? "" : descOrNull)

				+ (descOrNull != null && extraDescOrNull != null ? "\n" : "")

				+ (extraDescOrNull == null ? "" : extraDescOrNull);

	}

	private String mkLocationString(final XMLReportNode node, final SensorContext sensorContext) {
		if (node == null) {
			return "<no resource found>";
		}
		final InputFile inputFileOrNull = mkInputFileOrNull(node, sensorContext);
		if (inputFileOrNull == null) {
			return mkResourcePresentation(node) + ":" + node.getLineNoOrMinus1();
		}
		return inputFileOrNull.absolutePath() + ":" + node.getLineNoOrMinus1();
	}

	private String mkResourcePresentation(final XMLReportNode node) {
		return node.getClassFQNOrNull() != null ? node.getClassFQNOrNull()
				: node.getXFilePersistenceOrNull() != null ? node.getXFilePersistenceOrNull()
						.replace("XanitizerPath:${PROJECT_DIR}", "/") : "<no resource found>";
	}

	private InputFile mkInputFileOrNull(final XMLReportNode nodeOrNull,
			final SensorContext sensorContext) {
		if (nodeOrNull == null) {
			// LOG.warn("XANITIZER: nodeOrNull == null");
			return null;
		}
		final String classFQNOrNull = nodeOrNull.getClassFQNOrNull();
		if (classFQNOrNull == null) {
			// LOG.warn("XANITIZER: no class found in node '" +
			// nodeOrNull.getId() + "'");
			return null;
		}
		final Resource resource = m_JavaResourceLocator.findResourceByClassName(classFQNOrNull);
		if (resource == null) {
			// LOG.warn("XANITIZER: no resource found for " + classFQNOrNull + "
			// (from <Node>)");
			return null;
		}

		return mkInputFileOrNullFromResource(resource, sensorContext);
	}

	private InputFile mkInputFileOrNullFromResource(final Resource resource,
			final SensorContext sensorContext) {
		final String relativePath = resource.getPath();

		final FileSystem fs = sensorContext.fileSystem();

		final File absoluteFile = new File(fs.baseDir(), relativePath);
		final String absoluteFilePath = absoluteFile.getAbsolutePath();

		/*
		 * SonarQube 5.4 seems to sometimes enter an endless loop here, if a
		 * non-trivial predicate is given.
		 */
		final Iterable<InputFile> inputFilesIterable = sensorContext.fileSystem()
				.inputFiles(fs.predicates().hasAbsolutePath(absoluteFilePath));

		// Use first matching input file, or none.
		for (final InputFile inputFile : inputFilesIterable) {
			// LOG.info("Input file found for resource: " + absoluteFilePath);
			return inputFile;
		}

		// LOG.warn("No input file found for resource: " + absoluteFilePath);

		return null;
	}

	private InputFile mkInputFileOrNull(final String classFQNOrNull,
			final SensorContext sensorContext) {
		if (classFQNOrNull == null) {
			return null;
		}
		final Resource resourceOrNull = m_JavaResourceLocator
				.findResourceByClassName(classFQNOrNull);
		if (resourceOrNull == null) {
			// LOG.warn("XANITIZER: no resource found for " + xFileOrNull + "
			// (from <XFile>)");
			return null;
		}

		return mkInputFileOrNullFromResource(resourceOrNull, sensorContext);
	}

	private boolean generateIssueOnInputFileOrProject(final InputFile inputFileOrNull,
			final Project project, final int lineNo, final XanRule rule,
			final String descriptionOrNull, final XMLReportFinding xanFinding,
			final String analysisDatePresentation,
			final Map<Metric, Map<Resource, Integer>> metricValuesAccu,
			final SensorContext sensorContext) {

		/*
		 * At first try to find the resource in another module. Only if the root
		 * project is analyzed, all remaining findings have to be added.
		 */
		if (inputFileOrNull == null && !project.isRoot()) {
			return false;
		}

		if (!m_ActiveXanRuleNames.contains(rule.toString())) {
			return false;
		}

		final Resource resourceToBeUsed = inputFileOrNull == null ? project
				: sensorContext.getResource(inputFileOrNull);

		final Severity severity = mkSeverity(xanFinding);
		final RuleKey ruleKey = RuleKey.of(m_RepositoryKey, rule.name());

		final NewIssue newIssue = sensorContext.newIssue();

		{
			newIssue.forRule(ruleKey).overrideSeverity(severity);

			final NewIssueLocation newIssueLocation = newIssue.newLocation();

			if (inputFileOrNull != null) {
				newIssueLocation.on(inputFileOrNull);

				// If line number exceeds the current length of the file,
				// SonarQube will crash. So check length for robustness.
				if (lineNo > 0 && lineNo <= inputFileOrNull.lines()) {
					final TextRange textRange = inputFileOrNull.selectLine(lineNo);
					newIssueLocation.at(textRange);
				}
			} else {
				final InputModule inputModule = new DefaultInputModule(project.getKey());
				newIssueLocation.on(inputModule);
			}

			final String msg = (descriptionOrNull != null ? descriptionOrNull
					: rule.getShortHTMLDescription())
					+ mkIdSuffixForFinding(xanFinding, analysisDatePresentation);

			newIssueLocation.message(msg);

			newIssue.at(newIssueLocation);
		}

		final String problemTypeId = xanFinding.getProblemTypeId();

		newIssue.save();

		LOG.debug("Issue saved: " + resourceToBeUsed + ":" + lineNo + " - " + ruleKey + " - "
				+ descriptionOrNull);

		{
			final List<Metric> metrics = mkMetricsForRule(rule, problemTypeId);
			for (final Metric metric : metrics) {
				incrementValueForResourceAndContainingResources(metric, resourceToBeUsed, project,
						metricValuesAccu);
			}

			final String matchCode = xanFinding.getMatchCode();
			if (matchCode.equals("NOT")) {
				incrementValueForResourceAndContainingResources(
						XanMetrics.getMetricForNewXanFindings(), resourceToBeUsed, project,
						metricValuesAccu);
			}

			final Metric metricForSeverity = XanMetrics.getMetricForSeverity(severity);
			if (metricForSeverity != null) {
				incrementValueForResourceAndContainingResources(metricForSeverity, resourceToBeUsed,
						project, metricValuesAccu);
			}
		}

		return true;
	}

	private String mkIdSuffixForFinding(final XMLReportFinding xanFinding,
			final String analysisDatePresentation) {
		final String findingId = Integer.toString(xanFinding.getFindingID());
		return " (Xanitizer finding id " + findingId + ", analysis of " + analysisDatePresentation
				+ ")";
	}

	private List<Metric> mkMetricsForRule(final XanRule rule, final String bugTypeId) {
		final List<Metric> result = new ArrayList<>();
		result.add(XanMetrics.getMetricForAllXanFindings());

		final Metric metricOrNull = XanMetrics.mkMetricForBugTypeIdOrNull(bugTypeId);
		if (metricOrNull != null) {
			result.add(metricOrNull);
		}

		return result;
	}

	private static void incrementValueForResourceAndContainingResources(final Metric metric,
			final Resource resource, final Project project,
			final Map<Metric, Map<Resource, Integer>> metricValuesAccu) {
		Resource runner = resource;
		while (runner != null) {
			incrementValue(metric, runner, metricValuesAccu);
			if (ResourceUtils.isFile(runner)) {
				// Go to directory.
				runner = runner.getParent();
			} else if (ResourceUtils.isDirectory(runner)) {
				// Go to project.
				runner = project;
			} else {
				// This is a project. No container.
				runner = null;
			}
		}
	}

	private static void incrementValue(final Metric metric, final Resource resource,
			final Map<Metric, Map<Resource, Integer>> metricValuesAccu) {
		Map<Resource, Integer> innerMap = metricValuesAccu.get(metric);
		if (innerMap == null) {
			innerMap = new LinkedHashMap<>();
			metricValuesAccu.put(metric, innerMap);
		}

		Integer value = innerMap.get(resource);
		if (value == null) {
			value = 0;
		}

		innerMap.put(resource, 1 + value);
	}

	private Severity mkSeverity(final XMLReportFinding xanFinding) {
		final String findingClassification = xanFinding.getFindingClassificationOrNull();
		if (findingClassification == null) {
			// Should not occur.
			return Severity.MINOR;
		}
		switch (findingClassification.toUpperCase()) {
		case "MUST_FIX":
		case "URGENT_FIX":
			return Severity.BLOCKER;

		default:
			// For the rest, we use the rating.
			final double rating = xanFinding.getRating();
			if (rating > 7) {
				return Severity.CRITICAL;
			}
			if (rating > 4) {
				return Severity.MAJOR;
			}
			if (rating > 1) {
				return Severity.MINOR;
			}
			return Severity.INFO;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
