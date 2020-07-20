package cz.utb.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;
import java.util.Date;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;
import java.util.Date;

public class Network {
    static public final int port = 50201;
    public static final String TOUCH_START = "TouchStart";
    public static final String TOUCH_MOVE = "TouchMove";
    public static final String TOUCH_UP = "TouchUp";

    static public void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ArrayList.class);
        kryo.register(Integers.class);
        kryo.register(Register.class);
        kryo.register(RegisteredUsers.class);
        kryo.register(Touch.class);
        kryo.register(UseDatabase.class);
        kryo.register(java.util.Date.class);
        kryo.register(FollowClient.class);
    }

    static public class UseDatabase {
        public boolean useDatabase;
    }

    static public class FollowClient {
        public String token;
        public boolean follow;
    }

    static public class Integers {
        public ArrayList<Integer> arrayIntegers;
    }

    static public class Register {
        public String userName;
        public String token;
    }


    static public class RegisteredUsers {
        public ArrayList<Register> users;
    }

    static public class Touch {
        float x;
        float y;
        String touchType;
        Date clientCreated;
    }

}
