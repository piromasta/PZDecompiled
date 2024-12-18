package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public final class RVSAnimalOnRoad extends RandomizedVehicleStoryBase {
   public RVSAnimalOnRoad() {
      this.name = "Animal On Road";
      this.minZoneWidth = 2;
      this.minZoneHeight = 2;
      this.setChance(10);
      this.setMinimumDays(30);
      this.needsFarmland = true;
   }

   public static ArrayList<String> getBreeds() {
      ArrayList var0 = new ArrayList();
      var0.add("rhodeisland");
      var0.add("leghorn");
      var0.add("angus");
      var0.add("simmental");
      var0.add("holstein");
      var0.add("landrace");
      var0.add("largeblack");
      var0.add("suffolk");
      var0.add("rambouillet");
      var0.add("friesian");
      return var0;
   }

   public void randomizeVehicleStory(Zone var1, IsoChunk var2) {
      this.callVehicleStorySpawner(var1, var2, 0.0F);
   }

   public boolean initVehicleStorySpawner(Zone var1, IsoChunk var2, boolean var3) {
      VehicleStorySpawner var4 = VehicleStorySpawner.getInstance();
      var4.clear();
      var4.addElement("animal", 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
      var4.setParameter("zone", var1);
      ArrayList var5 = getBreeds();
      String var6 = (String)var5.get(Rand.Next(var5.size()));
      var4.setParameter("breed", var6);
      return true;
   }

   public void spawnElement(VehicleStorySpawner var1, VehicleStorySpawner.Element var2) {
      IsoGridSquare var3 = var2.square;
      if (var3 != null) {
         float var4 = var2.z;
         switch (var2.id) {
            case "animal":
               LinkedHashMap var7 = new LinkedHashMap();
               LinkedHashMap var8 = new LinkedHashMap();
               var7.put("rhodeisland", "hen");
               var7.put("leghorn", "hen");
               var7.put("angus", "cow");
               var7.put("simmental", "cow");
               var7.put("holstein", "cow");
               var7.put("landrace", "sow");
               var7.put("largeblack", "sow");
               var7.put("suffolk", "ewe");
               var7.put("rambouillet", "ewe");
               var7.put("friesian", "ewe");
               var8.put("rhodeisland", "cockerel");
               var8.put("leghorn", "cockerel");
               var8.put("angus", "bull");
               var8.put("simmental", "bull");
               var8.put("holstein", "bull");
               var8.put("landrace", "boar");
               var8.put("largeblack", "boar");
               var8.put("suffolk", "ram");
               var8.put("rambouillet", "ram");
               var8.put("friesian", "ram");
               if (var3 != null) {
                  String var9 = var1.getParameterString("breed");
                  String var10 = (String)var7.get(var9);
                  if (Rand.NextBool(5)) {
                     var10 = (String)var8.get(var9);
                  }

                  IsoAnimal var11 = new IsoAnimal(IsoWorld.instance.getCell(), (int)var2.position.x, (int)var2.position.y, (int)var4, var10, var9);
                  var11.addToWorld();
                  var11.randomizeAge();
               }
            default:
         }
      }
   }
}
