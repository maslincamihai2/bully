package org.example;

public class ThreadHeartbeat extends Thread {

    @Override
    public void run() {
        int contorEroare = 0;

        while (Thread.interrupted() == false) {
            try {
                // heartbeat se trimite din 3 in 3 secunde
                Thread.sleep(3000);

                // -1 marcheaza lipsa liderului
                // thread-ul nu poate fi oprit deoarece este interzisa repornirea
                if (Main.idLider == -1) {
                    continue;
                }
                // un peer care e lider nu isi trimite heartbeat singur
                if (Main.idLider == Main.id) {
                    continue;
                }

                // se trimite heartbeat
                String raspuns = Main.trimiteMesaj(Main.idLider, "Heartbeat");

                // se verifica raspunsul primit in urma heartbeat-ului
                if (raspuns.equals("Alive")) {
                    // dupa un Heartbeat efectuat cu succes se reseteaza contorul
                    contorEroare = 0;
                } else {
                    contorEroare++;
                    // dupa 3 erori consecutive se considera ca liderul a iesit din retea
                    if (contorEroare >= 3) {
                        contorEroare = 0;

                        if (Main.idLider != -1) {
                            // marcam lipsa liderului
                            Main.idLider = -1;

                            System.out.println("Liderul a parasit reteaua, se incepe electia");
                            System.out.println("-----------------------------------------------");

                            Main.bullyElection();
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
