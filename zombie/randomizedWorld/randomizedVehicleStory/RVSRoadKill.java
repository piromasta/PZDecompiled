package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

public final class RVSRoadKill extends RandomizedVehicleStoryBase {
   public RVSRoadKill() {
      this.name = "Roadkill - Large Farm Animal Struck By Vehicle";
      this.minZoneWidth = 5;
      this.minZoneHeight = 11;
      this.setChance(10);
      this.setMinimumDays(30);
      this.needsFarmland = true;
   }

   public static ArrayList<String> getBreeds() {
      ArrayList var0 = new ArrayList();
      var0.add("angus");
      var0.add("simmental");
      var0.add("holstein");
      var0.add("landrace");
      var0.add("largeblack");
      return var0;
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
      ArrayList var7 = getBreeds();
      String var8 = (String)var7.get(Rand.Next(var7.size()));
      var4.setParameter("breed", var8);
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
                  LinkedHashMap var9 = new LinkedHashMap();
                  LinkedHashMap var10 = new LinkedHashMap();
                  var9.put("angus", "cow");
                  var9.put("simmental", "cow");
                  var9.put("holstein", "cow");
                  var9.put("landrace", "sow");
                  var9.put("largeblack", "sow");
                  var9.put("suffolk", "ewe");
                  var9.put("rambouillet", "ewe");
                  var9.put("friesian", "ewe");
                  var10.put("angus", "bull");
                  var10.put("simmental", "bull");
                  var10.put("holstein", "bull");
                  var10.put("landrace", "boar");
                  var10.put("largeblack", "boar");
                  var10.put("suffolk", "ram");
                  var10.put("rambouillet", "ram");
                  var10.put("friesian", "ram");
                  String var11 = var1.getParameterString("breed");
                  String var12 = (String)var9.get(var11);
                  if (Rand.Next(5) == 0) {
                     var12 = (String)var10.get(var11);
                  }

                  IsoAnimal var13 = new IsoAnimal(IsoWorld.instance.getCell(), (int)var2.position.x, (int)var2.position.y, 0, var12, var11);
                  var13.randomizeAge();
                  var13.setHealth(0.0F);
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
