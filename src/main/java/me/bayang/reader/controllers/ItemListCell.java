package me.bayang.reader.controllers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfoenix.controls.JFXListCell;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import me.bayang.reader.backend.inoreader.ConnectServer;
import me.bayang.reader.rssmodels.FolderFeedOrder;
import me.bayang.reader.rssmodels.Item;

public class ItemListCell extends JFXListCell<Item> {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());
    
    @FXML
    private VBox cellWrapper;
    
    @FXML
    private HBox firstLine;
    
    @FXML
    private Label fromLabel;

    @FXML
    private Label dateLabel;
    
    @FXML
    private Label subjectLabel;

    @FXML
    private Label contentLabel;
    
    @FXML
    private ImageView icon;
    
    private FXMLLoader mLLoader;
    
    private ContextMenu menu = new ContextMenu();
    
    // FIXME remove this from here
    private ConnectServer connectServer;

    public ItemListCell(ConnectServer connectServer) {
        this.connectServer = connectServer;
        MenuItem starItem = new MenuItem("Mark Starred");
        MenuItem unStarItem = new MenuItem("Mark Unstarred");
        menu.getItems().addAll(starItem, unStarItem);

        starItem.setOnAction(event -> {
            System.out.println("mark star " + getListView().getSelectionModel().getSelectedItem().getDecimalId());
            new Thread(() -> this.connectServer.connectServer(ConnectServer.markStarredURL + this.getListView().getSelectionModel().getSelectedItem().getDecimalId())).start();

        });
        unStarItem.setOnAction(event -> {
            System.out.println("unstar " + this.getListView().getSelectionModel().getSelectedItem().getDecimalId());
            new Thread(() -> this.connectServer.connectServer(ConnectServer.markUnstarredURL + this.getListView().getSelectionModel().getSelectedItem().getDecimalId())).start();
        });
        this.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                e.consume();
            }
        });
    }

    @Override
    protected void updateItem(Item item, boolean empty) {
        super.updateItem(item, empty);
        this.prefWidthProperty().bind( this.getListView().widthProperty().subtract( 20 ) );
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(
                        getClass().getResource("/fxml/MailListCell.fxml"));
                mLLoader.setController(this);
                try {
                    mLLoader.load();
                } catch (IOException e) {
                    LOGGER.error("",e);
                }
            }
            
            //get the time style
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(item.getCrawlTimeMsec())), ZoneId.systemDefault());
            String timeString = localDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("kk:mm:ss"));
            if (!localDateTime.toLocalDate().equals(LocalDate.now())) {
                timeString = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"));
            }
            fromLabel.setTextFill(item.isRead() ? Color.GRAY : Color.BLACK);
            fromLabel.setText(StringEscapeUtils.unescapeHtml4(item.getOrigin().getTitle()));
            dateLabel.setTextFill(item.isRead() ? Color.GRAY : Color.BLACK);
            dateLabel.setText(timeString);
            subjectLabel.setStyle("-fx-font-weight: bold;");
            subjectLabel.setTextFill(item.isRead() ? Color.GRAY : Color.BLACK);
            subjectLabel.setWrapText(true);
            subjectLabel.setText(StringEscapeUtils.unescapeHtml4(item.getTitle()));
            if (item.getSummary().getContent().length() > 20) {
                contentLabel.setText(StringEscapeUtils.unescapeHtml4(item.getSummary().getContent().substring(0, 20)));
            }
            else {
                contentLabel.setText(StringEscapeUtils.unescapeHtml4(item.getSummary().getContent()));
            }
            
            
            setText(null);
            setTextFill(item.isRead() ? Color.GRAY : Color.BLACK);
            if (FolderFeedOrder.iconMap != null) {
                icon.setImage(FolderFeedOrder.iconMap.get(item.getOrigin().getStreamId()));
//                ImageView imageView = new ImageView(FolderFeedOrder.iconMap.get(item.getOrigin().getStreamId()));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
            }
            setGraphic(cellWrapper);
            setContextMenu(menu);
        }
    }

}
