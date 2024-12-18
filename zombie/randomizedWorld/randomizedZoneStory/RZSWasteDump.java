package zombie.randomizedWorld.randomizedZoneStory;

import zombie.iso.zones.Zone;

public class RZSWasteDump extends RandomizedZoneStoryBase {
   public RZSWasteDump() {
      this.name = "Waste Dump";
      this.chance = 1;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      // $FF: Couldn't be decompiled
   }
}
