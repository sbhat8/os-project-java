package org.osproject;

//Necessary imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Main extends Thread {
    public static void main(String[] args) {
        // user input, receives string of semicolon separated locations, which is split into an array
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n--> Source of information: The Weather Network, URL: www.theweathernetwork.com\n");
        System.out.print("Enter cities in semicolon separated list: ");
        String queryArray = scanner.nextLine();
        scanner.close();
        if (queryArray.isEmpty()) {
            System.out.println("Query cannot be empty.");
            return;
        }
        String[] queries = queryArray.split(";");

        // handles thread counts and initializes executor service for thread creation
        // futures array list to store the data received from the crawler at end of thread
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        // for each query, create a new thread
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

        // once the thread is completely finished executing, the results are obtained
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // closes executor service
        executorService.shutdown();
    }// end of main
    
    // Used for String padding format
    // Use 2 for center align, 3 for right align, anything else for left align
    public static String formatString(int length, int align, String source) {
    	
    	String leftPadding = "";
    	String rightPadding = "";
    	int leftPad = 0;
    	int rightPad = 0;
    	
    	// return if length of the given value is bigger than the space
    	if (source.length() > length) {
    		return source;
    	}
    	// giving equal space on either side of the string for central align
    	if (align == 2) {
    		int padding = length - source.length();
    		leftPad = padding / 2;
    		rightPad = padding - leftPad;
    	} else {
    		if (align == 3) { // no padding on the right side for right align
    			leftPad = length - source.length();
    			rightPad = 0;
    		}
    		else { // no padding on the left side for the left align
    			leftPad = 0;
    			rightPad = length - source.length();
    		}
    	}
    	// padding the string 
    	for (int i = 0; i < leftPad; ++i)
    		leftPadding += " ";
    	for (int i = 0; i < rightPad; ++i)
    		rightPadding += " ";
    	return String.format("%s%s%s", leftPadding, source, rightPadding);

    }// end of formatString

    // Parsing and printing the final weather results in a beautiful manner
    public static void parseResults(String query, String threadName) throws IOException {
        String resultUrl = getResultURL(query, threadName);
                
        // print if no results are found for a city
        if (resultUrl == null) {
        	System.out.println("\n--+--+--+--+--+--+--\n");
            System.out.println(threadName + ": No results found for " + query.toUpperCase() + ".");
        	System.out.println("\n--+--+--+--+--+--+--\n");
            return;
        }
        System.out.println(threadName + ": Result url: " + resultUrl);

        HashMap<String, List<String>> metrics = getMetrics(resultUrl, threadName);
        System.out.println("\n\n" + threadName + ": Weather details for " + query.toUpperCase() + "\n");
        
        // Format the output to be center aligned in each column
        int col1 = 13;
        int col2 = 10;
        int col3 = 19;
        int align = 2;
        
        // Header
        System.out.println("+-------------+----------+-------------------+");
        System.out.format("|%s|%s|%s|\n",formatString(col1,align,"Attribute"), formatString(col2,align,"Value"),formatString(col3,align,"Extra Information"));
        // Formatting check
        //                  12345678901234567890123456789012345678901234567890
        //                  |  Attribute  |  Value   | Extra Information |
        System.out.println("+-------------+----------+-------------------+");

        // Iterating over all attributes
        for (Map.Entry<String, List<String>> metric : metrics.entrySet()) {
        	List<String> details = metric.getValue();
        	
        	// printing weather details
        	if (details.size() > 2) {
                System.out.format("|%s|%s|%s|\n",formatString(col1,align,metric.getKey()), formatString(col2,align,details.get(0)  + " " + details.get(1)),formatString(col3,align,details.get(2)));
        	} else {
                System.out.format("|%s|%s|%s|\n",formatString(col1,align,metric.getKey()), formatString(col2,align,details.get(0)  + " " + details.get(1)),formatString(col3,align,""));
        	}
        }// end of for loop
        
        System.out.println("+-------------+----------+-------------------+");

    }// end of parseResults

    // function to obtain document for a URL, uses Jsoup (which doesn't process JavaScript code)
    public static Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .header("Host", "www.theweathernetwork.com")
                .referrer("https://www.theweathernetwork.com/us/")
                .get();
    }// end of getDocument

    // function to obtain document for a URL, uses Selenium WebDriver (which does process JavaScript code)
    // Selenium WebDriver is required for the result page due to the contents being populated with JS code.
    @SuppressWarnings("deprecation")
    public static Document getDocumentWithJS(String url, String threadName) {
        System.out.println("\n" + threadName + ": Loading result details...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        WebDriver driver = new ChromeDriver(options);
        driver.get(url);
        Document doc = Jsoup.parse(driver.getPageSource());
        driver.quit();
        return doc;
    }// end of getDocumentWithJS

    // function to obtain the result URL from the search page
    // e.g. query="atlanta", queryUrl="https://www.theweathernetwork.com/us/search?q=atlanta&lat=&lon="
    public static String getResultURL(String query, String threadName) throws IOException {
        
        // builds query url with domain, search parameter (replaces spacing with '+')
        String queryUrl = "https://www.theweathernetwork.com/us/search?q=" + query.replace(" ", "+") + "&lat=&lon=";
        System.out.println("\n" + threadName + ": Search Page URL for " + query + ": " + queryUrl + "\nLoading query results...");

        // obtains document using Jsoup and searches for result elements ("li.result")
        // "li" represents the HTML list element on the page, and "result" is the CSS class that represents the result elements
        Document doc = getDocument(queryUrl);
        Elements results = doc.select("li.result ");

        // If the query contains "city, state" format, just match on the city
        String[] queryElements = query.split(",");
        String city = queryElements[0];
        
        // for each result, check if the result name contains the query
        // e.g. if a result is "Atlanta, Georgia" and the query is "atlanta", it will process that result
        String resultUrl = null;
        for (Element result : results) {
            if (result == null) continue;
            if (result.text().toLowerCase().contains(city.toLowerCase())) {
                // once a result fulfills the condition, obtain the URL for the result
                // "a" is the HTML element, "href" is an attribute on that element that contains the URL
                Element resultElement = result.select("a[href]").first();
                if (resultElement != null) {
                    // prepend the domain to the resulting URL and exit loop
                    resultUrl = "https://www.theweathernetwork.com" + resultElement.attr("href");
                    break;
                }
            }
        }
        return resultUrl;
    }// end of getResultURL

    // function to obtain the weather details from the result page
    // e.g. resultUrl="https://www.theweathernetwork.com/us/weather/georgia/atlanta"
    public static HashMap<String, List<String>> getMetrics(String resultUrl, String threadName) {
        // uses the getDocumentWithJS function to obtain page, as details are populated with JS code
        Document doc = getDocumentWithJS(resultUrl, threadName);

        // initializes map to store metric information, structure being {"metric label", ["value", "unit", "vector"]}
        // e.g. {"Wind", ["3", "mph", "NW"]}, so the wind speed is 3 miles per hour northwest
        // e.g. {"Temperature", ["55", "°F", null"]}, so the Temperature is 55 degrees Fahrenheit (no vector for temp)
        HashMap<String, List<String>> metricsText = new HashMap<>();

        // temperature is displayed separately from other metrics
        // "span" is the HTML element, "temp" is the CSS class that represents the temperature information
        Element temp = doc.select("span.temp").first();
        String temperature = "";
        if (temp != null)
            temperature = temp.text();
        // "div" is the HTML element, "unitwrap" is the CSS class that represents the unit information
        Element unitWrap = doc.select("div.unitwrap").first();
        String tempUnit = "";
        if (unitWrap != null)
            tempUnit = unitWrap.text();
        // adds temperature details to map, vector is missing, so it is null
        metricsText.put("Temperature", List.of(temperature, tempUnit));

        // obtains all the other metric details
        // "div" is the HTML element, "detailed-metrics" is the CSS class that represents the metric details
        Elements metrics = doc.select("div.detailed-metrics");
        for (Element metric : metrics) {
            if (metric == null) continue;

            // obtains the label for the metric
            // "span" is the HTML element, "label" is the CSS class that represents the label
            // this pattern continues for the value, unit, and vector details
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
    }// end of getMetrics
    
    
}// End of class Main
