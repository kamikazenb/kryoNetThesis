package cz.utb;

import com.esotericsoftware.minlog.Log;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SQL {
    String TAG = "SQL";
    public java.sql.Connection connection;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public SQL() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Log.error(e.toString(),
                    e.getStackTrace()[0].toString());
//            throw new Error("Problem", e);
        }

    }


    public void connectToDatabase() {

        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb?useLegacyDatetimeCode=false&serverTimezone=Europe/Vienna", "root", "account");
            connection.setAutoCommit(false);
            Log.info("Successfully connected to database");

        } catch (SQLException e) {
            Log.error(e.toString(),
                    e.getStackTrace()[0].toString());
        }

    }

    public void deleteClientHasClient(int id) {
        String query = "DELETE FROM client_has_client WHERE " +
                " respondent_idclient = " + id + " or seeker_idclient =  " + id + "";
        executeUpdate(query);
    }

    public void deleteOldTouches() {
        ResultSet rs = executeQuery("SELECT COUNT(*) FROM touch;");
        try {
            while (rs.next()) {
                Log.info(TAG, "Rows in touch: " + rs.getInt(1));
                if (rs.getInt(1) > 1200) {
                    executeUpdate("delete from touch");
                    break;
                }
            }
        } catch (NullPointerException | SQLException e) {
            Log.error(e.getStackTrace()[0].toString(),
                    e.toString());
        }


    }


    public void insertTouch(String touchType, float x, float y, Date clientCreated, int idClient) {
        String query = "insert into touch (touchType, x, y, clientCreated, client_idclient)" +
                "values ('" + touchType + "', " + x + ", " + y + ", '" + df.format(clientCreated) + "', " + idClient + ")";
        executeUpdate(query);
    }

    public void executeUpdate(String query) {
        try {
            connection.setAutoCommit(false);
            connection.createStatement().executeUpdate(query);
            connection.commit();
        } catch (NullPointerException | SQLException e) {
            Log.error(e.toString(),
                    e.getStackTrace()[0].toString());
        }
    }

    public ResultSet executeQuery(String query) {
        ResultSet rs = null;
        try {
            rs = connection.createStatement().executeQuery(query);
            connection.commit();
        } catch (SQLException e) {
            Log.error(e.toString(),
                    e.getStackTrace()[0].toString());
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

    public int getIdByToken(String token) {
        int id = 0;
        try {
            ResultSet rs = connection.createStatement().executeQuery("select idclient " +
                    "from client where token = '" + token + "'");
            connection.commit();
            while (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (Exception e) {

        }
        return id;
    }

    public String getTokenById(int id) {
        String dbRespondentToken = "";
        try {
            ResultSet rs = connection.createStatement().executeQuery("select token " +
                    "from client where idclient = " + id + "");
            connection.commit();
            while (rs.next()) {
                dbRespondentToken = rs.getString(1);
            }
        } catch (Exception e) {

        }
        return dbRespondentToken;
    }

}
