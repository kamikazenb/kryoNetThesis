package cz.utb;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import cz.utb.serialization.Network;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Scanner;

public class KryoClient {
    Client client;
    String name;
    SpeedTestSocket speedTestSocket;
    TokenGenerator tokenGenerator = new TokenGenerator();

    public KryoClient() {
        client = new Client();
        client.start();
        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Network.register(client);
        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new Listener.ThreadedListener(new Listener() {
            public void connected(Connection connection) {
                Network.Register register = new Network.Register();
                register.userName = "testHost1";
                register.token = tokenGenerator.generateRandom(20);
                register.systemName = register.userName;
                client.sendTCP(register);
            }

            public void received(Connection connection, Object object) {
                if (object instanceof Network.Info) {
                    Network.Info info = (Network.Info) object;
                    System.out.println(info.message);
                }
                if (object instanceof Network.Register) {
                    Network.Register register = (Network.Register) object;
                    if (register.userName == null || register.systemName == null) {
                        System.out.println("no connection paired");
                    } else {
                        System.out.println("paired " + register.systemName + " " + register.token);
                    }
                }
                if (object instanceof Network.RegisteredUsers) {
                    Network.RegisteredUsers registeredUsers = (Network.RegisteredUsers) object;
                    try {
                        System.out.println("Registred users on server:");
                        for (int i = 0; i < registeredUsers.users.size(); i++) {
                            System.out.println(registeredUsers.users.get(i).systemName +
                                    " " + registeredUsers.users.get(i).userName +
                                    " " + registeredUsers.users.get(i).token);
                        }
                    } catch (Exception e) {
                    }
                }
            }

        }));
        Thread t = new Thread() {
            public void run() {
                try {
                    //195.178.94.66    localhost
                    client.connect(5000, "localhost", Network.port);
                    // Server communication after connection can go here, or in Listener#connected().
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
        t.start();
        name = "testHost1";
        speedTestSocket = new SpeedTestSocket();

// add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                // called when download/upload is complete
                BigDecimal divisor = new BigDecimal("1000000");
                System.out.println(report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                        report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps ");

            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download/upload error occur
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // called to notify download/upload progress

                BigDecimal divisor = new BigDecimal("1000000");
                System.out.print(percent + "%  " + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                        report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \r");
            }
        });
        new Console();
    }

    public class Console {
        public Console() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String ipnut = scanner.next().trim();
                switch (ipnut) {
                    case "pair":
                        Network.Pair pair = new Network.Pair();
                        pair.tokenPairRespondent =" aa";
                        pair.tokenPairSeeker = "token";
                        pair.seekerAccepted = true;
                        pair.seekerAccepted = true;
                        client.sendTCP(pair);
                    case "start":
                        Network.Integers integers = new Network.Integers();
                        integers.arrayIntegers = new ArrayList<Integer>();
                        for (int i = 0; i < 10; i++) {
                            integers.arrayIntegers.add((Integer) i);
                        }
                        client.sendTCP(integers);
                        break;
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

    public static void main(String[] args) {
        Log.set(Log.LEVEL_INFO);
        System.out.println("starting client");
        new KryoClient();
    }
}
