import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Handler {
    private Socket socket;
    ObjectInputStream in;
    private ObjectOutputStream out;
    private ServerMain server;

    public Handler(ServerMain server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
//            this.in = new ObjectInputStream(socket.getInputStream());
            this.out = new ObjectOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            System.out.println("/updatelist");
                            List<FileInfo> collect = Files.list(Paths.get(".")).map(FileInfo::new).collect(Collectors.toList());
                            out.writeObject(collect);
                            out.flush();
                            String str = in.readUTF();
//                            if (str.startsWith("/auth")) {
//
                            break;
//                            }
                        }

//                        while (true) {
//                            String str = in.readUTF();
//                            if (str.startsWith("/")) {
//                                if (str.equals("/end")) {
//
//                                    break;
//                                }
//
//                                if (str.startsWith("/blacklist ")) {
//
//                                }
//                            }
//                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
//                        try {
//                            in.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
