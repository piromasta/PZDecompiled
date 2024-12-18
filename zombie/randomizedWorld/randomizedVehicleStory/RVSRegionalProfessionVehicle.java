package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.Lua.LuaManager;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;

public final class RVSRegionalProfessionVehicle extends RandomizedVehicleStoryBase {
   public RVSRegionalProfessionVehicle() {
      this.name = "Regional Profession Vehicle - Will not always spawn a vehicle due to unique vehicle control";
      this.minZoneWidth = 2;
      this.minZoneHeight = 5;
      this.setChance(30);
      this.needsRegion = true;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      Vector2 var5 = IsoDirections.N.ToVector();
      float var6 = 0.5235988F;
      if (var3) {
         var6 = 0.0F;
      }

      var5.rotate(Rand.Next(-var6, var6));
      var4.addElement("vehicle1", 0.0F, 0.0F, var5.getDirection(), 2.0F, 5.0F);
      var4.setParameter("zone", var1);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         switch (var2.id) {
            case "vehicle1":
               String var8 = ItemPickerJava.getSquareRegion(var3);
               Object var9 = LuaManager.getFunctionObject("ProfessionVehicles.OnCreateRegion");
               if (var9 != null) {
                  LuaManager.caller.pcallvoid(LuaManager.thread, var9, var8, var3, IsoDirections.fromAngle(var2.direction));
               }
            default:
         }
      }
   }
}
