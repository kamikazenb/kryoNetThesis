package cz.utb.kryonet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cz.utb.SQL;
import fr.bmartel.speedtest.SpeedTestSocket;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.*;
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

    public MyServer(final SQL sql) throws IOException {
        this.sql = sql;
        Log.set(Log.LEVEL_INFO);
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
                if (object instanceof Network.Integers) {
                    System.out.println(((Network.Integers) object).arrayIntegers.size());
                }
                if (object instanceof Network.UseDatabase) {
                    useDatabase = ((Network.UseDatabase) object).useDatabase;
                    System.out.println("Database usage: "+useDatabase);
                    server.sendToAllExceptTCP(c.getID(), object);
                }
                if (object instanceof Network.Info) {
                    System.out.println(((Network.Info) object).message);
                }
                if (object instanceof Network.Pair) {
                    networkPair((Network.Pair) object, clientData, connection);
                }
                if (object instanceof Network.Register) {
                    networkRegister((Network.Register) object, clientData, connection);
                }
                if (object instanceof Network.TouchStart) {
                    try {
                        ((Network.TouchStart) object).serverReceived = new Date(System.currentTimeMillis());
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }
                if (object instanceof Network.TouchMove) {
                    try {
                        ((Network.TouchMove) object).serverReceived = new Date(System.currentTimeMillis());
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }

                if (object instanceof Network.CleanCanvas) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }
                if (object instanceof Network.TouchTolerance) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }
                if (object instanceof Network.TouchUp) {
                    try {
                        ((Network.TouchUp) object).serverReceived = new Date(System.currentTimeMillis());
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }
                if (object instanceof Network.ScreenSize) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    } catch (Exception e) {
                    }
                }
                if (object instanceof Network.Request) {
                    Network.Request request = (Network.Request) object;
                    if (request.registredUsers) {
                        sendRegisteredUsers();
                    }
                }
            }

            public void disconnected(Connection c) {
                ClientConnection connection = (ClientConnection) c;
                if (connection.clientData != null) {
                    unpair(((ClientConnection) c).clientData.token);
                    connections.remove(connection.clientData.systemName);
                    loggedIn.remove(connection.clientData);
                    try {
                        sql.connection.createStatement().executeUpdate("update  client set connected = false where token = '" + connection.clientData.token + "' ");
                    } catch (SQLException e) {
                        e.printStackTrace();
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
        Network.Register register = object;
        String userName = register.userName;
        String systemName = register.systemName;
        String token = register.token;
        boolean mainClient = register.mainClient;
        if (userName != null && systemName != null && token != null) {
            System.out.println("registered: user name <" + userName + "> system name:  <"
                    + systemName + "> token <" + token + ">");
            systemName = systemName.trim();

            if (systemName.length() != 0) {
                connection.clientData = new ClientData();
                connection.clientData.mainClient = mainClient;
                connection.clientData.userName = userName;
                connection.clientData.systemName = systemName;
                connection.clientData.token = token;
                loggedIn.add(connection.clientData);
                connections.put(connection.clientData.token, connection);

                try {
                    sql.connection.createStatement().executeUpdate("insert into " +
                            "client (name, token) " +
                            "values ('" + userName + "', '" + token + "')");

                } catch (SQLException e) {
                    e.printStackTrace();
                }
                connection.clientData.id = getIdByToken(connection.clientData.token);
                sendRegisteredUsers();
            }
        }
    }

    public void checkPair(int idSeeker, int idRespondent) {
        boolean alreadyBounded = false;
        try {
            String query = "select respondent_idclient " +
                    "from client_has_client where seeker_idclient = " + idSeeker + "";
            ResultSet rs = sql.executeQuery(query);
            int idclientRespondent = 0;
            while (rs.next()) {
                idclientRespondent = rs.getInt(1);
            }
            System.out.println("~~" + idclientRespondent + " idSeeker " + idSeeker + " idrespo" + idRespondent);
            if (idRespondent == idclientRespondent) {
                alreadyBounded = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (!alreadyBounded) {
            String query = "insert into client_has_client " +
                    "values (" + idSeeker + ", " + idRespondent + ") ";
            sql.executeUpdate(query);
        }
    }

    public int getIdByToken(String token) {
        int id = 0;
        try {
            ResultSet rs = sql.connection.createStatement().executeQuery("select idclient " +
                    "from client where token = '" + token + "'");
            sql.connection.commit();
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
            ResultSet rs = sql.connection.createStatement().executeQuery("select token " +
                    "from client where idclient = " + id + "");
            sql.connection.commit();
            while (rs.next()) {
                dbRespondentToken = rs.getString(1);
            }
        } catch (Exception e) {

        }
        return dbRespondentToken;
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
        } catch (SQLException e) {
            e.printStackTrace();
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

    /**
     * seeker, respondent, alive <br>
     * 1           0           1  -> seeker requested connection <br>
     * 1           1           1  -> respondent accepted connection <br>
     * 0           1           0  -> seeker stopped connection <br>
     * 1           0           0  -> respondent stopped connection
     *
     * @param object
     * @param clientData
     * @param c
     */
    public void networkPair(Network.Pair object, ClientData clientData, ClientConnection c) {
        Network.Pair pair = (Network.Pair) object;
        int idRespondent = getIdbyTokenJavaServer(pair.tokenPairRespondent);
        int idSeeker = getIdbyTokenJavaServer(pair.tokenPairSeeker);
        checkPair(idSeeker, idRespondent);
        sql.updateClient(idRespondent, pair.seekerAccepted, pair.respondentAccepted, pair.connectionAlive);
        sql.updateClient(idSeeker, pair.seekerAccepted, pair.respondentAccepted, pair.connectionAlive);

        if (pair.connectionAlive) {
            if (pair.seekerAccepted & !pair.respondentAccepted) {
                try {
                    server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (pair.seekerAccepted & pair.respondentAccepted) {
                if (pair.seekerAccepted && pair.respondentAccepted) {
                    try {
                        pair(pair.tokenPairRespondent, pair.tokenPairSeeker);
                        server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                        server.sendToTCP(connections.get(pair.tokenPairSeeker).getID(), pair);
                    } catch (Exception e) {
                    }
                }
            }

        } else {
            sql.updateClient(idRespondent, false, false, false);
            sql.updateClient(idSeeker, false, false, false);

            sql.deleteClientHasClient(idSeeker);
            sql.deleteClientHasClient(idRespondent);
            try {
                unpair(pair.tokenPairRespondent);
                unpair(pair.tokenPairSeeker);
                server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                server.sendToTCP(connections.get(pair.tokenPairSeeker).getID(), pair);
            } catch (Exception e) {
            }
        }

    }

    public void unpair(String incomeToken) {
        ClientConnection income = connections.get(incomeToken);
        if (income.clientData.pair != null) {
            ClientConnection paired = income.clientData.pair;
            paired.clientData.pair = null;
        }
    }

    public void pair(String c1Token, String c2Token) {
        ClientConnection c1 = connections.get(c1Token);
        ClientConnection c2 = connections.get(c2Token);
        c1.clientData.pair = c2;
        c2.clientData.pair = c1;
    }

    public boolean checkPair(String incomeToken) {
        ClientConnection income = connections.get(incomeToken);
        if (connections.size() < 2) {
            return false;
        }
        for (Map.Entry<String, ClientConnection> entry : connections.entrySet()) {
            String key = entry.getKey();
            ClientConnection value = entry.getValue();

            if (!key.equals(incomeToken)) {
                if (value.clientData.systemName.equals(income.clientData.systemName)) {
                    income.clientData.pair = value;
                    value.clientData.pair = income;
                    Network.Register partner = new Network.Register();
                    partner.systemName = income.clientData.systemName;
                    partner.userName = income.clientData.userName;
                    partner.token = income.clientData.token;
                    server.sendToTCP(income.getID(), partner);
                    partner.systemName = value.clientData.systemName;
                    partner.userName = value.clientData.userName;
                    partner.token = value.clientData.token;
                    server.sendToTCP(value.getID(), partner);
                    return true;
                }
            }
        }

        return false;
    }

    public void sendRegisteredUsers() {
        Network.RegisteredUsers registeredUsers = new Network.RegisteredUsers();
        registeredUsers.users = new ArrayList<Network.Register>();
        for (Iterator<ClientData> aa = loggedIn.iterator(); aa.hasNext(); ) {
            ClientData cd = aa.next();
            Network.Register register = new Network.Register();
            register.mainClient = cd.mainClient;
            register.userName = cd.userName;
            register.systemName = cd.systemName;
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
