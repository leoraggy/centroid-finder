package com.leoraggy.centroid_finder.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // Hides null fields in the final JSON output
public class JobStatusResponse {
    private String status;
    private String result;
    private String error;

    // Constructors
    public JobStatusResponse(String status) {
        this.status = status;
    }

    public JobStatusResponse(String status, String result, String error) {
        this.status = status;
        this.result = result;
        this.error = error;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}