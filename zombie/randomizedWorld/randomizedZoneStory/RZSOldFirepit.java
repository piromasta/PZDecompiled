package zombie.randomizedWorld.randomizedZoneStory;

import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSOldFirepit extends RandomizedZoneStoryBase {
   public RZSOldFirepit() {
      this.name = "Old Firepit";
      this.chance = 10;
      this.minZoneHeight = 5;
      this.minZoneWidth = 5;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      if (var4 != null) {
         this.cleanSquareAndNeighbors(var4);
         this.addRandomFirepit(var4);
         IsoGridSquare var5 = var4.getRandomAdjacent();
         if (var5 != null && Rand.NextBool(2)) {
            this.addItemOnGround(var5, "Base.TinCanEmpty");
         }

         if (!Rand.NextBool(2)) {
            var5 = var4.getRandomAdjacent();
            if (var5 != null) {
               int var6 = Rand.Next(2);
               switch (var6) {
                  case 0:
                     this.addItemOnGround(var5, "Base.PanForged");
                     break;
                  case 1:
                     this.addItemOnGround(var5, "Base.PotForged");
               }

            }
         }
      }
   }
}
