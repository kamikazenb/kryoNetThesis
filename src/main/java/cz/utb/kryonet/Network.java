package cz.utb.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.ArrayList;
import java.util.Date;

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
        kryo.register(TouchMove.class);
        kryo.register(TouchStart.class);
        kryo.register(TouchTolerance.class);
        kryo.register(TouchUp.class);
        kryo.register(CleanCanvas.class);
        kryo.register(ScreenSize.class);
        kryo.register(Request.class);
        kryo.register(Speed.class);
        kryo.register(UseDatabase.class);
        kryo.register(java.util.Date.class);
    }
    static public class UseDatabase {
        public boolean useDatabase;
    }
    static public class Speed {
        public float download;
        public float upload;
    }
    static public class Pair {
        public String tokenPairSeeker;
        public String tokenPairRespondent;
        public boolean seekerAccepted;
        public boolean respondentAccepted;
        public boolean connectionAlive;
    }

    static public class Integers{
        public ArrayList<Integer> arrayIntegers;
    }

    static public class Register {
        public boolean mainClient;
        public String userName;
        public String systemName;
        public String token;
    }
    static public class Request{
        public boolean registredUsers;
        public boolean internetSpeed;
    }

    static public class RegisteredUsers {
        public ArrayList<Register> users;
    }

    static public class Info{
        public String message;
    }
    static public class TouchStart{
        float x;
        float y;
        Date clientCreated;
        Date serverReceived;
    }
    static public class TouchMove{
        float x;
        float y;
        Date clientCreated;
        Date serverReceived;
    }
    static public class CleanCanvas{
        boolean cleanCanvas;
    }

    static public class TouchTolerance{
        float TOUCH_TOLERANCE;
    }
    static public  class TouchUp{
        boolean touchUp;
        Date clientCreated;
        Date serverReceived;
    }
    static public class ScreenSize{
        float x;
        float y;
    }
}
