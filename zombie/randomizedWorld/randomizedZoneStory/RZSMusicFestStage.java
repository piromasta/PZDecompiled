package zombie.randomizedWorld.randomizedZoneStory;

import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

public class RZSMusicFestStage extends RandomizedZoneStoryBase {
   public RZSMusicFestStage() {
      this.name = "Music Festival Stage";
      this.chance = 100;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.MusicFestStage.toString());
      this.alwaysDo = true;
   }

   public void randomizeZoneStory(Zone var1) {
      for(int var2 = 0; var2 < 2; ++var2) {
         if (Rand.NextBool(4)) {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarAcoustic");
         } else {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarElectric");
         }

         if (Rand.NextBool(2)) {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarPick");
         }
      }

      this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarElectricBass");
      if (Rand.NextBool(6)) {
         this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.Keytar");
      }

      this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.Speaker");
      this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.Speaker");
      this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.Drumstick");
      this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.Bag_ProtectiveCaseBulky_Audio");
      if (Rand.NextBool(2)) {
         this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarElectricNeck_Broken");
      }

      if (Rand.NextBool(2)) {
         this.addItemOnGround(this.getRandomFreeSquareFullZone(this, var1), "Base.GuitarElectricBassNeck_Broken");
      }

      this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, var1));
      this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, var1));
      this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, var1));
      this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, var1));
      this.addZombiesOnSquare(1, "Punk", 100, this.getRandomFreeSquareFullZone(this, var1));
   }
}
