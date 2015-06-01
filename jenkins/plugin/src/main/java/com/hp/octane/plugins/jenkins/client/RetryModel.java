// (C) Copyright 2003-2015 Hewlett-Packard Development Company, L.P.

package com.hp.octane.plugins.jenkins.client;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.configuration.ConfigurationListener;
import com.hp.octane.plugins.jenkins.configuration.ServerConfiguration;
import hudson.Extension;
import hudson.util.TimeUnit2;

@Extension
public class RetryModel implements ConfigurationListener {

    private static final long[] QUIET_PERIOD = { // TODO: janotav: verify against our Saas policy
            TimeUnit2.MINUTES.toMillis(1),
            TimeUnit2.MINUTES.toMillis(10),
            TimeUnit2.MINUTES.toMillis(60)
    };

    private long boundary;
    private int periodIndex;

    private TimeProvider timeProvider = new SystemTimeProvider();
    private EventPublisher eventPublisher = new JenkinsInsightEventPublisher();

    @Inject
    public RetryModel() {
        doSuccess();
    }

    /**
     * To be used by tests only.
     */
    public RetryModel(EventPublisher eventPublisher) {
        this();
        this.eventPublisher = eventPublisher;
    }

    public synchronized boolean isQuietPeriod() {
        return timeProvider.getTime() < boundary;
    }

    /**
     * @return true if event mechanism is either not active or paused
     */
    public boolean isEventSuspended() {
        return eventPublisher.isSuspended();
    }

    public synchronized void failure() {
        if (periodIndex < QUIET_PERIOD.length - 1) {
            periodIndex++;
        }
        boundary = timeProvider.getTime() + QUIET_PERIOD[periodIndex];
    }

    public void success() {
        doSuccess();
        eventPublisher.resume();
    }

    private synchronized void doSuccess() {
        periodIndex = -1;
        boundary = 0;
    }

    @Override
    public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
        doSuccess();
    }

    /**
     * To be used by tests only.
     */
    void setTimeProvider(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    private static class SystemTimeProvider implements TimeProvider {

        @Override
        public long getTime() {
            return System.currentTimeMillis();
        }
    }

    interface TimeProvider {

        long getTime();

    }

    interface EventPublisher {

        boolean isSuspended();

        void resume();

    }
}
