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
import java.util.HashSet;
import java.util.Scanner;

public class KryoServer {
    Server server;
    HashSet<ClientData> loggedIn = new HashSet();
    SpeedTestSocket speedTestSocket;

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
                if (object instanceof Network.Register) {
                    if (clientData != null) {
                        return;
                    }
                    System.out.println("register");
                    Network.Register register = (Network.Register) object;
                    String name = register.name;
                    if (name != null) {
                        name = name.trim();
                        if (name.length() != 0) {
                            clientData = new ClientData();
                            clientData.name = name;
                            Network.Info info = new Network.Info();
                            info.message = name + " connected";
                            server.sendToAllTCP(info);
                            info.message = "welcome";
                            server.sendToTCP(connection.getID(), info);
                            loggedIn.add(clientData);
                        }
                    }

                }
            }

            public void disconnected(Connection c) {
                ClientConnection connection = (ClientConnection) c;
                if (connection.clientData != null) {
                    loggedIn.remove(connection.clientData);
                    Network.Info info = new Network.Info();
                    info.message = "disconnected " + connection.clientData.name;
                    server.sendToAllTCP(info);
                }
            }
        });
        server.bind(Network.port);
        server.start();

        new Console();
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
                            +report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
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
                    System.out.print("[testing] "+percent + "%  "
                            + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                            report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \r");
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
