package de.erdbeerbaerlp.dcintegration.common;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/// Simple Off-ServerThread task executor
public final class WorkThread {
    private WorkThread() {
    } // static class

    private static final LinkedList<Job> jobQueue = new LinkedList<>();
    private static final Thread runner = do_start(new Thread(WorkThread::thread, "DiscordIntegration-Core WorkerThread"));
    private static Thread do_start(Thread thr) {
        thr.setDaemon(true); // mark as background thread
        thr.start();
        return thr;
    }

    private static void thread() {
        while(true) {
            { // this stack frame solely exists to allow j to be discarded
                Job j;
                while ((j = jobQueue.poll()) != null) {
                    j.run();
                }
            }
            LockSupport.park();
        }
    }
    private static void pushJob(Job job) {

        // Note if you experience very delayed execution of tasks
        //  remove the unpark variable checks, and just call
        //      LockSupport.unpark(runner);
        //  everytime

        boolean unpark = jobQueue.isEmpty();
        jobQueue.addLast(job);
        unpark = unpark || jobQueue.peekLast() == jobQueue.peekFirst();
        if (unpark)
            LockSupport.unpark(runner);
    }


    /// adds an execution job to the WorkThread, if executing under the WorkThread already the job is syncronously executed
    public static JobHandle executeJob(Runnable action) {
        final class JobAction implements Job {
            public JobAction(Runnable action, JobHandle handle) {
                this.action = action;
                this.handle = handle;
            }
            final Runnable action;
            final JobHandle handle;
            public void run() {
                try {
                    this.action.run();
                } catch (Exception ex) {
                    this.handle.error = ex;
                }
                this.handle.completed = true;
            }
        }

        JobHandle handle = new JobHandle();
        JobAction job = new JobAction(action, handle);

        if (Thread.currentThread() == runner) {
            job.run();
        }
        else {
            pushJob(job);
        }

        return handle;
    }

    /// adds an execution job to the WorkThread, if executing under the WorkThread already the job is syncronously executed
    public static <R>JobHandleWithResult<R> executeJobWithReturn (Supplier<R> action) {
        final class JobActionWithReturn implements Job {
            public JobActionWithReturn(Supplier<R> action, JobHandleWithResult<R> handle) {
                this.action = action;
                this.handle = handle;
            }
            final Supplier<R> action;
            final JobHandleWithResult<R> handle;
            public void run() {
                try {
                    this.handle.result = this.action.get();
                } catch (Exception ex) {
                    this.handle.error = ex;
                }
                this.handle.completed = true;
            }
        }


        JobHandleWithResult<R> handle = new JobHandleWithResult<>();
        JobActionWithReturn job = new JobActionWithReturn(action, handle);


        if (Thread.currentThread() == runner) {
            job.run();
        }
        else {
            pushJob(job);
        }

        return handle;
    }


    public static class JobHandleWithResult<R> {
        private Boolean completed;
        private Exception error = null;
        private R result;

        public Boolean isCompleted() {
            return this.completed;
        }
        public Optional<Exception> getError() {
            return Optional.ofNullable(this.error);
        }
        public Optional<R> getResult() {
            return Optional.ofNullable(this.result);
        }
    }
    public static class JobHandle {
        private Boolean completed = false;
        private Exception error = null;
        public Boolean isCompleted() {
            return this.completed;
        }
        public Optional<Exception> getError() {
            return Optional.ofNullable(this.error);
        }
    }
    private interface Job {
        void run();
    }
}