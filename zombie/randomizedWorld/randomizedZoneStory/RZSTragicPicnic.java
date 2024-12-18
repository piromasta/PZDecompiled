package zombie.randomizedWorld.randomizedZoneStory;

import zombie.characters.IsoGameCharacter;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

public class RZSTragicPicnic extends RandomizedZoneStoryBase {
   public RZSTragicPicnic() {
      this.name = "Tragic Picnic";
      this.chance = 2;
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
      if (var4 != null) {
         this.cleanSquareAndNeighbors(var4);
         InventoryContainer var5 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_PicnicBasket");
         if (var5 != null) {
            this.addItemOnGround(var4, var5);
            ItemPickerJava.rollContainerItem(var5, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var5.getType()));
            var5.getItemContainer().AddItem("Base.Ring_Left_RingFinger_GoldDiamond");
         }

         IsoGridSquare var6 = var4.getAdjacentSquare(IsoDirections.getRandom());
         if (var6 != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(var6);
            if (Rand.NextBool(2)) {
               this.addItemOnGround(var6, "Base.GuitarAcoustic");
            } else {
               this.addItemOnGround(var6, "Base.Paperback_Sexy");
               if (Rand.NextBool(2)) {
                  this.addItemOnGround(var6, "Base.Bra_Strapless_AnimalPrint");
               }
            }
         }

         IsoGridSquare var7 = var4.getAdjacentSquare(IsoDirections.getRandom());
         if (var7 != null) {
            if (var6 != var7) {
               RandomizedZoneStoryBase.cleanSquareForStory(var6);
            }

            if (Rand.NextBool(2)) {
               this.addItemOnGround(var7, "Base.Wine");
            } else {
               this.addItemOnGround(var7, "Base.Wine2");
            }

            this.addItemOnGround(var7, "Base.Corkscrew");
            this.addItemOnGround(var7, "Base.GlassWine");
            this.addItemOnGround(var7, "Base.GlassWine");
         }

         IsoGridSquare var8 = var4.getAdjacentSquare(IsoDirections.getRandom());
         if (var8 != null) {
            if (var8 != var6 && var8 != var7) {
               RandomizedZoneStoryBase.cleanSquareForStory(var6);
            }

            this.addItemOnGround(var8, "Base.Chocolate_HeartBox");
         }

         if (Rand.NextBool(2)) {
            return;
         }

         IsoGridSquare var9 = var4.getAdjacentSquare(IsoDirections.getRandom());
         if (var9 != null) {
            if (var9 != var6 && var9 != var7) {
               RandomizedZoneStoryBase.cleanSquareForStory(var6);
            }

            this.addItemOnGround(var9, "Base.Pillow_Heart");
         }
      }

   }
}
