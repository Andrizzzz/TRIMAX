package com.example.trimax;

import androidx.annotation.NonNull;

public class SearchResultItem {

    private final String shortName;
    private final String fullAddress;
    private final double latitude;
    private final double longitude;
    private final String placeType;

    public SearchResultItem(String shortName, String fullAddress, double latitude, double longitude, String placeType) {
        this.shortName = shortName != null ? shortName : "Location";
        this.fullAddress = fullAddress != null ? fullAddress : "San Jose del Monte, Bulacan";
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeType = placeType != null ? placeType : "place";
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPlaceType() {
        return placeType;
    }

    @NonNull
    @Override
    public String toString() {
        return shortName + " - " + fullAddress;
    }
}
