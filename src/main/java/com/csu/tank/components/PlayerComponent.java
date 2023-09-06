package com.csu.tank.components;

import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.core.util.LazyValue;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.EffectComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityGroup;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.BoundingBoxComponent;
import com.almasb.fxgl.entity.components.IDComponent;
import com.almasb.fxgl.time.LocalTimer;
import com.csu.tank.GameConfig;
import com.csu.tank.GameType;
import com.csu.tank.effects.ShipEffect;

import static com.almasb.fxgl.dsl.FXGL.*;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.spawn;

/**
 * @author LeeWyatt
 * 玩家的行为,移动和射击
 */
public class PlayerComponent extends Component {
    /**
     * 为了防止出现斜向上,斜向下等角度的移动,
     */
    public static IDComponent id;
    private boolean movedThisFrame = false;
    private double speed = 0;
    private Vec2 velocity = new Vec2();
    private BoundingBoxComponent bbox;
    private LazyValue<EntityGroup> blocksAll = new LazyValue<>(() -> entity.getWorld().getGroup(GameType.BRICK, GameType.FLAG, GameType.SEA, GameType.STONE, GameType.ENEMY, GameType.BORDER_WALL, GameType.PLAYER));
    private LazyValue<EntityGroup> blocks = new LazyValue<>(() -> entity.getWorld().getGroup(GameType.BRICK, GameType.FLAG, GameType.STONE, GameType.ENEMY, GameType.BORDER_WALL, GameType.PLAYER));
    private LocalTimer shootTimer = FXGL.newLocalTimer();
    private Dir moveDir = Dir.UP;

    @Override
    public void onUpdate(double tpf) {
        speed = tpf * GameConfig.PLAYER_SPEED;
        movedThisFrame = false;
    }

    public void right() {
        if (this != null && getb("gameOver") != true) {
            if (movedThisFrame) {
                return;
            }
            movedThisFrame = true;
            getEntity().setRotation(90);
            moveDir = Dir.RIGHT;
            move();
        }
    }

    public void left() {
        if (this != null && getb("gameOver") != true) {
            if (movedThisFrame) {
                return;
            }
            movedThisFrame = true;
            getEntity().setRotation(270);
            moveDir = Dir.LEFT;
            move();
        }
    }

    public void down() {
        if (this != null && getb("gameOver") != true) {
            if (movedThisFrame) {
                return;
            }
            movedThisFrame = true;
            getEntity().setRotation(180);
            moveDir = Dir.DOWN;
            move();
        }
    }

    public void up() {
        if(this != null && getb("gameOver") != true) {
            if (movedThisFrame) {
                return;
            }
            movedThisFrame = true;
            getEntity().setRotation(0);
            moveDir = Dir.UP;
            move();
        }
    }

    private void move() {
        if (!getEntity().isActive()) {
            return;
        }
        velocity.set((float) (moveDir.getVector().getX()*speed), (float) (moveDir.getVector().getY()*speed));
        int length = Math.round(velocity.length());
        velocity.normalizeLocal();
        List<Entity> blockList;
        if (entity.getComponent(EffectComponent.class).hasEffect(ShipEffect.class)) {
            blockList = blocks.get().getEntitiesCopy();
        } else {
            blockList = blocksAll.get().getEntitiesCopy();
        }
        for (int i = 0; i < length; i++) {
            entity.translate(velocity.x, velocity.y);
            Entity entitytmp;
            boolean collision = false;
            for (int j = 0; j < blockList.size(); j++) {
                entitytmp = blockList.get(j);
                if(entitytmp == entity){
                    continue;
                }
                if (entitytmp.getBoundingBoxComponent().isCollidingWith(bbox)) {
                    collision = true;
                    break;
                }
            }
            //运动, 遇到障碍物回退
            if (collision) {
                entity.translate(-velocity.x, -velocity.y);
                break;
            }
        }
    }

    public void shoot() {
        if (this != null && getb("gameOver") != true) {
            if (!shootTimer.elapsed(GameConfig.PLAYER_SHOOT_DELAY)) {
                return;
            }
            int x = 0;
            int y = 0;
            if(moveDir == Dir.UP){
                x = -5; y = -24;
            }else if(moveDir == Dir.DOWN){
                x = -5; y = 24;
            }else if(moveDir == Dir.RIGHT){
                x = 24; y = -5;
            }else if(moveDir == Dir.LEFT){
                x = -24; y = -5;
            }
            spawn("bullet", new SpawnData(getEntity().getCenter().add(x, y))
                .put("direction", moveDir.getVector())
                .put("owner", entity));
            shootTimer.capture();
        }
    }
}
