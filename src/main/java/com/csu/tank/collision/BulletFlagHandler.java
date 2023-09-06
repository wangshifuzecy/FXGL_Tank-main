package com.csu.tank.collision;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import com.csu.tank.GameType;
import com.csu.tank.TankApp;
import com.csu.tank.components.FlagViewComponent;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * @author LeeWyatt
 * 子弹不分阵营, 只要击中旗子,立刻判断为失败
 */
public class BulletFlagHandler extends CollisionHandler {

    public BulletFlagHandler() {
        super(GameType.BULLET, GameType.FLAG);
    }

    @Override
    protected void onCollisionBegin(Entity bullet, Entity flag) {
        if (!getb("gameOver")) {
            FlagViewComponent flagComponent = flag.getComponent(FlagViewComponent.class);
            flagComponent.hitFlag();
            play("normalBomb.wav");
            spawn("explode", bullet.getCenter().getX() - 25, bullet.getCenter().getY() - 20);
            bullet.removeFromWorld();
            TankApp app = getAppCast();
            if (!getb("gameOver")) {
                set("gameOver", true);
                getSceneService().pushSubScene(app.failedSceneLazyValue.get());
            }
        }
    }
}
