package org.osproject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Main extends Thread {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter query in comma separated list: ");
        String queryArray = scanner.nextLine();
        if (queryArray.isEmpty()) {
            System.out.println("Query cannot be empty.");
            return;
        }

        String[] queries = queryArray.split(",");
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<Future<?>> futures = new ArrayList<>();

        for (String query : queries) {
            Future<?> future = executorService.submit(() -> {
                try {
                    String threadName = Thread.currentThread().getName();
                    parseResults(query.strip(), threadName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
    }

    public static void parseResults(String query, String threadName) throws IOException {
        String resultUrl = getResultURL(query, threadName);
        if (resultUrl == null) {
            System.out.println(threadName + ": No results found.");
            return;
        }
        System.out.println(threadName + ": Result url: " + resultUrl);

        HashMap<String, List<String>> metrics = getMetrics(resultUrl, threadName);
        System.out.println("\n\n" + threadName + ": Weather details for " + query);
        for (Map.Entry<String, List<String>> metric : metrics.entrySet()) {
            System.out.print(metric.getKey() + ": ");
            List<String> details = metric.getValue();
            System.out.print(details.get(0) + " " + details.get(1));
            if (details.size() > 2) {
                System.out.print(" " + details.get(2));
            }
            System.out.print("\n");
        }
    }

    public static Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .header("Host", "www.theweathernetwork.com")
                .referrer("https://www.theweathernetwork.com/us/")
                .get();
    }

    public static Document getDocumentWithJS(String url, String threadName) {
        System.out.println("\n" + threadName + ": Loading result details...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);
        driver.get(url);
        Document doc = Jsoup.parse(driver.getPageSource());
        driver.quit();
        return doc;
    }

    public static String getResultURL(String query, String threadName) throws IOException {
        String queryUrl = "https://www.theweathernetwork.com/us/search?q=" + query.replace(" ", "+") + "&lat=&lon=";
        System.out.println("\n" + threadName+ ": Query url: " + queryUrl + "\nLoading query results...");
        Document doc = getDocument(queryUrl);
        Elements results = doc.select("li.result ");

        String resultUrl = null;
        for (Element result : results) {
            if (result == null) continue;
            if (result.text().toLowerCase().contains(query.toLowerCase())) {
                Element resultElement = result.select("a[href]").first();
                if (resultElement != null) {
                    resultUrl = "https://www.theweathernetwork.com" + resultElement.attr("href");
                    if (resultUrl.contains("airport")) {
                        resultUrl = null;
                        continue;
                    }
                    break;
                }
            }
        }
        return resultUrl;
    }

    public static HashMap<String, List<String>> getMetrics(String resultUrl, String threadName) {
        Document doc = getDocumentWithJS(resultUrl, threadName);
        HashMap<String, List<String>> metricsText = new HashMap<>();
        Element temp = doc.select("span.temp").first();
        String temperature = "";
        if (temp != null)
            temperature = temp.text();
        Element unitWrap = doc.select("div.unitwrap").first();
        String tempUnit = "";
        if (unitWrap != null)
            tempUnit = unitWrap.text();
        metricsText.put("Temperature", List.of(temperature, tempUnit));

        Elements metrics = doc.select("div.detailed-metrics");
        for (Element metric : metrics) {
            if (metric == null) continue;

            Element label = metric.select("span.label").first();
            if (label == null) continue;
            String labelText = label.text();

            Element value = metric.select("span.value").first();
            if (value == null) continue;
            String valueText = value.text();

            Element unit = metric.select("span.metric").first();
            String unitText = "";
            if (unit != null)
                unitText = unit.text();

            Element vector = metric.select("span.vector").first();
            String vectorText = "";
            if (vector != null)
                vectorText = vector.text();

            metricsText.put(labelText, List.of(valueText, unitText, vectorText));
        }
        return metricsText;
    }
}