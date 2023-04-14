package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {

    public GameServerUDP(int localPort) throws IOException {
        super(localPort, ProtocolType.UDP);
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int senderPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");

        if (msgTokens.length > 0) {
            if (msgTokens[0].compareTo("join") == 0) {
                try {
                    IClientInfo ci;
                    ci = getServerSocket().createClientInfo(senderIP, senderPort);
                    UUID clientID = UUID.fromString(msgTokens[1]);
                    addClient(ci, clientID);
                    System.out.println("Join request received from - " + clientID.toString());
                    sendJoinedMessage(clientID, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // format: create, localid, (x,y,z)
            if (msgTokens[0].compareTo("create") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                sendCreateMessages(clientID, pos);
                sendWantsDetailsMessage(clientID);
            }

            if (msgTokens[0].compareTo("bye") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                System.out.println("Exit request received from - " + clientID.toString());
                sendByeMessages(clientID);
                removeClient(clientID);
            }
            // case where server receives a DETAILS-FOR message
            if (msgTokens[0].compareTo("dsfr") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                UUID remoteID = UUID.fromString(msgTokens[2]);
                String[] pos = { msgTokens[3], msgTokens[4], msgTokens[5] };
                sendDetailsForMessage(clientID, remoteID, pos);

            }
            // case where server receives a MOVE message
            if (msgTokens[0].compareTo("move") == 0) {
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
                sendMoveMessages(clientID, pos);
            }

        }
    }

    private void sendDetailsForMessage(UUID clientID, UUID remoteID, String[] pos) {
        try {
            String message = new String("dsfr," + remoteID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMoveMessages(UUID clientID, String[] pos) {
        try {
            String message = new String("move," + clientID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendByeMessages(UUID clientID) {
        try {
            String message = new String("Goodbye!" + clientID.toString());

            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendWantsDetailsMessage(UUID clientID) {
        try {
            String message = new String("wsds," + clientID.toString());
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCreateMessages(UUID clientID, String[] pos) {
        try {
            String message = new String("create," + clientID.toString());
            message += "," + pos[0];
            message += "," + pos[1];
            message += "," + pos[2];
            forwardPacketToAll(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendJoinedMessage(UUID clientID, boolean success) {
        try {
            String message = new String("join,");
            if (success) {
                message += "success";
            } else {
                message += "failure";
            }
            sendPacket(message, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}