import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) throws IOException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
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
                rightPC.out.writeUTF("/download " + leftPC.getSelectedFilename());
                File file = new File(srcPath.toAbsolutePath().toString());
                rightPC.out.writeLong(file.length());
                rightPC.out.writeUTF(dstPath.toAbsolutePath().toString());
                FileInputStream fileInputStream = new FileInputStream(file);
                int x;
                while ((x = fileInputStream.read()) != -1) {
                    rightPC.out.write(x);
                    rightPC.out.flush();
                }
                System.out.println("File: " + leftPC.getSelectedFilename() + ", downloaded!");

                //обновить
                update(rightPC, currentPath);
                leftPC.updateList(Paths.get(leftPC.getCurrentPath()));

            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                alert.showAndWait();
            }
        }
        //←
        if (rightPC.getSelectedFilename() != null) {
            Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            Path leftPath = Paths.get(leftPC.getCurrentPath());
//            String currentPath = rightPC.getCurrentPath();
            String s = srcPath.toAbsolutePath().toString();
            rightPC.out.writeUTF("/putMy " + s);
            String readUTF = rightPC.in.readUTF();
            String[] s1 = null;
            if (readUTF.startsWith("/size")) {
                s1 = readUTF.split(" ", 2);
            }
            long length = Long.parseLong(s1[1]);

            File file = new File(leftPath.toAbsolutePath().toString(), rightPC.getSelectedFilename());
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            for (long i = 0; i < length; i++) {
                fos.write(rightPC.in.read());
            }
            fos.close();

            //обновить
//            rightPC.connect();
//            update(rightPC, currentPath);

            leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
            System.out.println("File: " + file.getName() + ", downloaded!");
        }


    }

    public void deleteBtnActionUnion(ActionEvent actionEvent) throws IOException, InterruptedException {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (leftPC.getSelectedFilename() != null) {
            try {
                Path srcPath = Paths.get(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
                Files.delete(srcPath);
                leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось удалить указанный файл", ButtonType.OK);
                alert.showAndWait();
            }
        }

        if (rightPC.getSelectedFilename() != null) {
            Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            String currentPath = rightPC.getCurrentPath();
            String s = srcPath.toAbsolutePath().toString();
            rightPC.out.writeUTF("/delete " + s);

            //обновляем
            update(rightPC, currentPath);
        }
    }

    private void update(CloudPanelController rightPC, String currentPath){
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
}