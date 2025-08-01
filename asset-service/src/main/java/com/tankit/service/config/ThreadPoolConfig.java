package com.tankit.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;

/**
 * Configuration for ThreadPoolExecutor used in asset processing
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${thread-pool.max-size:20}")
    private int maxPoolSize;

    @Value("${thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * Configure ThreadPoolExecutor for asset processing
     */
    @Bean
    public ThreadPoolExecutor assetProcessingExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new CustomThreadFactory("AssetProcessor"),
                new ThreadPoolExecutor.CallerRunsPolicy() // When queue is full, caller thread executes
        );

        // Allow core threads to timeout when idle
        executor.allowCoreThreadTimeOut(true);

        return executor;
    }

    /**
     * Custom ThreadFactory for better thread naming and monitoring
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int threadNumber = 1;

        CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber++);
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}