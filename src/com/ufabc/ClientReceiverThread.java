package com.ufabc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ArrayBlockingQueue;

public class ClientReceiverThread extends Thread {
    private final int PSIZE = 1024;
    private DatagramSocket socket;
    private ArrayBlockingQueue<Packet> window;

    public ClientReceiverThread(DatagramSocket socket, ArrayBlockingQueue<Packet> window){
        this.socket = socket;
        this.window = window;
    }

    public void run(){
        while(true) {
            byte [] currentPck = new byte[this.PSIZE];
            DatagramPacket datagramPacket = new DatagramPacket(currentPck, currentPck.length);

            try {
                this.socket.receive(datagramPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Packet rePacket = Packet.fromBytes(datagramPacket.getData());
                // Registrar ACK de pacote recebido
                for (Packet packet: this.window){
                    if (packet.getSeq() == rePacket.getSeq()){
                        packet.ack();
                    }
                }
                System.out.println("-------\n" + new String(rePacket.getData(), datagramPacket.getOffset(), rePacket.getData().length) + "\n-------");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
