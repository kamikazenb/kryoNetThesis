package cz.utb;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cz.utb.serialization.Network;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class KryoServer {
    Server server;
    HashSet<ClientData> loggedIn = new HashSet();
    HashMap<String, ClientConnection> connections = new HashMap<>();
    SpeedTestSocket speedTestSocket;
    TokenGenerator tokenGenerator = new TokenGenerator();

    public KryoServer() throws IOException {
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
                    Network.Pair pair = (Network.Pair) object;
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
                if (object instanceof Network.Register) {
                    if (clientData != null) {
                        return;
                    }
                    Network.Register register = (Network.Register) object;
                    String userName = register.userName;
                    String systemName = register.systemName;
                    String token = register.token;
                    if (userName != null && systemName != null && token != null) {
                        System.out.println("registered: user name <" + userName + "> system name:  <"
                                + systemName + "> token <" + token + ">");
                        systemName = systemName.trim();

                        if (systemName.length() != 0) {
                            ((ClientConnection) c).clientData = new ClientData();
                            ((ClientConnection) c).clientData.userName = userName;
                            ((ClientConnection) c).clientData.systemName = systemName;
                            ((ClientConnection) c).clientData.token = token;                            
                            loggedIn.add(((ClientConnection) c).clientData);
                            connections.put(((ClientConnection) c).clientData.token, connection);
                            sendRegisteredUsers();
                        }
                    }
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

        new Console();
    }

    public void unpair(String incomeToken) {
        ClientConnection income = connections.get(incomeToken);
        if(income.clientData.pair != null){
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

    public class Console {
        public Console() {
            speedTestSocket = new SpeedTestSocket();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(SpeedTestReport report) {
                    // called when download/upload is complete
                    BigDecimal divisor = new BigDecimal("1000000");
                    System.out.print("[completed] "
                            + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                            report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \n");
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // called to notify download/upload progress
                    BigDecimal divisor = new BigDecimal("1000000");
                    StringBuilder sb = new StringBuilder("[");
                    int round = (int) percent;
                    if (round < 10) {
                        sb.append("..........");
                    } else {
                        int firstDigit = Integer.parseInt(Integer.toString(round).substring(0, 1));
                        for (int i = 0; i < firstDigit - 1; i++) {
                            sb.append("=");
                        }
                        sb.append(">");
                        for (int i = 0; i < 10 - firstDigit; i++) {
                            sb.append(".");
                        }
                    }
                    sb.append("] "
                            + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                            report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \r");
                    System.out.print(sb.toString());
                }
            });
            start();
        }

        public void start() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String ipnut = scanner.next().trim();
                switch (ipnut) {
                    case "download":
                        speedTestSocket.startDownload("ftp://speedtest.tele2.net/5MB.zip");
                        break;
                    case "upload":
                        speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 1000000, 1000);
                        break;
                }

            }
        }
    }

    // This holds per connection state.
    static class ClientConnection extends Connection {
        public ClientData clientData;
    }

    public static void main(String[] args) throws IOException {
        Log.set(Log.LEVEL_INFO);
        System.out.println("Creating server...");
        new KryoServer();
    }
}
