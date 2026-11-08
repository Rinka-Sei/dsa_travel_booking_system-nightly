package gui;

import database.DBConnection;
import model.PackageDeal;
import service.BookingManagementService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.*;

/**
 * Panel view for managing customer bookings, itineraries, and printable invoices.
 * Supports booking creation with destination matching verification, cancellation, itinerary summaries,
 * search filtering, and invoice and executive report file exports (.md and .csv).
 */
public class BookingPanel extends JPanel implements ActionListener {

    private JTextField txtBookFlightId;
    private JTextField txtBookHotelId;
    private JTextField txtSearchBooking;
    private JButton btnBook;
    private JButton btnUpdate;
    private JButton btnItinerary;
    private JButton btnInvoice;
    private JButton btnCancel;
    private JButton btnFilter;
    private JButton btnResetFilter;
    private JButton btnExportMd;
    private JButton btnExportCsv;
    private JButton btnReport;
    private JButton btnExportReportCsv;
    private JTable tblBookings;
    private DefaultTableModel bookingModel;
    private MainFrame parent;
    private final BookingManagementService bookingService = new BookingManagementService();

    /**
     * Constructs the BookingPanel view and initializes its controls and table.
     *
     * @param parent the parent {@link MainFrame} dashboard container
     */
    public BookingPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(null);
        setBounds(0, 0, 1005, 595);

        boolean isAdmin = "ADMIN".equals(parent.getCurrentRole());

        // Row 1: Booking Management Operations
        JLabel lblFlightId = new JLabel("Flight ID:");
        lblFlightId.setBounds(10, 12, 60, 25);
        add(lblFlightId);

        txtBookFlightId = new JTextField();
        txtBookFlightId.setBounds(72, 12, 75, 25);
        add(txtBookFlightId);

        JLabel lblHotelId = new JLabel("Hotel ID:");
        lblHotelId.setBounds(155, 12, 60, 25);
        add(lblHotelId);

        txtBookHotelId = new JTextField();
        txtBookHotelId.setBounds(217, 12, 75, 25);
        add(txtBookHotelId);

        btnBook = new JButton("Book Package");
        btnBook.setBounds(302, 12, 125, 26);
        btnBook.addActionListener(this);
        add(btnBook);

        btnUpdate = new JButton("Update Selected");
        btnUpdate.setBounds(435, 12, 125, 26);
        btnUpdate.addActionListener(this);
        add(btnUpdate);

        btnItinerary = new JButton("View Itinerary");
        btnItinerary.setBounds(568, 12, 120, 26);
        btnItinerary.addActionListener(this);
        add(btnItinerary);

        btnInvoice = new JButton("Quick Invoice");
        btnInvoice.setBounds(696, 12, 125, 26);
        btnInvoice.addActionListener(this);
        add(btnInvoice);

        btnCancel = new JButton("Cancel Booking");
        btnCancel.setBounds(830, 12, 130, 26);
        btnCancel.addActionListener(this);
        add(btnCancel);

        // Row 2: Search, Invoices & Administrative Export Operations
        JLabel lblSearch = new JLabel("Search Customer / Flight:");
        lblSearch.setBounds(10, 48, 155, 25);
        add(lblSearch);

