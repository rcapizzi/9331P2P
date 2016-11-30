# 9331P2P
Circular DHT (P2P) program

This was a socket programming project written for COMP9331 Network Applications. The goal of this project was to implement a Circular DHT P2P network where peers are able to share (virtual) files and messages. Peers are able to leave and reconnect at any time without disrupting the network.

The project works by initialising the cdht process on a computer(s), initialising a peer. After several peers are created, they send out their socket address advertisements enabling other peers to form the network. Each peer is interacted with through a command line to choose other peers to send messages to, or leave the network.
