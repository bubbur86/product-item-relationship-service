/********************************************************************************
 * Copyright (c) 2021,2022
 *       2022: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0. *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.tractusx.irs.aaswrapper.job.AASTransferProcess;
import org.eclipse.tractusx.irs.aaswrapper.job.ItemDataRequest;
import org.eclipse.tractusx.irs.component.enums.JobState;
import org.eclipse.tractusx.irs.connector.job.JobInitiateResponse;
import org.eclipse.tractusx.irs.connector.job.JobOrchestrator;
import org.eclipse.tractusx.irs.connector.job.JobStore;
import org.eclipse.tractusx.irs.connector.job.ResponseStatus;
import org.eclipse.tractusx.irs.dto.JobParameter;
import org.eclipse.tractusx.irs.persistence.BlobPersistence;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = { "local", "test" })
@Import(TestConfig.class)
class IrsApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobStore jobStore;

    @Autowired
    private BlobPersistence inMemoryBlobStore;

    @Autowired
    private JobOrchestrator<ItemDataRequest, AASTransferProcess> jobOrchestrator;

    @Test
    void generatedOpenApiMatchesContract() throws Exception {
        final String generatedYaml = this.restTemplate.getForObject("http://localhost:" + port + "/api/api-docs.yaml",
                String.class);
        final String fixedYaml = Files.readString(new File("../api/irs-v1.0.yaml").toPath(), UTF_8);
        assertThat(generatedYaml).isEqualToIgnoringWhitespace(fixedYaml);
    }

    @Test
    void shouldStoreBlobResultWhenRunningJob() throws Exception {
        final JobParameter jobParameter = JobParameter.builder()
                                                      .rootItemId("rootitemid")
                                                      .treeDepth(5)
                                                      .aspectTypes(List.of())
                                                      .build();

        final JobInitiateResponse response = jobOrchestrator.startJob(jobParameter);

        assertThat(response.getStatus()).isEqualTo(ResponseStatus.OK);

        Awaitility.await()
                  .atMost(10, TimeUnit.SECONDS)
                  .pollInterval(100, TimeUnit.MILLISECONDS)
                  .until(() -> jobStore.find(response.getJobId())
                                       .map(s -> s.getJob().getJobState())
                                       .map(state -> state == JobState.COMPLETED)
                                       .orElse(false));

        assertThat(inMemoryBlobStore.getBlob(response.getJobId())).isNotEmpty();
    }

}
