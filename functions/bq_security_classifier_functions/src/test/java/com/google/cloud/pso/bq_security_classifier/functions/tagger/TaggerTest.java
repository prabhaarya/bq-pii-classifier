package com.google.cloud.pso.bq_security_classifier.functions.tagger;

import com.google.api.client.util.Lists;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.PolicyTags;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.pso.bq_security_classifier.functions.inspector.Inspector;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;
import com.google.cloud.pso.bq_security_classifier.services.BigQueryService;
import com.google.cloud.pso.bq_security_classifier.services.DlpService;
import com.google.privacy.dlp.v2.BigQueryTable;
import com.google.privacy.dlp.v2.DlpJob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaggerTest {

    @Mock Environment envMock;
    @Mock DlpService dlpServiceMock;
    @Mock BigQueryService bigQueryServiceMock;
    @Mock TaggerHelper taggerHelper;

    @Mock
    HttpRequest requestMock;

    @Mock
    HttpResponse responseMock;

    @InjectMocks
    Tagger function;

    @Test
    public void testTagger() throws IOException, InterruptedException {

        String jsonPayLoad = "{\"dlpJobName\":\"dlpJobId\"}";

        // use an empty string as the default request content
        BufferedReader reader = new BufferedReader(new StringReader(jsonPayLoad));
        when(requestMock.getReader()).thenReturn(reader);

        StringWriter responseOut = new StringWriter();
        BufferedWriter writerOut = new BufferedWriter(responseOut);
        when(responseMock.getWriter()).thenReturn(writerOut);

        // Mock Dlp service
        when(dlpServiceMock.getJobState("dlpJobId"))
                .thenReturn(DlpJob.JobState.DONE);
        when(dlpServiceMock.getInspectedTable("dlpJobId"))
                .thenReturn(BigQueryTable.newBuilder()
                        .setProjectId("targetProject")
                        .setDatasetId("targetDataset")
                        .setTableId("targetTable")
                        .build()
                );

        // Mock Env
        // env variables are not used by Mocks
//        when(envMock.getProjectId()).thenReturn("serviceProject");
//        when(envMock.getDatasetId()).thenReturn("resultsDataset");
//        when(envMock.getDlpResultsTable()).thenReturn("resultsTable");
//        when(envMock.getBqViewFieldsFindings()).thenReturn("bqView");

        // Mock Bq service
        when(bigQueryServiceMock.getTableSchemaFields("targetProject", "targetDataset", "targetTable"))
                .thenReturn(
                Arrays.asList(
                        new TableFieldSchema().setName("email")
                                .setPolicyTags(new TableFieldSchema.PolicyTags().setNames(Arrays.asList("auto_taxonomy/email"))),
                        new TableFieldSchema().setName("phone")
                                .setPolicyTags(new TableFieldSchema.PolicyTags().setNames(Arrays.asList("auto_taxonomy/phone"))),
                        new TableFieldSchema().setName("address")
                                .setPolicyTags(new TableFieldSchema.PolicyTags().setNames(Arrays.asList("manual_taxonomy/address"))),
                        new TableFieldSchema().setName("non_conf")
        ));


        // Mock TaggerHelper
        Map<String, String> fieldsToPolicyTagsMap = new HashMap<>();
        fieldsToPolicyTagsMap.put("email", "auto_taxonomy/email_new");
        fieldsToPolicyTagsMap.put("phone", "auto_taxonomy/phone");
        fieldsToPolicyTagsMap.put("address", "auto_taxonomy/address");
        when(taggerHelper.getFieldsToPolicyTagsMap(any(), any(), any(), any())).thenReturn(fieldsToPolicyTagsMap);


        Map<String, String> expectedFieldsAndPolicyTags = new HashMap<>();
        expectedFieldsAndPolicyTags.put("email", "auto_taxonomy/email_new"); // overwrite same taxonomy
        expectedFieldsAndPolicyTags.put("phone", "auto_taxonomy/phone"); // overwrite same taxonomy
        expectedFieldsAndPolicyTags.put("address", "manual_taxonomy/address"); // keep diff taxonomy
        expectedFieldsAndPolicyTags.put("non_conf", ""); // no tags

        function.service(requestMock, responseMock);
        Map<String, String> newFieldsAndPolicyTags = function.getFinalFieldsToPolicyTags();

        assertEquals(expectedFieldsAndPolicyTags, newFieldsAndPolicyTags);
    }
}
