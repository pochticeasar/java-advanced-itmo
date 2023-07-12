package info.kgeorgiy.ja.faizieva.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class IterativeParallelism implements ScalarIP {
    private ParallelMapper parallelMapper = null;

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    public IterativeParallelism() {

    }

    private <T, R> R calculateByTreads(int threads, List<? extends T> values,
                                       Function<Stream<? extends T>, ? extends R> task,
                                       Function<Stream<? extends R>, ? extends R> collector) throws InterruptedException {
        int treadsCnt = Math.min(values.size(), threads);
        int blockSize = values.size() / treadsCnt;
        int rem = values.size() % treadsCnt;

        List<Stream<? extends T>> subLists = new ArrayList<>();
        List<Thread> threadList = new ArrayList<>();
        List<R> ans = new ArrayList<>(Collections.nCopies(treadsCnt, null));

        int pos = 0;
        for (int i = 0; i < treadsCnt; i++) {
            subLists.add(values.subList(pos, pos += blockSize + (i < rem ? 1 : 0)).stream());
        }
        if (parallelMapper != null) {
            ans = parallelMapper.map(task, subLists);
        } else {

            for (int i = 0; i < treadsCnt; i++) {
                final int finalPos = i;
                List<R> finalAns = ans;
                threadList.add(new Thread(() -> finalAns.set(finalPos, task.apply(subLists.get(finalPos)))));
                threadList.get(i).start();
            }
            joinThreads(threadList);
        }
        return collector.apply(ans.stream());
    }

    private void joinThreads(List<Thread> threadList) throws InterruptedException {
        InterruptedException exceptions = new InterruptedException();
        for (int i = 0; i < threadList.size(); ) {
            Thread thread = threadList.get(i);
            try {
                thread.join();
                i++;
            } catch (InterruptedException e) {
                exceptions.addSuppressed(e);
            }
        }
        if (exceptions.getSuppressed().length != 0) {
            throw exceptions;
        }
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return calculateByTreads(threads, values, stream -> stream.max(comparator).orElse(null), stream -> stream.max(comparator).orElse(null));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return calculateByTreads(threads, values, stream -> stream.min(comparator).orElse(null), stream -> stream.min(comparator).orElse(null));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculateByTreads(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(bool -> bool));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculateByTreads(threads, values, stream -> stream.anyMatch(predicate), stream -> stream.anyMatch(bool -> bool));
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculateByTreads(threads, values, stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList())).size();
    }
}
