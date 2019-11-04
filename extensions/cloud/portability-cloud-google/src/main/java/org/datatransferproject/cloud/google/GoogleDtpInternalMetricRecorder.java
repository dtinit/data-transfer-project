/*
 * Copyright 2019 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.cloud.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagMetadata;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;

import java.io.IOException;
import java.time.Duration;

/**
 * A {@link DtpInternalMetricRecorder} that writes metrics to Stackdriver.
 * **/
class GoogleDtpInternalMetricRecorder implements DtpInternalMetricRecorder {
  private static GoogleDtpInternalMetricRecorder INSTANCE = null;

  private static final int EXPORT_INTERVAL_SECONDS = 60;
  private static final String NAME_BASE = "dtp/";
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();
  private static final Tagger tagger = Tags.getTagger();

  private static final TagKey KEY_DATA_TYPE = TagKey.create("data_type");
  private static final TagKey KEY_IMPORT_SERVICE = TagKey.create("import_service");
  private static final TagKey KEY_EXPORT_SERVICE = TagKey.create("export_service");
  private static final TagKey KEY_SUCCESS = TagKey.create("success");

  private static final TagKey KEY_GENERIC_SERVICE = TagKey.create("generic_service");
  private static final TagKey KEY_GENERIC_TAG = TagKey.create("generic_tag");
  private static final TagKey KEY_GENERIC_BOOL = TagKey.create("generic_bool");

  private static final TagMetadata TAG_METADATA =
      TagMetadata.create(TagMetadata.TagTtl.UNLIMITED_PROPAGATION);

  private static final Measure.MeasureLong JOB_STARTED = Measure.MeasureLong.create(
      "job_start",
      "Number of jobs that were started",
      "count");

  private static final Measure.MeasureLong JOB_FINISHED = Measure.MeasureLong.create(
      "job_finished",
      "Number of jobs that finished",
      "count");

  private static final Measure.MeasureLong JOB_FINISHED_DURATION = Measure.MeasureLong.create(
      "job_finished_duration",
      "Duration of a job in MS",
      "ms");

  private static final Measure.MeasureLong EXPORT_PAGE_ATTEMPT = Measure.MeasureLong.create(
      "export_page_attempt",
      "A single export attempt",
      "count");

  private static final Measure.MeasureLong EXPORT_PAGE_ATTEMPT_DURATION =
      Measure.MeasureLong.create(
          "export_page_attempt_duration",
          "Duration of a single export attempt in MS",
          "ms");

  private static final Measure.MeasureLong EXPORT_PAGE = Measure.MeasureLong.create(
      "export_page",
      "An export attempt including all retries",
      "count");

  private static final Measure.MeasureLong EXPORT_PAGE_DURATION =
      Measure.MeasureLong.create(
          "export_page_duration",
          "Duration of an export page including retries in MS",
          "ms");

  private static final Measure.MeasureLong IMPORT_PAGE_ATTEMPT = Measure.MeasureLong.create(
      "import_page_attempt",
      "A single import attempt",
      "count");

  private static final Measure.MeasureLong IMPORT_PAGE_ATTEMPT_DURATION =
      Measure.MeasureLong.create(
          "import_page_attempt_duration",
          "Duration of a single import attempt in MS",
          "ms");

  private static final Measure.MeasureLong IMPORT_PAGE = Measure.MeasureLong.create(
      "import_page",
      "An import attempt including all retries",
      "count");

  private static final Measure.MeasureLong IMPORT_PAGE_DURATION =
      Measure.MeasureLong.create(
          "import_page_duration",
          "Duration of an import page including retries in MS",
          "ms");

  private static final Measure.MeasureLong GENERIC_COUNT = Measure.MeasureLong.create(
      "generic_count",
      "A generic counter that services can use to hold arbitrary metrics",
      "count");

  private static final Measure.MeasureLong GENERIC_DURATION = Measure.MeasureLong.create(
      "generic_duration",
      "A generic counter that services can use to hold arbitrary duration metrics",
      "ms");

  private static final Measure.MeasureLong GENERIC_BOOLEAN = Measure.MeasureLong.create(
      "generic_boolean",
      "A generic counter that services can use to hold arbitrary boolean metrics",
      "count");

  private final ViewManager viewManager;

  // This is needed because Stackdriver can only be initialized once and the
  // GoogleDtpInternalMetricExtension is call more than once by the framework code.
  static synchronized GoogleDtpInternalMetricRecorder getInstance() throws IOException {
    if (INSTANCE == null) {
      INSTANCE = new GoogleDtpInternalMetricRecorder(
          GoogleCredentials.getApplicationDefault(),
          GoogleCloudUtils.getProjectId());
    }

    return INSTANCE;
  }

  private GoogleDtpInternalMetricRecorder(GoogleCredentials credentials, String projectId)
      throws IOException {
    // Enable OpenCensus exporters to export metrics to Stackdriver Monitoring.
    // Exporters use Application Default Credentials to authenticate.
    // See https://developers.google.com/identity/protocols/application-default-credentials
    // for more details.
    StackdriverStatsConfiguration configuration = StackdriverStatsConfiguration.builder()
        .setCredentials(credentials)
        .setProjectId(projectId)
        .setExportInterval(io.opencensus.common.Duration.create(EXPORT_INTERVAL_SECONDS, 0))
        .build();
    StackdriverStatsExporter.createAndRegister(configuration);
    this.viewManager = Stats.getViewManager();
    setupViews();
  }

