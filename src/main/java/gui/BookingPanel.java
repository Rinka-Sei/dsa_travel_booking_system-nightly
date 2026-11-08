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
 * <p>
 * <b>Nightly build update:</b> Itinerary + Invoice unified into a single view with inline
 * export buttons; admin report dialog embeds .md/.csv export actions; cancel button moved
 * to the bottom-right corner; standalone toolbar invoice-export buttons removed (exports
 * now live inside the {@code View Details} dialog).
 */
public class BookingPanel extends JPanel implements ActionListener {

    private JTextField txtBookFlightId;
    private JTextField txtBookHotelId;
    private JTextField txtSearchBooking;
    private JButton btnBook;
    private JButton btnUpdate;
    private JButton btnViewDetails;
    private JButton btnCancel;
    private JButton btnFilter;
    private JButton btnResetFilter;
    private JButton btnReport;
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

        // Unified Itinerary + Invoice button
        btnViewDetails = new JButton("View Details");
        btnViewDetails.setBounds(568, 12, 120, 26);
        btnViewDetails.addActionListener(this);
        add(btnViewDetails);

        // Row 2: Search & Administrative Report Operations
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

        if (isAdmin) {
            btnReport = new JButton("System Report");
            btnReport.setBounds(450, 48, 125, 26);
            btnReport.addActionListener(this);
            add(btnReport);
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
        sp.setBounds(10, 85, 985, 460);
        add(sp);

        // Cancel button — bottom-right corner of the panel
        btnCancel = new JButton("Cancel Booking");
        btnCancel.setBounds(830, 552, 165, 30);
        btnCancel.addActionListener(this);
        add(btnCancel);

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
     * Handles action events for booking creation, details display, and cancellation.
     *
     * @param e the ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnBook) {
            performBooking();
        } else if (e.getSource() == btnUpdate) {
            updateSelectedBooking();
        } else if (e.getSource() == btnViewDetails) {
            showUnifiedItineraryInvoice();
        } else if (e.getSource() == btnFilter) {
            filterBookings(txtSearchBooking.getText().trim());
        } else if (e.getSource() == btnResetFilter) {
            txtSearchBooking.setText("");
            loadBookings();
        } else if (btnReport != null && e.getSource() == btnReport) {
            generateSystemReport();
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
     * Unified action that produces both the travel itinerary and invoice in one
     * dialog and offers inline .md/.csv export buttons within the same prompt.
     * Refactored from the separate {@code generateItinerary()} and
     * {@code generateInvoice()} methods to reduce GUI clutter.
     */
    private void showUnifiedItineraryInvoice() {
        int selectedRow = tblBookings.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table to view its details.");
            return;
        }

        int bookingId = Integer.parseInt(bookingModel.getValueAt(selectedRow, 0).toString());
        Object bId = bookingModel.getValueAt(selectedRow, 0);
        Object user = bookingModel.getValueAt(selectedRow, 1);
        Object fId = bookingModel.getValueAt(selectedRow, 2);
        Object hId = bookingModel.getValueAt(selectedRow, 3);
        Object total = bookingModel.getValueAt(selectedRow, 4);

        StringBuilder content = new StringBuilder();
        content.append("=== TRAVEL ITINERARY & INVOICE ===\n");
        content.append("Booking ID: ").append(bId).append("\n");
        content.append("Customer: ").append(user).append("\n\n");

        try (Connection conn = DBConnection.getConnection()) {
            if (fId != null) {
                PreparedStatement psF = conn.prepareStatement("SELECT * FROM flights WHERE flight_id = ?");
                psF.setString(1, fId.toString());
                ResultSet rsF = psF.executeQuery();
                if (rsF.next()) {
                    content.append("[FLIGHT DETAILS]\n")
                            .append("Flight ID: ").append(rsF.getString("flight_id")).append("\n")
                            .append("From: ").append(rsF.getString("departure")).append(" (").append(rsF.getString("departure_time")).append(" HRS)\n")
                            .append("To: ").append(rsF.getString("arrival")).append(" (").append(rsF.getString("arrival_time")).append(" HRS)\n")
                            .append("Price: $").append(String.format("%.2f", rsF.getDouble("price"))).append("\n\n");
                }
            } else {
                content.append("[FLIGHT DETAILS]: No flight booked.\n\n");
            }

            if (hId != null) {
                PreparedStatement psH = conn.prepareStatement("SELECT * FROM hotels WHERE hotel_id = ?");
                psH.setString(1, hId.toString());
                ResultSet rsH = psH.executeQuery();
                if (rsH.next()) {
                    content.append("[HOTEL DETAILS]\n")
                            .append("Hotel: ").append(rsH.getString("name")).append("\n")
                            .append("Location: ").append(rsH.getString("location")).append("\n")
                            .append("Rating: ").append(rsH.getDouble("rating")).append(" Stars\n")
                            .append("Price/Night: $").append(String.format("%.2f", rsH.getDouble("price_per_night"))).append("\n\n");
                }
            } else {
                content.append("[HOTEL DETAILS]: No hotel booked.\n\n");
            }

            content.append("------------------------------------\n");
            content.append("TOTAL AMOUNT: $").append(total).append("\n");
            content.append("====================================");

            // Build custom dialog so we can add inline .md/.csv export buttons
            final JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    "Itinerary & Invoice", true);
            dialog.setLayout(null);
            dialog.setSize(640, 460);
            dialog.setLocationRelativeTo(this);

            JTextArea area = new JTextArea(content.toString());
            area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            area.setEditable(false);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setBounds(10, 10, 610, 350);
            dialog.add(scroll);

            JButton dlgExportMd = new JButton("Export (.md)");
            dlgExportMd.setBounds(10, 375, 145, 30);
            dialog.add(dlgExportMd);

            JButton dlgExportCsv = new JButton("Export (.csv)");
            dlgExportCsv.setBounds(165, 375, 145, 30);
            dialog.add(dlgExportCsv);

            JButton dlgOk = new JButton("OK");
            dlgOk.setBounds(520, 375, 100, 30);
            dialog.add(dlgOk);

            final int finalBookingId = bookingId;
            dlgExportMd.addActionListener(ev -> {
                File outDir = getExportDirectory();
                File targetFile = new File(outDir, "invoice_" + finalBookingId + ".md");
                try {
                    bookingService.exportInvoiceMarkdown(finalBookingId, targetFile);
                    JOptionPane.showMessageDialog(dialog,
                            "Invoice exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Failed to export invoice: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            dlgExportCsv.addActionListener(ev -> {
                File outDir = getExportDirectory();
                File targetFile = new File(outDir, "invoice_" + finalBookingId + ".csv");
                try {
                    bookingService.exportInvoiceCsv(finalBookingId, targetFile);
                    JOptionPane.showMessageDialog(dialog,
                            "Invoice exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Failed to export invoice: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            dlgOk.addActionListener(ev -> dialog.dispose());

            dialog.setVisible(true);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error building itinerary: " + ex.getMessage());
        }
    }

        /**
     * Generates and displays an executive administrative activity and revenue report.
     * The dialog embeds .md and .csv export buttons on the bottom-left and a custom
     * OK button on the bottom-right corner of the panel (replacing the default
     * {@link JOptionPane} OK button so the layout remains fully absolute-positioned
     * and compliant with {@code setLayout(null)} rules).
     */
    private void generateSystemReport() {
        if (!"ADMIN".equals(parent.getCurrentRole())) {
            JOptionPane.showMessageDialog(this, "Access denied. Only administrators may generate system reports.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String reportText = bookingService.generateSystemSummaryReport();

            // Build a custom dialog so we control the OK button placement
            final JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    "System Activity & Revenue Report", true);
            dialog.setLayout(null);
            dialog.setSize(600, 460);
            dialog.setLocationRelativeTo(this);

            JTextArea txtReport = new JTextArea(reportText);
            txtReport.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            txtReport.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(txtReport);
            scrollPane.setBounds(10, 10, 560, 350);
            dialog.add(scrollPane);

            JButton dlgReportMd = new JButton("Export (.md)");
            dlgReportMd.setBounds(10, 375, 145, 30);
            dialog.add(dlgReportMd);

            JButton dlgReportCsv = new JButton("Export (.csv)");
            dlgReportCsv.setBounds(165, 375, 145, 30);
            dialog.add(dlgReportCsv);

            // OK button — bottom-right corner of the dialog pane
            JButton dlgOk = new JButton("OK");
            dlgOk.setBounds(465, 375, 105, 30);
            dialog.add(dlgOk);

            dlgReportMd.addActionListener(ev -> {
                File outDir = getExportDirectory();
                File targetFile = new File(outDir, "executive_report.md");
                try {
                    String md = "# Executive Report\n\n```\n" + reportText + "\n```\n";
                    try (java.io.FileWriter fw = new java.io.FileWriter(targetFile)) {
                        fw.write(md);
                    }
                    JOptionPane.showMessageDialog(dialog,
                            "Executive report exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                            "Report Exported", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Failed to export executive report: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            dlgReportCsv.addActionListener(ev -> {
                File outDir = getExportDirectory();
                File targetFile = new File(outDir, "executive_report.csv");
                try {
                    bookingService.exportExecutiveReportCsv(targetFile);
                    JOptionPane.showMessageDialog(dialog,
                            "Executive report exported successfully!\n\nFile: " + targetFile.getAbsolutePath(),
                            "Report Exported", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Failed to export executive report: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            dlgOk.addActionListener(ev -> dialog.dispose());

            dialog.setVisible(true);
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