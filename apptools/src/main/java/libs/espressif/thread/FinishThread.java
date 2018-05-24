package libs.espressif.thread;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Finish blocking thread
 */
public abstract class FinishThread extends Thread {
    private final LinkedBlockingQueue<String> mFinishQueue = new LinkedBlockingQueue<>();

    private volatile boolean mFinished = false;

    /**
     * Call in {@link #run()}
     */
    public abstract void execute();

    @Override
    public void run() {
        execute();

        mFinishQueue.add("FINISH");
        mFinished = true;
    }

    /**
     * Finish the thread. Blocking if {@link #run()} hasn't returned
     */
    public void finish() {
        interrupt();
        try {
            mFinishQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isFinished() {
        return mFinished;
    }
}
