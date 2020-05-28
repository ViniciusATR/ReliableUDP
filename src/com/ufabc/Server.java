package com.ufabc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;


public class Server {
    private final int PSIZE = 1024;
    private final int WSIZE = 4;
    private DatagramSocket socket;
    private InetAddress clientAddress;
    private HashSet<Integer> ackedPackets; //set para armazenar numeros de sequencia ja acked
    private int clientPort;
    private int windowBase; //Menor numero de pacote ainda n recebido
    private int nextAck; //O valor do próximo ACK a ser enviado, inicializado com 0
    private ArrayBlockingQueue<Packet> window; //Janela/buffer do receiver

    public Server(){
        try {
            this.socket = new DatagramSocket(9876);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.window = new ArrayBlockingQueue<Packet>(this.WSIZE);
        this.nextAck = 0;
        this.windowBase = 0;
        this.ackedPackets = new HashSet<Integer>();
    }


    public void setNextSeq(int nextSeq) {
        this.nextAck = nextSeq;
    }

    public void receiveData(){
        byte [] currentPck = new byte[this.PSIZE];
        DatagramPacket datagramPacket = new DatagramPacket(currentPck, currentPck.length);

        try {
            this.socket.receive(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.clientAddress = datagramPacket.getAddress();
        this.clientPort = datagramPacket.getPort();

        try {
            Packet rePacket = Packet.fromBytes(datagramPacket.getData());

            if(this.ackedPackets.contains(rePacket.getSeq())) {
                this.sendData("Pacote Duplicado: " + rePacket.getSeq());
            }else if(this.window.size() < this.WSIZE) {
                this.window.add(rePacket);
                this.ackedPackets.add(rePacket.getSeq());
                this.setNextSeq(rePacket.getSeq());
                this.sendData("ACK do pacote " + rePacket.getSeq());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    * Método para entregar as mensagens no buffer/janela na ordem correta
    * */
    public void deliverMessages(){
        ArrayList<Packet> currentBuffer = new ArrayList<Packet>(this.window.size());
        for(Packet packet: this.window){
            currentBuffer.add(packet);
        }
        Collections.sort(currentBuffer, Comparator.comparingInt(Packet::getSeq));

        for(Packet packet: currentBuffer){
            if(packet.getSeq() == this.windowBase){
                System.out.println(new String(packet.getData(), 0, packet.getData().length));
                this.window.remove(packet);
                this.windowBase++;
            }
        }
    }

    private Packet packMessage(String data){

        Packet packet = new Packet(this.nextAck, PacketType.ACK, data.getBytes());
        this.nextAck++;

        return packet;
    }

    private void sendData(String data) {
        byte [] currentPck = new byte[this.PSIZE];
        Packet packet = packMessage(data);

        try {
            currentPck = Packet.toBytes(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatagramPacket datagramPacket = new DatagramPacket(currentPck,
                currentPck.length,
                this.clientAddress,
                this.clientPort);

        try {
            this.socket.send(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("-------\nMensagem: " + data + " enviada");
    }

    private void ackPack(){

    }

    public void close(){
        this.socket.close();
    }

    public static void main(String[] args) {
        Server server = new Server();

        while(true){
            server.receiveData();
            server.deliverMessages();
        }
    }
}
