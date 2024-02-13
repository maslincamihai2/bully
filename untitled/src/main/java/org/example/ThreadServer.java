package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadServer extends Thread {

    @Override
    public void run() {
        // declarare variabile
        ServerSocket socketServer = null;

        // initializare
        try {
            socketServer = new ServerSocket(Main.portServerLocal);
        } catch (Exception e) {
            // portul pe care se porneste serverul local poate fi folosit de alt proces
            e.printStackTrace();
            System.out.println("eroare pornire server");
        }

        while (Thread.interrupted() == false) {
            try {
                // asteapta conexiune noua initializata de un client
                Socket socketClient = socketServer.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                PrintWriter out = new PrintWriter(socketClient.getOutputStream(), true);

                // citire sir de caractere primit
                String sirCaractere = in.readLine();

                System.out.println("sir de caractere primit:");
                System.out.println(sirCaractere);
                System.out.println("-----------------------------------------------");

                //se imparte sirul de caractere primit in subsiruri
                //primul subsir pana la spatiu reprezinta un mesaj
                //celelalte subsiruri sunt argumente, de exemplu cine a trimis mesajul
                //limitam numarul de subsiruri la 3 deoarece nu avem comenzi cu mai mult de 2 argumente
                //iar al doiea argument poate fi un json care contine spatii
                String[] subsiruri = sirCaractere.split(" ", 3);
                String mesaj = subsiruri[0];


               if (mesaj.equals("Heartbeat")) {
                    out.println("Alive");
                } else if (mesaj.equals("Coordinator")) {
                    // obtinem id-ul noului lider
                    int idLiderNou = Integer.parseInt(subsiruri[1]);
                    // actualizam variabila locala
                    Main.idLider = idLiderNou;
                    //trimitem acknoledge
                    out.println("Ack");

                    System.out.println("Lider nou " + idLiderNou);
                    System.out.println("-----------------------------------------------");

                } else if (mesaj.equals("Election")) {
                    // opreste heartbeat prin marcare cu -1 a liderului
                   Main.idLider = -1;
                    // trimite ok inapoi la peer cu id mai mic
                    out.println("Ok");
                    // trimite mesaj Election mai departe la urmatorul peer cu id mai mare
                   Main.bullyElection();
                } else {
                    out.println("COMANDA INEXISTENTA IN IF CASE PE SERVER THREAD");
                    System.out.println("COMANDA INEXISTENTA IN IF CASE PE SERVER THREAD");
                    System.out.println("************************************************");
                }

                //eliberare resurse
                in.close();
                out.close();
                socketClient.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("eroare server");
            }
        }
    }
}
