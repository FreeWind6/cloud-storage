import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerMain {

    public ServerMain() {
        ServerSocket server = null;
        Socket socket = null;
        Scanner scanner;

        try {
            scanner = new Scanner(System.in);
            server = new ServerSocket(8189);


            Thread t1 = new Thread(() -> {
                System.out.println("Для выхода введите /exit");
                while (true) {
                    String str = scanner.nextLine();
                    if (str.equals("/exit")) {
                        System.exit(0);
                        break;
                    }
                }
            });
            t1.start();

            while (true) {
                socket = server.accept();
                System.out.println("Client connect!");
                new Handler(this, socket);
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
