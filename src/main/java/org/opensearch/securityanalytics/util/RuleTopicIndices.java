/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.securityanalytics.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.Client;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.securityanalytics.config.monitors.DetectorMonitorConfig;

public class RuleTopicIndices {
    private static final Logger log = LogManager.getLogger(DetectorIndices.class);

    private final Client client;

    private final ClusterService clusterService;

    public RuleTopicIndices(Client client, ClusterService clusterService) {
        this.client = client;
        this.clusterService = clusterService;
    }

    public static String ruleTopicIndexSettings() throws IOException {
        return new String(Objects.requireNonNull(DetectorIndices.class.getClassLoader().getResourceAsStream("mappings/detector-settings.json")).readAllBytes(), Charset.defaultCharset());
    }

    public void initRuleTopicIndexTemplate(ActionListener<AcknowledgedResponse> actionListener) throws IOException {
        if (!ruleTopicIndexTemplateExists()) {
            // Compose list of all patterns to cover all query indices
            List<String> indexPatterns = new ArrayList<>();
            for(String ruleIndex : DetectorMonitorConfig.getAllRuleIndices()) {
                indexPatterns.add(ruleIndex + "*");
            }
            PutIndexTemplateRequest indexRequest =
                    new PutIndexTemplateRequest(DetectorMonitorConfig.OPENSEARCH_SAP_RULE_INDEX_TEMPLATE)
                    .patterns(indexPatterns)
                    .settings(Settings.builder().loadFromSource(ruleTopicIndexSettings(), XContentType.JSON).build());
            client.admin().indices().putTemplate(indexRequest, actionListener);
        } else {
            actionListener.onResponse(new AcknowledgedResponse(true));
        }
    }

    public boolean ruleTopicIndexTemplateExists() {
        ClusterState clusterState = clusterService.state();
        return clusterState.metadata().templates()
                .get(DetectorMonitorConfig.OPENSEARCH_SAP_RULE_INDEX_TEMPLATE) != null;
    }
}