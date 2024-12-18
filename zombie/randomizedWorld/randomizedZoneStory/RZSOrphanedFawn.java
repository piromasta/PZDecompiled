package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public class RZSOrphanedFawn extends RandomizedZoneStoryBase {
   public RZSOrphanedFawn() {
      this.name = "Orphaned Fawn";
      this.chance = 2;
      this.minZoneHeight = 8;
      this.minZoneWidth = 8;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      IsoGridSquare var2 = getRandomExtraFreeUnoccupiedSquare(this, var1);
      if (var2 != null) {
         RandomizedZoneStoryBase.cleanSquareForStory(var2);
         IsoAnimal var3 = new IsoAnimal(IsoWorld.instance.getCell(), var2.getX(), var2.getY(), var2.getZ(), "doe", "whitetailed");
         var3.randomizeAge();
         var3.setHealth(0.0F);
         IsoGridSquare var4 = var2.getAdjacentSquare(IsoDirections.getRandom());
         if (var4 != null && !var4.isFree(true)) {
            var4 = getRandomExtraFreeUnoccupiedSquare(this, var1);
         }

         if (var4 != null) {
            IsoAnimal var5 = new IsoAnimal(IsoWorld.instance.getCell(), var4.getX(), var4.getY(), var4.getZ(), "fawn", "whitetailed");
            var5.randomizeAge();
            var5.addToWorld();
            var5.setDebugStress(100.0F);
         }
      }

      IsoGridSquare var7 = getRandomExtraFreeUnoccupiedSquare(this, var1);
      if (var7 != null) {
         this.addZombiesOnSquare(1, "Hunter", 0, var7);
         ArrayList var6 = new ArrayList();
         var6.add("Base.VarmintRifle");
         var6.add("Base.HuntingRifle");
         var6.add("Base.Shotgun");
         var6.add("Base.DoubleBarrelShotgun");
         this.addItemOnGround(var7, (String)var6.get(Rand.Next(var6.size())));
      }

   }
}
