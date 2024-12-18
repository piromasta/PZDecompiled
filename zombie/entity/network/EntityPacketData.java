package zombie.entity.network;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;

public class EntityPacketData {
   private static final ConcurrentLinkedDeque<EntityPacketData> pool = new ConcurrentLinkedDeque();
   private EntityPacketType entityPacketType;
   public final ByteBuffer bb = ByteBuffer.allocate(1000000);

   public static EntityPacketData alloc(EntityPacketType var0) {
      EntityPacketData var1 = (EntityPacketData)pool.poll();
      if (var1 == null) {
         var1 = new EntityPacketData();
      }

      var1.entityPacketType = var0;
      var0.saveToByteBuffer(var1.bb);
      return var1;
   }

   public static void release(EntityPacketData var0) {
      var0.bb.clear();

      assert !Core.bDebug || !pool.contains(var0) : "Object already exists in pool.";

      pool.offer(var0);
   }

   public EntityPacketData() {
   }

   public EntityPacketType getEntityPacketType() {
      return this.entityPacketType;
   }
}
