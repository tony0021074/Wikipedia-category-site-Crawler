package component;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.util.Callback;
import web.*;

public class Controller {

    @FXML
    private Stage stage;

    @FXML
    private MenuItem chooseDownloadDirectoryMenuItem;

    @FXML
    private MenuItem clearMenuItem;

    @FXML
    private MenuItem closeProgramMenuItem;

    @FXML
    private TextField inputTextField;

    @FXML
    private Button startButton;

    @FXML
    private RadioButton modeBFS;

    @FXML
    private RadioButton modeDFS;

    @FXML
    private TreeView<File> fileView;

    private ToggleGroup modeGroup;

    private boolean isCrawling;

    private CrawlerFactory crawlerFactory;

    private ExecutorService executorService;

    private static final String READMEPATH = "README.txt";

    private static String downloadPath;

    @FXML
    private void initialize() {
        fileView.setCellFactory(new Callback<TreeView<File>, TreeCell<File>>() {

            public TreeCell<File> call(TreeView<File> tv) {
                return new TreeCell<File>() {
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);

                        setText((empty || item == null) ? "" : item.getName());
                    }

                };
            }
        });
        File downloadDirectory = new File("Download"+File.separator);
        downloadDirectory.mkdir();
        try {
            downloadPath = downloadDirectory.getCanonicalPath()+File.separator;
        } catch (IOException e) {
            e.printStackTrace();
            closeProgram();
        }
        this.fileView.setRoot(new SimpleFileTreeItem(new File(downloadPath)));
        this.executorService = Executors.newWorkStealingPool();
        this.isCrawling = false;
        modeGroup = new ToggleGroup();
        modeDFS.setToggleGroup(modeGroup);
        modeBFS.setToggleGroup(modeGroup);
        modeBFS.setSelected(true);
    }

    @FXML
    private void chooseDownloadPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose a Download Directory");
        File selectedPath = directoryChooser.showDialog(stage);
        if (selectedPath!=null) {
            try{
                String newDownloadPath = selectedPath.getCanonicalPath()+File.separator;
                if (!downloadPath.equals(newDownloadPath)) {
                    this.crawlerFactory=null;
                    downloadPath = newDownloadPath;
                    this.fileView.setRoot(new SimpleFileTreeItem(new File(downloadPath)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void startOrStopCrawling() {
        if (this.startButton.getText().equals("Start")) {
            startCrawling();
        } else {
            stopCrawling();
        }
    }

    private void startCrawling() {
        this.isCrawling=true;
        String url = inputTextField.getText();
        if (url != null && !url.isEmpty()) {
            url = url.trim();
            if (URLHelper.isValidURL(url)) {
                setControlDisable(true);
                this.startButton.setText("Stop");
                CompletableFuture.runAsync(()->updateFileView());
                int mode;
                RadioButton radioButton = (RadioButton) modeGroup.getSelectedToggle();
                if (radioButton.getText().equals("DFS")) {
                    mode = CrawlerFactory.DFS;
                } else {
                    mode = CrawlerFactory.BFS;
                }
                if (!(this.crawlerFactory!=null&&url.equals(this.crawlerFactory.getSeedURL()))) {
                    crawlerFactory = new CrawlerFactory(url, downloadPath, this.executorService);
                }
                if (this.crawlerFactory!=null&&!this.crawlerFactory.getIsSwitchOn()) {
                    this.crawlerFactory.setMode(mode);
                    CompletableFuture.runAsync(() -> this.crawlerFactory.work(), this.executorService).
                            thenRun(()->Platform.runLater(()->this.startButton.setText("Start"))).
                            thenRun(()->setControlDisable(false)).
                            thenRun(()->this.isCrawling=false);
                }
            }
        }
    }

    private void stopCrawling() {
        if (this.crawlerFactory!=null) {
            this.crawlerFactory.switchOff();
        }
    }

    private void setControlDisable(boolean value) {
        Platform.runLater(()->{
            this.clearMenuItem.setDisable(value);
            this.chooseDownloadDirectoryMenuItem.setDisable(value);
            this.inputTextField.setDisable(value);
            this.modeGroup.getToggles().parallelStream().map(t->(RadioButton)t).forEach(r->r.setDisable(value));
        });
    }

    private void updateFileView() {
        while(isCrawling) {
            Platform.runLater(()->this.fileView.setRoot(new SimpleFileTreeItem(new File(downloadPath))));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void clear() {
        Path directory = Paths.get(downloadPath);
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file); // this will work because it's always a File
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (!dir.equals(directory)) {
                        Files.delete(dir); //this will work because Files in the directory are already deleted
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
    }
        this.crawlerFactory=null;
        this.fileView.setRoot(new SimpleFileTreeItem(new File(downloadPath)));
    }

    @FXML
    private void handleViewClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            ObservableList TreeItems = this.fileView.getSelectionModel().getSelectedItems();
            if (!TreeItems.isEmpty()&&TreeItems.size()==1) {
                TreeItem treeItem = (TreeItem) TreeItems.get(0);
                File file = (File) treeItem.getValue();
                try {
                    if (file.isFile()) {
                        Desktop.getDesktop().open(file.getParentFile());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void handleReadMeMenuItem() {
        try {
            Desktop.getDesktop().open(new File(READMEPATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeProgram() {
        Platform.exit();
    }

}