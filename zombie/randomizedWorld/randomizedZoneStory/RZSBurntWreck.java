package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.zones.Zone;

public class RZSBurntWreck extends RandomizedZoneStoryBase {
   public RZSBurntWreck() {
      this.name = "Burnt Wreck";
      this.chance = 5;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.SmashedBottle");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      ArrayList var2 = getForestClutter();
      this.cleanAreaForStory(this, var1);
      int var3 = var1.x;
      int var4 = var1.y;
      if (Rand.Next(2) == 0) {
         var3 += var1.getWidth();
      }

      if (Rand.Next(2) == 0) {
         var4 += var1.getHeight();
      }

      this.addVehicle(var1, getSq(var3, var4, var1.z), (IsoChunk)null, "normalburnt", (String)null, (Integer)null, (IsoDirections)null, (String)null);
      if (Rand.Next(2) == 0) {
         int var5 = Rand.Next(2, 5);

         for(int var6 = 0; var6 < var5; ++var6) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var2.get(Rand.Next(var2.size())));
         }
      }

   }
}
