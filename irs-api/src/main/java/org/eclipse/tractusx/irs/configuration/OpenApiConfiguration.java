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
package org.eclipse.tractusx.irs.configuration;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.irs.IrsApplication;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the springdoc OpenAPI generator.
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfiguration {

    /**
     * IRS configuration settings.
     */
    private final IrsConfiguration irsConfiguration;

    /**
     * Factory for generated Open API definition.
     *
     * @return Generated Open API configuration.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().addServersItem(new Server().url(irsConfiguration.getApiUrl().toString()))
                            .addSecurityItem(new SecurityRequirement().addList("oAuth2", List.of("read", "write")))
                            .info(new Info().title("IRS API")
                                            .version(IrsApplication.API_VERSION)
                                            .description(
                                                    "The API of the Item Relationship Service (IRS) for retrieving item graphs along the value chain of CATENA-X partners."));
    }

    /**
     * Generates example values in Swagger
     *
     * @return the customiser
     */
    @Bean
    public OpenApiCustomiser customiser() {
        return openApi -> {
            final Components components = openApi.getComponents();
            components.addSecuritySchemes("OAuth2", new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                                                        .flows(new OAuthFlows().clientCredentials(
                                                                                new OAuthFlow().tokenUrl("https://centralidp.demo.catena-x.net/auth/realms/CX-Central/protocol/openid-connect/token"))));
            new OpenApiExamples().createExamples(components);
        };
    }

}
