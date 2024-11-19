package com.pks.p2p;

import com.pks.p2p.enums.StringConstants;
import com.pks.p2p.manager.P2PManager;
import com.pks.p2p.util.IPUtil;
import com.pks.p2p.util.InputReaderUtil;
import com.pks.p2p.util.UserInputUtil;

import java.net.*;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws Exception {

        int port = 8080;

        if (args.length > 0){
            if ((args.length & 1) == 0) {
                for (int i = 0; i < args.length; i+=2) {
                    if (args[i].equals("-p")) {
                        port = Integer.parseInt(args[i+1]);
                    }
                }
            }
        } else {
            System.out.println("Enter the port number for your socket:");
            port = Integer.parseInt(Objects.requireNonNull(UserInputUtil.getUserValue(System.in, StringConstants.PORT_PATTERN.getValue(), "Invalid port format. Try again: ", () -> true)));
        }

        P2PManager p2pManager = new P2PManager(port);

        System.out.println("\nSocket is bound to port " + p2pManager.getPort());

        p2pManager.start();

        System.out.println("\nEnter the IP address of the peer:");
        String destinationAddress = UserInputUtil.getUserValue(System.in, StringConstants.IP_PATTERN.getValue(), "Invalid IP format. Try again: ", () -> !p2pManager.isConnected());
        System.out.println("Enter the port number of the peer:");
        String destinationPort = UserInputUtil.getUserValue(System.in, StringConstants.PORT_PATTERN.getValue(), "Invalid port format. Try again: ", () -> !p2pManager.isConnected());

        if(destinationPort != null && destinationAddress != null) {
            p2pManager.connect(InetAddress.getByName(destinationAddress), Integer.parseInt(destinationPort));
        }

        while(p2pManager.isConnected()) {
            System.out.println("\nEnter a message to send to the peer, ':ip!' to output your ip or':exit!' to close the program.");
            String line = InputReaderUtil.readInput(System.in, () -> true);
            if (":exit!".equals(line)) {
                break;
            } else if(":ip!".equals(line)) {
                System.out.println("Your IP address is: " + IPUtil.getIP());
                continue;
            }
            assert line != null;
//            p2pManager.sendData(MessageType.DATA, line);
        }

        System.out.println("Closing socket...");
        p2pManager.stop();
//        connection.getSocket().close();
    }
}