package com.example.trimax;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NominatimSearchService {

    private static final String TAG = "TRIMAX_NOMINATIM";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "TRIMAX-DeliveryApp/1.0 (Android; com.example.trimax; contact: support@trimax.com)";

    public interface SearchCallback {
        void onSearchResults(List<SearchResultItem> results, String query);
        void onError(String errorMessage);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());

    private Runnable pendingDebounceRunnable;
    private Future<?> activeNetworkTask;
    private long currentQueryId = 0;

    public void searchLocation(String query, SearchCallback callback) {
        if (query == null || query.trim().length() < 2) {
            if (callback != null) {
                callback.onSearchResults(new ArrayList<>(), query);
            }
            return;
        }

        final String cleanQuery = query.trim();

        // Cancel previous pending debounce runnable
        if (pendingDebounceRunnable != null) {
            debounceHandler.removeCallbacks(pendingDebounceRunnable);
        }

        // Cancel previous running network task if active
        if (activeNetworkTask != null && !activeNetworkTask.isDone()) {
            activeNetworkTask.cancel(true);
        }

        // Debounce delay of 500ms to adhere to Nominatim rate limits (max 1 req/sec)
        pendingDebounceRunnable = () -> {
            currentQueryId++;
            final long queryId = currentQueryId;

            activeNetworkTask = executorService.submit(() -> {
                try {
                    String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.name());
                    String requestUrlStr = NOMINATIM_BASE_URL + "?q=" + encodedQuery
                            + "&format=json&addressdetails=1&limit=10&countrycodes=ph";

                    URL url = new URL(requestUrlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", USER_AGENT);
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        JSONArray jsonArray = new JSONArray(response.toString());
                        List<SearchResultItem> results = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            double lat = Double.parseDouble(obj.getString("lat"));
                            double lon = Double.parseDouble(obj.getString("lon"));
                            String displayName = obj.optString("display_name", cleanQuery);
                            String placeType = obj.optString("type", "location");

                            JSONObject addressObj = obj.optJSONObject("address");
                            String shortName = extractShortName(obj, addressObj, cleanQuery);
                            String fullAddress = formatFullAddress(displayName, addressObj);

                            results.add(new SearchResultItem(shortName, fullAddress, lat, lon, placeType));
                        }

                        mainHandler.post(() -> {
                            if (queryId == currentQueryId && callback != null) {
                                callback.onSearchResults(results, cleanQuery);
                            }
                        });

                    } else {
                        mainHandler.post(() -> {
                            if (queryId == currentQueryId && callback != null) {
                                callback.onError("Search service returned code " + responseCode);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Nominatim search error: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        if (queryId == currentQueryId && callback != null) {
                            callback.onError("Unable to connect to location search. Check network connection.");
                        }
                    });
                }
            });
        };

        debounceHandler.postDelayed(pendingDebounceRunnable, 500);
    }

    private String extractShortName(JSONObject itemObj, JSONObject addressObj, String defaultQuery) {
        if (addressObj != null) {
            String name = addressObj.optString("amenity",
                          addressObj.optString("building",
                          addressObj.optString("shop",
                          addressObj.optString("tourism",
                          addressObj.optString("road",
                          addressObj.optString("suburb",
                          addressObj.optString("neighbourhood",
                          addressObj.optString("village",
                          addressObj.optString("town",
                          addressObj.optString("city", ""))))))))));
            if (!name.isEmpty()) {
                return name;
            }
        }
        String displayName = itemObj.optString("display_name", defaultQuery);
        String[] parts = displayName.split(",");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        return defaultQuery;
    }

    private String formatFullAddress(String displayName, JSONObject addressObj) {
        if (addressObj != null) {
            StringBuilder sb = new StringBuilder();
            String road = addressObj.optString("road", "");
            String suburb = addressObj.optString("suburb", addressObj.optString("neighbourhood", ""));
            String city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("municipality", "")));
            String state = addressObj.optString("state", addressObj.optString("province", ""));

            if (!road.isEmpty()) sb.append(road);
            if (!suburb.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(suburb);
            }
            if (!city.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(city);
            }
            if (!state.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(state);
            }

            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return displayName;
    }

    public void cancelPendingSearches() {
        if (pendingDebounceRunnable != null) {
            debounceHandler.removeCallbacks(pendingDebounceRunnable);
        }
        if (activeNetworkTask != null && !activeNetworkTask.isDone()) {
            activeNetworkTask.cancel(true);
        }
    }
}
