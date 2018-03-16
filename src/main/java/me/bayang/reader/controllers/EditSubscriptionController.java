package me.bayang.reader.controllers;

import org.springframework.beans.factory.annotation.Autowired;

import de.felixroske.jfxsupport.FXMLController;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.bayang.reader.FXMain;
import me.bayang.reader.backend.inoreader.ConnectServer;
import me.bayang.reader.rssmodels.Categories;
import me.bayang.reader.rssmodels.FolderFeedOrder;
import me.bayang.reader.rssmodels.Subscription;

@FXMLController
public class EditSubscriptionController {
    
    @Autowired
    private ConnectServer connectServer;
    
    @Autowired
    private RssController rssController;
    
    private Stage stage;
    
    private Subscription subscription;
    
    @FXML
    private Hyperlink subscriptionHtmlUrl;
    
    @FXML
    private Hyperlink subscriptionUrl;
    
    @FXML
    private Label subscriptionTitle;
    
    @FXML
    private ImageView subscriptionIcon;
    
    @FXML
    private VBox container;
    
    @FXML
    private VBox categoriesContainer;
    
    public EditSubscriptionController() {
    }
    
    @FXML
    public void onFeedUrlClicked() {
        openHyperlink(subscriptionUrl.getText());
    }
    
    @FXML
    public void onUrlClicked() {
        openHyperlink(subscriptionHtmlUrl.getText());
    }
    
    public void openHyperlink(String url) {
        FXMain.getAppHostServices().showDocument(url);
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        subscriptionTitle.setText(subscription.getTitle());
        subscriptionHtmlUrl.setText(subscription.getHtmlUrl());
        subscriptionUrl.setText(subscription.getUrl());
        if (FolderFeedOrder.iconMap != null) {
            subscriptionIcon.setImage(FolderFeedOrder.iconMap.get(subscription.getId()));
//            subscriptionIcon.setFitHeight(16);
//            subscriptionIcon.setFitWidth(16);
        }
        categoriesContainer.getChildren().clear();
        for (Categories category : subscription.getCategories()) {
            DeletableLabel de = new DeletableLabel(category.getLabel());
            de.getDeleteCross().setOnMouseClicked(event -> {
                if (subscription.getCategories().size() > 1 ) {
                    connectServer.removeFromFolder(subscription.getId(), category.getId());
                    categoriesContainer.getChildren().remove(de);
                    rssController.refreshFired();
                }
                else if (subscription.getCategories().size() == 1) {
                    connectServer.unsubscribe(subscription.getId());
                    categoriesContainer.getChildren().remove(de);
                    rssController.refreshFired();
                }
            });
            this.categoriesContainer.getChildren().add(de);
        }
    }

}
