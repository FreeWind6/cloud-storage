import db.HibernateUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerMain {

    public ServerMain() {
        ServerSocket server = null;
        Socket socket = null;
        HibernateUtil hibernateUtil;

        try {
            server = new ServerSocket(8189);
            hibernateUtil = new HibernateUtil();


            Thread t1 = new Thread(() -> {
                System.out.println("Для выхода введите /exit для справки help");
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String str = scanner.nextLine();
                    if (str.equals("/exit")) {
                        System.exit(0);
                        break;
                    }

                    if (str.contains("help")) {
                        System.out.println("Справка по командам:");
                        System.out.println("/exit - выход из программы");
                    }
                }
            });
            t1.start();

            while (true) {
                socket = server.accept();
                System.out.println("Client connect!");
                new Handler(this, socket, hibernateUtil);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
