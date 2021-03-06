import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class Controller {
    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TextField nameFolder, rename;

    public void btnExitAction() {
        try {
            CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");
            rightPC.out.writeUTF("/end");
            Platform.exit();
        } catch (Exception e) {
//            e.printStackTrace();
            Platform.exit();
        }
    }

    public void copyBtnAction(ActionEvent actionEvent) throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");
        try {
            if (rightPC.connect) {
                try {
                    if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
                        alert.showAndWait();
                        return;
                    }
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                //→
                if (leftPC.getSelectedFilename() != null) {
                    Path srcPath = Paths.get(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
                    Path dstPath = Paths.get(rightPC.getCurrentPath());
                    String currentPath = rightPC.getCurrentPath();

                    try {
                        if (!Files.isDirectory(srcPath)) {
                            String fileName = leftPC.getSelectedFilename();
                            rightPC.out.writeUTF("/download " + fileName);
                            File file = new File(srcPath.toString());
                            rightPC.out.writeLong(file.length());
                            rightPC.out.writeUTF(dstPath.toString());
                            FileInputStream fileInputStream = new FileInputStream(file);
                            int x;
                            Alert alert = new Alert(Alert.AlertType.NONE, "Файл копируется!");
                            alert.show();
                            byte[] buffer = new byte[8192];
                            while ((x = fileInputStream.read(buffer)) != -1) {
                                rightPC.out.write(buffer, 0, x);
                                rightPC.out.flush();
                            }
                            System.out.println("File: " + fileName + ", downloaded!");
                            fileInputStream.close();
                            alert.setAlertType(Alert.AlertType.INFORMATION);
                            alert.close();
                            //обновить
                            update(rightPC, currentPath);
                            leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Выбран не файл!", ButtonType.OK);
                            alert.showAndWait();
                        }
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                        alert.showAndWait();
                    }
                }
                //←
                if (rightPC.getSelectedFilename() != null) {
                    Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
                    Path leftPath = Paths.get(leftPC.getCurrentPath());
                    String currentPath = rightPC.getCurrentPath();
                    String s = srcPath.toString();
                    rightPC.out.writeUTF("/isDir " + s);
                    String isDir = rightPC.in.readUTF();
                    if (isDir.equals("false")) {
                        rightPC.out.writeUTF("/getFile " + s);
                        File file = new File(leftPath.toString(), rightPC.getSelectedFilename());
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        Alert alert = new Alert(Alert.AlertType.NONE, "Файл копируется!");
                        alert.show();
                        byte[] buffer = new byte[1024];
                        FileOutputStream fos = new FileOutputStream(file);
                        while (true) {
                            int read = rightPC.in.read(buffer);
                            System.out.println(read);
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
                        alert.setAlertType(Alert.AlertType.INFORMATION);
                        alert.close();

                        //обновить
                        update(rightPC, currentPath);
                        leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
                        System.out.println("File: " + file.getName() + ", downloaded!");
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Выбран не файл!", ButtonType.OK);
                        alert.showAndWait();
                    }
                }
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Соединение отсутствует!", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void deleteBtnActionUnion(ActionEvent actionEvent) throws IOException, InterruptedException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (leftPC.getSelectedFilename() != null) {
            try {
                Path srcPath = Paths.get(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
                //удаление всего
                deleteFolder(new File(srcPath.toString()));
                leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка удаления!", ButtonType.OK);
                alert.showAndWait();
            }
        }

        if (rightPC.getSelectedFilename() != null) {
            Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            String currentPath = rightPC.getCurrentPath();
            String s = srcPath.toString();
            rightPC.out.writeUTF("/delete " + s);

            //обновляем
            update(rightPC, currentPath);
        }
    }

    /**
     * Метод для полного удаления папки вместе с содержимым
     */
    private void deleteFolder(final File file) {
//        System.out.println("Удaляem фaйл: " + file.getAbsolutePath());
        if (file.isDirectory()) {
            String[] files = file.list();
            if ((null == files) || (files.length == 0)) {
                file.delete();
            } else {
                for (final String filename : files) {
                    deleteFolder(new File(file.getAbsolutePath() + File.separator + filename));
                }
                file.delete();
            }
        } else {
            file.delete();
        }
    }

    private void update(CloudPanelController rightPC, String currentPath) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            rightPC.out.writeUTF("/openFolder " + currentPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        rightPC.filesTable.getItems().clear();
        try {
            rightPC.filesTable.getItems().addAll((Collection<? extends FileInfo>) rightPC.in.readObject());
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
        rightPC.filesTable.sort();

        rightPC.pathField.setText(currentPath);
    }

    public void createFolderBtnAction(ActionEvent actionEvent) throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");
        try {

            if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Выберете файл в окне для понимаю области копирования", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            if (leftPC.getSelectedFilename() != null) {
                try {
                    Path srcPath = Paths.get(leftPC.getCurrentPath());
                    //Создание папки
                    File folder = new File(srcPath.toString() + "\\" + nameFolder.getText());
                    if (!folder.exists()) {
                        folder.mkdir();
                    }
                    nameFolder.clear();
                    leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка создания папки!", ButtonType.OK);
                    alert.showAndWait();
                }
            }

            if (rightPC.getSelectedFilename() != null) {
                String currentPath = rightPC.getCurrentPath();
                rightPC.out.writeUTF("/createFolder " + currentPath);
                rightPC.out.writeUTF("/nameFolder " + nameFolder.getText());
                nameFolder.clear();
                //обновляем
                update(rightPC, currentPath);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Выберете файл в окне для понимаю области копирования", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void renameBtnAction(ActionEvent actionEvent) throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");

        try {
            if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            if (leftPC.getSelectedFilename() != null) {
                try {
                    Path srcPath = Paths.get(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
                    File file = new File(srcPath.toString());

                    //переименованный путь
                    Path renamePath = Paths.get(leftPC.getCurrentPath(), rename.getText());

                    file.renameTo(new File(renamePath.toString()));
                    rename.clear();
                    leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка переименования!", ButtonType.OK);
                    alert.showAndWait();
                }
            }

            if (rightPC.getSelectedFilename() != null) {
                Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
                String currentPath = rightPC.getCurrentPath();
                String s = srcPath.toString();
                rightPC.out.writeUTF("/rename " + s);
                rightPC.out.flush();
                rightPC.out.writeUTF(rightPC.getCurrentPath());
                rightPC.out.flush();
                rightPC.out.writeUTF(rename.getText());
                rightPC.out.flush();
                rename.clear();

                //обновляем
                update(rightPC, currentPath);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Выберете файл!", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnClearCache() {
        File file = new File("temp.json");
        if (file.exists()) {
            file.delete();
            CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");
            try {
                rightPC.useJSONFile = false;
            } catch (Exception e) {
                rightPC.useJSONFile = false;
            }
        }
    }
}