package com.example.todoapp;

import com.example.todoapp.dao.TaskDao;
import com.example.todoapp.presentation.TaskHandler;
import com.example.todoapp.service.TaskService;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Application entry point. Wires up the 3-layer architecture and starts the HTTP server.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        TaskDao taskDao = new TaskDao();
        TaskService taskService = new TaskService(taskDao);
        TaskHandler taskHandler = new TaskHandler(taskService);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", taskHandler::handle);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }
}
