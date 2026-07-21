package com.flowforge.api.controller;

import com.flowforge.engine.DeadLetterQueue;
import com.flowforge.engine.JobQueueService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Failed Jobs (DLQ) table - list, inspect and retry
 */
@RestController
@RequestMapping("/api/dlq")
public class DlqController {

    private final DeadLetterQueue dlq;
    private final JobQueueService jobQueueService;
}
