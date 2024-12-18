package zombie.entity.events;

import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.entity.Component;

public class ComponentEvent {
   protected static final ConcurrentLinkedDeque<ComponentEvent> pool = new ConcurrentLinkedDeque();
   private ComponentEventType eventType;
   private Component sender;

   public static ComponentEvent Alloc(ComponentEventType var0, Component var1) {
      ComponentEvent var2 = (ComponentEvent)pool.poll();
      if (var2 == null) {
         var2 = new ComponentEvent();
      }

      var2.eventType = var0;
      var2.sender = var1;
      return var2;
   }

   private ComponentEvent() {
   }

   public ComponentEventType getEventType() {
      return this.eventType;
   }

   public Component getSender() {
      return this.sender;
   }

   protected void reset() {
      this.sender = null;
   }

   public void release() {
      this.reset();

      assert !Core.bDebug || !pool.contains(this) : "Object already in pool.";

      pool.offer(this);
   }
}
