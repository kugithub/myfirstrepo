package com.apple.wwrc.service.customer.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "DSID", "BadgeID", "FirstName", "LastName", "Email", "photo" })
public class DSUser {

    @JsonProperty("DSID")
    private String dsID;
    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("LastName")
    private String lastName;
    @JsonProperty("FullName")
    private String fullName;
    @JsonProperty("Email")
    private String email;
    @JsonProperty("BadgeID")
    private String badgeID;
    @JsonProperty("photo")
    private CustomerPhoto photo;
    @JsonProperty("employeeType")
    private int ptype;
    public DSUser() { }
    public DSUser(String dsID, String fName, String lName, String email, String badgeID, int pType) {
            this.dsID = dsID;
            this.firstName = fName;
            this.lastName = lName;
            this.fullName = fName + " " + lName;
            this.email = email;
            this.badgeID = badgeID;
            this.ptype = pType;
            this.photo = new CustomerPhoto();
    }

    public String getDsID() { return dsID; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getBadgeID() { return badgeID; }
    public CustomerPhoto getPhoto() { return this.photo; }
    public int getPtype() { return ptype; }

    public void setDsID(String dsID) { this.dsID = dsID; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setBadgeID(String badgeID) { this.badgeID = badgeID; }
    public void setPhoto(CustomerPhoto p) { this.photo = p; }
    public void setPtype(int ptype) { this.ptype = ptype; }
}
