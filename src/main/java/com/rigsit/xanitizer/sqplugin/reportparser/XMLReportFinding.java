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
 * Created on May 30, 2016
 */
package com.rigsit.xanitizer.sqplugin.reportparser;

/**
 * @author rust
 *
 */
public class XMLReportFinding {

	private final int findingID;
	private final String problemTypeId;
	private final FindingKind findingKind;
	private final String findingProducer;
	private final int lineNoOrMinus1;

	private final String descriptionOrNull;
	private final String extraDescriptionOrNull;

	private final String classFQNOrNull;
	private final String originalAbsFileOrNull;
	private final String classificationOrNull;
	private final double rating;
	private final String matchCode;
	private final String persistenceStringOrNull;

	private final XMLReportNode nodeOrNull;
	private final XMLReportNode startNodeOfPathOrNull;
	private final XMLReportNode endNodeOfPathOrNull;

	public XMLReportFinding(final int findingID, final String problemTypeId,
			final FindingKind findingKind, final String findingProducer, final int lineNoOrMinus1,

			final String descriptionOrNull, final String extraDescriptionOrNull,

			final String classFQNOrNull, final String originalAbsFileOrNull,
			final String classificationOrNull, final double rating, final String matchCode,
			final String persistenceStringOrNull, final XMLReportNode nodeOrNull,
			final XMLReportNode startNodeOfPathOrNull, final XMLReportNode endNodeOfPathOrNull) {
		this.findingID = findingID;
		this.problemTypeId = problemTypeId;
		this.findingKind = findingKind;
		this.findingProducer = findingProducer;
		this.lineNoOrMinus1 = lineNoOrMinus1;

		this.descriptionOrNull = descriptionOrNull;
		this.extraDescriptionOrNull = extraDescriptionOrNull;

		this.classFQNOrNull = classFQNOrNull;
		this.originalAbsFileOrNull = originalAbsFileOrNull;
		this.classificationOrNull = classificationOrNull;
		this.rating = rating;
		this.matchCode = matchCode;
		this.persistenceStringOrNull = persistenceStringOrNull;

		this.nodeOrNull = nodeOrNull;
		this.startNodeOfPathOrNull = startNodeOfPathOrNull;
		this.endNodeOfPathOrNull = endNodeOfPathOrNull;
	}

	public int getFindingID() {
		return findingID;
	}

	public String getProblemTypeId() {
		return problemTypeId;
	}

	public FindingKind getFindingKind() {
		return findingKind;
	}

	public String getFindingProducer() {
		return findingProducer;
	}

	public int getLineNoOrMinus1() {
		return lineNoOrMinus1;
	}

	public String getDescription() {
		if (descriptionOrNull == null) {
			return "";
		}
		return descriptionOrNull;
	}

	public String getExtraDescription() {
		if (extraDescriptionOrNull == null) {
			return "";
		}
		return extraDescriptionOrNull;
	}

	public String getClassFQNOrNull() {
		return classFQNOrNull;
	}

	public String getOriginalAbsFileOrNull() {
		return originalAbsFileOrNull;
	}

	public String getFindingClassificationOrNull() {
		return classificationOrNull;
	}

	public double getRating() {
		return rating;
	}

	public String getMatchCode() {
		return matchCode;
	}

	public String getPersistenceStringOrNull() {
		return persistenceStringOrNull;
	}

	public XMLReportNode getNodeOrNull() {
		return nodeOrNull;
	}

	public XMLReportNode getStartNodeOfPathOrNull() {
		return startNodeOfPathOrNull;
	}

	public XMLReportNode getEndNodeOfPathOrNull() {
		return endNodeOfPathOrNull;
	}

}