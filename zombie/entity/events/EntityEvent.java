package zombie.entity.events;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.entity.GameEntity;

public class EntityEvent {
   protected static final ConcurrentLinkedDeque<EntityEvent> pool = new ConcurrentLinkedDeque();
   private EntityEventType eventType;
   private GameEntity entity;

   public static EntityEvent Alloc(EntityEventType var0, GameEntity var1) {
      EntityEvent var2 = (EntityEvent)pool.poll();
      if (var2 == null) {
         var2 = new EntityEvent();
      }

      var2.eventType = var0;
      var2.entity = var1;
      return var2;
   }

   private EntityEvent() {
   }

   public EntityEventType getEventType() {
      return this.eventType;
   }

   public GameEntity getEntity() {
      return this.entity;
   }

   protected void reset() {
      this.entity = null;
   }

   public void release() {
      this.reset();

      assert !Core.bDebug || !pool.contains(this) : "Object already in pool.";

      pool.offer(this);
   }
}
