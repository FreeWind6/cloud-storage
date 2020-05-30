import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Controller {
    @FXML
    VBox leftPanel, rightPanel;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

        try {
            Files.copy(srcPath, dstPath);
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void moveBtnAction(ActionEvent actionEvent) {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        PanelController rightPC = (PanelController) rightPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        PanelController srcPC = null, dstPC = null;
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC;
            dstPC = rightPC;
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

        try {
            Files.copy(srcPath, dstPath);
            Files.delete(srcPath);
            srcPC.updateList(Paths.get(srcPC.getCurrentPath()));
            dstPC.updateList(Paths.get(dstPC.getCurrentPath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось переместить указанный файл", ButtonType.OK);
            alert.showAndWait();
        }
    }

/*    public void deleteBtnActionThis(ActionEvent actionEvent) {
        PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrlleftleft");

        if (leftPC.getSelectedFilename() == null) {
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
    }*/

    public void deleteBtnActionUnion(ActionEvent actionEvent) throws IOException {
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
            String s = srcPath.toAbsolutePath().toString();
            rightPC.out.writeUTF("/delete " + s);
            rightPC.connect();
        }
    }

/*    public void deleteBtnActionCloud(ActionEvent actionEvent) throws IOException {
        CloudPanelController rightPC = (CloudPanelController) rightPanel.getProperties().get("ctrright");
        if (rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (rightPC.getSelectedFilename() != null) {
            Path srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            String s = srcPath.toAbsolutePath().toString();
            rightPC.out.writeUTF("/delete " + s);
            rightPC.connect();
        }
    }*/
}