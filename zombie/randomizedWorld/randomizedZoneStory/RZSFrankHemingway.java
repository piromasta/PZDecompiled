package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSFrankHemingway extends RandomizedZoneStoryBase {
   public RZSFrankHemingway() {
      this.name = "Frank Hemingway";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.FrankHemingway.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = this.getRandomExtraFreeSquare(this, var1);
      this.addZombiesOnSquare(Rand.Next(15, 20), (String)null, (Integer)null, var2);
      ArrayList var3 = this.addZombiesOnSquare(1, "FrankHemingway", 0, var2);
      IsoZombie var4 = (IsoZombie)var3.get(0);
      var4.getHumanVisual().setSkinTextureIndex(1);
      SurvivorDesc var5 = var4.getDescriptor();
      if (var5 != null) {
         var5.setForename("Frank");
         var5.setSurname("Hemingway");
         var4.addRandomVisualDamages();
      }
   }
}
