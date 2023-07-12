package info.kgeorgiy.ja.faizieva.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@SuppressWarnings("all")
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloaderPool;
    private final ExecutorService extractionPool;

    @Override
    public Result download(final String url, final int depth) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String, IOException> exceptions = new ConcurrentHashMap<>();
        final Queue<String> queue = new ConcurrentLinkedQueue<>();
        Phaser phaser = new Phaser(1);
        downloaded.add(url);
        queue.add(url);
        // :NOTE: create Phaser once here
        IntStream.range(0, depth).forEach(layer -> {
             // :NOTE: не создавайте Phaser на каждой итерации цикла
            final int limit = queue.size();
            for (int i = 0; i < limit; i++) {
                final String next = queue.poll();
                phaser.register();
                downloaderPool.submit(() -> {
                    try {
                        final Document page = downloader.download(next);
                        downloaded.add(next);
                        if (depth > layer + 1) {
                            phaser.register();
                            extractionPool.execute(() -> {
                                try {
                                    page.extractLinks().stream()
                                            .filter(downloaded::add).forEach(queue::add);
                                } catch (IOException ignored) {

                                } finally {
                                    phaser.arriveAndDeregister();
                                }
                            });
                        }

                    } catch (final IOException e) {
                        exceptions.put(next, e);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
            }
            phaser.arriveAndAwaitAdvance();
        });
        //
        downloaded.removeAll(exceptions.keySet());
        return new Result(new ArrayList<>(downloaded), exceptions);
    }


    @Override
    public void close() {
        downloaderPool.shutdown();
        extractionPool.shutdown();

    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;

        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractionPool = Executors.newFixedThreadPool(extractors);

    }

    public static void main(String[] args) {
        // WebCrawler url [depth [downloads [extractors [perHost]]]]
        // WebCrawler exampla.com
        // WebCrawler exampla.com 1
        // WebCrawler exampla.com 1 2
        // WebCrawler exampla.com 1 2 3
        // WebCrawler exampla.com 1 2 3 4
        if (args == null || args.length != 5) { // :NOTE: check lower bound
            System.err.println("Wrong number of arguments");
            return;
        }
        try {
            int depth = Integer.parseInt(args[1]);
            int downloaders = Integer.parseInt(args[2]);
            int extractors = Integer.parseInt(args[3]);
            int perHost = Integer.parseInt(args[4]);
            try (Crawler crawler = new WebCrawler(new CachingDownloader(0), downloaders, extractors, perHost)) {
                Result result = crawler.download(args[0], depth);
                for (String url : result.getDownloaded()) {
                    System.out.println(url);
                }
                // :NOTE: print result.getErrors()
                for (IOException error : result.getErrors().values()) {
                    System.err.println(error.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Can't create downloader: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            // :NOTE: e.getMessage()
            System.err.println("Wrong format of arguments: " + e.getMessage());
        }
    }
}
