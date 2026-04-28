package com.example.todoapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

/**
 * Main class of the application. Managing routing and HTTP layer.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private static final TaskDao dao = new TaskDao();

    public static void main(String[] args) throws Exception {
        log.info("In-memory repository initialised");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", Application::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }

    private static void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        //region Manage GET /tasks (with optional todo-only filter)
        if ("GET".equals(method) && "/tasks".equals(path)) {
            Collection<Task> tasks = dao.findAll();
            
            if (nonNull(query) && query.contains("todo-only=true")) {
                tasks = tasks.stream()
                        .filter(t -> !t.done())
                        .collect(Collectors.toList());
            }

            if (tasks.isEmpty()) {
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
            }
            return;
        }
        //endregion

        //region Manage POST /tasks
        if ("POST".equals(method) && "/tasks".equals(path)) {
            Task input = JsonUtils.deserialize(new String(exchange.getRequestBody().readAllBytes(), UTF_8), Task.class);
            Task createdTask = dao.save(input);

            exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
            sendResponse(exchange, 201, JsonUtils.serialize(createdTask));
            return;
        }
        //endregion

        //region Endpoints with {id}
        Matcher m = ID_PATH.matcher(path);
        if (m.matches()) {
            int id = Integer.parseInt(m.group(1));

            // GET /tasks/{id}
            if ("GET".equals(method)) {
                Optional<Task> task = dao.findById(id);
                if (task.isPresent()) {
                    sendResponse(exchange, 200, JsonUtils.serialize(task.get()));
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }

            // DELETE /tasks/{id}
            if ("DELETE".equals(method)) {
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }

            // PUT /tasks/{id}
            if ("PUT".equals(method)) {
                Optional<Task> existing = dao.findById(id);
                if (existing.isPresent()) {
                    Task input = JsonUtils.deserialize(new String(exchange.getRequestBody().readAllBytes(), UTF_8), Task.class);
                    // Ensure ID from path is used
                    Task updated = new Task(id, input.title(), input.description(), input.done());
                    dao.save(updated);
                    sendResponse(exchange, 204, null);
                } else {
                    sendResponse(exchange, 404, null);
                }
                return;
            }
        }
        //endregion

        // Sinon → 404
        sendResponse(exchange, 404, null);
    }

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if(nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, -1); // -1 for no body
            exchange.close();
        }
    }
}
