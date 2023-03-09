package org.sikuli.recorder;

/**
 * Records the user's actions.
 */
public class Recorder {

    private boolean recording = false;
    private CaptureScreenshots captureScreenshots;

    public Recorder() {
        captureScreenshots = new CaptureScreenshots();
    }

    public void startRecording() {
        if (recording) return;
        recording = true;
        captureScreenshots.capture(1000);
    }
}
