package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.iso.zones.Zone;

public class RZSHunterCamp extends RandomizedZoneStoryBase {
   public RZSHunterCamp() {
      this.name = "Hunter Forest Camp";
      this.chance = 5;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.VarmintRifle");
      var0.add("Base.223Box");
      var0.add("Base.HuntingRifle");
      var0.add("Base.308Box");
      var0.add("Base.Shotgun");
      var0.add("Base.ShotgunShellsBox");
      var0.add("Base.DoubleBarrelShotgun");
      var0.add("Base.AssaultRifle");
      var0.add("Base.556Box");
      var0.add("Base.Lantern_Propane");
      var0.add("Base.Bag_RifleCaseCloth");
      var0.add("Base.Bag_RifleCaseCloth2");
      var0.add("Base.Bag_ShotgunCaseCloth");
      var0.add("Base.RifleCase4");
      var0.add("Base.Bag_AmmoBox_Hunting");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      // $FF: Couldn't be decompiled
   }
}
