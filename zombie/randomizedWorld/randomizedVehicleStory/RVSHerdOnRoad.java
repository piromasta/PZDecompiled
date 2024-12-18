package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public final class RVSHerdOnRoad extends RandomizedVehicleStoryBase {
   public RVSHerdOnRoad() {
      this.name = "Herd On Road";
      this.minZoneWidth = 4;
      this.minZoneHeight = 4;
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
      LinkedHashMap var3 = new LinkedHashMap();
      LinkedHashMap var4 = new LinkedHashMap();
      var3.put("rhodeisland", "hen");
      var3.put("leghorn", "hen");
      var3.put("angus", "cow");
      var3.put("simmental", "cow");
      var3.put("holstein", "cow");
      var3.put("landrace", "sow");
      var3.put("largeblack", "sow");
      var3.put("suffolk", "ewe");
      var3.put("rambouillet", "ewe");
      var3.put("friesian", "ewe");
      var4.put("rhodeisland", "cockerel");
      var4.put("leghorn", "cockerel");
      var4.put("angus", "bull");
      var4.put("simmental", "bull");
      var4.put("holstein", "bull");
      var4.put("landrace", "boar");
      var4.put("largeblack", "boar");
      var4.put("suffolk", "ram");
      var4.put("rambouillet", "ram");
      var4.put("friesian", "ram");
      ArrayList var5 = getBreeds();
      String var6 = (String)var5.get(Rand.Next(var5.size()));
      String var7 = (String)var4.get(var6);
      IsoGridSquare var8 = this.getCenterOfChunk(var1, var2);
      IsoGridSquare var9 = getRandomFreeUnoccupiedSquare(this, var1, var8);
      IsoAnimal var10 = null;
      if (var9 != null) {
         var10 = new IsoAnimal(IsoWorld.instance.getCell(), var9.getX(), var9.getY(), var9.getZ(), var7, var6);
         var10.addToWorld();
         var10.randomizeAge();
         if (Rand.NextBool(2)) {
            var10.setStateEventDelayTimer(0.0F);
         }
      }

      var7 = (String)var3.get(var6);
      int var11 = Rand.Next(1, 6);

      for(int var12 = 0; var12 < var11; ++var12) {
         var9 = getRandomFreeUnoccupiedSquare(this, var1, var8);
         if (var9 != null) {
            var10 = new IsoAnimal(IsoWorld.instance.getCell(), var9.getX(), var9.getY(), var9.getZ(), var7, var6);
            var10.addToWorld();
            var10.randomizeAge();
            if (Rand.NextBool(2)) {
               var10.setStateEventDelayTimer(0.0F);
            }
         }
      }

   }
}
