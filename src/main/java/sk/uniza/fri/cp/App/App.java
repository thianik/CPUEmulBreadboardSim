package sk.uniza.fri.cp.App;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sk.uniza.fri.cp.App.CPUControl.CPUController;

/**
 * Created by Moris on 7.2.2017.
 */
public class App extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //nacitanie sablony okna CPU
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/Main_CPUEmul.fxml"));
        Parent mainContent = loader.load();

        //nacitanie sceny
        Scene mainScene = new Scene(mainContent, 1280, 700);

        //priradenie CSS stylov k scene
        mainScene.getStylesheets().add(getClass().getResource("/css/assembly-keywords.css").toExternalForm());

        CPUController a = loader.getController();

        //zachytenie vsetkych klaves na mainScene (http://stackoverflow.com/questions/25397742/javafx-keyboard-event-shortcut-key)
        //a posielanie ich do CPU
        mainScene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent ke) {
                if(ke.getCode().isFunctionKey()) return;

                CPUController controller = loader.getController();
                if (controller.isExecuting()) {
                     controller.keyboardInput(ke);
                }
            }
        });

        //pri kliknuti na tlacidlo zatvorenia okna
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                                           @Override
                                           public void handle(WindowEvent event) {
                                               if (!((CPUController) loader.getController()).exit())
                                                   event.consume(); //ak si to uzivatel rozmysli, nezatvaraj aplikaciu
                                           }
                                       });

        primaryStage.setScene(mainScene);
        primaryStage.setTitle("CPU Emulator");
        primaryStage.show();
    }
}
