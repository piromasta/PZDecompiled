package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public final class RVSCarCrashDeer extends RandomizedVehicleStoryBase {
   public RVSCarCrashDeer() {
      this.name = "Car Crash Deer";
      this.minZoneWidth = 5;
      this.minZoneHeight = 11;
      this.setChance(10);
      this.needsRuralVegetation = true;
      this.notTown = true;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      float var3 = 0.5235988F;
      this.callVehicleStorySpawner(var1, var2, Rand.Next(-var3, var3));
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      Vector2 var5 = IsoDirections.N.ToVector();
      float var6 = 2.5F;
      var4.addElement("vehicle1", 0.0F, var6, var5.getDirection(), 2.0F, 5.0F);
      var4.addElement("corpse", 0.0F, var6 - (float)(var3 ? 7 : Rand.Next(4, 7)), var5.getDirection() + 3.1415927F, 1.0F, 2.0F);
      var4.setParameter("zone", var1);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         Zone var5 = (Zone)var1.getParameter("zone", Zone.class);
         BaseVehicle var6 = (BaseVehicle)var1.getParameter("vehicle1", BaseVehicle.class);
         switch (var2.id) {
            case "corpse":
               if (var6 != null) {
                  String var9 = "doe";
                  if (Rand.Next(2) == 0) {
                     var9 = "buck";
                  }

                  IsoAnimal var10 = new IsoAnimal(IsoWorld.instance.getCell(), (int)var2.position.x, (int)var2.position.y, 0, var9, "whitetailed");
                  var10.randomizeAge();
                  var10.setHealth(0.0F);
                  this.addTrailOfBlood(var2.position.x, var2.position.y, var2.z, var2.direction, 15);
               }
               break;
            case "vehicle1":
               var6 = this.addVehicle(var5, var2.position.x, var2.position.y, var4, var2.direction, "bad", (String)null, (Integer)null, (String)null);
               if (var6 != null) {
                  var6.setAlarmed(false);
                  var6 = var6.setSmashed("Front");
                  var6.setBloodIntensity("Front", 1.0F);
                  var1.setParameter("vehicle1", var6);
               }
         }

      }
   }
}
