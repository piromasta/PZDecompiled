package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.iso.zones.Zone;

public class RZSSadCamp extends RandomizedZoneStoryBase {
   public RZSSadCamp() {
      this.name = "Sad Campsite";
      this.chance = 5;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getOutfits() {
      ArrayList var0 = new ArrayList();
      var0.add("Evacuee");
      var0.add("Retiree");
      var0.add("Student");
      var0.add("Generic01");
      var0.add("Generic02");
      var0.add("Generic03");
      var0.add("Generic04");
      var0.add("Generic05");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      // $FF: Couldn't be decompiled
   }
}
