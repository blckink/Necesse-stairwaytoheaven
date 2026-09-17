package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.level.gameObject.FenceObject;
import necesse.level.maps.Level;

/**
 * The War Veteran's barbed wire ({@code barbedwirefence}): a vanilla
 * {@link FenceObject} (connecting posts and rails) whose entity cuts every
 * hostile mob touching the wire — see {@link BarbedWireObjectEntity}.
 */
public class BarbedWireFenceObject extends FenceObject {

    public BarbedWireFenceObject(String textureName, Color mapColor, int collisionWidth, int collisionHeight) {
        super(textureName, mapColor, collisionWidth, collisionHeight);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new BarbedWireObjectEntity(level, this.getStringID(), x, y);
    }
}
