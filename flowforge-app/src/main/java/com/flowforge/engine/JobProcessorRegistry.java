package com.flowforge.engine;

import com.flowforge.core.ports.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that routes job types to their respective processors.
 * Different job types (URL_SHORTEN, TEST_JOB, etc.) have different processing logic.
 */
@Component
public class JobProcessorRegistry {

        private final Map<String, JobProcessor> processorsByType = new HashMap<>();
        private static final Logger log = LoggerFactory.getLogger(JobProcessorRegistry.class);

        public JobProcessorRegistry(java.util.List<JobProcessor> processors) {

            // Register all JobProcessor beans
            for (JobProcessor processor : processors) {
                JobProcessor previous = processorsByType.put(processor.getJobType(), processor);
                if (previous != null) {
                    throw new IllegalStateException("Two processors claim job type '" + processor.getJobType()
                            + "': " + previous.getClass().getSimpleName()
                            + " and " + processor.getClass().getSimpleName());
                }
            }

            log.info("Registered job processors: {}", processorsByType.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().getClass().getSimpleName())
                    .toList());
        }

        /**
         * Returns the processor for the given job type.
         * Falls back to DefaultJobProcessor if no specific processor is registered.
         */
        public JobProcessor getProcessor(String jobType) {
            JobProcessor processor = processorsByType.get(jobType);
            if (processor == null) {
                throw new IllegalArgumentException("No processor registered for job type '" + jobType
                        + "'. Registered types: " + processorsByType.keySet());
            }
            return processor;
        }

        public boolean hasProcessor(String jobType) {
            return processorsByType.containsKey(jobType);
        }
}
