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
package com.rigsit.xanitizer.sqplugin.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonar.api.batch.rule.Severity;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

/**
 * @author rust
 * 
 */
public final class XanitizerMetrics implements Metrics {

	private static final String DOMAIN = "Xanitizer Findings";

	private static final String PFIX = "XanFindingMetric_";

	private static final Metric ALL_XAN_FINDINGS_METRIC;
	private static final Metric NEW_FINDINGS_METRIC;
	private static final Metric BLOCKER_FINDINGS_METRIC;
	private static final Metric CRITICAL_FINDINGS_METRIC;
	private static final Metric MAJOR_FINDINGS_METRIC;
	private static final Metric MINOR_FINDINGS_METRIC;
	private static final Metric INFO_FINDINGS_METRIC;

	static {
		ALL_XAN_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "All", "All Xanitizer Findings", Metric.ValueType.INT)

				.setDescription("All Xanitizer Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		NEW_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "New", "New Xanitizer Findings", Metric.ValueType.INT)

				.setDescription("New Xanitizer Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		BLOCKER_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "Blocker", "Xanitizer Blocker Findings", Metric.ValueType.INT)

				.setDescription("Xanitizer Blocker Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		CRITICAL_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "Critical", "Xanitizer Critical Findings", Metric.ValueType.INT)

				.setDescription("Xanitizer Critical Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		MAJOR_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "Major", "Xanitizer Major Findings", Metric.ValueType.INT)

				.setDescription("Xanitizer Major Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		MINOR_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "Minor", "Xanitizer Minor Findings", Metric.ValueType.INT)

				.setDescription("Xanitizer Minor Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();

		INFO_FINDINGS_METRIC =

		new Metric.Builder(PFIX + "Info", "Xanitizer Info Findings", Metric.ValueType.INT)

				.setDescription("Xanitizer Info Findings")

				.setQualitative(true)

				.setBestValue(0.0)

				.setDirection(Metric.DIRECTION_WORST)

				.setDomain(DOMAIN)

				.create();
	}

	private final List<Metric> metrics;

	public XanitizerMetrics() {
		final List<Metric> resultAccu = new ArrayList<>();
		resultAccu.add(ALL_XAN_FINDINGS_METRIC);
		resultAccu.add(NEW_FINDINGS_METRIC);
		resultAccu.add(BLOCKER_FINDINGS_METRIC);
		resultAccu.add(CRITICAL_FINDINGS_METRIC);
		resultAccu.add(MAJOR_FINDINGS_METRIC);
		resultAccu.add(MINOR_FINDINGS_METRIC);
		resultAccu.add(INFO_FINDINGS_METRIC);

		final Map<String, Integer> predefinedBugTypeIds = mkPredefinedBugTypeIdMap();

		for (final String bugTypeId : predefinedBugTypeIds.keySet()) {
			final Metric metricOrNull = mkMetricForBugTypeIdOrNull(bugTypeId);
			if (metricOrNull != null) {
				resultAccu.add(metricOrNull);
			}
		}

		// Result is ready.
		metrics = resultAccu;
	}

	@Override
	public List<Metric> getMetrics() {
		return metrics;
	}

	public static Metric mkMetricForBugTypeIdOrNull(final String bugTypeId) {
		final Map<String, Integer> predefinedBugTypeIds = mkPredefinedBugTypeIdMap();
		final Integer num = predefinedBugTypeIds.get(bugTypeId);
		if (num != null) {

			/*
			 * We use mainly numeric ids in order to avoid long ids - SonarQube
			 * just dies when an overly long id is used...
			 */
			final String metricId = PFIX + num;

			return new Metric.Builder(metricId, mkMetricName(bugTypeId), Metric.ValueType.INT)

					.setDescription("Xanitizer Findings for '" + bugTypeId + "'")

					.setQualitative(false)

					.setBestValue(0.0)

					.setDirection(Metric.DIRECTION_WORST)

					.setDomain(DOMAIN)

					.create();
		}

		return null;
	}

	private static String mkMetricName(final String bugTypeId) {
		/*
		 * There is a limit for metric names in SonarQube.
		 * 
		 * I (HRU) determined the limit 64 by trial and error, on 2015-10-12,
		 * with a test installation of SonarQube 5.1.2.
		 * 
		 * We use 60 to be on the safe side...
		 */
		final int limit = 60;
		String candidate = "Xanitizer Findings for "
				+ Util.mkPresentationNameForBugTypeId(bugTypeId);
		if (candidate.length() > limit) {
			candidate = candidate.substring(0, limit - 3) + "...";
		}
		return candidate;
	}

	private static Map<String, Integer> mkPredefinedBugTypeIdMap() {
		return GeneratedBugTypeIds.GENERATED_BUG_TYPE_IDS;
	}

	public static Metric getMetricForAllXanFindings() {
		return ALL_XAN_FINDINGS_METRIC;
	}

	public static Metric getMetricForNewXanFindings() {
		return NEW_FINDINGS_METRIC;
	}

	public static Metric getMetricForSeverity(final Severity severity) {

		switch (severity) {
		case BLOCKER:
			return BLOCKER_FINDINGS_METRIC;
		case CRITICAL:
			return CRITICAL_FINDINGS_METRIC;
		case MAJOR:
			return MAJOR_FINDINGS_METRIC;
		case MINOR:
			return MINOR_FINDINGS_METRIC;
		case INFO:
			return INFO_FINDINGS_METRIC;
		default:
			return null;
		}
	}
}