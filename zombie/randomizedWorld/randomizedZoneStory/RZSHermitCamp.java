package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSHermitCamp extends RandomizedZoneStoryBase {
   public RZSHermitCamp() {
      this.name = "Hermit Campsite";
      this.chance = 2;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public static ArrayList<String> getForestClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.TinCanEmpty");
      var0.add("Base.TreeBranch2");
      var0.add("Base.Log");
      var0.add("Base.Twigs");
      var0.add("Base.DoubleBarrelShotgun");
      var0.add("Base.WhiskeyEmpty");
      var0.add("Base.Hat_Raccoon");
      var0.add("Base.DeadRabbit");
      var0.add("Base.DeadSquirrel");
      var0.add("Base.KnifePocket");
      var0.add("Base.BucketWaterFull");
      var0.add("Base.CraftedFishingRodTwineLine");
      var0.add("Base.Lantern_Hurricane");
      var0.add("Base.MetalCup");
      if (Rand.Next(2) == 0) {
         var0.add("Base.Violin");
      } else {
         var0.add("Base.Banjo");
      }

      return var0;
   }

   public static ArrayList<String> getBagClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.Matches");
      var0.add("Base.Matchbox");
      var0.add("Base.TinnedBeans");
      var0.add("Base.Dogfood");
      var0.add("Base.Acorn");
      var0.add("Base.Dandelions");
      var0.add("Base.Nettles");
      var0.add("Base.Thistle");
      var0.add("Base.Rosehips");
      var0.add("Base.TinOpener");
      var0.add("Base.Twigs");
      var0.add("Base.Whiskey");
      var0.add("Base.DeadRabbit");
      var0.add("Base.DeadSquirrel");
      var0.add("Base.KnifePocket");
      var0.add("Base.CigaretteRolled");
      var0.add("Base.CigaretteRollingPapers");
      var0.add("Base.TobaccoLoose");
      var0.add("Base.HerbalistMag");
      var0.add("Base.Twine");
      return var0;
   }

   public static ArrayList<String> getFireClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.WaterPot");
      var0.add("Base.PotForged");
      var0.add("Base.PotOfStew");
      var0.add("Base.OpenBeans");
      var0.add("Base.DogfoodOpen");
      var0.add("Base.PanForged");
      var0.add("Base.PotForged");
      var0.add("Base.PanForged");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      ArrayList var5 = getForestClutter();
      ArrayList var6 = getBagClutter();
      ArrayList var7 = getFireClutter();
      this.cleanAreaForStory(this, var1);
      this.cleanSquareAndNeighbors(var4);
      this.addSimpleFire(var4);
      this.addItemOnGround(var4, (String)var7.get(Rand.Next(var7.size())));
      if (Rand.Next(10) == 0) {
         this.addItemOnGround(var4, "Base.FireplacePoker");
      }

      int var8 = Rand.Next(0, 1);
      int var9 = Rand.Next(0, 1);
      if (Rand.NextBool(2)) {
         this.addRandomShelterWestEast(var2 + var8 - 2, var3 + var9, var1.z);
      } else {
         this.addRandomShelterNorthSouth(var2 + var8, var3 + var9 - 2, var1.z);
      }

      int var10 = Rand.Next(2, 5);

      for(int var11 = 0; var11 < var10; ++var11) {
         this.addItemOnGround(this.getRandomFreeSquare(this, var1), (String)var5.get(Rand.Next(var5.size())));
      }

      this.addZombiesOnSquare(1, "Hobbo", (Integer)null, this.getRandomFreeSquare(this, var1));
      InventoryContainer var13 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.EmptySandbag");
      var10 = Rand.Next(2, 5);

      for(int var12 = 0; var12 < var10; ++var12) {
         var13.getItemContainer().AddItem((String)var6.get(Rand.Next(var6.size())));
      }

      this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), var13);
   }
}
