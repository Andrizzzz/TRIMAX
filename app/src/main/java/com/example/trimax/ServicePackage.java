package com.example.trimax;

import java.io.Serializable;
import java.util.List;

public class ServicePackage implements Serializable {
    private String id;
    private String name;
    private String description;
    private double startingPrice;
    private int imageResId;
    private List<String> includedServices;
    private String maxWeight;
    private String maxDimensions;
    private String estimatedTime;

    public ServicePackage(String id, String name, String description, double startingPrice, int imageResId,
                          List<String> includedServices, String maxWeight, String maxDimensions, String estimatedTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.imageResId = imageResId;
        this.includedServices = includedServices;
        this.maxWeight = maxWeight;
        this.maxDimensions = maxDimensions;
        this.estimatedTime = estimatedTime;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public int getImageResId() { return imageResId; }
    public List<String> getIncludedServices() { return includedServices; }
    public String getMaxWeight() { return maxWeight; }
    public String getMaxDimensions() { return maxDimensions; }
    public String getEstimatedTime() { return estimatedTime; }
}
