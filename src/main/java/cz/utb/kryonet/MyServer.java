package cz.utb.kryonet;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cz.utb.SQL;
import fr.bmartel.speedtest.SpeedTestSocket;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MyServer {
    public Server server;
    HashSet<ClientData> loggedIn = new HashSet();
    HashMap<String, ClientConnection> connections = new HashMap<>();
    SpeedTestSocket speedTestSocket;
    float lastDownload;
    float lastUpload;
    SQL sql;
    boolean useDatabase = true;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public MyServer(final SQL sql) throws IOException {
        this.sql = sql;
        server = new Server() {
            protected Connection newConnection() {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new ClientConnection();
            }
        };

        sql.removeOldRecords();
        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Network.register(server);

        server.addListener(new Listener() {

            public void received(Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                ClientConnection connection = (ClientConnection) c;
                ClientData clientData = connection.clientData;
                if (object instanceof Network.FollowClient) {
                    if (((Network.FollowClient) object).follow) {
                        clientData.followClient = ((Network.FollowClient) object).token;
                    } else {
                        clientData.followClient = null;
                    }
                }
                if (object instanceof Network.UseDatabase) {
                    useDatabase = ((Network.UseDatabase) object).useDatabase;
                    Log.info("Database", "using " + useDatabase);
                    server.sendToAllExceptTCP(c.getID(), object);
                }
                if (object instanceof Network.Register) {
                    networkRegister((Network.Register) object, clientData, connection);
                }
                if (object instanceof Network.Touch) {
                    int idClient = 0;
                    try {
                        if (useDatabase) {
                            idClient = sql.getIdByToken(((ClientConnection) c).clientData.token);
                        }
                    } catch (Exception e) {
                        Log.error(e.toString(),
                                e.getStackTrace()[0].toString());
                    }
                    sendTouchToFollovers(connection, object);
                    try {
                        sql.insertTouch(((Network.Touch) object).touchType,
                                ((Network.Touch) object).x,
                                ((Network.Touch) object).y,
                                ((Network.Touch) object).clientCreated,
                                idClient);
                    } catch (Exception e) {
                        Log.error(e.toString(),
                                e.getStackTrace()[0].toString());
                    }
                }
            }

            public void disconnected(Connection c) {
                ClientConnection connection = (ClientConnection) c;
                if (connection.clientData != null) {
                    connections.remove(connection.clientData.token);
                    loggedIn.remove(connection.clientData);
                    try {
                        sql.connection.createStatement().executeUpdate("update  client set connected = false where token = '" + connection.clientData.token + "' ");
                    } catch (NullPointerException | SQLException e) {
                        Log.error(e.toString(),
                                e.getStackTrace()[0].toString());
                    }
                    sendRegisteredUsers();
                }
            }
        });
        server.bind(Network.port);
        server.start();
    }

    public void networkRegister(Network.Register object, ClientData clientData, ClientConnection connection) {
        if (clientData != null) {
            return;
        }
        sql.deleteOldTouches();


        Network.Register register = object;
        String userName = register.userName;
        String token = register.token;
        if (userName != null && token != null) {
            Log.info("client", "registered <" + userName + "> token <" + token + ">");

            if (userName.length() != 0) {
                connection.clientData = new ClientData();
                connection.clientData.userName = userName;
                connection.clientData.token = token;
                loggedIn.add(connection.clientData);
                connections.put(connection.clientData.token, connection);

                try {
                    sql.connection.createStatement().executeUpdate("insert into " +
                            "client (name, token) " +
                            "values ('" + userName + "', '" + token + "')");

                } catch (NullPointerException | SQLException e) {
                    Log.error(e.toString(),
                            e.getStackTrace()[0].toString());
                }

                connection.clientData.id = sql.getIdByToken(connection.clientData.token);
                sendRegisteredUsers();
            }
        }
    }

    public void sendTouchToFollovers(ClientConnection clientConnection, Object o) {
        for (ClientConnection c : connections.values()) {
            try {
                if (c.clientData.followClient.equals(clientConnection.clientData.token)) {
                    c.sendTCP((Network.Touch) o);
                }
            } catch (Exception e) {
                Log.error(e.toString(),
                        e.getStackTrace()[0].toString());
            }
        }
    }


    public int getIdOfPairedSeeker(int idRespondent) {
        int idSeeker = 0;
        try {
            ResultSet rs = sql.connection.createStatement().executeQuery("select seeker_respondent " +
                    "from client_has_client where respondent_idclient" +
                    " =  " + idRespondent + " ");
            sql.connection.commit();
            while (rs.next()) {
                idSeeker = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idSeeker;
    }

    public int getIdOfPairedRespondent(int idSeeker) {
        int idRespondent = 0;
        try {
            ResultSet rs = sql.connection.createStatement().executeQuery("select seeker_respondent " +
                    "from client_has_client where respondent_idclient" +
                    " =  " + idSeeker + " ");
            sql.connection.commit();
            while (rs.next()) {
                idRespondent = rs.getInt(1);
            }
        } catch (NullPointerException | SQLException e) {
            Log.error(e.toString(),
                    e.getStackTrace()[0].toString());
        }
        return idRespondent;
    }

    public int getIdbyTokenJavaServer(String token) {
        int a = 0;
        for (Iterator<ClientData> aa = loggedIn.iterator(); aa.hasNext(); ) {
            ClientData cd = aa.next();
            if (cd.token.equals(token)) {
                a = cd.id;
                break;
            }
        }
        return a;
    }

    public void sendRegisteredUsers() {
        Network.RegisteredUsers registeredUsers = new Network.RegisteredUsers();
        registeredUsers.users = new ArrayList<Network.Register>();
        for (Iterator<ClientData> aa = loggedIn.iterator(); aa.hasNext(); ) {
            ClientData cd = aa.next();
            Network.Register register = new Network.Register();
            register.userName = cd.userName;
            register.token = cd.token;
            registeredUsers.users.add(register);
        }
        server.sendToAllTCP(registeredUsers);
    }


    // This holds per connection state.
    static class ClientConnection extends Connection {
        public ClientData clientData;
    }
}
