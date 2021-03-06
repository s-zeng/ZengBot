import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Simon on 1/9/2017.
 * Based largely off of Google's searcher example class
 *
 * You must have a valid youtube api key in java/resources/youtube.properties in order for searching to work; see
 * https://developers.google.com/youtube/v3/getting-started for instructions on how to acquire one
 *
 * This file contains some structures designed to include support for pages of searching,
 * but that feature hasn't been implemented yet as of 2017-01-10
 *
 * For an example on how to use this searcher, please see SearcherTest.java in test/java/zergtel/core/searcher
 */
public class Searcher {
    private static final String PROPERTIES_FILENAME = "youtube.properties";
    private static YouTube youtube;
    public boolean isSelecting = false;
    public String[] results = new String[5];

    /**
     * Searches youtube for a given query
     *
     * @param query - Search query
     * @return An ArrayList of String-String maps, where each map is a single search result, and each map contains the
     * following keys:
     *   - title: contains title of video
     *   - url: contains embed url of video
     *   - thumbnail: contains url to standard resolution thumbnail of video
     *   - description: provides a truncated (by google, don't know the exact length) description
     *   - channel: name of the channel
     *   - datePublished: returns date published in ISO format without time (YYYY-MM-DD)
     */
    public ArrayList<Map<String, String>> search(String query) {
        Properties properties = getProperties(PROPERTIES_FILENAME);
        try {
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), request -> {
            }).setApplicationName("ZTVDC").build();

            YouTube.Search.List search = youtube.search().list("id,snippet");

            String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(apiKey);
            search.setQ(query);

            search.setType("video");

            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/publishedAt,snippet/description,snippet/channelTitle)");
            search.setMaxResults((long)5);

            SearchListResponse searchResponse = search.execute();

            return parse(searchResponse);
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.err.println("Search error - no results found");
        return null;
    }


    /**
     * Gets properties from .properties files
     *
     * @param fileName - path and name of properties file
     * @return A Properties object with all the properties found in the file
     */
    private Properties getProperties(String fileName) {
        Properties properties = new Properties();
        try {
            InputStream in = Searcher.class.getResourceAsStream("/" + fileName);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + fileName + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        return properties;
    }

    private ArrayList<Map<String, String>> parse(SearchListResponse response) {
        ResourceId rId;
        ArrayList<Map<String, String>> output = new ArrayList<>();

        int counter = 0;
        for (SearchResult result : response.getItems()) {
            SearchResultSnippet snippet = result.getSnippet();
            rId = result.getId();
            Map<String, String> element = new HashMap<>();
            element.put("title", snippet.getTitle());
            results[counter] = "http://www.youtube.com/watch?v=" + rId.getVideoId();
            element.put("url", results[counter++]);
            element.put("channel", snippet.getChannelTitle());
            output.add(element);
            System.out.println(element.toString());
        }
        return output;
    }


}