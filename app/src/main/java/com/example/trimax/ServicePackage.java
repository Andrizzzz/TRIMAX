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
    private int quantity = 0;
    /** Product picture address (server products only); null shows the icon in imageResId. */
    private String imageUrl;

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
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    /** A real product from the TRIMAX server (id "product:<number>"), not a built-in sample. */
    public boolean isServerProduct() { return id != null && id.startsWith("product:"); }
}
