package zombie.randomizedWorld.randomizedZoneStory;

import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSWaterPump extends RandomizedZoneStoryBase {
   public RZSWaterPump() {
      this.name = "Water Pump";
      this.chance = 5;
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
         this.dirtBomb(var4);
         this.addTileObject(var4, "camping_01_6" + (4 + Rand.Next(4)));
         if (Rand.NextBool(2)) {
            this.addItemOnGround(var4, "Base.MetalCup");
         } else if (Rand.NextBool(2)) {
            this.addItemOnGround(var4, "Base.Bucket");
         } else if (Rand.NextBool(2)) {
            this.addItemOnGround(var4, "Base.BucketWood");
         }

      }
   }
}
