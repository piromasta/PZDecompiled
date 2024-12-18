package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public class RZSAttachedAnimal extends RandomizedZoneStoryBase {
   public RZSAttachedAnimal() {
      this.name = "Attached Animal";
      this.chance = 5;
      this.minZoneHeight = 8;
      this.minZoneWidth = 8;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.minimumDays = 30;
   }

   public static ArrayList<String> getBreeds() {
      ArrayList var0 = new ArrayList();
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
      var2.put("angus", "cow");
      var2.put("simmental", "cow");
      var2.put("holstein", "cow");
      var2.put("landrace", "sow");
      var2.put("largeblack", "sow");
      var2.put("suffolk", "ewe");
      var2.put("rambouillet", "ewe");
      var2.put("friesian", "ewe");
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
      IsoAnimal var8 = new IsoAnimal(IsoWorld.instance.getCell(), var7.getX(), var7.getY(), var7.getZ(), var6, var5);
      var8.addToWorld();
      var8.randomizeAge();
      IsoGridSquare var9 = getRandomExtraFreeUnoccupiedSquare(this, var1);
      IsoObject var10 = this.addTileObject(var9.getX(), var9.getY(), var9.getZ(), "location_community_cemetary_01_31");
      if (var10 != null) {
         var8.getData().setAttachedTree(var10);
      }

   }
}
