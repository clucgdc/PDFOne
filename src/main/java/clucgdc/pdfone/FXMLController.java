package clucgdc.pdfone;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class FXMLController implements Initializable {

    @FXML
    private BorderPane bpContainer;
    @FXML
    private HBox hboxCenter;
    @FXML
    private Label lblStatus;

    private TableView<Page> tblPages;
    private Stage stage;
    private FileChooser fileChooser_ToOpen;
    private FileChooser fileChooser_ToSave;

    private PDDocument pdfDoc;
    private PDFTextStripper reader;

    private static final String NO_TEXT = "(No text preview)";
    private static final String NO_PAGE_SELECTED = "No page was selected";

    private static final int PREVIEW_CHARS = 150;

    @FXML
    private void loadPdf(ActionEvent event) {
        File pdf = getUserOpenFile();
        if (pdf == null) {
            return;
        }

        try {
            pdfDoc = PDDocument.load(pdf);
            tblPages.setItems(FXCollections.observableArrayList(getSimplifiedPages()));
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void saveNewPdf(ActionEvent event) throws IOException {
        ObservableList<Page> simplifiedPages = tblPages.getItems();
        FilteredList<Page> selectedPages = simplifiedPages.filtered(p -> p.getSelected());
        if (selectedPages.isEmpty()) {
            showWarning(NO_PAGE_SELECTED);
            return;
        }

        File saveToFile = getUserSaveFile();
        if (saveToFile == null) {
            return;
        }

        PDDocument newDoc = new PDDocument();

        List<Integer> selectedPageIndexes = selectedPages.stream().map(p -> p.getPageIndex()).collect(Collectors.toList());
        Collections.sort(selectedPageIndexes);
        selectedPageIndexes.forEach((i) -> {
            newDoc.addPage(pdfDoc.getPage(i));
        });
        newDoc.save(saveToFile);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tblPages = createTable();
        hboxCenter.getChildren().add(tblPages);
        HBox.setHgrow(tblPages, Priority.ALWAYS);

        try {
            reader = new PDFTextStripper();
        } catch (IOException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private File getUserOpenFile() {
        if (stage == null || fileChooser_ToOpen == null) {
            stage = (Stage) bpContainer.getScene().getWindow();
            fileChooser_ToOpen = new FileChooser();
            fileChooser_ToOpen.setTitle("Open PDF File");
            fileChooser_ToOpen.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        }
        return fileChooser_ToOpen.showOpenDialog(stage);
    }

    private File getUserSaveFile() {
        if (stage == null || fileChooser_ToSave == null) {
            stage = (Stage) bpContainer.getScene().getWindow();
            fileChooser_ToSave = new FileChooser();
            fileChooser_ToSave.setTitle("Save as New PDF File");
            fileChooser_ToSave.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        }
        return fileChooser_ToSave.showSaveDialog(stage);
    }

    private TableView<Page> createTable() {
        TableView<Page> table = new TableView<>();

        TableColumn selectCol = new TableColumn("Include");
        selectCol.setMinWidth(70);
        selectCol.setCellValueFactory(new PropertyValueFactory("selected"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setSortable(false);

        TableColumn descriptionCol = new TableColumn("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory("description"));
        descriptionCol.prefWidthProperty().bind(table.widthProperty().subtract(selectCol.getWidth()));
        descriptionCol.setSortable(false);

        table.setEditable(true);

        table.getColumns().addAll(selectCol, descriptionCol);

        return table;
    }

    private List<Page> getSimplifiedPages() throws IOException {
        List<Page> pages = new ArrayList<>();
        String pageText;
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {
            reader.setStartPage(i);
            reader.setEndPage(i);
            pageText = reader.getText(pdfDoc);
            pages.add(new Page(false, i, "Page " + (i + 1) + ": " + getPreviewText(pageText)));
        }
        return pages;
    }

    private String getPreviewText(String pageText) {
        pageText = pageText.replaceAll("\\n", " ").replaceAll("\\r", "").trim();
        return pageText.isEmpty() ? NO_TEXT : pageText.substring(0, Math.min(PREVIEW_CHARS, pageText.length()));
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(AlertType.WARNING.name());
        alert.setHeaderText(msg);

        alert.showAndWait();
    }

    @FXML
    private void showReadme(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(AlertType.INFORMATION.name());
        alert.setHeaderText(Readme.HEADER);

        TextArea textArea = new TextArea(Readme.CONTENT);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }
}
