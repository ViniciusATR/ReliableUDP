package com.ufabc;

import java.net.*;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;

public class Client {
    private final int PSIZE = 1024;
    private final int WSIZE = 4;
    private InetAddress address;
    private final long timeout = 600;
    private Timer timer;
    private DatagramSocket socket;
    private LinkedList<Packet> packetQueue; //Fila de envio de pacotes em espera
    private int nextSeq;
    private ArrayBlockingQueue<Packet> window; // Janela de pacotes em processo de envio
    private ClientReceiverThread receiver; // Thread responsável pela receptação de respostas

    public Client() {
        try {
            address =  InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket  = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        this.nextSeq = 0;
        this.window = new ArrayBlockingQueue<Packet>(this.WSIZE);
        this.packetQueue = new LinkedList<Packet>();
        this.receiver = new ClientReceiverThread(this.socket, this.window);
        this.receiver.start();
    }


    private Packet packMessage(String data){

        Packet packet = new Packet(this.nextSeq, PacketType.DATA, data.getBytes());

        this.nextSeq++;

        return packet;
    }

    /*
    * Implementação pacote fora de ordem:
    * Para simular o pacote fora de ordem a mensagem é colocada
    * no topo da fila e n no fim da fila
    * */
    private void enqueueOutOfOrder(Packet packet) {
        this.packetQueue.addFirst(packet);
    }

    private void enqueuePacket(Packet packet) {
        this.packetQueue.addLast(packet);
    }

    /*
    * Método gerenciador da janela/buffer do sender
    * coloca pacotes da fila na janela e remove pacotes já acked da janela
    * */
    private void updateWindow() {

        // Remover pacotes que foram ACKED da janela
        for (Packet packet:this.window) {
            if (packet.isAcked()) {
                this.window.remove(packet);
            }
        }

        //Adicionar pacote a janela caso n esteja cheia
        if (this.window.size() >= this.WSIZE) {
            return;
        } else {
            this.window.add( this.packetQueue.pop() );
        }
    }

    public void sendData(String data, boolean outOfOrder, boolean duplicate, boolean lost, boolean slow) {

        byte [] currentPck = new byte[this.PSIZE];
        Packet newPacket = packMessage(data);
        int newPacketSeq = newPacket.getSeq();

        //Se for fora de ordem, furar a fila
        if(outOfOrder) {
            enqueuePacket(newPacket);
        }else{
            enqueueOutOfOrder(newPacket);
        }

        updateWindow();
        this.timer = new Timer();

        //Enviar todas as mensagens n enviadas ainda na janela
        for (Packet packet:this.window){
            if (!packet.is_sent()) {
                try {
                    currentPck = Packet.toBytes(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                DatagramPacket datagramPacket = new DatagramPacket(currentPck,
                        currentPck.length,
                        this.address,
                        9876);

                try {

                    /*
                    * Para simular o atraso, o timer de timeout é iniciado como se o pacote tivesse sido enviado
                    * e o pacote é enviado somente após um periodo de espera maior que o tempo de timeout
                    * */
                    if(slow) {
                        TimeoutTask packetTimeout = new TimeoutTask(packet);
                        this.timer.schedule(packetTimeout, this.timeout);
                        Thread.sleep(1000);
                        this.socket.send(datagramPacket);
                    }

                    /*
                    * Para simular a perda, o pacote é marcado como enviado
                    * e o timer de timeout também é ativado sem o envio real do pacote
                    * */
                    if(!lost && !slow) {
                        this.socket.send(datagramPacket);
                    }
                    // marcar que um pacote já foi enviado
                    packet.sent();
                    // Iniciar timer de timeout
                    TimeoutTask packetTimeout = new TimeoutTask(packet);
                    this.timer.schedule(packetTimeout, this.timeout);

                    /*
                    * Para simular duplicidade, após a rota normal, o pacote é reenviado.
                    * */
                    if(duplicate){
                        if(packet.getSeq() == newPacketSeq){
                            this.socket.send(datagramPacket);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("-------\nMensagem: " + new String(packet.getData()) + " enviada");
            }
        }
    }

    public void close(){
        this.socket.close();
    }

    public static void main (String args[]) {

        Client client = new Client();
        int typeMessage;
        String input;
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("-------\nDigite o número correspondente a forma da mensagem que gostaria de enviar" +
                    " + espaço em branco + a mensagem diretamente após o número e então pressione enter:\n1:Normal\n2:Fora De Ordem\n3:Duplicado\n4:Perdido\n5:Lento");
            typeMessage = in.nextInt();
            input = in.nextLine();

            System.out.println(input);
            switch (typeMessage) {
                case(1):
                    client.sendData(input,false, false, false, false);
                    break;
                case(2):
                    client.sendData(input,true, false, false, false);
                    break;
                case(3):
                    client.sendData(input, true, true, false, false);
                    break;
                case(4):
                    client.sendData(input,true, false, true, false);
                    break;
                case(5):
                    client.sendData(input,true, false, false, true);
                    break;
            }
        }

    }
}
