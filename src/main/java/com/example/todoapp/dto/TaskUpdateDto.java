package com.example.todoapp.dto;

/**
 * DTO for PUT /tasks/{id} request body.
 */
public class TaskUpdateDto {

    private String title;
    private String description;
    private boolean done;

    public TaskUpdateDto() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
