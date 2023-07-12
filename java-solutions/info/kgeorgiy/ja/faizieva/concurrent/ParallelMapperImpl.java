package info.kgeorgiy.ja.faizieva.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadList;
    private final Queue<Runnable> tasks;

    private static class SyncList<R> {
        private final List<R> ans;
        private int cnt;

        public SyncList(int needSize) {
            ans = new ArrayList<>(Collections.nCopies(needSize, null));
            cnt = 0;
        }

        public void set(final int pos, R element) {
            ans.set(pos, element);
            synchronized (this) {
                cnt++;
                if (cnt == ans.size()) {
                    notify();
                }
            }
        }

        public synchronized List<R> getAnswer() throws InterruptedException {
            while (cnt != ans.size()) {
                wait();
            }
            return ans;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        SyncList<R> syncList = new SyncList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int pos = i;
            Runnable task = null;
            try {
                task = (() -> syncList.set(pos, f.apply(args.get(pos))));
            } catch (RuntimeException ignored) {

            }
            synchronized (tasks) {
                tasks.add(task);
                tasks.notifyAll();
            }
        }
        return syncList.getAnswer();
    }

    public ParallelMapperImpl(int threads) {
        threadList = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                while (!Thread.interrupted()) {
                    Runnable task;
                    synchronized (tasks) {
                        try {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                        task = tasks.poll();
                    }
                    task.run();
                }
            }));
            threadList.get(i).start();
        }
    }

    @Override
    public void close() {
        threadList.forEach(Thread::interrupt);
        boolean isInterrupted = false;
        for (int i = 0; i < threadList.size(); ) {
            Thread thread = threadList.get(i);
            try {
                thread.join();
                i++;
            } catch (InterruptedException ignored) {
                isInterrupted = true;
            }
        }
        if (isInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

