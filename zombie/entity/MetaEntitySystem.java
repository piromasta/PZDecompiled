package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.debug.DebugLog;
import zombie.entity.util.ImmutableArray;

public class MetaEntitySystem extends EngineSystem {
   EntityBucket metaEntities;

   public MetaEntitySystem(int var1) {
      super(false, false, 2147483647, false, 2147483647);
   }

   public void addedToEngine(Engine var1) {
      this.metaEntities = var1.getCustomBucket("MetaEntities");
   }

   public void removedFromEngine(Engine var1) {
   }

   ByteBuffer saveMetaEntities(ByteBuffer var1) throws IOException {
      int var2 = this.metaEntities.getEntities().size() * 1024;
      var1 = GameEntityManager.ensureCapacity(var1, var1.position() + var2);
      ImmutableArray var3 = this.metaEntities.getEntities();
      var1.putInt(var3.size());
      DebugLog.Entity.println("Saving meta entities, size = " + var3.size());

      for(int var5 = 0; var5 < var3.size(); ++var5) {
         GameEntity var4 = (GameEntity)var3.get(var5);
         if (!(var4 instanceof MetaEntity)) {
            throw new IOException("Expected MetaEntity");
         }

         ((MetaEntity)var4).saveMetaEntity(var1);
         if (var1.position() > var1.capacity() - 1048576) {
            var1 = GameEntityManager.ensureCapacity(var1, var1.capacity() + 1048576);
         }
      }

      return var1;
   }

   void loadMetaEntities(ByteBuffer var1, int var2) throws IOException {
      int var3 = var1.getInt();
      DebugLog.Entity.println("Loading meta entities, size = " + var3);

      for(int var5 = 0; var5 < var3; ++var5) {
         MetaEntity var4 = MetaEntity.alloc();
         var4.loadMetaEntity(var1, var2);
         GameEntityManager.RegisterEntity(var4);
      }

   }
}
