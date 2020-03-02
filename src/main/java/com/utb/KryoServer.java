package com.utb;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.utb.serialization.Network;

import java.io.IOException;
import java.util.HashSet;

public class KryoServer {
    Server server;
    HashSet<ClientData> loggedIn = new HashSet();

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
                if(object instanceof Network.Integers){
                    System.out.println(((Network.Integers) object).arrayIntegers.size());
                }
                if(object instanceof Network.Info){
                    System.out.println(((Network.Info)object).message);
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
        System.out.println("its done");
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
