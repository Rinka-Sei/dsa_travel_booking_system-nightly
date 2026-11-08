package gui;

import database.DBConnection;
import service.BookingManagementService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Read-only panel view displaying the authenticated customer's previous completed bookings
 * and cumulative travel expenditure summary.
 * <p>
 * <b>Nightly build update:</b> Itinerary and invoice views merged into one
 * {@code View Details} action with inline .md/.csv export buttons inside the
 * same dialog. Standalone export buttons moved off the toolbar to reduce clutter.
 */
public class CustomerHistoryPanel extends JPanel implements ActionListener {

    private JLabel lblHeader;
    private JLabel lblSummary;
    private JButton btnRefresh;
    private JButton btnViewDetails;
    private JTable tblHistory;
    private DefaultTableModel historyModel;
    private MainFrame parent;
    private final BookingManagementService bookingService = new BookingManagementService();

    /**
     * Constructs the CustomerHistoryPanel view.
     *
     * @param parent the parent {@link MainFrame} dashboard container
     */
    public CustomerHistoryPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(null);
        setBounds(0, 0, 1005, 595);

        lblHeader = new JLabel("Travel Booking History for: " + parent.getCurrentUser());
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 13f));
        lblHeader.setBounds(10, 12, 320, 25);
        add(lblHeader);

        lblSummary = new JLabel("Total Completed Bookings: 0 | Total Expenditure: $0.00");
        lblSummary.setBounds(330, 12, 350, 25);
        add(lblSummary);

        btnRefresh = new JButton("Refresh");
        btnRefresh.setBounds(685, 12, 90, 26);
        btnRefresh.addActionListener(this);
        add(btnRefresh);

        btnViewDetails = new JButton("View Details");
        btnViewDetails.setBounds(785, 12, 145, 26);
        btnViewDetails.addActionListener(this);
        add(btnViewDetails);

        historyModel = new DefaultTableModel(new String[]{"Booking ID", "Flight ID", "Flight Route", "Hotel ID", "Hotel Name", "Total Paid"}, 0);
        tblHistory = new JTable(historyModel);
        JScrollPane sp = new JScrollPane(tblHistory);
        sp.setBounds(10, 48, 985, 535);
        add(sp);

        loadHistory();
    }

    /**
     * Returns the count of historical bookings currently displayed.
     *
     * @return past booking count
     */
    public int getHistoryCount() {
        return historyModel != null ? historyModel.getRowCount() : 0;
    }

    /**
     * Loads the authenticated customer's booking records from the database and computes total expenditure.
     */
    public void loadHistory() {
        historyModel.setRowCount(0);
        int totalTrips = 0;
        double totalExpenditure = 0.0;

        String sql = "SELECT * FROM bookings WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parent.getCurrentUser());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalTrips++;
                    int bookingId = rs.getInt("booking_id");
                    String flightId = rs.getString("flight_id");
                    String hotelId = rs.getString("hotel_id");
                    double totalPrice = rs.getDouble("total_price");
                    totalExpenditure += totalPrice;

                    String flightRoute = "None";
                    if (flightId != null && !flightId.isBlank()) {
                        try (PreparedStatement psF = conn.prepareStatement("SELECT departure, arrival FROM flights WHERE flight_id = ?")) {
                            psF.setString(1, flightId);
                            try (ResultSet rsF = psF.executeQuery()) {
                                if (rsF.next()) {
                                    flightRoute = rsF.getString("departure") + " -> " + rsF.getString("arrival");
                                }
                            }
                        }
                    }

                    String hotelName = "None";
                    if (hotelId != null && !hotelId.isBlank()) {
                        try (PreparedStatement psH = conn.prepareStatement("SELECT name FROM hotels WHERE hotel_id = ?")) {
                            psH.setString(1, hotelId);
                            try (ResultSet rsH = psH.executeQuery()) {
                                if (rsH.next()) {
                                    hotelName = rsH.getString("name");
                                }
                            }
                        }
                    }

                    historyModel.addRow(new Object[]{
                            bookingId,
                            flightId != null ? flightId : "N/A",
                            flightRoute,
                            hotelId != null ? hotelId : "N/A",
                            hotelName,
                            String.format("$%.2f", totalPrice)
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading travel history: " + ex.getMessage());
        }

        lblSummary.setText("Total Completed Bookings: " + totalTrips + " | Total Expenditure: $" + String.format("%.2f", totalExpenditure));

        if (parent != null) {
            parent.updateStatusBar();
        }
    }

    /**
     * Handles refresh and details dialog interactions.
     *
     * @param e ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRefresh) {
            loadHistory();
        } else if (e.getSource() == btnViewDetails) {
            showUnifiedItineraryInvoice();
        }
    }

    /**
     * Unified action: displays both itinerary and invoice for the selected history row
     * inside a single modal dialog that embeds .md/.csv export buttons.
     */
    private void showUnifiedItineraryInvoice() {
        int selectedRow = tblHistory.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from your history to view its details.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = Integer.parseInt(historyModel.getValueAt(selectedRow, 0).toString());
        Object fId = historyModel.getValueAt(selectedRow, 1);
        Object fRoute = historyModel.getValueAt(selectedRow, 2);
        Object hId = historyModel.getValueAt(selectedRow, 3);
        Object hName = historyModel.getValueAt(selectedRow, 4);
        Object paid = historyModel.getValueAt(selectedRow, 5);

        StringBuilder content = new StringBuilder();
        content.append("=== TRAVEL ITINERARY & INVOICE ===\n");
        content.append("Booking ID: ").append(bookingId).append("\n");
        content.append("Customer: ").append(parent.getCurrentUser()).append("\n\n");
        content.append("[FLIGHT DETAILS]\n");
        content.append("Flight ID: ").append(fId).append("\n");
        content.append("Route: ").append(fRoute).append("\n\n");
        content.append("[HOTEL DETAILS]\n");
        content.append("Hotel ID: ").append(hId).append("\n");
        content.append("Hotel Name: ").append(hName).append("\n\n");
        content.append("------------------------------------\n");
        content.append("TOTAL PAID: ").append(paid).append("\n");
        content.append("====================================");

        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                "Itinerary & Invoice", true);
        dialog.setLayout(null);
        dialog.setSize(640, 460);
        dialog.setLocationRelativeTo(this);

        JTextArea area = new JTextArea(content.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
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

        dlgExportMd.addActionListener(ev -> {
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            File outDir = (desktop.exists() && desktop.isDirectory()) ? desktop : new File(System.getProperty("user.home"));
            File targetFile = new File(outDir, "invoice_" + bookingId + ".md");
            try {
                bookingService.exportInvoiceMarkdown(bookingId, targetFile);
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
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            File outDir = (desktop.exists() && desktop.isDirectory()) ? desktop : new File(System.getProperty("user.home"));
            File targetFile = new File(outDir, "invoice_" + bookingId + ".csv");
            try {
                bookingService.exportInvoiceCsv(bookingId, targetFile);
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
    }
}