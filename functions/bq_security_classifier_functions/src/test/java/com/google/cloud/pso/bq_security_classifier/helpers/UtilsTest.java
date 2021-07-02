package com.google.cloud.pso.bq_security_classifier.helpers;

import org.junit.Test;
import static org.junit.Assert.*;
import com.google.cloud.pso.bq_security_classifier.helpers.Utils;

public class UtilsTest {

    @Test
    public void extractTaxonomyIdFromPolicyTagId() {

        String input = "projects/<project>/locations/<location>/taxonomies/<taxonomyID>/policyTags/<policyTagID";
        String expected = "projects/<project>/locations/<location>/taxonomies/<taxonomyID>";
        String actual = Utils.extractTaxonomyIdFromPolicyTagId(input);

        assertEquals(expected, actual);
    }
}