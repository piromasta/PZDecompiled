package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public class RZSOccultActivity extends RandomizedZoneStoryBase {
   public RZSOccultActivity() {
      this.name = "Occult Activity";
      this.chance = 1;
      this.minZoneHeight = 6;
      this.minZoneWidth = 6;
      this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
      this.setUnique(true);
   }

   public void randomizeZoneStory(Zone var1) {
      this.cleanAreaForStory(this, var1);
      int var2 = var1.pickedXForZoneStory;
      int var3 = var1.pickedYForZoneStory;
      IsoGridSquare var4 = getSq(var2, var3, var1.z);
      IsoGridSquare var5 = IsoWorld.instance.getCell().getGridSquare(var2, var3 - 1, var1.z);
      if (var4 != null) {
         if (var5 != null) {
            this.dirtBomb(var4);
            this.dirtBomb(var5);
            if (Rand.NextBool(2)) {
               this.addStoneAnvil(var5);
            } else {
               this.addTileObject(var5, "location_community_cemetary_01_30");
            }

            this.addCampfire(var4);
            IsoGridSquare var6 = var4.getAdjacentSquare(IsoDirections.E);
            if (var6 != null) {
               this.addItemOnGround(var6, "Base.Book_Occult");
               this.addItemOnGround(var6, "Base.Paperback_Occult");
               if (Rand.NextBool(2)) {
                  this.addItemOnGround(var6, "Base.TarotCardDeck");
               } else {
                  this.addItemOnGround(var6, "Base.OujiBoard");
               }

               InventoryItem var7 = InventoryItemFactory.CreateItem("Base.Candle");
               if (var7 instanceof DrainableComboItem) {
                  var7.setCurrentUses(var7.getMaxUses() / 2);
                  this.addItemOnGround(var6, var7);
               }
            }

            var6 = var4.getAdjacentSquare(IsoDirections.W);
            if (var6 != null) {
               ArrayList var9 = new ArrayList();
               var9.add("Base.HuntingKnife");
               var9.add("Base.KitchenKnife");
               var9.add("Base.SwitchKnife");
               RandomizedZoneStoryBase.cleanSquareForStory(var6);
               InventoryItem var8 = InventoryItemFactory.CreateItem((String)var9.get(Rand.Next(var9.size())));
               if (var8 instanceof HandWeapon) {
                  ((HandWeapon)var8).setBloodLevel(1.0F);
                  var8.setCondition(Rand.Next(var8.getConditionMax()), false);
                  this.addItemOnGround(var6, var8);
               }
            }

            var6 = var4.getAdjacentSquare(IsoDirections.S);
            if (var6 != null) {
               RandomizedZoneStoryBase.cleanSquareForStory(var6);
               IsoAnimal var10 = new IsoAnimal(IsoWorld.instance.getCell(), var6.x, var6.y, var6.z, "rabdoe", "swamp");
               var10.randomizeAge();
               var10.setHealth(0.0F);
               this.addTrailOfBlood((float)var6.x, (float)var6.y, (float)var6.z, 0.0F, 1);
               this.addBloodSplat(var6, Rand.Next(7, 12));
            }

         }
      }
   }
}
