package com.ufabc;

import java.io.*;
import java.util.Comparator;


public class Packet implements Serializable {

    private byte[] data;
    private int seq;
    private boolean acked;
    private boolean is_sent;
    private PacketType type;

    public Packet(int seq, PacketType type, byte[] data){
        this.data = data;
        this.type = type;
        this.seq  = seq;
        this.acked = false;
        this.is_sent = false;
    }

    public void sent(){
        this.is_sent = true;
    }

    public void unsetSent() {
        this.is_sent = false;
    }

    public boolean is_sent(){
        return this.is_sent;
    }

    public void ack(){
        this.acked = true;
    }

    public boolean isAcked() {
        return this.acked;
    }

    public PacketType getType() {
        return type;
    }

    public int getSeq() {
        return seq;
    }

    public byte[] getData() {
        return data;
    }

    public static byte[] toBytes(Packet packet) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

        objectOutputStream.writeObject(packet);
        objectOutputStream.flush();

        return outputStream.toByteArray();
    }
    
    public static Packet fromBytes(byte[] packet) throws Exception {
        ByteArrayInputStream inputStream;
        inputStream = new ByteArrayInputStream(packet);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        Packet result = (Packet) objectInputStream.readObject();

        return result;
        
    }
}
