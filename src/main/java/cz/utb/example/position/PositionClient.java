package cz.utb.example.position;

import cz.utb.example.position.*;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;

public class PositionClient {
    UI ui;
    Client client;
    String name;

    public PositionClient () {
        client = new Client();
        client.start();

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Network.register(client);

        // ThreadedListener runs the listener methods on a different thread.
        client.addListener(new Listener.ThreadedListener(new Listener() {
            public void connected (Connection connection) {
            }

            public void received (Connection connection, Object object) {
                if (object instanceof Network.RegistrationRequired) {
                    Network.Register register = new Network.Register();
                    register.name = name;
                    register.otherStuff = ui.inputOtherStuff();
                    client.sendTCP(register);
                }

                if (object instanceof Network.AddCharacter) {
                    Network.AddCharacter msg = (Network.AddCharacter)object;
                    ui.addCharacter(msg.character);
                    return;
                }

                if (object instanceof Network.UpdateCharacter) {
                    ui.updateCharacter((Network.UpdateCharacter)object);
                    return;
                }

                if (object instanceof Network.RemoveCharacter) {
                    Network.RemoveCharacter msg = (Network.RemoveCharacter)object;
                    ui.removeCharacter(msg.id);
                    return;
                }
            }

            public void disconnected (Connection connection) {
                System.exit(0);
            }
        }));

        ui = new UI();

        String host = ui.inputHost();
        try {
            client.connect(5000, host, Network.port);
            // Server communication after connection can go here, or in Listener#connected().
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        name = ui.inputName();
        Network.Login login = new Network.Login();
        login.name = name;
        client.sendTCP(login);

        while (true) {
            int ch;
            try {
                ch = System.in.read();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }

            Network.MoveCharacter msg = new Network.MoveCharacter();
            switch (ch) {
                case 'w':
                    msg.y = -1;
                    break;
                case 's':
                    msg.y = 1;
                    break;
                case 'a':
                    msg.x = -1;
                    break;
                case 'd':
                    msg.x = 1;
                    break;
                default:
                    msg = null;
            }
            if (msg != null) client.sendTCP(msg);
        }
    }

    static class UI {
        HashMap<Integer, Character> characters = new HashMap();

        public String inputHost () {
            String input = (String) JOptionPane.showInputDialog(null, "Host:", "Connect to server", JOptionPane.QUESTION_MESSAGE,
                    null, null, "localhost");
            if (input == null || input.trim().length() == 0) System.exit(1);
            return input.trim();
        }

        public String inputName () {
            String input = (String)JOptionPane.showInputDialog(null, "Name:", "Connect to server", JOptionPane.QUESTION_MESSAGE,
                    null, null, "Test");
            if (input == null || input.trim().length() == 0) System.exit(1);
            return input.trim();
        }

        public String inputOtherStuff () {
            String input = (String)JOptionPane.showInputDialog(null, "Other Stuff:", "Create account", JOptionPane.QUESTION_MESSAGE,
                    null, null, "other stuff");
            if (input == null || input.trim().length() == 0) System.exit(1);
            return input.trim();
        }

        public void addCharacter (Character character) {
            characters.put(character.id, character);
            System.out.println(character.name + " added at " + character.x + ", " + character.y);
        }

        public void updateCharacter (Network.UpdateCharacter msg) {
            Character character = characters.get(msg.id);
            if (character == null) return;
            character.x = msg.x;
            character.y = msg.y;
            System.out.println(character.name + " moved to " + character.x + ", " + character.y);
        }

        public void removeCharacter (int id) {
            Character character = characters.remove(id);
            if (character != null) System.out.println(character.name + " removed");
        }
    }

    public static void main (String[] args) {
        Log.set(Log.LEVEL_DEBUG);
        new PositionClient();
    }
}
