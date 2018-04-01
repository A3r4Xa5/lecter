package me.bayang.reader;

import java.util.ResourceBundle;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.bayang.reader.view.NoOpSplashScreen;
import me.bayang.reader.view.RssView;

@SpringBootApplication
public class FXMain extends AbstractJavaFxApplicationSupport {
    
    public static Stage pocketAddLinkStage = null;
    
    public static ResourceBundle bundle = ResourceBundle.getBundle("i18n.translations");
    
    public static void main(String[] args) {
        launch(FXMain.class, RssView.class, new NoOpSplashScreen() ,args);
    }
    
    public static void createPocketAddLinkStage() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(bundle.getString("pocketAddLinkStage"));
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(FXMain.getStage());
        dialogStage.getIcons().add(new Image("icon.png"));
        dialogStage.setResizable(true);
        FXMain.pocketAddLinkStage = dialogStage;
    }

}
