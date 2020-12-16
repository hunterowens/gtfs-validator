/*
 * Copyright 2020 Google LLC, MobilityData IO
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
 */

package org.mobilitydata.gtfsvalidator.table;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mobilitydata.gtfsvalidator.input.GtfsFeedName;
import org.mobilitydata.gtfsvalidator.notice.NoticeContainer;
import org.mobilitydata.gtfsvalidator.validator.ValidatorLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static com.google.common.truth.Truth.assertThat;

/**
 * Runs GtfsStopTimeTableLoader on test CSV data.
 */
@RunWith(JUnit4.class)
public class GtfsStopTimeTableLoaderTest {
    private static final GtfsFeedName FEED_NAME = GtfsFeedName.parseString("au-sydney-buses");

    @Test
    public void validFileShouldNotGenerateNotice() throws IOException {
        ValidatorLoader validatorLoader = new ValidatorLoader();
        Reader reader =
                new StringReader(
                        "trip_id,stop_id,stop_sequence"
                                + System.lineSeparator() +
                                "trip id value,stop id value,20");
        GtfsStopTimeTableLoader loader = new GtfsStopTimeTableLoader();
        NoticeContainer noticeContainer = new NoticeContainer();
        GtfsStopTimeTableContainer tableContainer =
                (GtfsStopTimeTableContainer) loader.load(reader, FEED_NAME, validatorLoader, noticeContainer);

        assertThat(noticeContainer.getNotices()).isEmpty();
        assertThat(tableContainer.entityCount()).isEqualTo(1);
        GtfsStopTime stopTime = tableContainer.byTripId("trip id value").get(0);
        assertThat(stopTime).isNotNull();
        assertThat(stopTime.tripId()).matches("trip id value");
        assertThat(stopTime.stopId()).matches("stop id value");
        assertThat(stopTime.stopSequence()).isEqualTo(20);

        reader.close();
    }

    @Test
    public void missingRequiredFieldShouldGenerateNotice() throws IOException {
        ValidatorLoader validatorLoader = new ValidatorLoader();
        Reader reader =
                new StringReader(
                        "trip_id,stop_id,stop_sequence"
                                + System.lineSeparator() +
                                ",stop id value,20");
        GtfsStopTimeTableLoader loader = new GtfsStopTimeTableLoader();
        NoticeContainer noticeContainer = new NoticeContainer();
        GtfsStopTimeTableContainer tableContainer =
                (GtfsStopTimeTableContainer) loader.load(reader, FEED_NAME, validatorLoader, noticeContainer);

        assertThat(noticeContainer.getNotices()).isNotEmpty();
        assertThat(noticeContainer.getNotices().get(0).getCode()).matches("missing_required_field");
        assertThat(noticeContainer.getNotices().get(0).getContext()).containsEntry("filename", "stop_times.txt");
        assertThat(noticeContainer.getNotices().get(0).getContext()).containsEntry("csvRowNumber", 2L);
        assertThat(noticeContainer.getNotices().get(0).getContext()).containsEntry("fieldName", "trip_id");
        assertThat(tableContainer.entityCount()).isEqualTo(0);
        reader.close();
    }

    @Test
    public void emptyFileShouldGenerateNotice() throws IOException {
        ValidatorLoader validatorLoader = new ValidatorLoader();
        Reader reader = new StringReader("");
        GtfsStopTimeTableLoader loader = new GtfsStopTimeTableLoader();
        NoticeContainer noticeContainer = new NoticeContainer();
        loader.load(reader, FEED_NAME, validatorLoader, noticeContainer);

        assertThat(noticeContainer.getNotices()).isNotEmpty();
        assertThat(noticeContainer.getNotices().get(0).getClass().getSimpleName())
                .isEqualTo("EmptyFileNotice");
        reader.close();
    }
}