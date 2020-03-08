package cz.utb.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;

public class Network {
    static public final int port = 50201;

    static public void register (EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ArrayList.class);
        kryo.register(Integers.class);
        kryo.register(Register.class);
        kryo.register(Info.class);
        kryo.register(RegisteredUsers.class);
        kryo.register(Pair.class);
    }
    static public class Pair{
        public String tokenPairSeeker;
        public String tokenPairRespondent;
        public boolean seekerAccepted;
        public boolean respondentAccepted;
    }

    static public class Integers{
        public ArrayList<Integer> arrayIntegers;
    }

    static public class Register {
        public String userName;
        public String systemName;
        public String token;
    }
    static public class RegisteredUsers {
        public ArrayList<Register> users;
    }

    static public class Info{
        public String message;
    }
}
