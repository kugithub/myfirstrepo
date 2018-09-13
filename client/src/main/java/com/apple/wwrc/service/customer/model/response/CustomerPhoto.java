package com.apple.wwrc.service.customer.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"lowResImage", "lowResImageType", "highResImage", "highResImageType" })
public class CustomerPhoto implements Serializable {
    private static final long serialVersionUID = 388512293703028908L;

    @JsonProperty("LowRes")
    private String lowResPhoto;
    @JsonProperty("LowResImageType")
    private String lowResPhotoType;
    @JsonProperty("HighRes")
    private String highResPhoto;
    @JsonProperty("HighResImageType")
    private String highResPhotoType;

    public CustomerPhoto() {
        this.lowResPhoto = "";
        this.lowResPhotoType = "";
        this.highResPhoto = "";
        this.highResPhotoType = "";
    }

    public CustomerPhoto(String lowResPhoto, String lowResPhotoType, String highResPhoto, String highResPhotoType) {
        this.lowResPhoto = lowResPhoto;
        this.lowResPhotoType = lowResPhotoType;
        this.highResPhoto = highResPhoto;
        this.highResPhotoType = highResPhotoType;
    }

    public void setLowResPhoto(String lowResPhoto) { this.lowResPhoto = lowResPhoto; }
    public void setHighResPhoto(String highResPhoto) { this.highResPhoto = highResPhoto; }
    public void setLowResPhotoType(String lowResPhotoType) { this.lowResPhotoType = lowResPhotoType; }
    public void setHighResPhotoType(String highResPhotoType) { this.highResPhotoType = highResPhotoType; }

    public String getLowResPhoto() { return lowResPhoto; }
    public String getLowResPhotoType() { return lowResPhotoType; }
    public String getHighResPhoto() { return highResPhoto; }
    public String getHighResPhotoType() { return highResPhotoType; }
}
