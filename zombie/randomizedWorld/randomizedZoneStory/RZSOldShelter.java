package zombie.randomizedWorld.randomizedZoneStory;

import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSOldShelter extends RandomizedZoneStoryBase {
   public RZSOldShelter() {
      this.name = "OldShelter";
      this.chance = 5;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      this.cleanAreaForStory(this, var1);
      boolean var5 = Rand.NextBool(2);
      if (var5) {
         this.cleanSquareAndNeighbors(var4);
         this.addSimpleFire(var4);
      }

      int var6 = Rand.Next(0, 1);
      int var7 = Rand.Next(0, 1);
      if (Rand.NextBool(2)) {
         this.addRandomShelterWestEast(var2 + var6 - 2, var3 + var7, var1.z);
      } else {
         this.addRandomShelterNorthSouth(var2 + var6, var3 + var7 - 2, var1.z);
      }

      if (!Rand.NextBool(4)) {
         if (Rand.NextBool(2)) {
            this.addItemOnGround(var4, "Base.TinCanEmpty");
         }

         if (var5 && Rand.NextBool(2)) {
            int var8 = Rand.Next(2);
            switch (var8) {
               case 0:
                  this.addItemOnGround(var4, "Base.PanForged");
                  break;
               case 1:
                  this.addItemOnGround(var4, "Base.PotForged");
            }
         }

         if (!Rand.NextBool(3)) {
            this.addItemOnGround(this.getRandomFreeSquare(this, var1), getOldShelterClutterItem());
         }
      }
   }
}
