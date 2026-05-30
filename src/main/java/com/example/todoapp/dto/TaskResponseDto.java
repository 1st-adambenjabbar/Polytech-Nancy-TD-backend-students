package com.example.todoapp.dto;

/**
 * DTO returned by all read endpoints (GET /tasks, GET /tasks/{id}).
 */
public class TaskResponseDto {

    private final Integer id;
    private final String title;
    private final String description;
    private final boolean done;

    public TaskResponseDto(Integer id, String title, String description, boolean done) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.done = done;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }
}
