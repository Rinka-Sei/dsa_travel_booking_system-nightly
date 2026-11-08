package service;

import database.DBConnection;
import model.User;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer coordinating User and Customer registration, authentication, and caching via {@code ds.CustomerHashTable}.
 * Mirrors the {@code CustomerRegistry} service architecture from the starter project blueprint.
 */
public class CustomerRegistryService {

    private final Object customerHashTable;
    private final Method putMethod;
    private final Method getMethod;

    /**
     * Constructs CustomerRegistryService with an initial hash table cache capacity.
     */
    public CustomerRegistryService() {
        Object table = null;
        Method put = null;
        Method get = null;
        try {
            Class<?> tableClass = Class.forName("ds.CustomerHashTable");
            table = tableClass.getDeclaredConstructor(int.class).newInstance(50);
            put = tableClass.getMethod("put", String.class, User.class);
            get = tableClass.getMethod("get", String.class);
        } catch (ReflectiveOperationException ignored) {
            // JDBC remains the source of truth when the optional DSA is unavailable.
        }
        customerHashTable = table;
        putMethod = put;
        getMethod = get;
    }

    /**
     * Seeds existing users from the MySQL database into the hash table cache.
     *
     * @throws SQLException if a database access error occurs
     */
    public void loadUsersIntoCache() throws SQLException {
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User u = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                cacheUser(u);
            }
        }
    }

    /**
     * Authenticates a user against the in-memory cache with database fallback.
     *
     * @param username user identifier
     * @param password user password
     * @return the authenticated {@link User} object, or {@code null} if credentials are invalid
     * @throws SQLException if a database access error occurs
     */
    public User authenticate(String username, String password) throws SQLException {
        User u = getCachedUser(username);
        if (u == null) {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        u = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                        cacheUser(u);
                    }
                }
            }
        }
        if (u != null && PasswordUtil.verifyPassword(password, u.getPassword())) {
            return u;
        }
        return null;
    }

    /**
     * Registers a new customer into both the database and the hash table cache.
     *
     * @param username customer username
     * @param password customer password
     * @throws SQLException if username already exists or insert fails
     */
    public void registerCustomer(String username, String password) throws SQLException {
        String hashedPassword = PasswordUtil.hashPassword(password);
        String sql = "INSERT INTO users(username, password, role) VALUES(?, ?, 'CUSTOMER')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.executeUpdate();
        }
        cacheUser(new User(username, hashedPassword, "CUSTOMER"));
    }

    /**
     * Retrieves all customer users from the database.
     *
     * @return list of customer accounts
     * @throws SQLException if a database access error occurs
     */
    public List<User> getAllCustomers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'CUSTOMER'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(rs.getString("username"), rs.getString("password"), rs.getString("role")));
            }
        }
        return list;
    }

    /**
     * Gets the backing customer hash table cache when available.
     *
     * @return reflected customer hash table instance, or {@code null}
     */
    public Object getCustomerHashTable() {
        return customerHashTable;
    }

    /**
     * Manually inserts a user into the in-memory reflection cache.
     * Enables isolated testing of the reflected cache without requiring database access.
     *
     * @param user the user to cache
     */
    public void cacheUser(User user) {
        if (user == null || customerHashTable == null || putMethod == null) {
            return;
        }
        try {
            putMethod.invoke(customerHashTable, user.getUsername(), user);
        } catch (ReflectiveOperationException ignored) {
            // A failed cache write does not invalidate the persisted user.
        }
    }

    /**
     * Retrieves a user directly from the in-memory reflection cache without touching the database.
     *
     * @param username the username to look up
     * @return the cached {@link User}, or {@code null} if absent or reflection is unavailable
     */
    public User getCachedUser(String username) {
        if (username == null || customerHashTable == null || getMethod == null) {
            return null;
        }
        try {
            return (User) getMethod.invoke(customerHashTable, username);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
