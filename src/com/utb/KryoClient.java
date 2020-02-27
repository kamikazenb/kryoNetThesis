package com.utb;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.utb.serialization.Network;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Scanner;

public class KryoClient {
    Client client;
    String name;

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
                register.name = "testHost1";
                client.sendTCP(register);
            }

            public void received(Connection connection, Object object) {
                if (object instanceof Network.Info) {
                    Network.Info info = (Network.Info) object;
                    System.out.println(info.message);
                }
            }
        }));
        try {
            client.connect(5000, "localhost", Network.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        name = "testHost1";
        new Console();

    }

    public class Console {
        public Console() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String ipnut = scanner.next().trim();
                switch (ipnut) {
                    case "start":
                        Network.Integers integers = new Network.Integers();
                        integers.arrayIntegers = new ArrayList<Integer>();
                        for (int i = 0; i < 10; i++) {
                            integers.arrayIntegers.add((Integer) i);
                        }
                        client.sendTCP(integers);
                        int i = 0;
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
