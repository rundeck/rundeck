/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.datasketches.theta.JaccardSimilarity
import org.apache.datasketches.theta.Sketch
import org.apache.datasketches.theta.UpdateSketch
import rundeck.ScheduledExecution

@Slf4j
@Transactional
class JobSimilarityDetectionService {
    public static final BigDecimal JOB_SIMILARITY_THRESHOLD = 0.8
    public static final String SIMILARITY_COMPARISON_FORMAT = 'json'


    GenAIService genAIService
    GithubJobDescriptionsService githubJobDescriptionsService
    ProjectManagerService projectManagerService
    ScheduledExecutionService scheduledExecutionService

    Set<ScheduledExecution> jobsInProject(IRundeckProject project) {
        // TODO: ACL check must be done in real implementation
        ScheduledExecution.findAllByProject(project.name)
    }

    Map<ScheduledExecution, Sketch> jobSketchesInProject(IRundeckProject project) {
        jobsInProject(project).collectEntries { [it, createMinHashSketch(scheduledExecutionService.generateJobExportDefinition(it, SIMILARITY_COMPARISON_FORMAT))] }
    }

    Set<ScheduledExecution> findSimilarJobs(IRundeckProject projectProperties, ScheduledExecution sourceJob) {

        BigDecimal projectJobSimilarityThreshold = BigDecimal.valueOf(Double.parseDouble( projectProperties.getProperty('project.job-description-gen.similarity.threshold') ?: "$JOB_SIMILARITY_THRESHOLD"))

        def project = projectManagerService.getFrameworkProject(sourceJob.project)
        final Sketch sourceJobSketch = createMinHashSketch(scheduledExecutionService.generateJobExportDefinition(sourceJob, SIMILARITY_COMPARISON_FORMAT))

        jobSketchesInProject(project).findAll {
            def similarityLog = "Similarity of ${sourceJob.jobName} with ${it.key.jobName} was: ${new JaccardSimilarity().jaccard(sourceJobSketch, it.value)}"
            log.info(similarityLog)
            areSimilar(sourceJobSketch, it.value, projectJobSimilarityThreshold)
        }.collect { it.key }
                .findAll { it.uuid != sourceJob.uuid } // remove the sourceJob itself
    }

    static Sketch createMinHashSketch(String content) {
        UpdateSketch sketch = UpdateSketch.builder().build()
        content.split("\\s+").each { word ->
            sketch.update(word)
        }
        return sketch.compact()
    }

    static boolean areSimilar(String str1, String str2, BigDecimal threshold = JOB_SIMILARITY_THRESHOLD) {
        Sketch sketch1 = createMinHashSketch(str1)
        Sketch sketch2 = createMinHashSketch(str2)
        areSimilar(sketch1, sketch2, threshold)
    }

    static boolean areSimilar(Sketch sketch1, Sketch sketch2, BigDecimal threshold = JOB_SIMILARITY_THRESHOLD) {
        new JaccardSimilarity().similarityTest(sketch1, sketch2, threshold)
    }

    static double[] jaccard(String str1, String str2) {
        Sketch sketch1 = createMinHashSketch(str1)
        Sketch sketch2 = createMinHashSketch(str2)
        new JaccardSimilarity().jaccard(sketch1, sketch2)
    }

}
