# Communication over UDP - PKS Assignment

This project is a solution to the **Communication over UDP** assignment for the PKS course at FIIT STU. The goal is to implement a reliable communication system over the inherently unreliable UDP protocol, ensuring proper error handling and recovery using specified ARQ methods.

## Overview

The solution establishes a Peer-to-Peer (P2P) communication system over a local Ethernet network. The communication protocol is built on top of UDP, with reliability achieved through the implementation of Automatic Repeat Request (ARQ) method, error detection, and recovery mechanisms.

### Key Features

- **P2P Communication**: Direct message exchange between peers using UDP.
- **ARQ Support**: Implements the following ARQ method:
    - Improved Go-Back-N/Selective Repeat
- **Error Handling**:
    - Error detection using checksums.
    - Positive (ACK) and negative (NACK) acknowledgments.
- **Custom Protocol**: Defines message formats for data, ACK, and NACK communication.

---

## Getting Started

### Prerequisites

- Java Runtime Environment (JRE) 11 or higher
- Basic understanding of networking and UDP
- Local Ethernet network setup for testing

### Installation

1. Build the project using Gradle (a prepared Gradle build configuration is included):
   ```bash
   ./gradlew build
   ```

### Configuration

- Use the `-p` option to specify the port your connection will listen on.

---

## Usage

### Running the Application

1. Start the program:

   ```bash
   java -jar pks_p2p.jar
   ```

2. After establishing the connection, you will be prompted with available commands for sending data, changing fragment sizes, setting the default path for received files, etc.

### Options

- Use command-line arguments to bind your socket to a specific port (e.g., 8080):
  ```bash
  java -jar pks_p2p.jar -p 8080
  ```

---

## Implementation Details

### ARQ Methods

1. **Improved Go-Back-N/Selective Repeat**:
    - Combines optimizations to improve throughput and reduce latency in error recovery.

### Protocol Details

- **Message Types**:
    - Data packets
    - ACK packets
    - NACK packets
- **Error Detection**:
    - Uses checksums to verify packet integrity.
- **Timeout Handling**:
    - Configurable timeout intervals for retransmissions.

---

## Troubleshooting

- **Connection Issues**:
    - Ensure UDP ports are not blocked by the firewall.
- **Timeout Errors**:
    - Adjust timeout settings based on network latency.

---

## License

This project is licensed under the Apache-2.0 License. See the `LICENSE` file for more details.

