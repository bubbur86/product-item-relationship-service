package net.catenax.irs.services;

import static net.catenax.irs.util.TestMother.registerJobWithDepthAndAspect;
import static net.catenax.irs.util.TestMother.registerJobWithoutDepth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.catenax.irs.TestConfig;
import net.catenax.irs.component.Job;
import net.catenax.irs.component.JobErrorDetails;
import net.catenax.irs.component.JobHandle;
import net.catenax.irs.component.RegisterJob;
import net.catenax.irs.component.enums.AspectType;
import net.catenax.irs.component.enums.JobState;
import net.catenax.irs.connector.job.JobStore;
import net.catenax.irs.connector.job.MultiTransferJob;
import net.catenax.irs.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = { "test" })
@Import(TestConfig.class)
class IrsItemGraphQueryServiceSpringBootTest {

    private final UUID jobId = UUID.randomUUID();

    @Autowired
    private JobStore jobStore;

    @Autowired
    private IrsItemGraphQueryService service;

    @Test
    void registerItemJobWithoutDepthShouldBuildFullTree() {
        // given
        final RegisterJob registerJob = registerJobWithoutDepth();
        final int expectedRelationshipsSizeFullTree = 6; // stub

        // when
        final JobHandle registeredJob = service.registerItemJob(registerJob);

        // then
        given().ignoreException(EntityNotFoundException.class)
               .await()
               .atMost(10, TimeUnit.SECONDS)
               .until(() -> getRelationshipsSize(registeredJob.getJobId()), equalTo(expectedRelationshipsSizeFullTree));
    }

    @Test
    void registerItemJobWithDepthShouldBuildTreeUntilGivenDepth() {
        // given
        final RegisterJob registerJob = registerJobWithDepthAndAspect(0, null);
        final int expectedRelationshipsSizeFirstDepth = 3; // stub

        // when
        final JobHandle registeredJob = service.registerItemJob(registerJob);

        // then
        given().ignoreException(EntityNotFoundException.class)
               .await()
               .atMost(10, TimeUnit.SECONDS)
               .until(() -> getRelationshipsSize(registeredJob.getJobId()),
                       equalTo(expectedRelationshipsSizeFirstDepth));
    }

    @Test
    void cancelJobById() {
        final String idAsString = String.valueOf(jobId);
        final MultiTransferJob multiTransferJob = MultiTransferJob.builder()
                                                                  .job(Job.builder()
                                                                          .jobId(UUID.fromString(idAsString))
                                                                          .jobState(JobState.UNSAVED)
                                                                          .exception(JobErrorDetails.builder()
                                                                                                    .errorDetail(
                                                                                                            "Job should be canceled")
                                                                                                    .exceptionDate(
                                                                                                            Instant.now())
                                                                                                    .build())
                                                                          .build())
                                                                  .build();

        jobStore.create(multiTransferJob);

        assertThat(service.cancelJobById(jobId)).isNotNull();

        final Optional<MultiTransferJob> fetchedJob = jobStore.find(idAsString);
        assertThat(fetchedJob).isNotEmpty();

        final JobState state = fetchedJob.get().getJob().getJobState();
        assertThat(state).isEqualTo(JobState.CANCELED);
    }

    @Test
    void registerJobWithoutAspectsShouldUseDefault() {
        // given
        final String defaultAspectType = "SerialPartTypization";
        final List<AspectType> emptyAspectTypeFilterList = List.of();
        final RegisterJob registerJob = registerJobWithDepthAndAspect(null, emptyAspectTypeFilterList);

        // when
        final JobHandle jobHandle = service.registerItemJob(registerJob);
        final Optional<MultiTransferJob> multiTransferJob = jobStore.find(jobHandle.toString());

        // then
        assertThat(multiTransferJob).isPresent();
        assertThat(multiTransferJob.get().getJobParameter().getAspectTypes()).contains(defaultAspectType);
    }

    private int getRelationshipsSize(final UUID jobId) {
        return service.getJobForJobId(jobId, false).getRelationships().size();
    }

}