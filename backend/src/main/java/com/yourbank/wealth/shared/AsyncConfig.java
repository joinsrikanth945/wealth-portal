package com.yourbank.wealth.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Turns on @Async (background report generation). With virtual threads enabled, each task gets one. */
@Configuration
@EnableAsync
class AsyncConfig {
}
