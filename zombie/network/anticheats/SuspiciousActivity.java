package zombie.network.anticheats;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;

public class SuspiciousActivity {
   private final EnumMap<AntiCheat, Integer> counters = new EnumMap(AntiCheat.class);
   private final UpdateLimit ulDecreaseInterval = new UpdateLimit(150000L);
   protected final UdpConnection connection;

   public SuspiciousActivity(UdpConnection var1) {
      this.connection = var1;
   }

   public void update() {
      boolean var1 = this.ulDecreaseInterval.Check();
      Iterator var2 = this.counters.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         if (var1) {
            if ((Integer)var3.getValue() > 0) {
               var3.setValue((Integer)var3.getValue() - 1);
               AntiCheat.log(this.connection, (AntiCheat)var3.getKey(), (Integer)var3.getValue(), (String)null);
            } else {
               var3.setValue(0);
            }
         }
      }

   }

   public int report(AntiCheat var1) {
      if (AntiCheat.None.equals(var1)) {
         return 0;
      } else {
         int var2 = (Integer)this.counters.getOrDefault(var1, 0) + 1;
         this.counters.put(var1, var2);
         return var2;
      }
   }

   public EnumMap<AntiCheat, Integer> getCounters() {
      return this.counters;
   }
}
