package cz.utb.kryonet;

import com.esotericsoftware.kryonet.Connection;
import cz.utb.KryoServer;

public class ClientData extends Connection {
    public String userName;
    public String token;
    public int id;
    public String followClient;
}
