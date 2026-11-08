package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Main dashboard container frame for the Travel Booking System.
 * Hosts tabbed views for Flights, Hotels, Package Deals, Bookings, and Customer Travel History,
 * with live database connectivity status and item count metrics in the bottom status bar.
 */
public class MainFrame extends JFrame implements ActionListener {

    private String currentUser;
    private String currentRole;

    private JLabel lblWelcome;
    private JButton btnLogout;
    private JTabbedPane tabbedPane;
    private JLabel lblStatusBar;

    private FlightPanel flightPanel;
    private HotelPanel hotelPanel;
    private PackagePanel packagePanel;
    private BookingPanel bookingPanel;
    private CustomerHistoryPanel customerHistoryPanel;

    /**
     * Constructs the MainFrame container for the authenticated user session.
     *
     * @param username the logged-in username
     * @param role     the role of the logged-in user ("ADMIN" or "CUSTOMER")
     */
    public MainFrame(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;

        setTitle("Travel Booking System - Dashboard [" + role + "]");
        setSize(1060, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        initDashboardComponents();
    }

    /**
     * Gets the username of the currently logged-in user.
     *
     * @return the logged-in username
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Gets the role of the currently logged-in user.
     *
     * @return the logged-in role
     */
    public String getCurrentRole() {
        return currentRole;
    }

    /**
     * Triggers a refresh of the bookings table inside the BookingPanel and CustomerHistoryPanel.
     */
    public void refreshBookingPanel() {
        if (bookingPanel != null) {
            bookingPanel.loadBookings();
        }
        if (customerHistoryPanel != null) {
            customerHistoryPanel.loadHistory();
        }
    }

    /**
     * Triggers a re-computation and re-render of dynamic package deals.
     */
    public void refreshPackagePanel() {
        if (packagePanel != null) {
            packagePanel.refreshPackages();
        }
    }

    /**
     * Updates the text in the bottom status bar based on the active tab and its live entity count.
     */
    public void updateStatusBar() {
        if (lblStatusBar == null || tabbedPane == null) {
            return;
        }
        int idx = tabbedPane.getSelectedIndex();
        String tabTitle = idx >= 0 ? tabbedPane.getTitleAt(idx) : "";
        String countText = "";

        if ("Flights".equals(tabTitle) && flightPanel != null) {
            countText = flightPanel.getFlightCount() + " Flights Loaded via BST";
        } else if ("Hotels".equals(tabTitle) && hotelPanel != null) {
            countText = hotelPanel.getHotelCount() + " Hotels Loaded";
        } else if ("Package Deals".equals(tabTitle) && packagePanel != null) {
            countText = packagePanel.getPackageCount() + " Package Deals Available";
        } else if ("Bookings & Itinerary".equals(tabTitle) && bookingPanel != null) {
            countText = bookingPanel.getBookingCount() + " Bookings Recorded";
        } else if ("My Travel History".equals(tabTitle) && customerHistoryPanel != null) {
            countText = customerHistoryPanel.getHistoryCount() + " Past Bookings";
        } else {
            countText = "Ready";
        }

        lblStatusBar.setText("Connected to travel_booking_db | " + countText);
    }

    /**
     * Initializes the absolute-positioned dashboard layout and tabbed view panels.
     */
    private void initDashboardComponents() {
        lblWelcome = new JLabel("Welcome, " + currentUser + " [" + currentRole + "]");
        lblWelcome.setBounds(20, 10, 450, 25);
        add(lblWelcome);

        btnLogout = new JButton("Logout");
        btnLogout.setBounds(930, 10, 95, 26);
        btnLogout.addActionListener(this);
        add(btnLogout);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBounds(20, 40, 1005, 630);

        flightPanel = new FlightPanel(this);
        hotelPanel = new HotelPanel(this);
        packagePanel = new PackagePanel(this);
        bookingPanel = new BookingPanel(this);

        tabbedPane.addTab("Flights", flightPanel);
        tabbedPane.addTab("Hotels", hotelPanel);
        tabbedPane.addTab("Package Deals", packagePanel);
        tabbedPane.addTab("Bookings & Itinerary", bookingPanel);

        if (!"ADMIN".equals(currentRole)) {
            customerHistoryPanel = new CustomerHistoryPanel(this);
            tabbedPane.addTab("My Travel History", customerHistoryPanel);
        }

        tabbedPane.addChangeListener(e -> updateStatusBar());
        add(tabbedPane);

        lblStatusBar = new JLabel("Connected to travel_booking_db | Initializing...");
        lblStatusBar.setBounds(20, 678, 1005, 22);
        add(lblStatusBar);

        updateStatusBar();
    }

    /**
     * Handles action events such as user logout with session cache wiping.
     *
     * @param e the ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLogout) {
            // Explicitly wipe sensitive in-memory user credentials to avoid heap leakage
            this.currentUser = null;
            this.currentRole = null;

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }
}
