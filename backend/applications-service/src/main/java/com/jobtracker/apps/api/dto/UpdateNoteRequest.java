package com.jobtracker.apps.api.dto;
//DTO for updating a note associated with a job application
public class UpdateNoteRequest {
    private String content;

    public UpdateNoteRequest() {
    }

    public UpdateNoteRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
