package zombie.randomizedWorld.randomizedZoneStory;

import zombie.iso.zones.Zone;

public class RZSCharcoalBurner extends RandomizedZoneStoryBase {
   public RZSCharcoalBurner() {
      this.name = "Charcoal Burner";
      this.chance = 1;
      this.minZoneHeight = 9;
      this.minZoneWidth = 9;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      // $FF: Couldn't be decompiled
   }
}
