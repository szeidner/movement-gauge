package com.stevezeidner.movementgauge.core;

/**
 * Constants used throughout the app
 */
public class Constants {
    // broadcast keys
    public static final String SAMPLE_RESULT = "com.stevezeidner.movementgauge.service.SamplingService.REQUEST_PROCESSED";
    public static final String SAMPLE_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.SAMPLE_VALUE";
    public static final String CUMULATIVE_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.CUMULATIVE_VALUE";
    public static final String TIMESTAMP_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.TIMESTAMP_VALUE";
    public static final String CUMULATIVE_STARTUP_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.CUMULATIVE_STARTUP_VALUE";

    // shared prefs keys
    public static final String CUMULATIVE_PREFS_KEY = "Cumulative";

    // PubNub keys
    public static final String PUBNUB_PUB = "pub-c-3031c78d-7e71-43ac-8e87-9657a7bbc7b6";
    public static final String PUBNUB_SUB = "sub-c-40913ce6-d10d-11e4-9f3d-0619f8945a4f";
    public static final String PUBNUB_SEC = "sec-c-OTRlZjUyNGQtNDRiNi00N2ZiLWE2Y2EtYTI1NDAzMTAwNGU0";
    public static final String PUBNUB_CHANNEL = "accelerometer";

    // network request parameters
    public static final int LOW_SEND_TIME = 5; // seconds
    public static final int HIGH_SEND_TIME = 10; // seconds
}
