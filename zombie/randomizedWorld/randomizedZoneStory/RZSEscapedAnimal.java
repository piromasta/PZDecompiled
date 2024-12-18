package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public class RZSEscapedAnimal extends RandomizedZoneStoryBase {
   public RZSEscapedAnimal() {
      this.name = "Escaped Animal";
      this.chance = 10;
      this.minZoneHeight = 2;
      this.minZoneWidth = 2;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.minimumDays = 30;
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

   public void randomizeZoneStory(Zone var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      LinkedHashMap var3 = new LinkedHashMap();
      var2.put("rhodeisland", "hen");
      var2.put("leghorn", "hen");
      var2.put("angus", "cow");
      var2.put("simmental", "cow");
      var2.put("holstein", "cow");
      var2.put("landrace", "sow");
      var2.put("largeblack", "sow");
      var2.put("suffolk", "ewe");
      var2.put("rambouillet", "ewe");
      var2.put("friesian", "ewe");
      var3.put("rhodeisland", "cockerel");
      var3.put("leghorn", "cockerel");
      var3.put("angus", "bull");
      var3.put("simmental", "bull");
      var3.put("holstein", "bull");
      var3.put("landrace", "boar");
      var3.put("largeblack", "boar");
      var3.put("suffolk", "ram");
      var3.put("rambouillet", "ram");
      var3.put("friesian", "ram");
      ArrayList var4 = getBreeds();
      String var5 = (String)var4.get(Rand.Next(var4.size()));
      String var6 = (String)var2.get(var5);
      if (Rand.Next(5) == 0) {
         var6 = (String)var3.get(var5);
      }

      IsoGridSquare var7 = getRandomExtraFreeUnoccupiedSquare(this, var1);
      if (var7 != null) {
         IsoAnimal var8 = new IsoAnimal(IsoWorld.instance.getCell(), var7.getX(), var7.getY(), var7.getZ(), var6, var5);
         var8.addToWorld();
         var8.randomizeAge();
         if (Rand.NextBool(3)) {
            var8.setStateEventDelayTimer(0.0F);
         }

      }
   }
}
