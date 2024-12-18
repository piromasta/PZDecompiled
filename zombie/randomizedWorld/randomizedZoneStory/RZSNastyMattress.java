package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

public class RZSNastyMattress extends RandomizedZoneStoryBase {
   public RZSNastyMattress() {
      this.name = "Nasty Mattress";
      this.chance = 3;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.SmashedBottle");
      var0.add("Base.LighterDisposable");
      var0.add("Base.CigaretteRollingPapers");
      var0.add("Base.HottieZ");
      var0.add("Base.RippedSheetsDirty");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getForestClutter();
      this.cleanAreaForStory(this, var1);
      int var5;
      int var6;
      if (Rand.Next(2) == 0) {
         var5 = Rand.Next(-1, 2);
         var6 = Rand.Next(-1, 2);
         this.addMattressWestEast(var2 + var5 - 3, var3 + var6, var1.z);
      } else {
         var5 = Rand.Next(-1, 2);
         var6 = Rand.Next(-1, 2);
         this.addMattressNorthSouth(var2 + var5, var3 + var6 - 3, var1.z);
      }

      var5 = Rand.Next(3, 7);

      for(var6 = 0; var6 < var5; ++var6) {
         this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), (String)var4.get(Rand.Next(var4.size())));
      }

   }
}
