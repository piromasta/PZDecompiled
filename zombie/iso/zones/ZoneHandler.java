package zombie.iso.zones;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import zombie.debug.DebugLog;

public class ZoneHandler<U extends Zone> {
   private HashMap<UUID, U> zones = new HashMap();

   public ZoneHandler() {
   }

   public void Dispose() {
      this.zones.clear();
   }

   public void addZone(U var1) {
      DebugLog.Zone.debugln(var1);
      this.zones.put(var1.id, var1);
   }

   public U getZone(UUID var1) {
      return (Zone)this.zones.get(var1);
   }

   public Collection<U> getZones() {
      return this.zones.values();
   }
}
