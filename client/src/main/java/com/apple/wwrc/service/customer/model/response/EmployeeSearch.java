package com.apple.wwrc.service.customer.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "page", "limit", "employees" })
public class EmployeeSearch implements Serializable {
    private static final long serialVersionUID = -4186899895608947025L;
    @JsonProperty("page")
    private int page;
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("employees")
    private List<DSUser> employees;

    public EmployeeSearch() { }

    public EmployeeSearch(int page, int limit, List<DSUser> users) {
        this.page = page;
        this.limit = limit;
        this.employees = users;
    }

    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public List<DSUser> getEmployees() { return employees; }
    public void setPage(int page) { this.page = page; }
    public void setLimit(int limit) { this.limit = limit; }
    public void setEmployees(List<DSUser> employees) { this.employees = employees; }
}
