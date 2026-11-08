package gui;

import database.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;

/**
 * Panel view for managing and searching hotel listings.
 * Provides role-based administrative controls (Add/Remove Hotel), Enter-key submission,
 * deletion confirmation dialogs, and real-time location search filtering.
 */
public class HotelPanel extends JPanel implements ActionListener {

    private JTextField txtHotelId;
    private JTextField txtHotelName;
    private JTextField txtLocation;
    private JTextField txtRating;
    private JTextField txtHotelPrice;
    private JTextField txtSearchHotel;
    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JTable tblHotels;
    private DefaultTableModel hotelModel;
    private MainFrame parentFrame;

    /**
     * Constructs the HotelPanel view and initializes components.
     *
     * @param parent the parent {@link MainFrame} dashboard container
     */
    public HotelPanel(MainFrame parent) {
        this.parentFrame = parent;
        setLayout(null);
        setBounds(0, 0, 1005, 595);

        boolean isAdmin = "ADMIN".equals(parent.getCurrentRole());

        if (isAdmin) {
            JLabel lblId = new JLabel("Hotel ID:");
            lblId.setBounds(10, 12, 60, 25);
            add(lblId);

            txtHotelId = new JTextField();
            txtHotelId.setBounds(72, 12, 75, 25);
            add(txtHotelId);

            JLabel lblName = new JLabel("Name:");
            lblName.setBounds(155, 12, 45, 25);
            add(lblName);

            txtHotelName = new JTextField();
            txtHotelName.setBounds(203, 12, 140, 25);
            add(txtHotelName);

            JLabel lblLoc = new JLabel("Location:");
            lblLoc.setBounds(350, 12, 60, 25);
            add(lblLoc);

            txtLocation = new JTextField();
            txtLocation.setBounds(413, 12, 130, 25);
            add(txtLocation);

            JLabel lblRat = new JLabel("Rating:");
            lblRat.setBounds(550, 12, 50, 25);
            add(lblRat);

            txtRating = new JTextField();
            txtRating.setBounds(603, 12, 50, 25);
            add(txtRating);

            JLabel lblPrice = new JLabel("Price/Night:");
            lblPrice.setBounds(660, 12, 75, 25);
            add(lblPrice);

            txtHotelPrice = new JTextField();
            txtHotelPrice.setBounds(738, 12, 75, 25);
            add(txtHotelPrice);

            btnAdd = new JButton("Add Hotel");
            btnAdd.setBounds(10, 48, 105, 26);
            btnAdd.addActionListener(this);
            add(btnAdd);

            btnUpdate = new JButton("Update Selected");
            btnUpdate.setBounds(123, 48, 130, 26);
            btnUpdate.addActionListener(this);
            add(btnUpdate);

            btnDelete = new JButton("Remove Selected");
            btnDelete.setBounds(261, 48, 135, 26);
            btnDelete.addActionListener(this);
            add(btnDelete);

            // Enter-key trigger for quick addition from any form field
            KeyAdapter enterSubmitAdapter = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        btnAdd.doClick();
                    }
                }
            };
            txtHotelId.addKeyListener(enterSubmitAdapter);
            txtHotelName.addKeyListener(enterSubmitAdapter);
            txtLocation.addKeyListener(enterSubmitAdapter);
            txtRating.addKeyListener(enterSubmitAdapter);
            txtHotelPrice.addKeyListener(enterSubmitAdapter);
        }

        JLabel lblSearch = new JLabel("Search Location:");
        lblSearch.setBounds(isAdmin ? 420 : 10, isAdmin ? 48 : 12, 115, 25);
        add(lblSearch);

        txtSearchHotel = new JTextField();
        txtSearchHotel.setBounds(isAdmin ? 540 : 130, isAdmin ? 48 : 12, 180, 25);
        add(txtSearchHotel);

        txtSearchHotel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterHotels(txtSearchHotel.getText().trim());
            }
        });

        hotelModel = new DefaultTableModel(new String[]{"ID", "Name", "Location", "Rating", "Price/Night"}, 0);
        tblHotels = new JTable(hotelModel);
        if (isAdmin) {
            tblHotels.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && tblHotels.getSelectedRow() != -1) {
                    int r = tblHotels.getSelectedRow();
                    txtHotelId.setText(tblHotels.getValueAt(r, 0).toString());
                    txtHotelName.setText(tblHotels.getValueAt(r, 1).toString());
                    txtLocation.setText(tblHotels.getValueAt(r, 2).toString());
                    txtRating.setText(tblHotels.getValueAt(r, 3).toString());
                    txtHotelPrice.setText(tblHotels.getValueAt(r, 4).toString());
                }
            });
        }
        JScrollPane sp = new JScrollPane(tblHotels);
        sp.setBounds(10, isAdmin ? 85 : 48, 985, isAdmin ? 495 : 535);
        add(sp);

        loadHotels();
    }

    /**
     * Gets the number of hotels currently loaded in the table model.
     *
     * @return current hotel count
     */
    public int getHotelCount() {
        return hotelModel != null ? hotelModel.getRowCount() : 0;
    }

    /**
     * Handles action events for adding, updating, and deleting hotels.
     *
     * @param e the ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO hotels VALUES(?, ?, ?, ?, ?)")) {
                ps.setString(1, txtHotelId.getText().trim());
                ps.setString(2, txtHotelName.getText().trim());
                ps.setString(3, txtLocation.getText().trim());
                ps.setDouble(4, Double.parseDouble(txtRating.getText().trim()));
                ps.setDouble(5, Double.parseDouble(txtHotelPrice.getText().trim()));
                ps.executeUpdate();

                // Clear all input textfields after adding
                txtHotelId.setText("");
                txtHotelName.setText("");
                txtLocation.setText("");
                txtRating.setText("");
                txtHotelPrice.setText("");

                loadHotels();
                if (parentFrame != null) {
                    parentFrame.refreshPackagePanel();
                }
                JOptionPane.showMessageDialog(this, "Hotel added successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        } else if (e.getSource() == btnUpdate) {
            int selectedRow = tblHotels.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a hotel from the table to update.");
                return;
            }
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE hotels SET name = ?, location = ?, rating = ?, price_per_night = ? WHERE hotel_id = ?")) {
                ps.setString(1, txtHotelName.getText().trim());
                ps.setString(2, txtLocation.getText().trim());
                ps.setDouble(3, Double.parseDouble(txtRating.getText().trim()));
                ps.setDouble(4, Double.parseDouble(txtHotelPrice.getText().trim()));
                ps.setString(5, txtHotelId.getText().trim());
                ps.executeUpdate();

                loadHotels();
                if (parentFrame != null) {
                    parentFrame.refreshPackagePanel();
                }
                JOptionPane.showMessageDialog(this, "Hotel updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating hotel: " + ex.getMessage());
            }
        } else if (e.getSource() == btnDelete) {
            int selectedRow = tblHotels.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a hotel to remove.");
                return;
            }
            String hotelId = tblHotels.getValueAt(selectedRow, 0).toString();
            String name = tblHotels.getValueAt(selectedRow, 1).toString();
            String loc = tblHotels.getValueAt(selectedRow, 2).toString();
            String rating = tblHotels.getValueAt(selectedRow, 3).toString();
            String price = tblHotels.getValueAt(selectedRow, 4).toString();

            String confirmMsg = "Are you sure you want to permanently delete this hotel?\n\n"
                    + "Hotel ID: " + hotelId + "\n"
                    + "Name: " + name + "\n"
                    + "Location: " + loc + "\n"
                    + "Rating: " + rating + " Stars\n"
                    + "Price/Night: $" + price;

            int opt = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm Hotel Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) {
                return;
            }

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM hotels WHERE hotel_id = ?")) {
                ps.setString(1, hotelId);
                ps.executeUpdate();
                loadHotels();
                if (parentFrame != null) {
                    parentFrame.refreshPackagePanel();
                }
                JOptionPane.showMessageDialog(this, "Hotel removed successfully.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error removing hotel: " + ex.getMessage());
            }
        }
    }

    /**
     * Loads all hotel records from the database into the JTable.
     */
    public void loadHotels() {
        hotelModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM hotels")) {
            while (rs.next()) {
                hotelModel.addRow(new Object[]{rs.getString("hotel_id"), rs.getString("name"), rs.getString("location"), rs.getDouble("rating"), rs.getDouble("price_per_night")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading hotels: " + ex.getMessage());
        }

        if (parentFrame != null) {
            parentFrame.updateStatusBar();
        }
    }

    /**
     * Filters hotels by location or hotel name query.
     *
     * @param query the search substring
     */
    private void filterHotels(String query) {
        hotelModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM hotels WHERE LOWER(location) LIKE ? OR LOWER(name) LIKE ?")) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                hotelModel.addRow(new Object[]{rs.getString("hotel_id"), rs.getString("name"), rs.getString("location"), rs.getDouble("rating"), rs.getDouble("price_per_night")});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Search error: " + ex.getMessage());
        }

        if (parentFrame != null) {
            parentFrame.updateStatusBar();
        }
    }
}
