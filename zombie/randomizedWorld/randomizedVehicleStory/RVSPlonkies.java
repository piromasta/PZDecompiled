package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RVSPlonkies extends RandomizedVehicleStoryBase {
   public RVSPlonkies() {
      this.name = "Plonkies";
      this.minZoneWidth = 5;
      this.minZoneHeight = 5;
      this.setChance(1);
      this.setUnique(true);
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      boolean var5 = Rand.NextBool(2);
      if (var3) {
         var5 = true;
      }

      int var6 = var5 ? 1 : -1;
      boolean var7 = Rand.NextBool(2);
      Vector2 var8 = IsoDirections.N.ToVector();
      float var9 = 0.5235988F;
      if (var3) {
         var9 = 0.0F;
      }

      var8.rotate(Rand.Next(-var9, var9));
      var4.addElement("vehicle1", (float)(-var6) * 2.0F, 0.0F, var8.getDirection(), 2.0F, 5.0F);
      float var10 = 0.0F;
      int var11 = Rand.Next(2, 5);

      for(int var12 = 0; var12 < var11; ++var12) {
         var4.addElement("bag", (float)var6 * Rand.Next(0.0F, 3.0F), -Rand.Next(0.7F, 2.3F), 0.0F, 1.0F, 1.0F);
      }

      var4.setParameter("damageRightWheel", var7);
      var4.setParameter("zone", var1);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         boolean var6 = var1.getParameterBoolean("damageRightWheel");
         BaseVehicle var7 = (BaseVehicle)var1.getParameter("vehicle1", BaseVehicle.class);
         switch (var2.id) {
            case "bag":
               if (var7 != null) {
                  String var13 = "Base.Plonkies";
                  InventoryItem var14 = InventoryItemFactory.CreateItem(var13);
                  this.addItemOnGround(var3, var14);
               }
               break;
            case "vehicle1":
               var7 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, (String)null, "StepVan_Plonkies", (Integer)null, (String)null);
               if (var7 != null) {
                  var7.setAlarmed(false);
                  var7.setGeneralPartCondition(0.7F, 40.0F);
                  var7.setRust(0.0F);
                  VehiclePart var10 = var7.getPartById(var6 ? "TireRearRight" : "TireRearLeft");
                  var10.setCondition(0);
                  VehiclePart var11 = var7.getPartById("GasTank");
                  var11.setContainerContentAmount(0.0F);
                  String var12 = "PlonkiesGuy";
                  this.addZombiesOnVehicle(1, var12, 0, var7);
                  var1.setParameter("vehicle1", var7);
                  var7.addKeyToWorld();
               }
         }

      }
   }
}
