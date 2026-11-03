package org.vkrt.accessiblechess;

/** Host-testable bounds for physical hold-to-talk gestures. */
final class VoiceUxPolicy {
    private VoiceUxPolicy() {}
    static boolean shouldStopOnRelease(long durationMillis) {return durationMillis >= 450L;}
}
