package gui;

import database.DBConnection;
import model.Flight;
import model.Hotel;
import model.PackageDeal;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel view for browsing and booking dynamic flight-hotel package deals.
 * Features smart destination-matching filter, real-time location search, and toggleable QuickSort buttons with directional arrow indicators.
 */
public class PackagePanel extends JPanel implements ActionListener {

    private JButton btnSortPrice;
    private JButton btnSortRating;
    private JButton btnBookPackage;
    private JTextField txtSearchPackage;
    private JButton btnResetFilter;
    private JTable tblPackages;
    private DefaultTableModel packageModel;
    private MainFrame parentFrame;

    private boolean priceAscending = true;
    private boolean ratingAscending = false;
    private String activeSortMode = "PRICE";

    /**
     * Constructs the PackagePanel view and initializes its controls and table layout.
     *
     * @param parent the parent {@link MainFrame} dashboard container
     */
    public PackagePanel(MainFrame parent) {
        this.parentFrame = parent;
        setLayout(null);
        setBounds(0, 0, 1005, 595);

        btnSortPrice = new JButton("Sort Price (Ascending \u2191)");
        btnSortPrice.setBounds(10, 12, 195, 26);
        btnSortPrice.addActionListener(this);
        add(btnSortPrice);

        btnSortRating = new JButton("Sort Rating");
        btnSortRating.setBounds(213, 12, 195, 26);
        btnSortRating.addActionListener(this);
        add(btnSortRating);

        btnBookPackage = new JButton("Book Selected Package");
        btnBookPackage.setBounds(416, 12, 185, 26);
        btnBookPackage.addActionListener(this);
        add(btnBookPackage);

        JLabel lblFilter = new JLabel("Filter Location:");
        lblFilter.setBounds(610, 12, 95, 25);
        add(lblFilter);

        txtSearchPackage = new JTextField();
        txtSearchPackage.setBounds(710, 12, 140, 25);
        txtSearchPackage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshPackages();
            }
        });
        add(txtSearchPackage);

        btnResetFilter = new JButton("Reset");
        btnResetFilter.setBounds(860, 12, 110, 26);
        btnResetFilter.addActionListener(this);
        add(btnResetFilter);

        packageModel = new DefaultTableModel(new String[]{"Flight ID", "Flight Price", "Hotel ID", "Hotel Name", "Rating", "Total Price"}, 0);
        tblPackages = new JTable(packageModel);
        JScrollPane sp = new JScrollPane(tblPackages);
        sp.setBounds(10, 48, 985, 535);
        add(sp);

        loadPackagesQuickSort("PRICE", priceAscending);
    }

    /**
     * Gets the number of packages currently loaded in the table model.
     *
     * @return current package count
     */
    public int getPackageCount() {
        return packageModel != null ? packageModel.getRowCount() : 0;
    }

    /**
     * Handles action events for price sorting, rating sorting, reset filtering, and package booking.
     * Toggles directional arrow indicators exclusively on the active sort button.
     *
     * @param e the ActionEvent triggered
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSortPrice) {
            if (!"PRICE".equals(activeSortMode)) {
                activeSortMode = "PRICE";
                priceAscending = true;
            } else {
                priceAscending = !priceAscending;
            }
            btnSortPrice.setText("Sort Price (" + (priceAscending ? "Ascending \u2191" : "Descending \u2193") + ")");
            btnSortRating.setText("Sort Rating");
            loadPackagesQuickSort("PRICE", priceAscending);

        } else if (e.getSource() == btnSortRating) {
            if (!"RATING".equals(activeSortMode)) {
                activeSortMode = "RATING";
                ratingAscending = false; // Default descending (higher rating first)
            } else {
                ratingAscending = !ratingAscending;
            }
            btnSortRating.setText("Sort Rating (" + (ratingAscending ? "Ascending \u2191" : "Descending \u2193") + ")");
            btnSortPrice.setText("Sort Price");
            loadPackagesQuickSort("RATING", ratingAscending);

        } else if (e.getSource() == btnResetFilter) {
            txtSearchPackage.setText("");
            refreshPackages();

        } else if (e.getSource() == btnBookPackage) {
            bookSelectedPackage();
        }
    }

    /**
     * Persists a booking for the currently selected package deal row in the table.
     */
    private void bookSelectedPackage() {
        int selectedRow = tblPackages.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a package deal from the table.");
            return;
        }

        String fId = tblPackages.getValueAt(selectedRow, 0).toString();
        String hId = tblPackages.getValueAt(selectedRow, 2).toString();
        double totalPrice = Double.parseDouble(tblPackages.getValueAt(selectedRow, 5).toString());

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO bookings(username, flight_id, hotel_id, total_price) VALUES(?, ?, ?, ?)")) {
            ps.setString(1, parentFrame.getCurrentUser());
            ps.setString(2, fId);
            ps.setString(3, hId);
            ps.setDouble(4, totalPrice);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Package Booked Successfully!");
            parentFrame.refreshBookingPanel();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Booking Failed: " + ex.getMessage());
        }
    }

    /**
     * Fetches available flight and hotel records and constructs PackageDeal pairs strictly when
     * flight arrival destination matches hotel location using smart string normalization.
     *
     * @return a {@link List} of destination-matched {@link PackageDeal} objects
     */
    private List<PackageDeal> fetchAllPackages() {
        List<PackageDeal> packages = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt1 = conn.createStatement();
             ResultSet rsF = stmt1.executeQuery("SELECT * FROM flights")) {

            List<Flight> flights = new ArrayList<>();
            while (rsF.next()) {
                flights.add(new Flight(
                        rsF.getString("flight_id"),
                        rsF.getString("departure"),
                        rsF.getString("departure_time"),
                        rsF.getString("arrival"),
                        rsF.getString("arrival_time"),
                        rsF.getDouble("price")
                ));
            }

            Statement stmt2 = conn.createStatement();
            ResultSet rsH = stmt2.executeQuery("SELECT * FROM hotels");
            List<Hotel> hotels = new ArrayList<>();
            while (rsH.next()) {
                hotels.add(new Hotel(
                        rsH.getString("hotel_id"),
                        rsH.getString("name"),
                        rsH.getString("location"),
                        rsH.getDouble("rating"),
                        rsH.getDouble("price_per_night")
                ));
            }

            for (Flight f : flights) {
                for (Hotel h : hotels) {
                    if (PackageDeal.isDestinationMatch(f.getArrival(), h.getLocation())) {
                        packages.add(new PackageDeal(f, h));
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching packages: " + ex.getMessage());
        }
        return packages;
    }

    /**
     * Re-fetches all matching packages and re-renders them according to the currently active sort mode.
     */
    public void refreshPackages() {
        if ("RATING".equals(activeSortMode)) {
            loadPackagesQuickSort("RATING", ratingAscending);
        } else {
            loadPackagesQuickSort("PRICE", priceAscending);
        }
    }

    /**
     * Sorts an in-memory list of package deals using {@code ds.PackageQuickSort} via reflection,
     * falling back gracefully if the class or methods are unavailable.
     *
     * @param packages  the list of package deals to sort
     * @param mode      the sorting criterion ("PRICE" or "RATING")
     * @param ascending {@code true} for ascending order, {@code false} for descending order
     */
    public static void sortPackagesWithReflection(List<PackageDeal> packages, String mode, boolean ascending) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        try {
            Class<?> quickSortClass = Class.forName("ds.PackageQuickSort");
            if ("PRICE".equals(mode)) {
                Method sortPriceMethod = quickSortClass.getMethod("sortByPrice", List.class, int.class, int.class, boolean.class);
                sortPriceMethod.invoke(null, packages, 0, packages.size() - 1, ascending);
            } else if ("RATING".equals(mode)) {
                Method sortRatingMethod = quickSortClass.getMethod("sortByRating", List.class, int.class, int.class, boolean.class);
                sortRatingMethod.invoke(null, packages, 0, packages.size() - 1, ascending);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Loads package deals, filters by destination search query, and sorts them using {@code ds.PackageQuickSort} via reflection.
     *
     * @param mode      the sorting criterion ("PRICE" or "RATING")
     * @param ascending {@code true} for ascending order, {@code false} for descending order
     */
    public void loadPackagesQuickSort(String mode, boolean ascending) {
        packageModel.setRowCount(0);
        List<PackageDeal> allPackages = fetchAllPackages();

        String filterQuery = txtSearchPackage != null ? txtSearchPackage.getText().trim().toLowerCase() : "";
        List<PackageDeal> packages;
        if (!filterQuery.isEmpty()) {
            packages = new ArrayList<>();
            for (PackageDeal pkg : allPackages) {
                String arrival = pkg.getFlight().getArrival().toLowerCase();
                String loc = pkg.getHotel().getLocation().toLowerCase();
                String name = pkg.getHotel().getName().toLowerCase();
                if (arrival.contains(filterQuery) || loc.contains(filterQuery) || name.contains(filterQuery)) {
                    packages.add(pkg);
                }
            }
        } else {
            packages = allPackages;
        }

        sortPackagesWithReflection(packages, mode, ascending);

        for (PackageDeal pkg : packages) {
            packageModel.addRow(new Object[]{
                    pkg.getFlight().getFlightId(),
                    pkg.getFlight().getPrice(),
                    pkg.getHotel().getHotelId(),
                    pkg.getHotel().getName(),
                    pkg.getHotelRating(),
                    pkg.getTotalPrice()
            });
        }

        if (parentFrame != null) {
            parentFrame.updateStatusBar();
        }
    }
}
