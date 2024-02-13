package org.example;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {

    public static int id;
    public static int portServerLocal;
    public static HashMap<Integer, String> perechiIdIp = new HashMap<>();
    public static HashMap<Integer, Integer> perechiIdPort = new HashMap<>();
    public static int idLider = -1;

    public static void main(String[] args) {
        // daca nu sunt date argumentele necesare, programul se opreste imediat
        if (args.length != 2) {
            System.exit(-1);
        }

        // citire argumente
        id = Integer.parseInt(args[0]);
        portServerLocal = Integer.parseInt(args[1]);

        // initializare hashmaps
        perechiIdIp.put(1, "127.0.0.1");
        perechiIdIp.put(2, "127.0.0.1");
        perechiIdIp.put(3, "127.0.0.1");
        perechiIdIp.put(4, "127.0.0.1");

        perechiIdPort.put(1, 5001);
        perechiIdPort.put(2, 5002);
        perechiIdPort.put(3, 5003);
        perechiIdPort.put(4, 5004);

        // sterge ip si port propriu din dictionare
        // ca sa nu trimita un peer mesaj catre ele insusi
        perechiIdIp.remove(id);
        perechiIdPort.remove(id);

        System.out.println("ID: " + id + ", port: " + portServerLocal);
        System.out.println(perechiIdIp.toString());
        System.out.println(perechiIdPort.toString());

        // pornire server pe alt fir de executie
        ThreadServer threadServer = new ThreadServer();
        threadServer.start();

        // pornire thread heartbeat
        ThreadHeartbeat threadHeartbeat = new ThreadHeartbeat();
        threadHeartbeat.start();

        // se declanseaza electia de fiecare data cand un peer nou intra in retea
        bullyElection();
    }

    /**
     * Metoda pentru trimiterea unui mesaj catre un alt peer cand se cunoaste id-ul acestuia
     * @param idDestinatar este id-ul acelui peer caruia i se trimite mesajul
     * @param mesaj este continutul mesajului
     * @return raspunsul primit in urma mesajului sau sir gol ("") in caz de eroare
     */
    public static String trimiteMesaj(int idDestinatar, String mesaj) {
        //gasire ip si port destinatar in dictionare pe baza id-ului(cheii)
        String ipDestinatar = perechiIdIp.get(idDestinatar);
        int portDestinatar = perechiIdPort.get(idDestinatar);

        try {
            Socket socketClient = new Socket(ipDestinatar, portDestinatar);

            BufferedReader in = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            PrintWriter out = new PrintWriter(socketClient.getOutputStream(), true);

            out.println(mesaj);
            String raspuns = in.readLine();

            System.out.println("Raspuns pentru mesajul " + mesaj + " catre " + idDestinatar);
            System.out.println(raspuns);
            System.out.println("-----------------------------------------------");
            return raspuns;
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("eroare trimitere mesaj " + mesaj + " catre " + idDestinatar);
            System.out.println("************************************************");
            return "";
        }
    }

    public static ArrayList<String> broadcast(String mesaj) {
        System.out.println("Am trimis broadcast mesajul: " + mesaj);
        System.out.println("-----------------------------------------------");

        ArrayList<String> raspunsuriPrimite = new ArrayList<>();

        Set<Map.Entry<Integer, String>> entrySet = perechiIdIp.entrySet();
        for (Map.Entry<Integer, String> entry : entrySet) {
            int id = entry.getKey();
            String raspuns = trimiteMesaj(id, mesaj);
            raspunsuriPrimite.add(raspuns);
        }

        return raspunsuriPrimite;
    }

    public static ArrayList<Integer> cautaPeersIdMaiMare() {
        ArrayList<Integer> colectiePeersIdMaiMare = new ArrayList<>();
        Set<Map.Entry<Integer, String>> entrySet = perechiIdIp.entrySet();
        for (Map.Entry<Integer, String> entry : entrySet) {
            int idPeer = entry.getKey();
            if (idPeer > id) {
                colectiePeersIdMaiMare.add(idPeer);
            }
        }
        return colectiePeersIdMaiMare;
    }

    public static boolean sendElectionMessage() {
        // se cauta in colectie acei peers id mai mare decat peer curent
        ArrayList<Integer> colectiePeersIdMaiMare = cautaPeersIdMaiMare();

        boolean receivedOk = false;

        // se trimite un mesaj de electie pe rand catre fiecare peer cu id mai mare decat acest peer
        for (int id : colectiePeersIdMaiMare) {
            String raspuns = trimiteMesaj(id, "Election");
            // daca cineva raspunde cu Ok
            if (raspuns.equals("Ok")) {
                // returneaza true
                // asteapta un alt peer sa anunte cine e coordonator
                receivedOk = true;
            }
        }
        // altfel returneaza false
        // si va deveni el insusi coordinator
        return receivedOk;
    }


    public static void bullyElection() {
        // se trimite un mesaj "Election" catre acei peers cu id mai mare
        // daca unul raspunde cu Ok se returneaza true
        boolean okFlag = sendElectionMessage();

        // situatia exceptionala in care nimeni nu raspunde la mesajul Election
        // adica peer precedent detecteaza caderea liderului
        // de exemplu idLider = 6, idPeerCurent = 5
        if (okFlag == false) {
            // acest peer devine lider
            idLider = id;

            // transmite un mesaj Coordinator catre toti ceilalti
            broadcast("Coordinator " + id);

            System.out.println("Am devenit lider!");
            System.out.println("-----------------------------------------------");
        }
    }
}