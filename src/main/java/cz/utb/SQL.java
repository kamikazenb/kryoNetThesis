package cz.utb;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SQL {
    public java.sql.Connection connection;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public SQL() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (ClassNotFoundException e) {
            throw new Error("Problem", e);
        }

    }

    public void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/mydb?useLegacyDatetimeCode=false&serverTimezone=Europe/Vienna", "root", "");
            connection.setAutoCommit(false);
            System.out.println("DBZ connected");
        } catch (SQLException e) {
            throw new Error("Problem", e);
        }
    }

    public void deleteClientHasClient(int id) {
        String query = "DELETE FROM client_has_client WHERE " +
                " respondent_idclient = " + id + " or seeker_idclient =  " + id + "";
        executeUpdate(query);
    }

    public void insertTouch(String touchType, float x, float y, Date clientCreated, Date serverReceived, String token) {
        String query = "select idclient from client where token = '" + token + "'";
        int idClient = 0;
        ResultSet rs = executeQuery(query);
        try {
            while (rs.next()) {
                idClient = rs.getInt(1);
            }
            query = "insert into touch (touchType, x, y, clientCreated, serverReceived, serverType_idserverType, client_idclient)" +
                    "values ('" + touchType + "', " + x + ", " + y + ", '" + df.format(clientCreated) + "', '" + df.format(serverReceived) + "', 1, " + idClient + ")";
            executeUpdate(query);
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public void executeUpdate(String query) {
        try {
            connection.setAutoCommit(false);
            connection.createStatement().executeUpdate(query);
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String query) {
        ResultSet rs = null;
        try {
            rs = connection.createStatement().executeQuery(query);
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    public void removeOldRecords() {
        executeUpdate("update client set connected = false where connected = true ");
    }

    public void updateClient(int id, boolean pairSeeker, boolean pairRespondent, boolean pairAccepted) {
        String query = "update client " +
                "set pairSeeker = " + boolToInt(pairSeeker) + ", pairRespondent = " + boolToInt(pairRespondent) + ", " +
                "pairAccepted = " + boolToInt(pairAccepted) + " " +
                "where idclient = " + id + "";
        executeUpdate(query);
    }

    public int boolToInt(boolean bool) {
        return bool ? 1 : 0;
    }

}
