package TablaConductores;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TablaConductores {
    private Connection conn;
    private JFrame frame;
    private JComboBox<String> comboBox;
    private JTable table;
    private DefaultTableModel tableModel;

    public TablaConductores() {
        // Establecer conexión a la base de datos PostgreSQL
        connectDB();

        // Crear la interfaz gráfica
        createGUI();
    }

    private void connectDB() {
        try {
            String url = "jdbc:postgresql://localhost:5432/ListaCorredores";
            String user = "postgres";
            String password = "root";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión establecida con PostgreSQL.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("Tabla de Drivers por Año de Carrera");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 450);

        // Combo box para seleccionar el año de carrera
        comboBox = new JComboBox<>();
        populateComboBox();
        comboBox.addActionListener(e -> {
            // Cuando se seleccione un año, actualizar la tabla de corredores
            updateTable();
        });

        // Tabla para mostrar los datos de corredores y carreras
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Centrar el contenido de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        frame.getContentPane().add(comboBox, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void populateComboBox() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT year FROM races ORDER BY year DESC");
            while (rs.next()) {
                comboBox.addItem(rs.getString("year"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTable() {
        try {
            String selectedYear = (String) comboBox.getSelectedItem();
            if (selectedYear != null) {
                // Consulta para obtener los corredores que participaron en las carreras del año seleccionado
                String query = "SELECT DISTINCT ON (d.driver_id) d.driver_id, d.forename, d.surname, d.dob, d.nationality, " +
                        "(SELECT COUNT(*) FROM driver_standings ds INNER JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r.year = ? AND ds.position = 1) AS carreras_ganadas, " +
                        "(SELECT COUNT(*) FROM driver_standings ds INNER JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE ds.driver_id = d.driver_id AND r.year = ?) AS num_races " +
                        "FROM drivers d " +
                        "JOIN driver_standings ds ON d.driver_id = ds.driver_id " +
                        "JOIN races r ON ds.race_id = r.race_id " +
                        "WHERE r.year = ? " +
                        "ORDER BY d.driver_id, r.date";

                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, Integer.parseInt(selectedYear));
                pstmt.setInt(2, Integer.parseInt(selectedYear));
                pstmt.setInt(3, Integer.parseInt(selectedYear));
                ResultSet rs = pstmt.executeQuery();

                // Obtener columnas
                Vector<String> columnNames = new Vector<>();
                columnNames.add("Driver ID");
                columnNames.add("Nombre");
                columnNames.add("Apellido");
                columnNames.add("Fecha Nacimiento");
                columnNames.add("Nacionalidad");
                columnNames.add("Numero Carreras");
                columnNames.add("Carreras Ganadas");

                // Obtener filas
                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("driver_id"));
                    row.add(rs.getString("forename"));
                    row.add(rs.getString("surname"));
                    row.add(rs.getDate("dob"));
                    row.add(rs.getString("nationality"));
                    row.add(rs.getInt("num_races"));
                    row.add(rs.getInt("carreras_ganadas"));
                    data.add(row);
                }

                // Actualizar modelo de la tabla
                tableModel.setDataVector(data, columnNames);

                // Centrar el contenido de las celdas
                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(JLabel.CENTER);
                table.setDefaultRenderer(Object.class, centerRenderer);

                rs.close();
                pstmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TablaConductores::new);
    }
}