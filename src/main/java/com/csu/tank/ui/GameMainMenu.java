package com.csu.tank.ui;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.input.view.KeyView;
import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.ui.DialogService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;
import static javafx.scene.input.KeyCode.*;

/**
 * @author LeeWyatt
 * 游戏的主菜单场景
 */
public class GameMainMenu extends FXGLMenu {
    private static boolean isP1 = true;
    private static int playerNum  = 1;
    private static boolean isOnline = false;
    private final TranslateTransition tt;
    private final Pane defaultPane;

    public GameMainMenu() {
        super(MenuType.GAME_MENU);
        Texture texture = texture("ui/logo.png");
        texture.setLayoutX(144);
        texture.setLayoutY(100);

        MainMenuButton singleGameBtn = new MainMenuButton("SINGLE PLAYER GAME", ()->{
            playerNum = 1;
            isOnline = false;
            System.out.println("single player game start");
            fireNewGame();
        });
        MainMenuButton twoGameBtn = new MainMenuButton("TWO PLAYERS GAME", () ->{
            playerNum = 2;
            isOnline = false;
            System.out.println("two player game start");
            fireNewGame();
        });
        MainMenuButton onlineGameBtn = new MainMenuButton("ONLINE GAME", ()-> {
            System.out.println("onlineGame start");
            isOnline = true;
            fireNewGame();
        });
        MainMenuButton constructBtn = new MainMenuButton("CONSTRUCT", () -> {
            getContentRoot().getChildren().setAll(new ConstructPane());
        });
        MainMenuButton helpBtn = new MainMenuButton("HELP", this::instructions);
        MainMenuButton exitBtn = new MainMenuButton("EXIT", () -> getGameController().exit());
        ToggleGroup tg = new ToggleGroup();
        tg.getToggles().addAll(singleGameBtn, twoGameBtn, onlineGameBtn, constructBtn, helpBtn, exitBtn);
        singleGameBtn.setSelected(true);
        VBox menuBox = new VBox(
                4,
                singleGameBtn,
                twoGameBtn,
                onlineGameBtn,
                constructBtn,
                helpBtn,
                exitBtn
        );
        menuBox.setAlignment(Pos.CENTER_LEFT);
        menuBox.setLayoutX(240);
        menuBox.setLayoutY(300);
        menuBox.setVisible(false);

        Texture tankTexture = FXGL.texture("ui/tankLoading.png");

        tt = new TranslateTransition(Duration.seconds(2), tankTexture);
        tt.setInterpolator(Interpolators.ELASTIC.EASE_OUT());
        tt.setFromX(172);
        tt.setFromY(180);
        tt.setToX(374);
        tt.setToY(180);
        tt.setOnFinished(e -> menuBox.setVisible(true));

        Rectangle bgRect = new Rectangle(getAppWidth(), getAppHeight());
        Line line = new Line(30, 580, 770, 580);
        line.setStroke(Color.web("#B9340D"));
        line.setStrokeWidth(2);
        Texture textureWall = texture("ui/fxgl.png");
        textureWall.setLayoutX(310);
        textureWall.setLayoutY(600);

        defaultPane = new Pane(bgRect, texture, tankTexture, menuBox, line, textureWall);
        getContentRoot().getChildren().setAll(defaultPane);
    }

    @Override
    public void onCreate() {
        getContentRoot().getChildren().setAll(defaultPane);
        FXGL.play("mainMenuLoad.wav");
        tt.play();
    }

    /**
     * 显示玩家使用帮助.比如如何移动坦克,如何发射子弹
     */
    private void instructions() {
        GridPane pane = new GridPane();
        pane.setHgap(20);
        pane.setVgap(15);
        KeyView kvW = new KeyView(W);
        kvW.setPrefWidth(38);
        TilePane tp1 = new TilePane(kvW, new KeyView(S), new KeyView(A), new KeyView(D));
        tp1.setPrefWidth(200);
        tp1.setHgap(2);
        tp1.setAlignment(Pos.CENTER_LEFT);

        pane.addRow(0, getUIFactoryService().newText("Movement"), tp1);
        pane.addRow(1, getUIFactoryService().newText("Shoot"), new KeyView(F));
        KeyView kvL = new KeyView(LEFT);
        kvL.setPrefWidth(38);
        TilePane tp2 = new TilePane(new KeyView(UP), new KeyView(DOWN), kvL, new KeyView(RIGHT));
        tp2.setPrefWidth(200);
        tp2.setHgap(2);
        tp2.setAlignment(Pos.CENTER_LEFT);
        pane.addRow(2, getUIFactoryService().newText("Movement"), tp2);
        pane.addRow(3, getUIFactoryService().newText("Shoot"), new KeyView(SPACE));
        DialogService dialogService = getDialogService();
        dialogService.showBox("Help", pane, getUIFactoryService().newButton("OK"));
    }

    public static int getPlayerNum(){
        return playerNum;
    }

    public static boolean getIsOnline(){
        return isOnline;
    }

    public static boolean getIsP1(){ return isP1; }
}
