package com.flowforge.url_short;

import com.flowforge.core.domain.Job;
import com.flowforge.core.ports.JobProcessor;
import com.flowforge.core.ports.ShortUrlStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlShortenerJobProcessor implements JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerJobProcessor.class);
    private static final String BASE62_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_COLLISION_RETRIES = 10;

    private final ShortUrlStore shortUrlStore;

    public UrlShortenerJobProcessor(ShortUrlStore shortUrlStore) {
        this.shortUrlStore = shortUrlStore;
    }



    @Override
    public Result process(Job job) throws Exception {
        return null;
    }

    @Override
    public String getJobType() {
        return "URL_SHORTEN";
    }
}
