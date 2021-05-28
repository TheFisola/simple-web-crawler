package com.thefisola.webcrawler;

import com.thefisola.webcrawler.exception.InvalidUrlException;
import com.thefisola.webcrawler.socket.factory.CustomSSLSocketFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.platform.commons.util.StringUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebCrawler {
    private static final String CSS_GET_LINKS_QUERY = "a[href]";
    private static final int NO_OF_FIXED_THREADS = 10;
    private static final int DEFAULT_MAX_RETRY_COUNT = 1;
    private static final Set<String> invalidContentTypes = new HashSet<>();

    static {
        invalidContentTypes.addAll(Set.of("pdf", "jpg", "csv", "png"));
    }

    private final int maximumRetryCount;
    private final Set<String> visitedUrls = new HashSet<>();
    private final Set<String> urlsToBeRetried = new HashSet<>();


    public WebCrawler() {
        this.maximumRetryCount = DEFAULT_MAX_RETRY_COUNT;
    }

    public WebCrawler(int maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
    }

    public void crawlUrl(String startingUrl) {
        long startTime = System.currentTimeMillis();
        crawl(startingUrl);
        retryTimedOutRequests();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        printExecutionTime(elapsedTime);
    }

    private void crawl(String url) {
        try {
            validateUrl(url);
            Elements links = getAllLinksFromHTMLDoc(url);
            Set<String> foundUrls = getURLSFromLinks(url, links);
            markAsVisitedUrl(url);
            printURLsVisitedWithLinks(url, foundUrls);
            visitFoundUrls(foundUrls);
        } catch (SocketTimeoutException | ConnectException e) {
            // add socket/connection timeout urls to be retried
            urlsToBeRetried.add(url);
        } catch (Exception e) {
            // ignore: invalid url exception ,io exceptions, http exceptions etc
        }

    }

    private void validateUrl(String url) {
        if (hasBeenVisitedAlready(url) || isRedundant(url) || hasInvalidContentType(url)) {
            throw new InvalidUrlException("URL not valid for crawling");
        }
    }

    private Elements getAllLinksFromHTMLDoc(String url) throws IOException {
        var connection = Jsoup.connect(url);
        var htmlDocument = connection
                .sslSocketFactory(CustomSSLSocketFactory.getSocketFactory())
                .get();
        return htmlDocument.select(CSS_GET_LINKS_QUERY);
    }

    private Set<String> getURLSFromLinks(String url, Elements links) {
        Set<String> foundUrls = new HashSet<>();
        for (Element link : links) {
            String absoluteUrl = getAbsoluteUrl(link);
            if (isValidDomainMatch(absoluteUrl, url)) {
                foundUrls.add(absoluteUrl);
            }
        }
        return foundUrls;
    }

    private String getAbsoluteUrl(Element link) {
        return link.absUrl("href");
    }

    private boolean hasBeenVisitedAlready(String url) {
        return visitedUrls.contains(url) || visitedUrls.contains(url + "/");
    }

    private boolean isRedundant(String url) {
        return url.contains("#");
    }

    private boolean hasInvalidContentType(String urlString) {
        String[] urlSplit = urlString.split("\\.");
        String endOfUrl = urlSplit[urlSplit.length - 1].replace("/","");
        return invalidContentTypes.contains(endOfUrl);
    }

    private boolean isValidDomainMatch(String foundUrl, String currentUrl) {
        String domainOne = getDomainFromUrl(foundUrl);
        String domainTwo = getDomainFromUrl(currentUrl);
        boolean domainIsNotNull = StringUtils.isNotBlank(domainOne) && StringUtils.isNotBlank(domainTwo);
        return domainIsNotNull && domainOne.equalsIgnoreCase(domainTwo);
    }

    private String getDomainFromUrl(String url) {
        String domain = null;
        try {
            var uri = new URI(url);
            domain = uri.getHost();
        } catch (Exception e) {
            // Invalid url
        }
        return domain;
    }

    private void markAsVisitedUrl(String url) {
        visitedUrls.add(url);
        urlsToBeRetried.remove(url);
    }

    private void retryTimedOutRequests() {
        if (maximumRetryCount == 0) return;
        var retryCount = 0;
        while (retryCount <= maximumRetryCount) {
            if (urlsToBeRetried.isEmpty()) break;
            visitFoundUrls(urlsToBeRetried);
            retryCount++;
        }
    }

    private void visitFoundUrls(Set<String> foundUrls) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NO_OF_FIXED_THREADS);
        try {
            for (String url : foundUrls) {
                executor.execute(() -> crawl(url));
            }
            executor.shutdown();
            while (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                // awaiting termination
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    private void printURLsVisitedWithLinks(String url, Set<String> links) {
        System.out.println("URL VISITED - " + url);
        System.out.println("Links Found: " + links);
        System.out.println();
    }

    private void printExecutionTime(long elapsedTime){
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("***************************************************************");
        System.out.println("TIME TAKEN TO EXECUTE THIS METHOD:" + elapsedTime);
        System.out.println("***************************************************************");
        System.out.println("TOTAL VISITED URLS: " + visitedUrls.size());
    }


}
