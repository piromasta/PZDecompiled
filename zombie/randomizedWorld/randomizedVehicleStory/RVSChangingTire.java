package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemSpawner;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RVSChangingTire extends RandomizedVehicleStoryBase {
   public RVSChangingTire() {
      this.name = "Changing Tire";
      this.minZoneWidth = 5;
      this.minZoneHeight = 5;
      this.setChance(30);
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      float var3 = 0.5235988F;
      this.callVehicleStorySpawner(var1, var2, Rand.Next(-var3, var3));
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      boolean var5 = Rand.NextBool(2);
      if (var3) {
         var5 = true;
      }

      int var6 = var5 ? 1 : -1;
      Vector2 var7 = IsoDirections.N.ToVector();
      var4.addElement("vehicle1", (float)var6 * -1.5F, 0.0F, var7.getDirection(), 2.0F, 5.0F);
      var4.addElement("tire1", (float)var6 * 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
      var4.addElement("tool1", (float)var6 * 0.8F, -0.2F, 0.0F, 1.0F, 1.0F);
      var4.addElement("tool2", (float)var6 * 1.2F, 0.2F, 0.0F, 1.0F, 1.0F);
      var4.addElement("tire2", (float)var6 * 2.0F, 0.0F, 0.0F, 1.0F, 1.0F);
      var4.setParameter("zone", var1);
      var4.setParameter("removeRightWheel", var5);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = PZMath.max(var2.position.x - (float)var3.x, 0.001F);
         float var5 = PZMath.max(var2.position.y - (float)var3.y, 0.001F);
         float var6 = 0.0F;
         float var7 = var2.z;
         Zone var8 = (Zone)var1.getParameter("zone", Zone.class);
         boolean var9 = var1.getParameterBoolean("removeRightWheel");
         BaseVehicle var10 = (BaseVehicle)var1.getParameter("vehicle1", BaseVehicle.class);
         InventoryItem var14;
         switch (var2.id) {
            case "tire1":
               if (var10 != null) {
                  var14 = ItemSpawner.spawnItem("Base.ModernTire" + var10.getScript().getMechanicType(), var3, var4, var5, var6);
                  if (var14 != null) {
                     var14.setItemCapacity((float)var14.getMaxCapacity());
                  }

                  this.addBloodSplat(var3, Rand.Next(10, 20));
               }
               break;
            case "tire2":
               if (var10 != null) {
                  var14 = ItemSpawner.spawnItem("Base.OldTire" + var10.getScript().getMechanicType(), var3, var4, var5, var6);
                  if (var14 != null) {
                     var14.setCondition(0, false);
                  }
               }
               break;
            case "tool1":
               if (Rand.Next(2) == 0) {
                  ItemSpawner.spawnItem("Base.LugWrench", var3, var4, var5, var6);
               } else {
                  ItemSpawner.spawnItem("Base.TireIron", var3, var4, var5, var6);
               }
               break;
            case "tool2":
               ItemSpawner.spawnItem("Base.Jack", var3, var4, var5, var6);
               break;
            case "vehicle1":
               var10 = this.addVehicle(var8, var2.position.x, var2.position.y, var7, var2.direction, "good", (String)null, (Integer)null, (String)null);
               if (var10 != null) {
                  var10.setAlarmed(false);
                  var10.setGeneralPartCondition(0.7F, 40.0F);
                  var10.setRust(0.0F);
                  VehiclePart var13 = var10.getPartById(var9 ? "TireRearRight" : "TireRearLeft");
                  var10.setTireRemoved(var13.getWheelIndex(), true);
                  var13.setModelVisible("InflatedTirePlusWheel", false);
                  var13.setInventoryItem((InventoryItem)null);
                  this.addZombiesOnVehicle(2, (String)null, (Integer)null, var10);
                  var1.setParameter("vehicle1", var10);
               }
         }

      }
   }
}