        txtSearchBooking = new JTextField();
        txtSearchBooking.setBounds(168, 48, 120, 25);
        txtSearchBooking.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterBookings(txtSearchBooking.getText().trim());
            }
        });
        add(txtSearchBooking);

        btnFilter = new JButton("Filter");
        btnFilter.setBounds(295, 48, 70, 26);
        btnFilter.addActionListener(this);
        add(btnFilter);

        btnResetFilter = new JButton("Reset");
        btnResetFilter.setBounds(372, 48, 70, 26);
        btnResetFilter.addActionListener(this);
        add(btnResetFilter);

        btnExportMd = new JButton("Export (.md)");
        btnExportMd.setBounds(450, 48, 115, 26);
        btnExportMd.addActionListener(this);
        add(btnExportMd);

        btnExportCsv = new JButton("Export (.csv)");
        btnExportCsv.setBounds(572, 48, 115, 26);
        btnExportCsv.addActionListener(this);
        add(btnExportCsv);

        if (isAdmin) {
            btnReport = new JButton("System Report");
            btnReport.setBounds(695, 48, 125, 26);
            btnReport.addActionListener(this);
            add(btnReport);

            btnExportReportCsv = new JButton("Export Report (.csv)");
            btnExportReportCsv.setBounds(828, 48, 155, 26);
            btnExportReportCsv.addActionListener(this);
            add(btnExportReportCsv);
        }

        // Table
        bookingModel = new DefaultTableModel(new String[]{"Booking ID", "Customer", "Flight ID", "Hotel ID", "Total Price"}, 0);
        tblBookings = new JTable(bookingModel);
        tblBookings.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && tblBookings.getSelectedRow() != -1) {
                int row = tblBookings.getSelectedRow();
                setFieldValue(txtBookFlightId, bookingModel.getValueAt(row, 2));
                setFieldValue(txtBookHotelId, bookingModel.getValueAt(row, 3));
            }
        });
        JScrollPane sp = new JScrollPane(tblBookings);
        sp.setBounds(10, 85, 985, 495);
        add(sp);

        loadBookings();
    }

    /**
     * Gets the number of bookings currently loaded in the table model.
     *
     * @return current booking count
     */
    public int getBookingCount() {
        return bookingModel != null ? bookingModel.getRowCount() : 0;
    }

    /**
     * Handles action events for booking creation, itinerary display, invoice generation, exports, and cancellation.
     *
     * @param e the ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnBook) {
            performBooking();
        } else if (e.getSource() == btnUpdate) {
            updateSelectedBooking();
        } else if (e.getSource() == btnItinerary) {
            generateItinerary();
        } else if (e.getSource() == btnInvoice) {
            generateInvoice();
        } else if (e.getSource() == btnFilter) {
            filterBookings(txtSearchBooking.getText().trim());
        } else if (e.getSource() == btnResetFilter) {
            txtSearchBooking.setText("");
            loadBookings();
        } else if (e.getSource() == btnExportMd) {
            exportSelectedInvoice(true);
        } else if (e.getSource() == btnExportCsv) {
            exportSelectedInvoice(false);
        } else if (btnReport != null && e.getSource() == btnReport) {
            generateSystemReport();
        } else if (btnExportReportCsv != null && e.getSource() == btnExportReportCsv) {
            exportSystemReportCsv();
        } else if (e.getSource() == btnCancel) {
            cancelBooking();
        }
    }

    /**
     * Resolves the target directory on the user's Desktop or user home directory.
     *
     * @return file path to the output directory
     */
    private File getExportDirectory() {
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists() && desktop.isDirectory()) {
            return desktop;
        }
        return new File(System.getProperty("user.home"));
    }

    /**
     * Exports an invoice for the currently selected booking to a file (.md or .csv).
     *
     * @param isMarkdown {@code true} for .md format, {@code false} for .csv format
     */
    private void exportSelectedInvoice(boolean isMarkdown) {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table to export its invoice.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = Integer.parseInt(bookingModel.getValueAt(selectedRow, 0).toString());
        File outDir = getExportDirectory();
        String filename = "invoice_" + bookingId + (isMarkdown ? ".md" : ".csv");
        File targetFile = new File(outDir, filename);

        try {
            if (isMarkdown) {
                bookingService.exportInvoiceMarkdown(bookingId, targetFile);
            } else {
                bookingService.exportInvoiceCsv(bookingId, targetFile);
            }
            JOptionPane.showMessageDialog(this,
                    "Invoice exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export invoice: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Exports the executive summary report to a CSV file on the desktop.
     */
    private void exportSystemReportCsv() {
        if (!"ADMIN".equals(parent.getCurrentRole())) {
            JOptionPane.showMessageDialog(this, "Access denied. Only administrators may export executive reports.", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File outDir = getExportDirectory();
        File targetFile = new File(outDir, "executive_report.csv");
        try {
            bookingService.exportExecutiveReportCsv(targetFile);
            JOptionPane.showMessageDialog(this,
                    "Executive report exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                    "Report Exported",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export executive report: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates a new booking in the database based on the provided flight and hotel IDs.
     * Verifies that the flight arrival destination and hotel location match, displaying a warning if there is a mismatch.
     */
    private void performBooking() {
        String fId = txtBookFlightId.getText().trim();
        String hId = txtBookHotelId.getText().trim();

        if (fId.isEmpty() && hId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least a Flight ID or Hotel ID.");
            return;
        }

        double flightPrice = 0;
        double hotelPrice = 0;
        String flightArrival = null;
        String hotelLocation = null;

        try (Connection conn = DBConnection.getConnection()) {
            if (!fId.isEmpty()) {
                PreparedStatement ps = conn.prepareStatement("SELECT arrival, price FROM flights WHERE flight_id = ?");
                ps.setString(1, fId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    flightArrival = rs.getString("arrival");
                    flightPrice = rs.getDouble("price");
                } else {
                    JOptionPane.showMessageDialog(this, "Flight ID '" + fId + "' not found.", "Invalid Flight ID", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            if (!hId.isEmpty()) {
                PreparedStatement ps = conn.prepareStatement("SELECT location, price_per_night FROM hotels WHERE hotel_id = ?");
                ps.setString(1, hId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    hotelLocation = rs.getString("location");
                    hotelPrice = rs.getDouble("price_per_night");
                } else {
                    JOptionPane.showMessageDialog(this, "Hotel ID '" + hId + "' not found.", "Invalid Hotel ID", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            if (!fId.isEmpty() && !hId.isEmpty() && flightArrival != null && hotelLocation != null) {
                if (!PackageDeal.isDestinationMatch(flightArrival, hotelLocation)) {
                    JOptionPane.showMessageDialog(this,
                            "Destination mismatch! Flight arrival ('" + flightArrival + "') does not match hotel location ('" + hotelLocation + "').",
                            "Location Mismatch Warning",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            double total = flightPrice + hotelPrice;

            PreparedStatement psInsert = conn.prepareStatement("INSERT INTO bookings(username, flight_id, hotel_id, total_price) VALUES(?, ?, ?, ?)");
            psInsert.setString(1, parent.getCurrentUser());
            psInsert.setString(2, fId.isEmpty() ? null : fId);
            psInsert.setString(3, hId.isEmpty() ? null : hId);
            psInsert.setDouble(4, total);
            psInsert.executeUpdate();

            txtBookFlightId.setText("");
            txtBookHotelId.setText("");

            JOptionPane.showMessageDialog(this, "Booking Successful!");
            loadBookings();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Booking Failed: " + ex.getMessage());
        }
    }

    /**
     * Loads booking records from the database into the JTable.
     * Administrators see all user bookings, whereas customers see only their own bookings.
     */
    public void loadBookings() {
        bookingModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String role = parent.getCurrentRole();
            String user = parent.getCurrentUser();
            String query = "ADMIN".equals(role) ? "SELECT * FROM bookings" : "SELECT * FROM bookings WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            if (!"ADMIN".equals(role)) {
                ps.setString(1, user);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bookingModel.addRow(new Object[]{
                        rs.getInt("booking_id"),
                        rs.getString("username"),
                        rs.getString("flight_id"),
                        rs.getString("hotel_id"),
                        rs.getDouble("total_price")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + ex.getMessage());
        }

        if (parent != null) {
            parent.updateStatusBar();
        }
    }

    /**
     * Filters booking records by customer username or flight identifier.
     *
     * @param query the search substring
     */
    private void filterBookings(String query) {
        if (query == null || query.isBlank()) {
            loadBookings();
            return;
        }
        bookingModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String role = parent.getCurrentRole();
            String user = parent.getCurrentUser();
            String pattern = "%" + query.trim().toLowerCase() + "%";

            String sql;
            if ("ADMIN".equals(role)) {
                sql = "SELECT * FROM bookings WHERE LOWER(username) LIKE ? OR LOWER(IFNULL(flight_id, '')) LIKE ?";
            } else {
                sql = "SELECT * FROM bookings WHERE username = ? AND LOWER(IFNULL(flight_id, '')) LIKE ?";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if ("ADMIN".equals(role)) {
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                } else {
                    ps.setString(1, user);
                    ps.setString(2, pattern);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        bookingModel.addRow(new Object[]{
                                rs.getInt("booking_id"),
                                rs.getString("username"),
                                rs.getString("flight_id"),
                                rs.getString("hotel_id"),
                                rs.getDouble("total_price")
                        });
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Search error: " + ex.getMessage());
        }

        if (parent != null) {
            parent.updateStatusBar();
        }
    }

    /**
     * Updates the selected booking and recalculates its total from current
     * flight and hotel prices.
     */
    private void updateSelectedBooking() {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to update.");
            return;
        }

        String flightId = txtBookFlightId.getText().trim();
        String hotelId = txtBookHotelId.getText().trim();
        if (flightId.isEmpty() && hotelId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A booking must contain a flight or hotel.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            int bookingId = Integer.parseInt(bookingModel.getValueAt(selectedRow, 0).toString());
            double total = calculateBookingTotal(conn, flightId, hotelId);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE bookings SET flight_id = ?, hotel_id = ?, total_price = ? WHERE booking_id = ?")) {
                ps.setString(1, flightId.isEmpty() ? null : flightId);
                ps.setString(2, hotelId.isEmpty() ? null : hotelId);
                ps.setDouble(3, total);
                ps.setInt(4, bookingId);
                ps.executeUpdate();
            }
            loadBookings();
            JOptionPane.showMessageDialog(this, "Booking updated successfully.");
        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Error updating booking: " + ex.getMessage());
        }
    }

    private double calculateBookingTotal(Connection conn, String flightId, String hotelId) throws SQLException {
        double total = 0;
        String arrival = null;
        String location = null;
        if (!flightId.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT arrival, price FROM flights WHERE flight_id = ?")) {
                ps.setString(1, flightId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Flight ID '" + flightId + "' was not found.");
                    }
                    arrival = rs.getString("arrival");
                    total += rs.getDouble("price");
                }
            }
        }
        if (!hotelId.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT location, price_per_night FROM hotels WHERE hotel_id = ?")) {
                ps.setString(1, hotelId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Hotel ID '" + hotelId + "' was not found.");
                    }
                    location = rs.getString("location");
                    total += rs.getDouble("price_per_night");
                }
            }
        }
        if (arrival != null && location != null && !PackageDeal.isDestinationMatch(arrival, location)) {
            throw new SQLException("Flight arrival and hotel location do not match.");
        }
        return total;
    }

    private void setFieldValue(JTextField field, Object value) {
        field.setText(value == null ? "" : value.toString());
    }

    /**
     * Generates and displays a detailed travel itinerary for the selected booking entry.
     */
    private void generateItinerary() {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table to view its itinerary.");
            return;
        }

        Object bId = bookingModel.getValueAt(selectedRow, 0);
        Object fId = bookingModel.getValueAt(selectedRow, 2);
        Object hId = bookingModel.getValueAt(selectedRow, 3);

        StringBuilder itinerary = new StringBuilder("=== TRAVEL ITINERARY ===\n");
        itinerary.append("Booking ID: ").append(bId).append("\n\n");

        try (Connection conn = DBConnection.getConnection()) {
            if (fId != null) {
                PreparedStatement psF = conn.prepareStatement("SELECT * FROM flights WHERE flight_id = ?");
                psF.setString(1, fId.toString());
                ResultSet rsF = psF.executeQuery();
                if (rsF.next()) {
                    itinerary.append("[FLIGHT DETAILS]\n")
                            .append("Flight ID: ").append(rsF.getString("flight_id")).append("\n")
                            .append("From: ").append(rsF.getString("departure")).append(" (").append(rsF.getString("departure_time")).append(" HRS)\n")
                            .append("To: ").append(rsF.getString("arrival")).append(" (").append(rsF.getString("arrival_time")).append(" HRS)\n\n");
                }
            } else {
                itinerary.append("[FLIGHT DETAILS]: No flight booked.\n\n");
            }

            if (hId != null) {
                PreparedStatement psH = conn.prepareStatement("SELECT * FROM hotels WHERE hotel_id = ?");
                psH.setString(1, hId.toString());
                ResultSet rsH = psH.executeQuery();
                if (rsH.next()) {
                    itinerary.append("[HOTEL DETAILS]\n")
                            .append("Hotel: ").append(rsH.getString("name")).append("\n")
                            .append("Location: ").append(rsH.getString("location")).append("\n")
                            .append("Rating: ").append(rsH.getDouble("rating")).append(" Stars\n\n");
                }
            } else {
                itinerary.append("[HOTEL DETAILS]: No hotel booked.\n\n");
            }

            JOptionPane.showMessageDialog(this, itinerary.toString(), "Itinerary Summary", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error building itinerary: " + ex.getMessage());
        }
    }

    /**
     * Generates and displays a text invoice dialog for the selected booking entry.
     */
    private void generateInvoice() {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table.");
            return;
        }

        Object bId = bookingModel.getValueAt(selectedRow, 0);
        Object user = bookingModel.getValueAt(selectedRow, 1);
        Object fId = bookingModel.getValueAt(selectedRow, 2);
        Object hId = bookingModel.getValueAt(selectedRow, 3);
        Object total = bookingModel.getValueAt(selectedRow, 4);

        String invoice = "====================================\n"
                + "          TRAVEL INVOICE            \n"
                + "====================================\n"
                + "Booking ID: " + bId + "\n"
                + "Customer: " + user + "\n"
                + "Flight Booked: " + (fId == null ? "None" : fId) + "\n"
                + "Hotel Booked: " + (hId == null ? "None" : hId) + "\n"
                + "------------------------------------\n"
                + "TOTAL AMOUNT: $" + total + "\n"
                + "====================================";

        JOptionPane.showMessageDialog(this, invoice, "Invoice Details", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Generates and displays an executive administrative activity and revenue report.
     */
    private void generateSystemReport() {
        if (!"ADMIN".equals(parent.getCurrentRole())) {
            JOptionPane.showMessageDialog(this, "Access denied. Only administrators may generate system reports.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String reportText = bookingService.generateSystemSummaryReport();

            JTextArea txtReport = new JTextArea(reportText);
            txtReport.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            txtReport.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(txtReport);
            scrollPane.setPreferredSize(new java.awt.Dimension(500, 350));

            JOptionPane.showMessageDialog(this, scrollPane, "System Activity & Revenue Report", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage());
        }
    }

    /**
     * Cancels the selected booking record from the database upon user confirmation.
     */
    private void cancelBooking() {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel.");
            return;
        }

        int bookingId = (int) bookingModel.getValueAt(selectedRow, 0);
        String customer = bookingModel.getValueAt(selectedRow, 1).toString();
        Object fId = bookingModel.getValueAt(selectedRow, 2);
        Object hId = bookingModel.getValueAt(selectedRow, 3);
        Object total = bookingModel.getValueAt(selectedRow, 4);

        String confirmMsg = "Are you sure you want to cancel this booking?\n\n"
                + "Booking ID: " + bookingId + "\n"
                + "Customer: " + customer + "\n"
                + "Flight ID: " + (fId == null ? "None" : fId) + "\n"
                + "Hotel ID: " + (hId == null ? "None" : hId) + "\n"
                + "Total Price: $" + total;

        int choice = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm Booking Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM bookings WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Booking cancelled successfully.");
                loadBookings();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error cancelling booking: " + ex.getMessage());
            }
        }
    }
}
