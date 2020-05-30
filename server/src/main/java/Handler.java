import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Handler {
    private Socket socket;
    private DataInputStream in;
    private ObjectOutputStream out;
    private ServerMain server;

    public Handler(ServerMain server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new ObjectOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.equals("/path")) {
                                Path path = Paths.get("D:\\");
                                out.writeUTF(path.normalize().toAbsolutePath().toString());
                                out.flush();
                            }

                            if (str.equals("/updateList")) {
                                List<FileInfo> collect = Files.list(Paths.get("D:\\")).map(FileInfo::new).collect(Collectors.toList());
                                out.writeObject(collect);
                                out.flush();
                            }

                            if (str.startsWith("/delete")) {
                                String[] s = str.split(" ", 2);
                                Files.delete(Paths.get(s[1]));
                            }

                            if (str.startsWith("/download")) {
                                String[] s = str.split(" ", 2);
                                String filename = s[1];
                                long length = in.readLong();
                                File file = new File(filename);
                                if (!file.exists()) {
                                    file.createNewFile();
                                }
                                FileOutputStream fos = new FileOutputStream(file);
                                for (long i = 0; i < length; i++) {
                                    fos.write(in.read());
                                }
                                fos.close();
                                System.out.println("File: " + filename + ", downloaded!");
                            }

                            if (str.startsWith("/putMy")) {
                                String[] s = str.split(" ", 2);
                                String filename = s[1];
                                File file = new File(filename);
                                long s1 = file.length();
                                out.writeUTF("/size " + s1);
                                out.writeUTF(file.getName());
                                FileInputStream fileInputStream = new FileInputStream(file);
                                int x;
                                while ((x = fileInputStream.read()) != -1) {
                                    out.write(x);
                                    out.flush();
                                }
                                System.out.println("File: " + filename + ", downloaded!");
                            }

                            if (str.startsWith("/openFolder")) {
                                String[] s = str.split(" ", 2);
                                String path = s[1];
                                List<FileInfo> collect = Files.list(Paths.get(path)).map(FileInfo::new).collect(Collectors.toList());
                                out.writeObject(collect);
                                out.flush();

                            }
//                            if (str.startsWith("/openFolder")) {
//                                String[] s = str.split(" ");
//                                String pathMy = s[1];
//                                Path path = Paths.get(".", pathMy);
//                                List<FileInfo> collect = Files.list(Paths.get(pathMy)).map(FileInfo::new).collect(Collectors.toList());
//                                out.writeObject(collect);
//                                out.flush();
//                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
