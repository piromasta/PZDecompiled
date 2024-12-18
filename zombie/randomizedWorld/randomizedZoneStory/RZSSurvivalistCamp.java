package zombie.randomizedWorld.randomizedZoneStory;

import zombie.iso.zones.Zone;

public class RZSSurvivalistCamp extends RandomizedZoneStoryBase {
   public RZSSurvivalistCamp() {
      this.name = "Survivalist Campsite";
      this.chance = 2;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public void randomizeZoneStory(Zone var1) {
      // $FF: Couldn't be decompiled
   }
}
