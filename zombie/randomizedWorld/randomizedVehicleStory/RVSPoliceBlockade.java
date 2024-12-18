package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RVSPoliceBlockade extends RandomizedVehicleStoryBase {
   public RVSPoliceBlockade() {
      this.name = "Police Blockade";
      this.minZoneWidth = 8;
      this.minZoneHeight = 8;
      this.setChance(30);
      this.setMaximumDays(30);
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      float var5 = 0.17453292F;
      if (var3) {
         var5 = 0.0F;
      }

      float var6 = 1.5F;
      float var7 = 1.0F;
      if (this.zoneWidth >= 10) {
         var6 = 2.5F;
         var7 = 0.0F;
      }

      IsoDirections var8 = Rand.NextBool(2) ? IsoDirections.W : IsoDirections.E;
      Vector2 var9 = var8.ToVector();
      var9.rotate(Rand.Next(-var5, var5));
      var4.addElement("vehicle1", -var6, var7, var9.getDirection(), 2.0F, 5.0F);
      var9 = var8.Rot180().ToVector();
      var9.rotate(Rand.Next(-var5, var5));
      var4.addElement("vehicle2", var6, -var7, var9.getDirection(), 2.0F, 5.0F);
      String var10 = "Base.CarLightsPolice";
      if (Rand.NextBool(3)) {
         var10 = "Base.PickUpVanLightsPolice";
      }

      var4.setParameter("zone", var1);
      var4.setParameter("script", var10);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         String var6 = var1.getParameterString("script");
         switch (var2.id) {
            case "vehicle1":
            case "vehicle2":
               BaseVehicle var9 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, (String)null, var6, (Integer)null, (String)null);
               if (var9 != null) {
                  var9.setAlarmed(false);
                  if (Rand.NextBool(3)) {
                     var9.setHeadlightsOn(true);
                     var9.setLightbarLightsMode(2);
                     VehiclePart var10 = var9.getBattery();
                     if (var10 != null) {
                        var10.setLastUpdated(0.0F);
                     }
                  }

                  String var11 = "Police";
                  if (var9.getZombieType() != null) {
                     var11 = var9.getRandomZombieType();
                  }

                  this.addZombiesOnVehicle(Rand.Next(2, 4), var11, (Integer)null, var9);
               }
            default:
         }
      }
   }
}
