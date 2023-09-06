package com.csu.tank;

import com.almasb.fxgl.app.CursorInfo;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.*;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.core.util.LazyValue;
import com.almasb.fxgl.dsl.components.EffectComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.entity.level.tiled.TMXLevelLoader;
import com.almasb.fxgl.time.TimerAction;
import com.csu.tank.collision.*;
import com.csu.tank.ui.*;
import com.csu.tank.collision.*;
import com.csu.tank.collision.*;
import com.csu.tank.components.PlayerComponent;
import com.csu.tank.effects.HelmetEffect;
import com.csu.tank.ui.*;
import com.csu.tank.ui.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * @author LeeWyatt
 */
public class TankApp extends GameApplication {
    private static Entity player;
    private static Entity player2;
    // 2p
    private static PlayerComponent playerComponent = null;
    private static PlayerComponent playerComponent2 = null;
    private Random random = new Random();
    public static LazyValue<FailedScene> failedSceneLazyValue = new LazyValue<>(FailedScene::new);
    private static LazyValue<SuccessScene> successSceneLazyValue = new LazyValue<>(SuccessScene::new);

    /**
     * 顶部的三个点,用于产生敌军坦克
     */
    private int[] enemySpawnX = {30, 295 + 30, 589 + 20};

    /**
     * 基地加固定时器动作
     */
    private TimerAction spadeTimerAction;
    /**
     * 敌军冻结计的定时器动作
     */
    private TimerAction freezingTimerAction;
    /**
     * 定时刷新敌军坦克
     */
    private TimerAction spawnEnemyTimerAction;
    @Override
    protected void onPreInit() {
        getSettings().setGlobalSoundVolume(0.5);
        getSettings().setGlobalMusicVolume(0.5);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(28 * 24 + 6 * 24);
        settings.setHeight(28 * 24);
        settings.setTitle("90 Tank");
        settings.setAppIcon("ui/icon.png");
        settings.setVersion("Version 0.3");
        settings.setMainMenuEnabled(true);
        settings.setGameMenuEnabled(true);
        settings.getCSSList().add("tankApp.css");
        settings.setDefaultCursor(new CursorInfo("ui/cursor.png", 0, 0));
        //FPS,CPU,RAM等信息的显示
        //settings.setProfilingEnabled(true);
        //开发模式.这样可以输出较多的日志异常追踪
        //settings.setApplicationMode(ApplicationMode.DEVELOPER);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public StartupScene newStartup(int width, int height) {
                //自定义启动场景
                return new GameStartupScene(width, height);
            }

            @Override
            public FXGLMenu newMainMenu() {
                //主菜单场景
                return new GameMainMenu();
            }

