package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSDuke extends RandomizedZoneStoryBase {
   public RZSDuke() {
      this.name = "Duke";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Duke.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = this.getRandomExtraFreeSquare(this, var1);
      ArrayList var3 = this.addZombiesOnSquare(1, "Duke", 0, var2);
      IsoZombie var4 = (IsoZombie)var3.get(0);
      var4.getHumanVisual().setSkinTextureIndex(4);
      SurvivorDesc var5 = var4.getDescriptor();
      if (var5 != null) {
         var5.setForename("Duke");
         var5.setSurname("Redding");
      }
   }
}
