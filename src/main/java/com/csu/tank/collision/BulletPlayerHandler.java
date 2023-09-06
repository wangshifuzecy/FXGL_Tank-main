package com.csu.tank.collision;

import com.almasb.fxgl.dsl.components.EffectComponent;
import com.almasb.fxgl.dsl.components.HealthIntComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.physics.CollisionHandler;
import com.csu.tank.GameType;
import com.csu.tank.TankApp;
import com.csu.tank.effects.HelmetEffect;
import com.csu.tank.ui.GameMainMenu;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * @author LeeWyatt
 * 子弹和玩家碰撞(为了扩展多个玩家,所以这里忽略了来自同盟的子弹,友军不能误伤;同样忽略的代码在产生子弹实体的方法里)
 * 子弹消失,玩家减少生命值
 */
public class BulletPlayerHandler extends CollisionHandler {
    public BulletPlayerHandler() {
        super(GameType.BULLET, GameType.PLAYER);
    }

    protected void onCollisionBegin(Entity bullet, Entity player) {
        play("normalBomb.wav");
        if (player.getComponent(EffectComponent.class).hasEffect(HelmetEffect.class)) {
            bullet.removeFromWorld();
            return;
        }
        spawn("explode", bullet.getCenter().getX() - 25, bullet.getCenter().getY() - 20);
        bullet.removeFromWorld();
        HealthIntComponent hp = player.getComponent(HealthIntComponent.class);
        hp.damage(1);
        //System.out.println("take damage");
        TankApp tankApp = getAppCast();
        if (hp.isZero()) {
            if (!getb("gameOver")) {
                player.removeFromWorld();
                inc("liveNum",-1);
                if(GameMainMenu.getIsOnline()) {
                    TankApp.onlineOver();
                }
            }
        }
    }
}
