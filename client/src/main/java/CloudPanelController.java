import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ResourceBundle;

public class CloudPanelController implements Initializable {
    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    TextField pathField;

    ObjectInputStream in;
    DataOutputStream out;
    Socket socket;
    Boolean connect;

    final String IP_ADPRESS = "localhost";
    final int PORT = 8189;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    try {
                        out.writeUTF("/isDir " + path);
                        String isDir = in.readUTF();
                        if (isDir.equals("true")) {
                            System.out.println(path.toAbsolutePath().toString());
                            openFolder(path.toAbsolutePath().toString());
                            pathField.setText(path.toAbsolutePath().toString());
                        } else {
                            out.writeUTF("/putMy " + path);
                            String readUTF = in.readUTF();
                            String[] s1 = null;
                            if (readUTF.startsWith("/size")) {
                                s1 = readUTF.split(" ", 2);
                            }
                            long length = Long.parseLong(s1[1]);
                            File folder = new File("temp");
                            if (!folder.exists()) {
                                folder.mkdir();
                            }
                            File file = new File("temp\\" + filesTable.getSelectionModel().getSelectedItem().getFilename());
                            System.out.println(file.getAbsolutePath());
                            file.createNewFile();
                            FileOutputStream fos = new FileOutputStream(file);
                            for (long i = 0; i < length; i++) {
                                fos.write(in.read());
                            }
                            fos.close();
                            Desktop desktop = null;
                            if (Desktop.isDesktopSupported()) {
                                desktop = Desktop.getDesktop();
                            }
                            try {
                                desktop.open(new File(file.getAbsolutePath()));
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                            System.out.println("File: " + file.getName() + ", downloaded!");
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void openFolder(String path) throws IOException, ClassNotFoundException {
        out.writeUTF("/openFolder " + path);
        filesTable.getItems().clear();
        filesTable.getItems().addAll((Collection<? extends FileInfo>) in.readObject());
        filesTable.sort();
    }

    public void btnPathUpActionCloud(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        openFolder(upperPath.toAbsolutePath().toString());
        pathField.setText(upperPath.toAbsolutePath().toString());
    }

    public void startList() {

        try {
            out.writeUTF("/updateList");
            filesTable.getItems().clear();
            filesTable.getItems().addAll((Collection<? extends FileInfo>) in.readObject());
            filesTable.sort();

            out.writeUTF("/path");
            pathField.setText(in.readUTF());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }


    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }

    public void connect() {
        try {
            socket = new Socket(IP_ADPRESS, PORT);
            in = new ObjectInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            startList();
            connect = true;

            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            System.out.println(str);
                            if (str.startsWith("/delete ok ")) {
                                String[] mass = str.split(" ");
                                System.out.println(mass[3]);
                                break;
                            }
                        }

                        while (true) {
                            String str = in.readUTF();
                            System.out.println(str);
                            if (str.equals("/serverclosed")) break;
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
            }).start();*/
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер не доступен!", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnConnect(ActionEvent actionEvent) {
        connect();
    }
}
