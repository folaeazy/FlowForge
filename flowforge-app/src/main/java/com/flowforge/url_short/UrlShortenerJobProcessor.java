package com.flowforge.url_short;

import com.flowforge.core.domain.Job;
import com.flowforge.core.ports.JobProcessor;

public class UrlShortenerJobProcessor implements JobProcessor {


    @Override
    public Result process(Job job) throws Exception {
        return null;
    }

    @Override
    public String getJobType() {
        return "";
    }
}