            @Override
            public LoadingScene newLoadingScene() {
                //游戏前的加载场景
                return new GameLoadingScene();
            }
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        if (getFileSystemService().exists(GameConfig.CUSTOM_LEVEL_PATH)) {
            vars.put("level", 0);
        }else {
            vars.put("level", 1);
        }
        vars.put("playerBulletLevel", 1);
        vars.put("freezingEnemy", false);
        vars.put("destroyedEnemy", 0);
        vars.put("spawnedEnemy", 0);
        vars.put("gameOver", false);
        vars.put("spawnPlayer",0); //已召唤的玩家
        vars.put("liveNum",1);//现存玩家数量
        vars.put("isDead",false);//联机模式中操纵的玩家是否死亡
    }

    @Override
    protected void initInput() {
        onKeyDown(KeyCode.BACK_SPACE, TankApp::winAction) ;//胜利
        onKeyDown(KeyCode.F1, TankApp::loseAction);//失败

        onKey(KeyCode.W, this::moveUpAction);
        onKey(KeyCode.S, this::moveDownAction);
        onKey(KeyCode.A, this::moveLeftAction);
        onKey(KeyCode.D, this::moveRightAction);
        onKey(KeyCode.SPACE, this::shootAction);

        onKey(KeyCode.UP, this::moveUpAction2);
        onKey(KeyCode.DOWN, this::moveDownAction2);
        onKey(KeyCode.LEFT, this::moveLeftAction2);
        onKey(KeyCode.RIGHT, this::moveRightAction2);
        onKey(KeyCode.NUMPAD0, this::shootAction2);
        onKey(KeyCode.ENTER, this::shootAction2);
        /*
        onKey(KeyCode.UP, ()->playerComponent2.up());
        onKey(KeyCode.DOWN, ()->playerComponent2.down());
        onKey(KeyCode.LEFT, ()->playerComponent2.left());
        onKey(KeyCode.RIGHT, ()->playerComponent2.right());
        onKey(KeyCode.NUMPAD0,()->playerComponent2.shoot());

        onKey(KeyCode.W, ()->playerComponent.up());
        onKey(KeyCode.S, ()->playerComponent.down());
        onKey(KeyCode.A, ()->playerComponent.left());
        onKey(KeyCode.D, ()->playerComponent.right());
        onKey(KeyCode.SPACE,()->playerComponent.shoot());
         */
    }

    //胜利播片
    private static void winAction(){
        //System.out.println("skip");
        set("gameOver",true);
        runOnce(
            () -> getSceneService().pushSubScene(successSceneLazyValue.get()),
            Duration.seconds(1.5));
    }

    //失败播片
    private static void loseAction(){
        set("gameOver",true);
        runOnce(
            () -> getSceneService().pushSubScene(failedSceneLazyValue.get()),
            Duration.seconds(1.5));
    }

    private boolean tankIsReady() {
        return player != null && playerComponent != null && !getb("gameOver") && player.isActive();
    }

    private boolean tankIsReady2() {
        return player2 != null && playerComponent2 != null && !getb("gameOver") && player2.isActive();
    }

    private void shootAction() {
        //System.out.println("shoot");
        if (tankIsReady()) {
            playerComponent.shoot();
        }
    }

    private void moveRightAction() {
        if (tankIsReady()) {
            playerComponent.right();
        }
    }

    private void moveLeftAction() {
        if (tankIsReady()) {
            playerComponent.left();
        }
    }

    private void moveDownAction() {
        if (tankIsReady()) {
            playerComponent.down();
        }
    }

    private void moveUpAction() {
        if (tankIsReady()) {
            playerComponent.up();
        }
    }
    private void shootAction2() {
        if (tankIsReady2()) {
            playerComponent2.shoot();
        }
    }

    private void moveRightAction2() {
        if (tankIsReady2()) {
            playerComponent2.right();
        }
    }

    private void moveLeftAction2() {
        if (tankIsReady2()) {
            playerComponent2.left();
        }
    }

    private void moveDownAction2() {
        if (tankIsReady2()) {
            playerComponent2.down();
        }
    }

    private void moveUpAction2() {
        if (tankIsReady2()) {
            playerComponent2.up();
        }
    }
    @Override
    protected void initGame() {
        getGameScene().setBackgroundColor(Color.BLACK);
        getGameWorld().addEntityFactory(new GameEntityFactory());

        buildAndStartLevel();
        if(!GameMainMenu.getIsOnline()) {
            getip("destroyedEnemy").addListener((ob, ov, nv) -> {
                if (nv.intValue() == GameConfig.ENEMY_AMOUNT) {
                    winAction();
                }
            });
            getip("liveNum").addListener((ob, ov, nv) -> {
                if (nv.intValue() == 0) {
                    loseAction();
                }
            });
        }
    }

    public void buildAndStartLevel() {
        //1. 清理上一个关卡的残留(这里主要是清理声音残留)
        //清理关卡的残留(这里主要是清理声音残留)
        getGameWorld().getEntitiesByType(
                GameType.BULLET, GameType.ENEMY, GameType.PLAYER
        ).forEach(Entity::removeFromWorld);

        //2. 开场动画
        Rectangle rect1 = new Rectangle(getAppWidth(), getAppHeight() / 2.0, Color.web("#333333"));
        Rectangle rect2 = new Rectangle(getAppWidth(), getAppHeight() / 2.0, Color.web("#333333"));
        rect2.setLayoutY(getAppHeight() / 2.0);
        Text text = null;
        if(GameMainMenu.getIsOnline()){
            text = new Text("GAME START");
        }else{
            text = new Text("STAGE " + geti("level"));
        }
        text.setFill(Color.WHITE);
        text.setFont(new Font(35));
        text.setLayoutX(getAppWidth() / 2.0 - 80);
        text.setLayoutY(getAppHeight() / 2.0 - 5);
        Pane p1 = new Pane(rect1, rect2, text);

        addUINode(p1);

        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(1.2),
                        new KeyValue(rect1.translateYProperty(), -getAppHeight() / 2.0),
                        new KeyValue(rect2.translateYProperty(), getAppHeight() / 2.0)
                ));
        tl.setOnFinished(e -> removeUINode(p1));

        PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
        Text finalText = text;
        pt.setOnFinished(e -> {
            finalText.setVisible(false);
            tl.play();
            //3. 开始新关卡
            startLevel();
        });
        pt.play();
    }

    private void startLevel() {
        if (spawnEnemyTimerAction != null) {
            spawnEnemyTimerAction.expire();
            spawnEnemyTimerAction = null;
        }
        set("gameOver", false);
        //清除上一关残留的道具影响
        set("freezingEnemy", false);
        //恢复消灭敌军数量
        set("destroyedEnemy", 0);
        //恢复生成敌军的数量
        set("spawnedEnemy", 0);
        set("spawnPlayer",0);
        set("liveNum", GameMainMenu.getPlayerNum());
        expireAction(freezingTimerAction);
        expireAction(spadeTimerAction);


        //如果关卡是 0 ,那么从用户自定义地图开始游戏
       if(GameMainMenu.getIsOnline()){
           setLevelFromMap("vs.tmx");
           player = spawn("player", 24 + 3, 25 * 24);
           playerComponent = player.getComponent(PlayerComponent.class);
           player2 = spawn("player", 25 * 24 + 3, 24);
           playerComponent2 = player2.getComponent(PlayerComponent.class);
           playerComponent2.getEntity().setRotation(180);
       }else {
           if (geti("level") == 0) {
               Level level;
               try {
                   level = new TMXLevelLoader()
                       .load(new File(GameConfig.CUSTOM_LEVEL_PATH).toURI().toURL(), getGameWorld());
                   getGameWorld().setLevel(level);
               } catch (MalformedURLException e) {
                   throw new RuntimeException(e);
               }
           } else {
               setLevelFromMap("level" + geti("level") + ".tmx");
           }
           play("start.wav");
           player = spawn("player", 9 * 24 + 3, 25 * 24);
           player.getComponent(EffectComponent.class).startEffect(new HelmetEffect());
           playerComponent = player.getComponent(PlayerComponent.class);
           if(GameMainMenu.getPlayerNum() == 2) {
               player2 = spawn("player", 17 * 24 + 3, 25 * 24);
               player2.getComponent(EffectComponent.class).startEffect(new HelmetEffect());
               playerComponent2 = player2.getComponent(PlayerComponent.class);
           }
           getGameScene().addGameView(new GameView(new InfoPane(), 100));
           for (int i = 0; i < enemySpawnX.length; i++) {
               spawn("enemy",
                   new SpawnData(enemySpawnX[i], 30).put("assentName", "tank/E" + FXGLMath.random(1, 12) + "U.png"));
               inc("spawnedEnemy", 1);
           }
           spawnEnemy();
           //每局开始玩家坦克都有无敌保护时间
           //显示信息的UI
           //首先产生几个敌方坦克
       }
    }

    private void spawnEnemy() {
        if (spawnEnemyTimerAction != null) {
            spawnEnemyTimerAction.expire();
            spawnEnemyTimerAction = null;
        }
        //用于检测碰撞的实体(避免坦克一产生就和其他坦克发生碰撞,"纠缠到一起")
        Entity spawnBox = spawn("spawnBox", new SpawnData(-100, -100));

        //用于产生敌人的定时器, 定期尝试产生敌军坦克, 但是如果生成敌军坦克的位置,被其他现有的坦克占据, 那么此次就不生成敌军坦克
        spawnEnemyTimerAction = run(() -> {
            //尝试产生敌军坦克的次数; 2次或者3次
            int testTimes = FXGLMath.random(2, 3);
            for (int i = 0; i < testTimes; i++) {
                if (geti("spawnedEnemy") < GameConfig.ENEMY_AMOUNT) {
                    boolean canGenerate = true;
                    //随机抽取数组的一个x坐标
                    int x = enemySpawnX[random.nextInt(3)];
                    int y = 30;
                    spawnBox.setPosition(x, y);
                    List<Entity> tankList = getGameWorld().getEntitiesByType(GameType.ENEMY, GameType.PLAYER);
                    //如果即将产生的敌军坦克位置和 目前已有的坦克位置冲突, 那么此处就不产生坦克
                    for (Entity tank : tankList) {
                        if (tank.isActive() && spawnBox.isColliding(tank)) {
                            canGenerate = false;
                            break;
                        }
                    }
                    //如果可以产生敌军坦克,那么生成坦克
                    if (canGenerate) {
                        inc("spawnedEnemy", 1);
                        spawn("enemy",
                                new SpawnData(x, y).put("assentName", "tank/E" + FXGLMath.random(1, 12) + "U.png"));
                    }
                    //隐藏这个实体
                    spawnBox.setPosition(-100, -100);

                } else {
                    if (spawnEnemyTimerAction != null) {
                        spawnEnemyTimerAction.expire();
                    }
                }
            }
        }, GameConfig.SPAWN_ENEMY_TIME);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().addCollisionHandler(new BulletEnemyHandler());
        getPhysicsWorld().addCollisionHandler(new BulletPlayerHandler());
        BulletBrickHandler bulletBrickHandler = new BulletBrickHandler();
        getPhysicsWorld().addCollisionHandler(bulletBrickHandler);
        getPhysicsWorld().addCollisionHandler(bulletBrickHandler.copyFor(GameType.BULLET, GameType.STONE));
        getPhysicsWorld().addCollisionHandler(bulletBrickHandler.copyFor(GameType.BULLET, GameType.GREENS));
        getPhysicsWorld().addCollisionHandler(new BulletFlagHandler());
        getPhysicsWorld().addCollisionHandler(new BulletBorderHandler());
        getPhysicsWorld().addCollisionHandler(new BulletBulletHandler());
        getPhysicsWorld().addCollisionHandler(new PlayerItemHandler());
    }

    public void freezingEnemy() {
        expireAction(freezingTimerAction);
        set("freezingEnemy", true);
        freezingTimerAction = runOnce(() -> {
            set("freezingEnemy", false);
        }, GameConfig.STOP_MOVE_TIME);
    }

    public void spadeBackUpBase() {
        expireAction(spadeTimerAction);
        //升级基地周围为石头墙
        updateWall(true);
        spadeTimerAction = runOnce(() -> {
            //基地周围的墙,还原成砖头墙
            updateWall(false);
        }, GameConfig.SPADE_TIME);
    }
    /**
     * 基地四周的防御
     * 按照游戏规则: 默认是砖头墙, 吃了铁锨后,升级成为石头墙;
     */
    private void updateWall(boolean isStone) {
        //循环找到包围基地周围的墙
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                if (row != 0 && (col == 1 || col == 2)) {
                    continue;
                }
                //删除旧的墙
                List<Entity> entityTempList = getGameWorld().getEntitiesAt(new Point2D(288 + col * 24, 576 + row * 24));
                for (Entity entityTemp : entityTempList) {
                    Serializable type = entityTemp.getType();
                    //如果是玩家自建的地图, 那么需要判断是不是水面草地雪地等
                    if (type == GameType.STONE || type == GameType.BRICK || type == GameType.SNOW || type == GameType.SEA || type == GameType.GREENS) {
                        if (entityTemp.isActive()) {
                            entityTemp.removeFromWorld();
                        }
                    }
                }
                //创建新的墙
                if (isStone) {
                    spawn("itemStone", new SpawnData(288 + col * 24, 576 + row * 24));
                } else {
                    spawn("brick", new SpawnData(288 + col * 24, 576 + row * 24));
                }
            }
        }
    }

    public static void onlineOver(){
        set("gameOver", true);
        if(
            (GameMainMenu.getIsP1() && player.isActive())
                ||
            (!GameMainMenu.getIsP1() && !player.isActive())
        ){
            winAction();
        }else{
            loseAction();
        }
    }

    /**
     * 让TimeAction过期
     */
    public void expireAction(TimerAction action) {
        if (action == null) {
            return;
        }
        if (!action.isExpired()) {
            action.expire();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
