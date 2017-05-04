package sk.uniza.fri.cp.App;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import sk.uniza.fri.cp.App.BreadboardControl.BreadboardController;
import sk.uniza.fri.cp.App.CPUControl.CPUController;
import sk.uniza.fri.cp.BreadboardSim.Movable;

/**
 * Created by Moris on 7.2.2017.
 */
public class App extends Application {
    private static final int CPU_WINDOW_WIDTH = 1280;
    private static final int CPU_WINDOW_HEIGHT = 640;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //nacitanie sablony okna CPU
        FXMLLoader CpuLayoutLoader = new FXMLLoader();
        CpuLayoutLoader.setLocation(getClass().getResource("/fxml/Main_CPUEmul.fxml"));
        Parent mainContent = CpuLayoutLoader.load();

        //nacitanie sceny CPU
        Scene mainScene = new Scene(mainContent, CPU_WINDOW_WIDTH, CPU_WINDOW_HEIGHT);

        //priradenie CSS stylov k scene
        mainScene.getStylesheets().add(getClass().getResource("/css/CPUEmul_style.css").toExternalForm());

        //zachytenie vsetkych klaves (okrem funkcnych klaves) na mainScene (http://stackoverflow.com/questions/25397742/javafx-keyboard-event-shortcut-key)
        //a posielanie ich do CPU
        mainScene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent ke) {
                if(ke.getCode().isFunctionKey()) return;

                CPUController controller = CpuLayoutLoader.getController();
                if (controller.isExecuting())
                     controller.keyboardInput(ke);
            }
        });

        //pri kliknuti na tlacidlo zatvorenia okna
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                                           @Override
                                           public void handle(WindowEvent event) {
                                               if (!((CPUController) CpuLayoutLoader.getController()).exit())
                                                   event.consume(); //ak si to uzivatel rozmysli, nezatvaraj aplikaciu
                                           }
                                       });

        primaryStage.setScene(mainScene);
        primaryStage.setTitle("CPU Emulator");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/cpu_icon.png")));
        primaryStage.show();

        //Breadboard
        FXMLLoader breadboardLayoutLoader = new FXMLLoader();
        breadboardLayoutLoader.setLocation(getClass().getResource("/fxml/BreadboardSim.fxml"));
        BreadboardController bc = new BreadboardController();
        breadboardLayoutLoader.setController(bc);

        Parent breadboardContent = breadboardLayoutLoader.load();

        Scene breadboardScene = new Scene(breadboardContent);
        breadboardScene.getStylesheets().add(getClass().getResource("/css/BreadboardSim_style.css").toExternalForm());
        Stage breadboardStage = new Stage();
        breadboardStage.setTitle("Simulátor - Nový obvod");
        breadboardStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/simulator_icon_128.png")));
        breadboardScene.addEventFilter(KeyEvent.KEY_RELEASED, event ->{
                    //ak bol stalceny delete
                    if(event.getCode().equals(KeyCode.DELETE)){
                        ((BreadboardController) breadboardLayoutLoader.getController()).callDelete();
                    }
                }
        );

        breadboardStage.setScene(breadboardScene);
        ((CPUController) CpuLayoutLoader.getController()).setBreadboardStage(breadboardStage, breadboardLayoutLoader.getController());
    }
}
