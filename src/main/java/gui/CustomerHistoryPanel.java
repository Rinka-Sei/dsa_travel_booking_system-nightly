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
 */
public class CustomerHistoryPanel extends JPanel implements ActionListener {

    private JLabel lblHeader;
    private JLabel lblSummary;
    private JButton btnRefresh;
    private JButton btnExportMd;
    private JButton btnExportCsv;
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

        btnExportMd = new JButton("Export Invoice (.md)");
        btnExportMd.setBounds(785, 12, 145, 26);
        btnExportMd.addActionListener(this);
        add(btnExportMd);

        btnExportCsv = new JButton("Export (.csv)");
        btnExportCsv.setBounds(935, 12, 110, 26);
        btnExportCsv.addActionListener(this);
        add(btnExportCsv);

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
     * Handles refresh and invoice export button clicks.
     *
     * @param e ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRefresh) {
            loadHistory();
        } else if (e.getSource() == btnExportMd || e.getSource() == btnExportCsv) {
            int selectedRow = tblHistory.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a booking from your history to export its invoice.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int bookingId = Integer.parseInt(historyModel.getValueAt(selectedRow, 0).toString());
            boolean isMd = (e.getSource() == btnExportMd);
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            File outDir = (desktop.exists() && desktop.isDirectory()) ? desktop : new File(System.getProperty("user.home"));
            File targetFile = new File(outDir, "invoice_" + bookingId + (isMd ? ".md" : ".csv"));

            try {
                if (isMd) {
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
    }
}
