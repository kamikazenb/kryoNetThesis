package cz.utb.kryonet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cz.utb.KryoServer;


import java.io.IOException;
import java.util.*;

public class MyServer {
    public Server server;
    HashSet<ClientData> loggedIn = new HashSet();
    HashMap<String, ClientConnection> connections = new HashMap<>();

    public MyServer() throws IOException {
        Log.set(Log.LEVEL_INFO);
        server = new Server() {
            protected Connection newConnection() {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new ClientConnection();
            }
        };
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
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
                if (object instanceof Network.TouchMove) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
                if (object instanceof Network.CleanCanvas) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
                if (object instanceof Network.TouchTolerance) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
                if (object instanceof Network.TouchUp) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
                if (object instanceof Network.ScreenSize) {
                    try {
                        server.sendToTCP(connection.clientData.pair.getID(), object);
                    }catch (Exception e){}
                }
            }

            public void disconnected(Connection c) {
                ClientConnection connection = (ClientConnection) c;
                if (connection.clientData != null) {
                    unpair(((ClientConnection) c).clientData.token);
                    connections.remove(connection.clientData.systemName);
                    loggedIn.remove(connection.clientData);
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
        if (userName != null && systemName != null && token != null) {
            System.out.println("registered: user name <" + userName + "> system name:  <"
                    + systemName + "> token <" + token + ">");
            systemName = systemName.trim();

            if (systemName.length() != 0) {
                connection.clientData = new ClientData();
                connection.clientData.userName = userName;
                connection.clientData.systemName = systemName;
                connection.clientData.token = token;
                loggedIn.add(connection.clientData);
                connections.put(connection.clientData.token, connection);
                sendRegisteredUsers();
            }
        }
    }

    public void networkPair(Network.Pair object, ClientData clientData, ClientConnection c) {
        Network.Pair pair = (Network.Pair) object;
        if (connections.get(pair.tokenPairSeeker).clientData.pair != null ||
                connections.get(pair.tokenPairRespondent).clientData.pair != null) {
            if (!pair.seekerAccepted) {
                try {
                    unpair(pair.tokenPairRespondent);
                    unpair(pair.tokenPairSeeker);
                    server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                    server.sendToTCP(connections.get(pair.tokenPairSeeker).getID(), pair);
                } catch (Exception e) {
                }
            }
        } else {
            if (pair.seekerAccepted && !pair.respondentAccepted) {
                try {
                    server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                } catch (Exception e) {
                }
            }
            if (pair.seekerAccepted && pair.respondentAccepted) {
                try {
                    pair(pair.tokenPairRespondent, pair.tokenPairSeeker);
                    server.sendToTCP(connections.get(pair.tokenPairRespondent).getID(), pair);
                    server.sendToTCP(connections.get(pair.tokenPairSeeker).getID(), pair);
                } catch (Exception e) {
                }
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
