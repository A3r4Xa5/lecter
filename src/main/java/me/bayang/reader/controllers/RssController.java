package me.bayang.reader.controllers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jfoenix.controls.JFXListView;

import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.bayang.reader.FXMain;
import me.bayang.reader.backend.inoreader.ConnectServer;
import me.bayang.reader.rssmodels.Categories;
import me.bayang.reader.rssmodels.Feed;
import me.bayang.reader.rssmodels.FolderFeedOrder;
import me.bayang.reader.rssmodels.Item;
import me.bayang.reader.rssmodels.Readability;
import me.bayang.reader.rssmodels.Subscription;
import me.bayang.reader.rssmodels.Tag;
import me.bayang.reader.rssmodels.UserInfo;

/**
 * The RssController class for JavaFX application.
 */
@FXMLController
public class RssController {
    
    private static Logger LOGGER = LoggerFactory.getLogger(RssController.class);
    
    @FXML
    private TreeView<Feed> treeView;
    @FXML
    private Button refreshButton;
    @FXML
    private JFXListView<Item> listView;
    @FXML
    private WebView webView;
    @FXML
    private Button markReadButton;
    @FXML
    private Button popUpButton;
    @FXML
    private Label statusLabel;
    @FXML
    private RadioButton rssRadioButton;
    @FXML
    private RadioButton webRadioButton;
    @FXML
    private RadioButton readabilityRadioButton;
    @FXML
    private ProgressIndicator progressIndicator;
    
    @Autowired
    private ConnectServer connectServer;
    
    @Autowired
    private FolderFeedOrder folderFeedOrder;
    
    @Autowired
    private OauthView oauthView;
    private OauthController oauthController;
    
    private Stage oauthDialogStage = null;

    private List<Item> itemList;
    private List<Item> starredList;
    private Instant lastUpdateTime;
    private Task<TreeItem<Feed>> treeTask;
    private Task<List<Item>> itemListTask;
    private Task<List<Item>> starredListTask;
    private Task<Map<String, Integer>> unreadCountsTask;
    private static Map<String, Integer> unreadCountsMap;
    private static TreeItem<Feed> root;

    @FXML
    private void initialize() {
//        FXMain.getStage().getIcons().add(new Image("icon.png"));
        eventHandleInitialize();
        radioButtonInitialize();
    }

