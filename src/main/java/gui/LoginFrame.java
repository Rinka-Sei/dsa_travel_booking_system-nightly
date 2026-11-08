package gui;

import database.DBConnection;
import model.User;
import service.PasswordUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.sql.*;

/**
 * Login and registration view for authentication in the Travel Booking System.
 * Supports role-based authentication (ADMIN and CUSTOMER) and integrates
 * with {@code ds.CustomerHashTable} via Java reflection for decoupled caching.
 */
public class LoginFrame extends JFrame implements ActionListener {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private Object userTableInstance = null;
    private Method hashTablePutMethod = null;
    private Method hashTableGetMethod = null;

    /**
     * Constructs and initializes the LoginFrame window and components.
     */
    public LoginFrame() {
        setTitle("Travel Booking System - Login");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        initHashTableReflection();
        loadUsersIntoHashTable();
        initUI();
    }

    /**
     * Initializes reflection handles for {@code ds.CustomerHashTable} if available.
     */
    private void initHashTableReflection() {
        try {
            Class<?> hashTableClass = Class.forName("ds.CustomerHashTable");
            userTableInstance = hashTableClass.getDeclaredConstructor(int.class).newInstance(20);
            hashTablePutMethod = hashTableClass.getMethod("put", String.class, User.class);
            hashTableGetMethod = hashTableClass.getMethod("get", String.class);
        } catch (Exception ignored) {
            // CustomerHashTable is optional / decoupled
        }
    }

    /**
     * Loads existing users from the MySQL database into the hash table cache via reflection.
     */
    private void loadUsersIntoHashTable() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                User u = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                if (userTableInstance != null && hashTablePutMethod != null) {
                    try {
                        hashTablePutMethod.invoke(userTableInstance, u.getUsername(), u);
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Connection Error: " + ex.getMessage());
        }
    }

    /**
     * Initializes absolute-positioned Swing UI elements.
     */
    private void initUI() {
        JLabel lblTitle = new JLabel("TRAVEL BOOKING SYSTEM LOGIN");
        lblTitle.setBounds(110, 30, 250, 30);
        add(lblTitle);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(80, 90, 80, 25);
        add(lblUser);

        txtUsername = new JTextField();
        txtUsername.setBounds(170, 90, 180, 25);
        add(txtUsername);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(80, 135, 80, 25);
        add(lblPass);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(170, 135, 180, 25);
        add(txtPassword);

        btnLogin = new JButton("Login");
        btnLogin.setBounds(100, 190, 100, 30);
        btnLogin.addActionListener(this);
        add(btnLogin);

        btnRegister = new JButton("Register");
        btnRegister.setBounds(220, 190, 100, 30);
        btnRegister.addActionListener(this);
        add(btnRegister);
    }

    /**
     * Handles user interaction events for login and registration buttons.
     *
     * @param e the ActionEvent triggered by clicking login or register
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLogin) {
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword()).trim();

            User u = null;
            if (userTableInstance != null && hashTableGetMethod != null) {
                try {
                    u = (User) hashTableGetMethod.invoke(userTableInstance, user);
                } catch (Exception ignored) {}
            }

            if (u == null) {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                    ps.setString(1, user);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        u = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                    }
                } catch (SQLException ignored) {}
            }

            if (u != null && PasswordUtil.verifyPassword(pass, u.getPassword())) {
                JOptionPane.showMessageDialog(this, "Login Successful! Welcome " + u.getUsername());
                MainFrame mainFrame = new MainFrame(u.getUsername(), u.getRole());
                mainFrame.setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
            }
        } else if (e.getSource() == btnRegister) {
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.");
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(pass);
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username, password, role) VALUES(?, ?, 'CUSTOMER')")) {
                ps.setString(1, user);
                ps.setString(2, hashedPassword);
                ps.executeUpdate();

                if (userTableInstance != null && hashTablePutMethod != null) {
                    try {
                        hashTablePutMethod.invoke(userTableInstance, user, new User(user, hashedPassword, "CUSTOMER"));
                    } catch (Exception ignored) {}
                }

                txtUsername.setText("");
                txtPassword.setText("");
                JOptionPane.showMessageDialog(this, "Customer Registered Successfully!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Registration failed or user exists: " + ex.getMessage());
            }
        }
    }
}
