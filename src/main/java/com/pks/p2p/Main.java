package com.pks.p2p;

import com.pks.p2p.configs.Configurations;
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
        String destinationAddress = UserInputUtil.getUserValue(System.in, StringConstants.IP_PATTERN.getValue(),
                "Invalid IP format. Try again: ",
                () -> !p2pManager.isConnected());

        System.out.println("Enter the port number of the peer:");
        String destinationPort = UserInputUtil.getUserValue(System.in, StringConstants.PORT_PATTERN.getValue(),
                "Invalid port format. Try again: ",
                () -> !p2pManager.isConnected());

        while (destinationPort != null && Integer.parseInt(destinationPort) > Configurations.MAX_PORT_NUMBER) {
            System.out.println("Port number is beyond the available range of port numbers. Try again:");
            destinationPort = UserInputUtil.getUserValue(System.in, StringConstants.PORT_PATTERN.getValue(),
                    "Invalid port format. Try again: ",
                    () -> !p2pManager.isConnected());
        }

        if(destinationPort != null && destinationAddress != null) {
            p2pManager.connect(InetAddress.getByName(destinationAddress), Integer.parseInt(destinationPort));
        }

        while(p2pManager.isConnected()) {
            System.out.println("""
                    
                    Enter a message to send to the peer, 'file:[filepath]' to send file to the peer, ':ip!' to output your ip, ':disconnect!' to disconnect and close the program,\
                    
                    ':fragment! to set the fragment size, ':path!' to set the path for saving files, or ':corrupted!' to simulate transmission of the corrupted data:
                    """);

            String line = InputReaderUtil.readInput(System.in, p2pManager::isConnected);

            if (":disconnect!".equals(line)) {
                p2pManager.disconnect();
                continue;
            } else if (":ip!".equals(line)) {
                System.out.println("Your IP address is: " + IPUtil.getIP());
                continue;
            } else if (":fragment!".equals(line)) {
                System.out.println("Enter the size of the fragment:");
                String fragmentSize = UserInputUtil.getUserValue(System.in, StringConstants.FRAGMENT_PATTERN.getValue(), "Invalid fragment format. Try again: ", p2pManager::isConnected);

                while (fragmentSize != null && !Configurations.setFragmentSize(Integer.parseInt(fragmentSize))) {
                    System.out.println("Fragment size is beyond the available range (" + Configurations.MIN_FRAGMENT_SIZE + ", " + Configurations.MAX_FRAGMENT_SIZE + ") of fragment sizes. Try again:");
                    fragmentSize = UserInputUtil.getUserValue(System.in, StringConstants.FRAGMENT_PATTERN.getValue(), "Invalid fragment format. Try again: ", p2pManager::isConnected);
                }
                continue;
            } else if (":corrupted!".equals(line)) {
                System.out.println("Enter the message or 'file:[filepath]' to send the file to the peer:");
                line = InputReaderUtil.readInput(System.in, p2pManager::isConnected);

                if (line != null) {
                    p2pManager.send(line, true);
                }

                continue;
            } else if (":path!".equals(line)) {
                System.out.println("Enter the path for saving files:");
                String path = InputReaderUtil.readInput(System.in, p2pManager::isConnected);

                while (path != null && !Configurations.setDownloadPath(path)) {
                    System.out.println("Invalid path. Try again:");
                    path = InputReaderUtil.readInput(System.in, p2pManager::isConnected);
                }

                continue;
            }

            if (line != null) {
                p2pManager.send(line, false);
            }
        }

        System.out.println("\nClosing socket...");
        p2pManager.stop();
        System.out.println("Socket closed.");
    }
}