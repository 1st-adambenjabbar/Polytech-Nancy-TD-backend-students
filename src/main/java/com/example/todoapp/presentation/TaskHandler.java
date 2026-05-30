package com.example.todoapp.presentation;

import com.example.todoapp.dto.TaskCreateDto;
import com.example.todoapp.dto.TaskResponseDto;
import com.example.todoapp.dto.TaskUpdateDto;
import com.example.todoapp.dto.ValidationErrorDto;
import com.example.todoapp.service.TaskService;
import com.example.todoapp.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * Presentation layer: handles HTTP routing, DTO validation, and response building.
 */
public class TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskHandler.class);
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");

    private final TaskService taskService;

    public TaskHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Entry point for all /tasks requests.
     */
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();
        String query  = exchange.getRequestURI().getQuery();

        try {
            dispatch(exchange, method, path, query);
        } catch (Exception e) {
            log.error("Unexpected error: {} {}", method, path, e);
            try {
                sendResponse(exchange, 500, null);
            } catch (IOException ignored) {
                // best-effort: connection may already be closed
            }
        }
    }

    private void dispatch(HttpExchange exchange, String method, String path, String query) throws IOException {

        // GET /tasks
        if ("GET".equals(method) && "/tasks".equals(path)) {
            boolean todoOnly = nonNull(query) && query.contains("todo-only=true");
            log.info("GET /tasks todoOnly={}", todoOnly);
            List<TaskResponseDto> tasks = taskService.getAllTasks(todoOnly);
            if (tasks.isEmpty()) {
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
            }
            return;
        }

        // POST /tasks
        if ("POST".equals(method) && "/tasks".equals(path)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            TaskCreateDto dto;
            try {
                dto = JsonUtils.deserialize(body, TaskCreateDto.class);
            } catch (IOException e) {
                log.warn("Invalid JSON body for POST /tasks");
                sendResponse(exchange, 400, null);
                return;
            }

            List<ValidationErrorDto> errors = validateCreateDto(dto);
            if (!errors.isEmpty()) {
                sendResponse(exchange, 400, JsonUtils.serialize(errors));
                return;
            }

            log.info("POST /tasks title={}", dto.getTitle());
            TaskResponseDto created = taskService.createTask(dto);
            exchange.getResponseHeaders().add("Location", "/tasks/" + created.getId());
            sendResponse(exchange, 201, JsonUtils.serialize(created));
            return;
        }

        Matcher m = ID_PATH.matcher(path);
        if (m.matches()) {
            int id = Integer.parseInt(m.group(1));

            // GET /tasks/{id}
            if ("GET".equals(method)) {
                log.info("GET /tasks/{}", id);
                Optional<TaskResponseDto> task = taskService.getTaskById(id);
                if (task.isPresent()) {
                    sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }

            // DELETE /tasks/{id}
            if ("DELETE".equals(method)) {
                log.info("DELETE /tasks/{}", id);
                boolean deleted = taskService.deleteTask(id);
                sendResponse(exchange, deleted ? 204 : 404, null);
                return;
            }

            // PUT /tasks/{id}
            if ("PUT".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                TaskUpdateDto dto;
                try {
                    dto = JsonUtils.deserialize(body, TaskUpdateDto.class);
                } catch (IOException e) {
                    log.warn("Invalid JSON body for PUT /tasks/{}", id);
                    sendResponse(exchange, 400, null);
                    return;
                }

                List<ValidationErrorDto> errors = validateUpdateDto(dto);
                if (!errors.isEmpty()) {
                    sendResponse(exchange, 400, JsonUtils.serialize(errors));
                    return;
                }

                log.info("PUT /tasks/{}", id);
                boolean updated = taskService.updateTask(id, dto);
                sendResponse(exchange, updated ? 204 : 404, null);
                return;
            }
        }

        sendResponse(exchange, 404, null);
    }

    private List<ValidationErrorDto> validateCreateDto(TaskCreateDto dto) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            errors.add(new ValidationErrorDto("title", "Le titre est obligatoire."));
        } else if (dto.getTitle().length() > 50) {
            errors.add(new ValidationErrorDto("title", "Le titre ne doit pas dépasser 50 caractères."));
        }
        if (dto.getDescription() != null && dto.getDescription().length() > 255) {
            errors.add(new ValidationErrorDto("description", "La description ne doit pas dépasser 255 caractères."));
        }
        return errors;
    }

    private List<ValidationErrorDto> validateUpdateDto(TaskUpdateDto dto) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            errors.add(new ValidationErrorDto("title", "Le titre est obligatoire."));
        } else if (dto.getTitle().length() > 50) {
            errors.add(new ValidationErrorDto("title", "Le titre ne doit pas dépasser 50 caractères."));
        }
        if (dto.getDescription() != null && dto.getDescription().length() > 255) {
            errors.add(new ValidationErrorDto("description", "La description ne doit pas dépasser 255 caractères."));
        }
        return errors;
    }

    private void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }
    }
}
