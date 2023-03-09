package org.sikuli.recorder;

/**
 * Records the user's actions and captures screenshots.
 */
public class Recorder {

    private boolean recording = false;
    private CaptureScreenshots captureScreenshots;
    private CaptureUserInputs captureUserInputs;

    public Recorder() {
        captureScreenshots = new CaptureScreenshots();
        captureUserInputs = new CaptureUserInputs(new RecordInputsXML());
    }

    /**
     * Starts recording the user's actions and capturing screenshots.
     * The screenshots are saved in the directory defined below.
     */
    public void startRecording() {
        if (recording) return;
        recording = true;
        captureScreenshots.startCapturing("sikulix-recorder",1000);
        captureUserInputs.startRecording();
    }

    /**
     * Stops recording the user's actions and capturing screenshots.
     * The user's actions are saved in the path defined below.
     */
    public void stopRecording() {
        if (!recording) return;
        recording = false;
        captureScreenshots.stopCapturing();
        captureUserInputs.stopRecording("\\sikulix-recorder\\input-history.xml");
    }
}
