package com.flowforge.engine;

import com.flowforge.core.ports.JobProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that routes job types to their respective processors.
 * Different job types (URL_SHORTEN, TEST_JOB, etc.) have different processing logic.
 */
@Component
public class JobProcessorRegistry {

        private final Map<String, JobProcessor> processorsByType;

        public JobProcessorRegistry(java.util.List<JobProcessor> processors) {
            this.processorsByType = new HashMap<>();

            // Register all JobProcessor beans
            for (JobProcessor processor : processors) {
                String jobType = processor.getJobType();
                if (jobType != null) {
                    processorsByType.put(jobType, processor);
                }
            }
        }

        /**
         * Returns the processor for the given job type.
         * Falls back to DefaultJobProcessor if no specific processor is registered.
         */
        public JobProcessor getProcessor(String jobType) {
            return processorsByType.getOrDefault(jobType,
                    processorsByType.get("DEFAULT"));
        }

        public boolean hasProcessor(String jobType) {
            return processorsByType.containsKey(jobType);
        }
}
