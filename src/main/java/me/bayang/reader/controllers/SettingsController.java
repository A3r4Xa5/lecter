package me.bayang.reader.controllers;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;

import de.felixroske.jfxsupport.FXMLController;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.bayang.reader.FXMain;
import me.bayang.reader.storage.IStorageService;
import me.bayang.reader.view.PocketOauthView;
import me.bayang.reader.view.RssView;

@FXMLController
public class SettingsController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsController.class);
    
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.translations");
    
    @Autowired
    private IStorageService configStorage;
    
    @FXML
    private JFXToggleButton layoutToggle;
    
    @FXML
    private JFXToggleButton pocketActivate;
    
    @FXML
    private JFXButton pocketConfigure;
    
    @FXML
    private JFXButton backButton;
    
    @FXML
    private Label pocketStatus;
    
    @Autowired
    private PocketOauthView pocketOauthView;
    private PocketOauthController pocketOauthController;
    private Stage pocketOauthStage;
    
    @FXML
    public void initialize() {
        pocketActivate.selectedProperty().bindBidirectional(configStorage.pocketEnabledProperty());
        layoutToggle.selectedProperty().bindBidirectional(configStorage.prefersGridLayoutProperty());
    }
    
    @FXML
    public void showMainScreen() {
        FXMain.showView(RssView.class);
    }
    
    @FXML
    public void showPocketOauthStage() {
        if (pocketOauthStage == null) {
            Stage dialogStage = new Stage();
            dialogStage.setTitle(bundle.getString("pocketLogin"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(FXMain.getStage());
            dialogStage.getIcons().add(new Image("icon.png"));
            dialogStage.setResizable(true);
            Scene scene = new Scene(pocketOauthView.getView());
            dialogStage.setScene(scene);
            this.pocketOauthStage = dialogStage;
            pocketOauthController = (PocketOauthController) pocketOauthView.getPresenter();
            pocketOauthController.setStage(dialogStage);
            // Show the dialog and wait until the user closes it
            pocketOauthController.processLogin();
            dialogStage.showAndWait();
        }
        else {
            pocketOauthController.processLogin();
            pocketOauthStage.showAndWait();
        }
    }
    

}
