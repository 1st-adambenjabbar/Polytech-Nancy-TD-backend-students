package com.example.todoapp.model;

/**
 * Task domain model.
 *
 * @param id          task identifier (null before persistence)
 * @param title       task title
 * @param description task description
 * @param done        completion status
 */
public record Task(Integer id, String title, String description, boolean done) {
}
