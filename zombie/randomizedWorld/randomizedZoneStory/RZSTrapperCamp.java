package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSTrapperCamp extends RandomizedZoneStoryBase {
   public RZSTrapperCamp() {
      this.name = "Trappers Forest Camp";
      this.chance = 7;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getTrapList() {
      ArrayList var0 = new ArrayList();
      var0.add("constructedobjects_01_3");
      var0.add("constructedobjects_01_4");
      var0.add("constructedobjects_01_7");
      var0.add("constructedobjects_01_8");
      var0.add("constructedobjects_01_11");
      var0.add("constructedobjects_01_13");
      var0.add("constructedobjects_01_16");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getTrapList();
      IsoGridSquare var5 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      this.cleanSquareAndNeighbors(var5);
      this.addSimpleFire(var5);
      int var6 = Rand.Next(-1, 2);
      int var7 = Rand.Next(-1, 2);
      this.addRandomTentWestEast(var2 + var6 - 2, var3 + var7, var1.z);
      if (Rand.Next(100) < 70) {
         this.addRandomTentNorthSouth(var2 + var6, var3 + var7 - 2, var1.z);
      }

      int var8 = Rand.Next(2, 5);

      for(int var9 = 0; var9 < var8; ++var9) {
         IsoGridSquare var10 = this.getRandomExtraFreeSquare(this, var1);
         this.addTileObject(var10, (String)var4.get(Rand.Next(var4.size())));
      }

      this.addZombiesOnSquare(Rand.Next(2, 5), "Hunter", 0, this.getRandomExtraFreeSquare(this, var1));
   }
}
