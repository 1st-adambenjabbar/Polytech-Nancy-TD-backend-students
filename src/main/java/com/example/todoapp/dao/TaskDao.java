package com.example.todoapp.dao;

import com.example.todoapp.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Task}. Uses SQLite via JDBC.
 */
public class TaskDao {

    private static final Logger log = LoggerFactory.getLogger(TaskDao.class);
    private static final String DB_URL = "jdbc:sqlite:tasks.db";

    public TaskDao() {
        initializeDatabase();
    }

    /** Creates the tasks table if it does not already exist. */
    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        title       TEXT    NOT NULL,
                        description TEXT,
                        done        INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            log.info("SQLite database initialized (tasks.db)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Retrieve all tasks.
     *
     * @return list of all tasks
     */
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, title, description, done FROM tasks")) {
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve tasks", e);
        }
        return tasks;
    }

    /**
     * Retrieve tasks that are not yet done.
     *
     * @return list of incomplete tasks
     */
    public List<Task> findAllTodo() {
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, description, done FROM tasks WHERE done = 0")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve todo tasks", e);
        }
        return tasks;
    }

    /**
     * Find a task by its identifier.
     *
     * @param id task identifier
     * @return optional containing the task if found
     */
    public Optional<Task> findById(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, description, done FROM tasks WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find task id=" + id, e);
        }
        return Optional.empty();
    }

    /**
     * Insert a new task and return it with its generated id.
     *
     * @param task task to insert (id field is ignored)
     * @return inserted task with its generated id
     */
    public Task insert(Task task) {
        String sql = "INSERT INTO tasks (title, description, done) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, task.title());
            stmt.setString(2, task.description());
            stmt.setInt(3, task.done() ? 1 : 0);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    log.info("Task inserted id={}", generatedId);
                    return new Task(generatedId, task.title(), task.description(), task.done());
                }
            }
            throw new RuntimeException("No generated key returned after insert");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert task", e);
        }
    }

    /**
     * Update an existing task.
     *
     * @param task task with updated values (must carry the target id)
     * @return true if a row was updated, false if the id was not found
     */
    public boolean update(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, done = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, task.title());
            stmt.setString(2, task.description());
            stmt.setInt(3, task.done() ? 1 : 0);
            stmt.setInt(4, task.id());
            int rows = stmt.executeUpdate();
            log.info("Task update id={} found={}", task.id(), rows > 0);
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task id=" + task.id(), e);
        }
    }

    /**
     * Delete a task by its identifier.
     *
     * @param id task identifier
     * @return true if a row was deleted, false if the id was not found
     */
    public boolean delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            log.info("Task delete id={} found={}", id, rows > 0);
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task id=" + id, e);
        }
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("done") == 1
        );
    }
}