    private void eventHandleInitialize() {
//        listView.setCellFactory(l -> new MyListCell());
        listView.setCellFactory(l -> new ItemListCell(connectServer));
        treeView.setCellFactory(t -> new MyTreeCell());

        //handle event between listView and webView
        listView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getSummary().getContent() != null) {
                if (rssRadioButton.isSelected()) {
                    webView.getEngine().loadContent(Item.processContent(newValue.getTitle(), newValue.getSummary().getContent()));
                } else if (webRadioButton.isSelected()) {
                    webView.getEngine().load(newValue.getCanonical().get(0).getHref());
                } else if (readabilityRadioButton.isSelected()) {
                    new Thread(() -> {
                        String content = Readability.getReadabilityContent(newValue.getCanonical().get(0).getHref());
                        Platform.runLater(() -> webView.getEngine().loadContent(Item.processContent(newValue.getTitle(), content)));
                    }).start();
                }
                //send mark feed read to server if not in the starred list
                if (!treeView.getSelectionModel().getSelectedItem().getValue().getId().equals("user/" + UserInfo.getUserId() + "/state/com.google/starred")) {
                    if (!newValue.isRead()) {
                        connectServer.markAsRead(newValue.getDecimalId());
//                        new Thread(() -> connectServer.connectServer(ConnectServer.markFeedReadURL + newValue.getDecimalId())).start();

                        String streamId = newValue.getOrigin().getStreamId();
                        Integer count = unreadCountsMap.get(streamId);
                        unreadCountsMap.put(streamId, --count);

                        Integer allCount = unreadCountsMap.get("All Items");
                        unreadCountsMap.put("All Items", --allCount);

                        //set parent count
                        if (getParentItem(streamId) != null) {
                            String parent = getParentItem(streamId).getValue().getId();
                            Integer parentCount = unreadCountsMap.get(parent);
                            unreadCountsMap.put(parent, --parentCount);
                        }

                        treeView.refresh();
                    }
                    newValue.setRead(true);//change state to read and change color in listView
                    listView.refresh();//force refresh, or it will take a while to show the difference
                }
            }
        }));
        //handle event between treeView and listView
        treeView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            List<Item> chosenItemList = new ArrayList<>();

            if (starredList != null && newValue != null) {
                if (newValue.getValue().getId().equals("user/" + UserInfo.getUserId() + "/state/com.google/starred")) {
                    chosenItemList = starredList;
                }
            }
            if (itemList != null && newValue != null) {
                if (newValue.getValue().getId().equals("All Items")) {//handle the special all items tag
                    chosenItemList = itemList;
                }
                if (newValue.isLeaf()) {//leaf chosen list
                    for (Item item : itemList) {
                        if (newValue.getValue().getId().equals(item.getOrigin().getStreamId())) {
                            chosenItemList.add(item);
                        }
                    }
                } else {//handle the folder chosen list
                    for (Item item : itemList) {
                        for (String s : item.getCategories()) {
                            if (newValue.getValue().getId().equals(s)) {
                                chosenItemList.add(item);
                            }
                        }
                    }
                }
            }
            chosenItemList.sort((o1, o2) -> (int) (Long.parseLong(o1.getCrawlTimeMsec()) - Long.parseLong(o2.getCrawlTimeMsec())));
            listView.setItems(FXCollections.observableArrayList(chosenItemList));
        }));
    }

    private void radioButtonInitialize() {
        //set webView User Agent
        webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");

        //initialize the radio button
        ToggleGroup toggleGroup = new ToggleGroup();
        rssRadioButton.setToggleGroup(toggleGroup);
        webRadioButton.setToggleGroup(toggleGroup);
        readabilityRadioButton.setToggleGroup(toggleGroup);

        rssRadioButton.setSelected(true);
        toggleGroup.selectedToggleProperty().addListener(((observable, oldValue, newValue) -> {
            if (toggleGroup.getSelectedToggle() != null) {
                if (listView.getSelectionModel().getSelectedItem() != null) {
                    if (rssRadioButton.isSelected()) {
                        Item item = listView.getSelectionModel().getSelectedItem();
                        webView.getEngine().loadContent(Item.processContent(item.getTitle(), item.getSummary().getContent()));
                    } else if (webRadioButton.isSelected()) {
                        webView.getEngine().load(listView.getSelectionModel().getSelectedItem().getCanonical().get(0).getHref());
                    } else if (readabilityRadioButton.isSelected()) {
                        new Thread(() -> {
                            String content = Readability.getReadabilityContent(listView.getSelectionModel().getSelectedItem().getCanonical().get(0).getHref());
                            Platform.runLater(() -> webView.getEngine().loadContent(Item.processContent(listView.getSelectionModel().getSelectedItem().getTitle(), content)));
                        }).start();

                    }
                }
            }
        }));
    }

    private void taskInitialize() {
        //when get the treeItem from the URL, change the view
        treeTask = new Task<TreeItem<Feed>>() {
            @Override
            protected TreeItem<Feed> call() throws Exception {
                System.out.println("start treeTask");
                return handleFolderFeedOrder();
            }
        };
        treeTask.setOnSucceeded(event -> {
            treeView.setRoot(treeTask.getValue());
            treeView.setShowRoot(false);
            progressIndicator.setProgress(progressIndicator.getProgress() + 0.25);
            statusLabel.setText("Get Feeds Complete.");
            System.out.println("finish treeTask");
        });

        //initialize itemList
        itemListTask = new Task<List<Item>>() {
            @Override
            protected List<Item> call() throws Exception {
                System.out.println("start itemListTask");
                return connectServer.getStreamContent(ConnectServer.streamContentURL);
            }
        };
        itemListTask.setOnSucceeded(event -> {
            itemList = itemListTask.getValue();
            progressIndicator.setProgress(progressIndicator.getProgress() + 0.25);
            statusLabel.setText("Get New Items Complete.");
            System.out.println("finish itemListTask " + itemList.size());
        });
        //initialize starredList
        starredListTask = new Task<List<Item>>() {
            @Override
            protected List<Item> call() throws Exception {
                System.out.println("start starredListTask");
                return connectServer.getStreamContent(ConnectServer.starredContentURL);
            }
        };
        starredListTask.setOnSucceeded(event -> {
            starredList = starredListTask.getValue();
            progressIndicator.setProgress(progressIndicator.getProgress() + 0.25);
            statusLabel.setText("Get Starred Items Complete.");
            System.out.println("finish starredListTask " + starredList.size());
        });
        //initialize unreadCountsMap
        unreadCountsTask = new Task<Map<String, Integer>>() {
            @Override
            protected Map<String, Integer> call() throws Exception {
                System.out.println("start unreadCountsTask");
                return connectServer.getUnreadCountsMap();
            }
        };
        unreadCountsTask.setOnSucceeded(event -> {
            unreadCountsMap = unreadCountsTask.getValue();
            progressIndicator.setProgress(progressIndicator.getProgress() + 0.25);
            statusLabel.setText("Get UnreadCounts Complete");
            System.out.println("finish unreadCountsTask");
        });
    }

    private class MyTreeCell extends TreeCell<Feed> {
        private ContextMenu menu = new ContextMenu();

        public MyTreeCell() {
            MenuItem item = new MenuItem("unsubscribe");
            menu.getItems().add(item);
            item.setOnAction(event -> {
                TreeItem<Feed> sub = treeView.getSelectionModel().getSelectedItem();
//                new Thread(() -> {
//                    connectServer.connectServer(ConnectServer.editSubscriptionURL + "ac=unsubscribe&s=" + treeView.getSelectionModel().getSelectedItem().getValue().getId());
//                }).start();
                connectServer.unsubscribe(treeView.getSelectionModel().getSelectedItem().getValue().getId());
                //To clear the count, use markRead
                markReadButtonFired();
                sub.getParent().getChildren().remove(sub);
            });
        }

        @Override
        protected void updateItem(Feed item, boolean empty) {//set the treeView style show title and icons
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hBox = new HBox();
                hBox.setSpacing(10);
                hBox.setMaxWidth(250);
                if (item instanceof Subscription) {
                    String title = ((Subscription) item).getTitle();
                    Integer countInteger = unreadCountsMap.get(item.getId());
                    //create spaces to make counts in a row
                    Label countLabel = new Label(Objects.toString(countInteger, ""));
                    countLabel.getStyleClass().add("badge-label");
                    countLabel.setAlignment(Pos.CENTER);
                    countLabel.setPadding(new Insets(1.0));
                    Label titleLabel = new Label(title);
                    titleLabel.setPrefWidth(150);
                    Tooltip tooltip = new Tooltip(title);
                    this.setTooltip(tooltip);
                    //using HBox to add all the content
                    if (FolderFeedOrder.iconMap != null) {
                        ImageView imageView = new ImageView(FolderFeedOrder.iconMap.get(item.getId()));
                        imageView.setFitHeight(16);
                        imageView.setFitWidth(16);
                        hBox.getChildren().addAll(imageView, titleLabel, countLabel);
                    } else {
                        hBox.getChildren().addAll(titleLabel, countLabel);
                    }

                    setContextMenu(menu);
                }
                else if (item instanceof Categories) {
                    String s = item.getId();
                    Integer countInteger = unreadCountsMap.getOrDefault(s, null);
                    String countString = Objects.toString(countInteger, "");
                    Label countLabel = new Label(countString);
                    countLabel.getStyleClass().add("badge-label");
                    countLabel.setAlignment(Pos.CENTER);
                    countLabel.setPadding(new Insets(1.0));
                    Label titleLabel = new Label(item.getLabel());
                    titleLabel.setPrefWidth(120);
                    Tooltip tooltip = new Tooltip(item.getLabel());
                    this.setTooltip(tooltip);

                    hBox.getChildren().addAll(titleLabel, countLabel);
                }
                else {
                    String s = item.getId();
                    Integer countInteger = unreadCountsMap.get(s);
                    String countString = Objects.toString(countInteger, "");
                    Label countLabel = new Label(countString);
                    Label titleLabel = new Label(s.substring(s.lastIndexOf("/") + 1));
                    titleLabel.setPrefWidth(120);

                    hBox.getChildren().addAll(titleLabel, countLabel);
                }
                setGraphic(hBox);
            }
        }
    }


    @FXML
    private void refreshFired() {
        // FIXME afficher popup correcte
        if (connectServer.isShouldAskPermissionOrLogin()) {
            new Alert(Alert.AlertType.ERROR, "Please Login.");
        }
        else {
            progressIndicator.setProgress(0);
            statusLabel.setText("Refreshing...");
            taskInitialize();
            new Thread(treeTask).start();
            new Thread(itemListTask).start();
            new Thread(unreadCountsTask).start();
            new Thread(starredListTask).start();
            lastUpdateTime = Instant.now();
        }
    }

    @FXML
    private void markReadButtonFired() {
     // FIXME afficher popup correcte
        if (connectServer.isShouldAskPermissionOrLogin()) {
            new Alert(Alert.AlertType.ERROR, "Please Login.");
        } else {
            for (Item item : listView.getItems()) {
                item.setRead(true);
            }
            listView.refresh();
            //inform treeView to refresh the unread count
            if (!treeView.getSelectionModel().getSelectedItem().getValue().getId().equals("user/" + UserInfo.getUserId() + "/state/com.google/starred")) {
                Feed feed = treeView.getSelectionModel().getSelectedItem().getValue();
                if (feed instanceof Subscription) {
                    Integer count = unreadCountsMap.get(feed.getId());
                    unreadCountsMap.put(feed.getId(), 0);

                    Feed parent = treeView.getSelectionModel().getSelectedItem().getParent().getValue();
                    if (unreadCountsMap.get(parent.getId()) != null) {
                        unreadCountsMap.put(parent.getId(), unreadCountsMap.get(parent.getId()) - count);
                    }

                    unreadCountsMap.put("All Items", unreadCountsMap.get("All Items") - count);
                } else if (!feed.getId().equals("All Items")) {//parent treeItems, except All Items
                    Integer count = unreadCountsMap.get(feed.getId());
                    unreadCountsMap.put(feed.getId(), 0);
                    for (TreeItem<Feed> son : treeView.getSelectionModel().getSelectedItem().getChildren()) {
                        unreadCountsMap.put(son.getValue().getId(), 0);
                    }
                    unreadCountsMap.put("All Items", unreadCountsMap.get("All Items") - count);
                } else {//All Items
                    unreadCountsMap.put("All Items", 0);
                    root.getChildren().stream().filter(parent -> !parent.getValue().getId().equals("user/" + UserInfo.getUserId() + "/state/com.google/starred")).forEach(parent -> {
                        if (parent.getValue() instanceof Tag) {
                            for (TreeItem<Feed> son : parent.getChildren()) {
                                unreadCountsMap.put(son.getValue().getId(), 0);
                            }
                        }
                        unreadCountsMap.put(parent.getValue().getId(), 0);
                    });
                }
            }
            treeView.refresh();

            connectServer.markAllAsRead(lastUpdateTime.getEpochSecond(), treeView.getSelectionModel().getSelectedItem().getValue().getId());
//            new Thread(() -> {
//                connectServer.connectServer(ConnectServer.markAllReadURL + lastUpdateTime.getEpochSecond() + "&s=" + treeView.getSelectionModel().getSelectedItem().getValue().getId());
//            }).start();
        }
    }

    @FXML
    private void loginMenuFired() {
        // Create the dialog Stage.
        if (oauthDialogStage == null) {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Login");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(FXMain.getStage());
            dialogStage.getIcons().add(new Image("icon.png"));
            dialogStage.setResizable(false);
            Scene scene = new Scene(oauthView.getView());
            dialogStage.setScene(scene);
            this.oauthDialogStage = dialogStage;
            oauthController = (OauthController) oauthView.getPresenter();
            oauthController.setStage(dialogStage);
            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();
        }
        else {
            oauthDialogStage.showAndWait();
        }
    }
    
    @FXML
    private void displayPopUp() {
        FXMain.showView(OauthView.class, Modality.NONE);
    }

    @FXML
    private void exitMenuFired() {
        System.exit(0);
    }

    @FXML
    private void addSubscriptionFired() {
        FXMain.getAddSubscriptionStage().show();
    }

    private TreeItem<Feed> handleFolderFeedOrder() {
        root = new TreeItem<>(new Tag("root", "root")); //the root node doesn't show;
        root.setExpanded(true);

        Map<Feed, List<Subscription>> map = folderFeedOrder.getAlphabeticalOrder();
        for (Feed feed : map.keySet()) {
            TreeItem<Feed> tag = new TreeItem<>(feed);
            if (map.get(feed) != null) {
                for (Subscription sub : map.get(feed)) {
                    TreeItem<Feed> subscription = new TreeItem<>(sub);
                    tag.getChildren().add(subscription);
                }
            }
            root.getChildren().add(tag);
        }
        return root;

    }

    private TreeItem<Feed> getParentItem(String streamId) {
        for (TreeItem<Feed> parent : root.getChildren()) {
            for (TreeItem<Feed> item : parent.getChildren()) {
                if (item.getValue().getId().equals(streamId)) {
                    return parent;
                }
            }
        }
        return null;
    }
}

