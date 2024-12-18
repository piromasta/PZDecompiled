package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

public class RZSMurderScene extends RandomizedZoneStoryBase {
   public RZSMurderScene() {
      this.name = "Murder Scene";
      this.chance = 1;
      this.minZoneHeight = 4;
      this.minZoneWidth = 4;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public static ArrayList<String> getMurderClutter() {
      ArrayList var0 = new ArrayList();
      var0.add("Base.PetrolCan");
      var0.add("Base.Hammer");
      var0.add("Base.Tarp");
      var0.add("Base.Rope");
      var0.add("Base.DuctTape");
      var0.add("Base.Saw");
      var0.add("Base.Garbagebag");
      var0.add("Base.Scissors");
      var0.add("Base.BlowTorch");
      var0.add("Base.BallPeenHammer");
      var0.add("Base.ClubHammer");
      var0.add("Base.Gloves_LeatherGlovesBlack");
      var0.add("Base.Hat_BalaclavaFull");
      var0.add("Base.Screwdriver");
      var0.add("Base.Zipties");
      var0.add("Base.EmptySandbag");
      var0.add("Base.Shovel");
      var0.add("Base.SmashedBottle");
      var0.add("Base.MetalBar");
      var0.add("Base.StraightRazor");
      var0.add("Base.TireIron");
      return var0;
   }

   public void randomizeZoneStory(Zone var1) {
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      ArrayList var4 = getMurderClutter();
      this.cleanAreaForStory(this, var1);
      IsoDeadBody var5 = null;
      var5 = createRandomDeadBody(getSq(var2, var3, var1.z), (IsoDirections)null, true, Rand.Next(100), 10, (String)null, 0);
      int var6;
      if (var5 != null) {
         this.addBloodSplat(var5.getSquare(), 20);

         for(var6 = 0; var6 < var5.getWornItems().size(); ++var6) {
            if (var5.getWornItems().get(var6).getItem() instanceof Clothing) {
               ((Clothing)var5.getWornItems().get(var6).getItem()).randomizeCondition(0, 25, 50, 50);
            }
         }
      }

      int var7;
      if (Rand.Next(2) == 0) {
         var6 = var1.x;
         var7 = var1.y;
         if (Rand.Next(2) == 0) {
            var6 += var1.getWidth();
         }

         if (Rand.Next(2) == 0) {
            var7 += var1.getHeight();
         }

         this.addVehicle(var1, getSq(var6, var7, var1.z), (IsoChunk)null, "normalburnt", (String)null, (Integer)null, (IsoDirections)null, (String)null);
         if (Rand.Next(2) == 0) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, var1), "Base.EmptyPetrolCan");
         }
      }

      if (Rand.Next(2) == 0) {
         IsoGridSquare var12 = this.getRandomExtraFreeSquare(this, var1);
         if (var12 != null) {
            var7 = var12.getX();
            int var8 = var12.getY();
            this.addTileObject(var7, var8, var1.z, "location_community_cemetary_01_32");
            this.addTileObject(var7 + 1, var8, var1.z, "location_community_cemetary_01_33");
            this.addItemOnGround(getSq(var12.x, var12.y + 2, var12.z), "Base.Shovel");
         }
      }

      var6 = Rand.Next(2, 5);

      for(var7 = 0; var7 < var6; ++var7) {
         IsoGridSquare var13 = this.getRandomExtraFreeSquare(this, var1);
         super.addBloodSplat(var13, Rand.Next(15, 20));
         InventoryItem var9 = InventoryItemFactory.CreateItem((String)var4.get(Rand.Next(var4.size())));
         if (var9 instanceof HandWeapon) {
            ((HandWeapon)var9).setBloodLevel(1.0F);
            var9.setCondition(Rand.Next(var9.getConditionMax()), false);
         }

         if (var9 instanceof Clothing) {
            ((Clothing)var9).randomizeCondition(25, 50, 100, 0);
            ((Clothing)var9).randomizeCondition(0, 0, 100, 0);
         }

         if (var9 instanceof DrainableComboItem) {
            float var10 = (float)var9.getMaxUses();
            float var11 = var10 - 1.0F;
            var9.setCurrentUses(Rand.Next(1, (int)var11));
         }

         if (var9 != null) {
            this.addItemOnGround(var13, var9);
         }
      }

      this.addZombiesOnSquare(Rand.Next(3, 4), "MobCasual", 0, this.getRandomExtraFreeSquare(this, var1));
   }
}
