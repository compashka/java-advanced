package info.kgeorgiy.ja.pleshanov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * WebCrawler class.
 *
 * @author Pleshanov Pavel
 */
public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final Map<String, HostDownloader> hostMapper;

    private static final int DEFAULT_VALUE = 1;

    /**
     * Constructor of WebCrawler class.
     *
     * @param downloader  the Downloader to use for downloading web pages
     * @param downloaders the number of downloaders to use
     * @param extractors  the number of extractors to use
     * @param perHost     the maximum number of downloads per host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.hostMapper = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args == null || args.length > 5 || args.length < 1 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong Arguments!");
            return;
        }
        try {
            int depth = getValue(args, 1);
            int downloaders = getValue(args, 2);
            int extractors = getValue(args, 3);
            int perHost = getValue(args, 4);

            try (WebCrawler crawler = new WebCrawler(new CachingDownloader(0), downloaders, extractors, perHost)) {
                crawler.download(args[0], depth);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Downloads web pages starting from the given URL up to the given depth using multiple threads.
     *
     * @param url   the starting URL to download pages from
     * @param depth the maximum depth of links to follow from the starting URL
     * @return a Result object containing a list of downloaded page URLs and any errors encountered during the process
     */
    @Override
    public Result download(String url, int depth) {
        final ConcurrentLinkedQueue<String> linksPool = new ConcurrentLinkedQueue<>();
        final Set<String> extractedPages = ConcurrentHashMap.newKeySet();
        final Set<String> res = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();

        linksPool.add(url);

        for (int i = depth; i > 0; --i) {
            int currDepth = i;
            Phaser phaser = new Phaser(1);
            // :NOTE: изначальный url попадает сюда depth раз, хотя мы уже на 2 итерации знаем все про него (fixed)
            var linksPoolList = List.copyOf(linksPool);
            linksPool.clear();
            linksPoolList.stream()
                    .filter(extractedPages::add)
                    .forEach(link -> pageDownload(link, currDepth, phaser, errors, res, linksPool));
            phaser.arriveAndAwaitAdvance();
        }

        return new Result(List.copyOf(res), errors);
    }

    /**
     * Shuts down the executor services used for downloading and extracting web pages.
     */
    @Override
    public void close() {
        // :NOTE: этого недостаточно (fixed)
        shutdownAndAwaitTermination(downloaders);
        shutdownAndAwaitTermination(extractors);
    }

    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(1, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int getValue(String[] args, int index) {
        return (index < args.length) ? Integer.parseInt(args[index]) : DEFAULT_VALUE;
    }


    private class HostDownloader {
        private final Queue<Runnable> pagesPool = new ArrayDeque<>();
        private int number;

        public synchronized void addPage(Runnable task) {
            if (number >= perHost) {
                pagesPool.add(task);
            } else {
                ++number;
                downloaders.submit(task);
            }
        }

        private synchronized void runTasks() {
            try {
                downloaders.submit(pagesPool.remove());
            } catch (NoSuchElementException ignored) {
                --number;
            }
        }
    }

    private void pageDownload(String link, int depth, Phaser phaser,
                              ConcurrentMap<String, IOException> errors, Set<String> res,
                              ConcurrentLinkedQueue<String> linksPool) {
        String host;
        try {
            host = URLUtils.getHost(link);
        } catch (MalformedURLException e) {
            errors.put(link, e);
            return;
        }

        HostDownloader hostDownloader = hostMapper.computeIfAbsent(host, h -> new HostDownloader());
        phaser.register();
        hostDownloader.addPage(() -> {
            try {
                Document document = downloader.download(link);
                linkDownload(document, phaser, linksPool, depth);
                res.add(link);
            } catch (IOException e) {
                errors.put(link, e);
            } finally {
                phaser.arrive();
                hostDownloader.runTasks();
            }
        });
    }

    private void linkDownload(Document document, Phaser phaser, ConcurrentLinkedQueue<String> linksPool, int depth) {
        if (depth <= 1) {
            return;
        }
        phaser.register();
        extractors.submit(() -> {
            try {
                List<String> links = document.extractLinks();
                linksPool.addAll(links);
            } catch (IOException ignored) {
            } finally {
                phaser.arrive();
            }
        });
    }
}
