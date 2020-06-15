import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ResourceBundle;

public class CloudPanelController implements Initializable {

    @FXML
    HBox isNotAuthPanel, isAuthPanel;

    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    TextField pathField, loginField, passwordField;

    private boolean isAuthorized;

    ObjectInputStream in;
    DataOutputStream out;
    Socket socket;
    Boolean connect;
    String startPath;

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
                    try {
                        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                        out.writeUTF("/isDir " + path);
                        String isDir = in.readUTF();
                        if (isDir.equals("true")) {
                            openFolder(path.toString());
                            pathField.setText(path.toString());
                        } else {
                            out.writeUTF("/getFile " + path);
                            File folder = new File("temp");
                            if (!folder.exists()) {
                                folder.mkdir();
                            }
                            File file = new File("temp\\" + filesTable.getSelectionModel().getSelectedItem().getFilename());
                            System.out.println(file.getAbsolutePath());
                            file.createNewFile();
                            byte[] buffer = new byte[1024];
                            FileOutputStream fos = new FileOutputStream(file);
                            while (true) {
                                int read = in.read(buffer);
                                if (read == 1024) {
                                    fos.write(buffer);
                                } else {
                                    byte[] teil = new byte[read];
                                    if (read >= 0) {
                                        System.arraycopy(buffer, 0, teil, 0, read);
                                    }
                                    fos.write(teil);
                                    break;
                                }
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
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер не доступен!", ButtonType.OK);
                        alert.showAndWait();
                    } catch (NullPointerException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Ничего не выбрано!", ButtonType.OK);
                        alert.showAndWait();
                    }
                }
            }
        });
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            isAuthPanel.setVisible(false);
            isAuthPanel.setManaged(false);
            isNotAuthPanel.setVisible(true);
            isNotAuthPanel.setManaged(true);
        } else {
            isAuthPanel.setVisible(true);
            isAuthPanel.setManaged(true);
            isNotAuthPanel.setVisible(false);
            isNotAuthPanel.setManaged(false);
        }
    }

    private void openFolder(String path) throws IOException, ClassNotFoundException {
        out.writeUTF("/openFolder " + path);
        filesTable.getItems().clear();
        filesTable.getItems().addAll((Collection<? extends FileInfo>) in.readObject());
        filesTable.sort();
    }

    public void btnPathUpActionCloud(ActionEvent actionEvent) throws IOException, ClassNotFoundException {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null) {
            openFolder(upperPath.toString());
            pathField.setText(upperPath.toString());
        }
    }

    public void startList() {
        try {
            out.writeUTF("/updateList");
            filesTable.getItems().clear();
            filesTable.getItems().addAll((Collection<? extends FileInfo>) in.readObject());
            filesTable.sort();

            out.writeUTF("/path");
            startPath = in.readUTF();
            pathField.setText(startPath);

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
            setAuthorized(false);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер не доступен!", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnConnect(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + md5Custom(passwordField.getText()));
            out.flush();
            String str = in.readUTF();
            if (str.startsWith("/authok")) {
                loginField.clear();
                passwordField.clear();
                startList();
                setAuthorized(true);
                connect = true;
            } else if (str.startsWith("/autherror")) {
                loginField.setText("");
                passwordField.setText("");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Неправильный логин/пароль!", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер не доступен!", ButtonType.OK);
            alert.showAndWait();
        } catch (NullPointerException e) {

        }
    }

    public static String md5Custom(String st) {
        MessageDigest messageDigest = null;
        byte[] digest = new byte[0];

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(st.getBytes());
            digest = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BigInteger bigInt = new BigInteger(1, digest);
        String md5Hex = bigInt.toString(16);

        while (md5Hex.length() < 32) {
            md5Hex = "0" + md5Hex;
        }

        return md5Hex;
    }

    public void btnExitUser(ActionEvent actionEvent) throws IOException {
        out.writeUTF("/end");
        setAuthorized(false);
        filesTable.getItems().clear();
        socket.close();
        connect = false;
    }

    public void btnReg(ActionEvent actionEvent) throws IOException {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/reg " + loginField.getText() + " " + md5Custom(passwordField.getText()));
            out.flush();
            String str = in.readUTF();
            if (str.startsWith("/regok")) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Успешная регистрация!", ButtonType.OK);
                alert.showAndWait();
            } else if (str.startsWith("/regerror")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка регестрации!", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер не доступен!", ButtonType.OK);
            alert.showAndWait();
        } catch (NullPointerException e) {

        } finally {
            out.writeUTF("/end");
            setAuthorized(false);
            filesTable.getItems().clear();
            socket.close();
            connect = false;
        }
    }
}
