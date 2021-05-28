package com.thefisola.webcrawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test Web Crawler")
class WebCrawlerTest {

    @Test
    void crawlUrl() {
        String baseUrl = "https://monzo.com/";
        WebCrawler webCrawler = new WebCrawler();
        webCrawler.crawlUrl(baseUrl);
        List<String> visitedUrls = new ArrayList<>(webCrawler.getVisitedUrls());
        assertTrue(visitedUrls.contains("https://monzo.com/"));
        assertTrue(visitedUrls.size() > 1000);
        // check if crawled urls starts with base url
        assertTrue(visitedUrls.get(WebCrawlerTestUtils.getRandomNumber(visitedUrls.size())).startsWith(baseUrl));
        assertTrue(visitedUrls.get(WebCrawlerTestUtils.getRandomNumber(visitedUrls.size())).startsWith(baseUrl));
        assertTrue(visitedUrls.get(WebCrawlerTestUtils.getRandomNumber(visitedUrls.size())).startsWith(baseUrl));


    }
}