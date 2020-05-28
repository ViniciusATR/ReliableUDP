package com.ufabc;

import java.util.TimerTask;

public class TimeoutTask extends TimerTask {
    private Packet packet;

    public TimeoutTask(Packet packet){
        this.packet = packet;
    }

    public void run() {
        if (!this.packet.isAcked()){
            System.out.println("-------\nTimeout ocorreu para o pacote: " + packet.getSeq() + "\n-------\n");
            this.packet.unsetSent();
        }
    }
}
