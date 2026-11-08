package service;

import database.DBConnection;
import model.Booking;
import model.Hotel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer coordinating hotel CRUD, booking transactions, invoice exports, and report generation.
 * Mirrors the {@code RentalService} service architecture from the starter project blueprint.
 */
public class BookingManagementService {

    /**
     * Constructs a BookingManagementService instance.
     */
    public BookingManagementService() {
    }

    // ==================== HOTEL CRUD ====================

    /**
     * Adds a new hotel listing to the MySQL database.
     *
     * @param hotel hotel entity
     * @throws SQLException on database error
     */
    public void addHotel(Hotel hotel) throws SQLException {
        if (hotel == null) {
            return;
        }
        String sql = "INSERT INTO hotels(hotel_id, name, location, rating, price_per_night) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hotel.getHotelId());
            ps.setString(2, hotel.getName());
            ps.setString(3, hotel.getLocation());
            ps.setDouble(4, hotel.getRating());
            ps.setDouble(5, hotel.getPricePerNight());
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing hotel listing in the database.
     *
     * @param hotel updated hotel entity
     * @throws SQLException on database error
     */
    public void updateHotel(Hotel hotel) throws SQLException {
        if (hotel == null) {
            return;
        }
        String sql = "UPDATE hotels SET name = ?, location = ?, rating = ?, price_per_night = ? WHERE hotel_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hotel.getName());
            ps.setString(2, hotel.getLocation());
            ps.setDouble(3, hotel.getRating());
            ps.setDouble(4, hotel.getPricePerNight());
            ps.setString(5, hotel.getHotelId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a hotel record from the database by ID.
     *
     * @param hotelId hotel identifier
     * @throws SQLException on database error
     */
    public void deleteHotel(String hotelId) throws SQLException {
        String sql = "DELETE FROM hotels WHERE hotel_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hotelId);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves all hotels from the database.
     *
     * @return list of hotels
     * @throws SQLException on database error
     */
    public List<Hotel> getAllHotels() throws SQLException {
        List<Hotel> list = new ArrayList<>();
        String sql = "SELECT * FROM hotels";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Hotel(
                        rs.getString("hotel_id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getDouble("rating"),
                        rs.getDouble("price_per_night")
                ));
            }
        }
        return list;
    }

    /**
     * Filters hotels matching a location query.
     *
     * @param query search location
     * @return list of matching hotels
     * @throws SQLException on database error
     */
    public List<Hotel> searchHotels(String query) throws SQLException {
        List<Hotel> list = new ArrayList<>();
        String sql = "SELECT * FROM hotels WHERE location LIKE ? OR name LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Hotel(
                            rs.getString("hotel_id"),
                            rs.getString("name"),
                            rs.getString("location"),
                            rs.getDouble("rating"),
                            rs.getDouble("price_per_night")
                    ));
                }
            }
        }
        return list;
    }

    // ==================== BOOKINGS ====================

    /**
     * Creates a new booking in the database.
     *
     * @param username   customer username
     * @param flightId   flight ID
     * @param hotelId    hotel ID
     * @param totalPrice booking total
     * @throws SQLException on database error
     */
    public void createBooking(String username, String flightId, String hotelId, double totalPrice) throws SQLException {
        String sql = "INSERT INTO bookings(username, flight_id, hotel_id, total_price) VALUES(?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, flightId != null && !flightId.isEmpty() ? flightId : null);
            ps.setString(3, hotelId != null && !hotelId.isEmpty() ? hotelId : null);
            ps.setDouble(4, totalPrice);
            ps.executeUpdate();
        }
    }

    /**
     * Cancels / deletes a booking by ID.
     *
     * @param bookingId booking identifier
     * @throws SQLException on database error
     */
    public void cancelBooking(int bookingId) throws SQLException {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves all bookings from the database.
     *
     * @param currentUser logged-in user
     * @param isAdmin     whether user is admin
     * @return list of bookings
     * @throws SQLException on database error
     */
    public List<Booking> getBookings(String currentUser, boolean isAdmin) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = isAdmin ? "SELECT * FROM bookings" : "SELECT * FROM bookings WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!isAdmin) {
                ps.setString(1, currentUser);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Booking(
                            rs.getInt("booking_id"),
                            rs.getString("username"),
                            rs.getString("flight_id"),
                            rs.getString("hotel_id"),
                            rs.getDouble("total_price")
                    ));
                }
            }
        }
        return list;
    }

    // ==================== ADMINISTRATIVE REPORT GENERATION ====================

    /**
     * Generates a comprehensive summary report of booking transactions, total revenue, and activity.
     *
     * @return formatted report string
     * @throws SQLException on database error
     */
    public String generateSystemSummaryReport() throws SQLException {
        int totalBookings = 0;
        double totalRevenue = 0;
        Map<String, Integer> customerActivity = new HashMap<>();

        String sql = "SELECT username, total_price FROM bookings";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                totalBookings++;
                totalRevenue += rs.getDouble("total_price");
                String user = rs.getString("username");
                customerActivity.put(user, customerActivity.getOrDefault(user, 0) + 1);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=====================================================\n");
        sb.append("         TRAVEL BOOKING SYSTEM - EXECUTIVE REPORT   \n");
        sb.append("=====================================================\n\n");
        sb.append(String.format("Total Completed Bookings : %d\n", totalBookings));
        sb.append(String.format("Gross System Revenue     : $%,.2f\n", totalRevenue));
        sb.append(String.format("Average Booking Value    : $%,.2f\n\n", totalBookings > 0 ? (totalRevenue / totalBookings) : 0.0));
        sb.append("Customer Transaction Activity Breakdown:\n");
        sb.append("-----------------------------------------------------\n");
        if (customerActivity.isEmpty()) {
            sb.append("No booking activity found.\n");
        } else {
            for (Map.Entry<String, Integer> entry : customerActivity.entrySet()) {
                sb.append(String.format(" - %-20s : %d bookings\n", entry.getKey(), entry.getValue()));
            }
        }
        sb.append("=====================================================\n");
        return sb.toString();
    }

    /**
     * Generates a structured CSV representation of the system executive report.
     *
     * @return CSV formatted executive report string
     * @throws SQLException on database error
     */
    public String generateExecutiveReportCsv() throws SQLException {
        int totalBookings = 0;
        double totalRevenue = 0;
        Map<String, Integer> customerActivity = new HashMap<>();
        Map<String, Double> customerExpenditure = new HashMap<>();

        String sql = "SELECT username, total_price FROM bookings";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                totalBookings++;
                double price = rs.getDouble("total_price");
                totalRevenue += price;
                String user = rs.getString("username");
                customerActivity.put(user, customerActivity.getOrDefault(user, 0) + 1);
                customerExpenditure.put(user, customerExpenditure.getOrDefault(user, 0.0) + price);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Section,Metric,Value\n");
        sb.append("Executive Summary,Total Completed Bookings,").append(totalBookings).append("\n");
        sb.append("Executive Summary,Gross System Revenue,$").append(String.format("%.2f", totalRevenue)).append("\n");
        sb.append("Executive Summary,Average Booking Value,$").append(String.format("%.2f", totalBookings > 0 ? (totalRevenue / totalBookings) : 0.0)).append("\n");
        sb.append("Executive Summary,Generated At,").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        sb.append("Customer Breakdown,Username,Total Bookings,Total Expenditure ($)\n");
        for (Map.Entry<String, Integer> entry : customerActivity.entrySet()) {
            String u = entry.getKey();
            sb.append("Customer,")
                    .append(u).append(",")
                    .append(entry.getValue()).append(",")
                    .append(String.format("%.2f", customerExpenditure.getOrDefault(u, 0.0)))
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Exports the executive report as a CSV file to the designated file location.
     *
     * @param targetFile destination file
     * @throws SQLException on database error
     * @throws IOException  on file write error
     */
    public void exportExecutiveReportCsv(File targetFile) throws SQLException, IOException {
        String csv = generateExecutiveReportCsv();
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(csv);
        }
    }

    /**
     * Generates a formatted Markdown invoice string for a specific booking.
     *
     * @param bookingId the target booking identifier
     * @return formatted Markdown invoice string
     * @throws SQLException on database error
     */
    public String generateInvoiceMarkdown(int bookingId) throws SQLException {
        String username = "";
        String flightId = "";
        String hotelId = "";
        double totalPrice = 0.0;

        String fDep = "N/A", fArr = "N/A", fDepTime = "N/A", fArrTime = "N/A";
        double fPrice = 0.0;
        String hName = "N/A", hLoc = "N/A";
        double hRating = 0.0, hPrice = 0.0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM bookings WHERE booking_id = ?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                    flightId = rs.getString("flight_id");
                    hotelId = rs.getString("hotel_id");
                    totalPrice = rs.getDouble("total_price");
                } else {
                    return "# Invoice Error\nBooking ID " + bookingId + " not found.";
                }
            }
        }

        if (flightId != null && !flightId.isBlank()) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM flights WHERE flight_id = ?")) {
                ps.setString(1, flightId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fDep = rs.getString("departure");
                        fArr = rs.getString("arrival");
                        fDepTime = rs.getString("departure_time");
                        fArrTime = rs.getString("arrival_time");
                        fPrice = rs.getDouble("price");
                    }
                }
            }
        }

        if (hotelId != null && !hotelId.isBlank()) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM hotels WHERE hotel_id = ?")) {
                ps.setString(1, hotelId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hName = rs.getString("name");
                        hLoc = rs.getString("location");
                        hRating = rs.getDouble("rating");
                        hPrice = rs.getDouble("price_per_night");
                    }
                }
            }
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("# TRAVEL BOOKING SYSTEM — OFFICIAL INVOICE\n\n");
        sb.append("**Invoice Reference:** INV-").append(bookingId).append("  \n");
        sb.append("**Customer Account:** ").append(username).append("  \n");
        sb.append("**Issue Date:** ").append(now).append("  \n\n");
        sb.append("## Itinerary Details\n\n");
        sb.append("| Component | Item / Identifier | Details | Price |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        sb.append("| **Flight** | ").append(flightId != null ? flightId : "None").append(" | ")
                .append(fDep).append(" (").append(fDepTime).append(") \u2192 ")
                .append(fArr).append(" (").append(fArrTime).append(") | $")
                .append(String.format("%.2f", fPrice)).append(" |\n");
        sb.append("| **Hotel** | ").append(hotelId != null ? hotelId : "None").append(" | ")
                .append(hName).append(" (").append(hLoc).append(", ").append(hRating).append(" \u2605) | $")
                .append(String.format("%.2f", hPrice)).append(" |\n\n");
        sb.append("## Billing Breakdown\n\n");
        double combined = fPrice + hPrice;
        double discount = combined > totalPrice ? (combined - totalPrice) : 0.0;
        sb.append("- **Catalog Subtotal:** $").append(String.format("%.2f", combined)).append("\n");
        if (discount > 0.001) {
            sb.append("- **Package Deal Savings:** -$").append(String.format("%.2f", discount)).append("\n");
        }
        sb.append("- **Total Amount Charged:** **$").append(String.format("%.2f", totalPrice)).append("**\n\n");
        sb.append("*(Thank you for booking with Travel Booking System!)*\n");

        return sb.toString();
    }

    /**
     * Generates a CSV invoice string for a specific booking.
     *
     * @param bookingId the target booking identifier
     * @return CSV formatted invoice string
     * @throws SQLException on database error
     */
    public String generateInvoiceCsv(int bookingId) throws SQLException {
        String username = "";
        String flightId = "";
        String hotelId = "";
        double totalPrice = 0.0;
        double fPrice = 0.0;
        double hPrice = 0.0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM bookings WHERE booking_id = ?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                    flightId = rs.getString("flight_id");
                    hotelId = rs.getString("hotel_id");
                    totalPrice = rs.getDouble("total_price");
                }
            }
        }

        if (flightId != null && !flightId.isBlank()) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT price FROM flights WHERE flight_id = ?")) {
                ps.setString(1, flightId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) fPrice = rs.getDouble("price");
                }
            }
        }

        if (hotelId != null && !hotelId.isBlank()) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT price_per_night FROM hotels WHERE hotel_id = ?")) {
                ps.setString(1, hotelId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) hPrice = rs.getDouble("price_per_night");
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Property,Value\n");
        sb.append("Invoice ID,INV-").append(bookingId).append("\n");
        sb.append("Customer Username,").append(username).append("\n");
        sb.append("Flight ID,").append(flightId != null ? flightId : "None").append("\n");
        sb.append("Flight Price,").append(String.format("%.2f", fPrice)).append("\n");
        sb.append("Hotel ID,").append(hotelId != null ? hotelId : "None").append("\n");
        sb.append("Hotel Price/Night,").append(String.format("%.2f", hPrice)).append("\n");
        sb.append("Total Price Charged,").append(String.format("%.2f", totalPrice)).append("\n");
        return sb.toString();
    }

    /**
     * Exports a booking invoice as a Markdown file.
     *
     * @param bookingId  booking identifier
     * @param targetFile destination file
     * @throws SQLException on database error
     * @throws IOException  on file write error
     */
    public void exportInvoiceMarkdown(int bookingId, File targetFile) throws SQLException, IOException {
        String md = generateInvoiceMarkdown(bookingId);
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(md);
        }
    }

    /**
     * Exports a booking invoice as a CSV file.
     *
     * @param bookingId  booking identifier
     * @param targetFile destination file
     * @throws SQLException on database error
     * @throws IOException  on file write error
     */
    public void exportInvoiceCsv(int bookingId, File targetFile) throws SQLException, IOException {
        String csv = generateInvoiceCsv(bookingId);
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(csv);
        }
    }
}
