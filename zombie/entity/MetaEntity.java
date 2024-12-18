package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public class MetaEntity extends GameEntity {
   private static final ConcurrentLinkedQueue<MetaEntity> pool = new ConcurrentLinkedQueue();
   private long entityNetID;
   private GameEntityType originalEntityType;
   private float x;
   private float y;
   private float z;
   boolean scheduleForReleaseToPool = false;

   static MetaEntity alloc(GameEntity var0) {
      if (Core.bDebug && !(var0 instanceof IsoObject)) {
         throw new RuntimeException("Can't alloc non-isoObject");
      } else {
         MetaEntity var1 = (MetaEntity)pool.poll();
         if (var1 == null) {
            var1 = new MetaEntity();
         }

         var1.entityNetID = var0.getEntityNetID();
         var1.x = var0.getX();
         var1.y = var0.getY();
         var1.z = var0.getZ();
         var1.originalEntityType = var0.getGameEntityType();
         return var1;
      }
   }

   static MetaEntity alloc() {
      MetaEntity var0 = (MetaEntity)pool.poll();
      if (var0 == null) {
         var0 = new MetaEntity();
      }

      return var0;
   }

   static void release(MetaEntity var0) {
      if (var0 != null) {
         var0.reset();
         pool.offer(var0);
      }
   }

   private MetaEntity() {
   }

   public final void saveMetaEntity(ByteBuffer var1) throws IOException {
      var1.putLong(this.entityNetID);
      var1.put(this.originalEntityType.getByteId());
      var1.putFloat(this.x);
      var1.putFloat(this.y);
      var1.putFloat(this.z);
      this.saveEntity(var1);
   }

   public final void loadMetaEntity(ByteBuffer var1, int var2) throws IOException {
      this.entityNetID = var1.getLong();
      this.originalEntityType = GameEntityType.FromID(var1.get());
      this.x = var1.getFloat();
      this.y = var1.getFloat();
      this.z = var1.getFloat();
      this.loadEntity(var1, var2);
   }

   public GameEntityType getGameEntityType() {
      return GameEntityType.MetaEntity;
   }

   public GameEntityType getOriginalGameEntityType() {
      return this.originalEntityType;
   }

   public boolean isEntityValid() {
      return true;
   }

   public IsoGridSquare getSquare() {
      return null;
   }

   public long getEntityNetID() {
      return this.entityNetID;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getZ() {
      return this.z;
   }

   public boolean isMeta() {
      return true;
   }

   public boolean isUsingPlayer(IsoPlayer var1) {
      return false;
   }

   public IsoPlayer getUsingPlayer() {
      return null;
   }

   public void setUsingPlayer(IsoPlayer var1) {
   }

   public void reset() {
      super.reset();
      this.scheduleForReleaseToPool = false;
      this.entityNetID = -9223372036854775808L;
      this.x = 0.0F;
      this.y = 0.0F;
      this.z = 0.0F;
   }
}
