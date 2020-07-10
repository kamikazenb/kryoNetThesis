package cz.utb.kryonet;

import cz.utb.KryoServer;

import java.sql.Connection;

public class ClientData {
    public boolean mainClient;
    public String systemName;
    public String userName;
    public String token;
    public int id;
    public MyServer.ClientConnection pair;
}