  private void setupViews() {
    setupView(JOB_STARTED, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_IMPORT_SERVICE);

    setupView(JOB_FINISHED, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_IMPORT_SERVICE, KEY_SUCCESS);
    setupView(
        JOB_FINISHED_DURATION,
        KEY_DATA_TYPE,
        KEY_EXPORT_SERVICE,
        KEY_IMPORT_SERVICE,
        KEY_SUCCESS);

    setupView(EXPORT_PAGE_ATTEMPT, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);
    setupView(EXPORT_PAGE_ATTEMPT_DURATION, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);

    setupView(EXPORT_PAGE, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);
    setupView(EXPORT_PAGE_DURATION, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);

    setupView(IMPORT_PAGE_ATTEMPT, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);
    setupView(IMPORT_PAGE_ATTEMPT_DURATION, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);

    setupView(IMPORT_PAGE, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);
    setupView(IMPORT_PAGE_DURATION, KEY_DATA_TYPE, KEY_EXPORT_SERVICE, KEY_SUCCESS);

    setupView(GENERIC_COUNT, KEY_DATA_TYPE, KEY_GENERIC_SERVICE, KEY_GENERIC_TAG);
    setupView(
        GENERIC_BOOLEAN,
        KEY_DATA_TYPE,
        KEY_GENERIC_SERVICE,
        KEY_GENERIC_TAG,
        KEY_GENERIC_BOOL);
    setupView(GENERIC_DURATION, KEY_DATA_TYPE, KEY_GENERIC_SERVICE, KEY_GENERIC_TAG);
  }

  private void setupView(Measure measure, TagKey... keys) {
    // Register the view. It is imperative that this step exists,
    // otherwise recorded metrics will be dropped and never exported.
    View view = View.create(
        View.Name.create(NAME_BASE + measure.getName()),
        measure.getDescription(),
        measure,
        Aggregation.Count.create(),
        ImmutableList.copyOf(keys));

    viewManager.registerView(view);
  }

  @Override
  public void startedJob(String dataType, String exportService, String importService) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_EXPORT_SERVICE, TagValue.create(exportService), TAG_METADATA)
        .put(KEY_IMPORT_SERVICE, TagValue.create(importService), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap().put(JOB_STARTED, 1).record();
    }
  }

  @Override
  public void exportPageAttemptFinished(
      String dataType,
      String service,
      boolean success,
      Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_EXPORT_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_SUCCESS, TagValue.create(Boolean.toString(success)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(EXPORT_PAGE_ATTEMPT, 1)
          .put(EXPORT_PAGE_ATTEMPT_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void exportPageFinished(
      String dataType,
      String service,
      boolean success,
      Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_EXPORT_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_SUCCESS, TagValue.create(Boolean.toString(success)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(EXPORT_PAGE, 1)
          .put(EXPORT_PAGE_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void importPageAttemptFinished(
      String dataType,
      String service,
      boolean success,
      Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_IMPORT_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_SUCCESS, TagValue.create(Boolean.toString(success)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(IMPORT_PAGE_ATTEMPT, 1)
          .put(IMPORT_PAGE_ATTEMPT_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void importPageFinished(
      String dataType,
      String service,
      boolean success,
      Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_IMPORT_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_SUCCESS, TagValue.create(Boolean.toString(success)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(IMPORT_PAGE, 1)
          .put(IMPORT_PAGE_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void finishedJob(
      String dataType,
      String exportService,
      String importService,
      boolean success,
      Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_EXPORT_SERVICE, TagValue.create(exportService), TAG_METADATA)
        .put(KEY_IMPORT_SERVICE, TagValue.create(importService), TAG_METADATA)
        .put(KEY_SUCCESS, TagValue.create(Boolean.toString(success)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(JOB_FINISHED, 1)
          .put(JOB_FINISHED_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag) {
    recordGenericMetric(dataType, service, tag, 1);
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, boolean bool) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_GENERIC_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_GENERIC_TAG, TagValue.create(tag), TAG_METADATA)
        .put(KEY_GENERIC_BOOL, TagValue.create(Boolean.toString(bool)), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(GENERIC_BOOLEAN, 1)
          .record();
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, Duration duration) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_GENERIC_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_GENERIC_TAG, TagValue.create(tag), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(GENERIC_DURATION, duration.toMillis())
          .record();
    }
  }

  @Override
  public void recordGenericMetric(String dataType, String service, String tag, int value) {
    TagContext tctx = tagger.emptyBuilder()
        .put(KEY_DATA_TYPE, TagValue.create(dataType), TAG_METADATA)
        .put(KEY_GENERIC_SERVICE, TagValue.create(service), TAG_METADATA)
        .put(KEY_GENERIC_TAG, TagValue.create(tag), TAG_METADATA)
        .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      STATS_RECORDER.newMeasureMap()
          .put(GENERIC_COUNT, value)
          .record();
    }
  }
}
