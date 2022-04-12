//
// Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
//
// See the AUTHORS file(s) distributed with this work for additional
// information regarding authorship.
//
// See the LICENSE file(s) distributed with this work for
// additional information regarding license terms.
//
package net.catenax.irs.testing;

import com.github.javafaker.Faker;
import net.catenax.irs.component.events.PartRelationshipUpdate;
import net.catenax.irs.component.events.PartRelationshipsUpdateRequest;
import net.catenax.irs.dtos.ItemLifecycleStage;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * Object Mother to generate fake DTO data for testing.
 *
 * @see <a href="https://martinfowler.com/bliki/ObjectMother.html">
 * https://martinfowler.com/bliki/ObjectMother.html</a>
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class UpdateRequestMother {
    /**
     * JavaFaker instance used to generate random data.
     */
    private final transient Faker faker = new Faker();
    /**
     * Object Mother to generate core DTO data for testing.
     */
    //private final transient DtoMother generate = new DtoMother();

    /**
     * Generate a {@link PartRelationshipsUpdateRequest} containing a single relationship update
     * with random data.
     *
     * @return never returns {@literal null}.
     */
    public PartRelationshipsUpdateRequest partRelationshipUpdateList() {
        return partRelationshipUpdateList(partRelationshipUpdate());
    }

    /**
     * Generate a {@link PartRelationshipsUpdateRequest} containing provided relationship updates.
     *
     * @param relationships part relationships to include in the generated request.
     * @return never returns {@literal null}.
     */
    public PartRelationshipsUpdateRequest partRelationshipUpdateList(final PartRelationshipUpdate... relationships) {
        return PartRelationshipsUpdateRequest.builder()
                .withRelationships(Arrays.asList(relationships))
                .build();
    }

    /**
     * Generate a {@link PartRelationshipUpdate} containing random data,
     * with an effect time in the past.
     *
     * @return never returns {@literal null}.
     */
    public PartRelationshipUpdate partRelationshipUpdate() {
        return PartRelationshipUpdate.builder()
                //.withRelationship(generate.relationship())
                .withRemove(false)
                .withStage(faker.options().option(ItemLifecycleStage.class))
                .withEffectTime(faker.date().past(100, DAYS).toInstant())
                .build();
    }
}
