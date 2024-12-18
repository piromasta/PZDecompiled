package zombie.network.server;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameTime;
import zombie.core.NetTimedAction;
import zombie.debug.DebugLog;

public class AnimEventEmulator {
   private static final AnimEventEmulator instance = new AnimEventEmulator();
   private final ArrayList<AnimEvent> events = new ArrayList();

   public static AnimEventEmulator getInstance() {
      return instance;
   }

   private AnimEventEmulator() {
   }

   public long getDurationMax() {
      return 1800000L;
   }

   public void create(NetTimedAction var1, long var2, boolean var4, String var5, String var6) {
      DebugLog.Action.debugln("%s %s (%s) %s %d ms", var1.type, var5, var6, var4 ? "after" : "every", var2);
      this.events.add(new AnimEvent(var1, var2, var4, var5, var6));
   }

   public void remove(NetTimedAction var1) {
      this.events.removeIf((var1x) -> {
         return var1x.action == var1;
      });
   }

   public void update() {
      long var1 = GameTime.getServerTimeMills();
      Iterator var3 = this.events.iterator();

      while(var3.hasNext()) {
         AnimEvent var4 = (AnimEvent)var3.next();
         if (var4.action != null && var1 >= var4.time + var4.duration) {
            var4.action.animEvent(var4.event, var4.parameter);
            if (var4.isOnce) {
               var4.time = var4.start + this.getDurationMax();
            } else {
               var4.time = GameTime.getServerTimeMills();
            }
         }
      }

      this.events.removeIf((var3x) -> {
         return var1 >= var3x.start + this.getDurationMax();
      });
   }

   public static class AnimEvent {
      private final NetTimedAction action;
      private final String event;
      private final String parameter;
      private final long start;
      private final long duration;
      private boolean isOnce;
      private long time;

      private AnimEvent(NetTimedAction var1, long var2, boolean var4, String var5, String var6) {
         long var7 = GameTime.getServerTimeMills();
         this.action = var1;
         this.start = var7;
         this.time = var7;
         this.duration = var2;
         this.isOnce = var4;
         this.event = var5;
         this.parameter = var6;
      }
   }
}
