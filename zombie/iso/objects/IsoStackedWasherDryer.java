package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public class IsoStackedWasherDryer extends IsoObject {
   private final ClothingWasherLogic m_washer = new ClothingWasherLogic(this);
   private final ClothingDryerLogic m_dryer = new ClothingDryerLogic(this);

   public IsoStackedWasherDryer(IsoCell var1) {
      super(var1);
   }

   public IsoStackedWasherDryer(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      super(var1, var2, var3);
   }

   public String getObjectName() {
      return "StackedWasherDryer";
   }

   public void createContainersFromSpriteProperties() {
      super.createContainersFromSpriteProperties();
      PropertyContainer var1 = this.getProperties();
      if (var1 != null) {
         ItemContainer var2;
         if (this.getContainerByType("clothingwasher") == null) {
            var2 = new ItemContainer("clothingwasher", this.getSquare(), this);
            if (var1.Is("ContainerCapacity")) {
               var2.Capacity = PZMath.tryParseInt(var1.Val("ContainerCapacity"), 20);
            }

            if (this.getContainer() == null) {
               this.setContainer(var2);
            } else {
               this.addSecondaryContainer(var2);
            }
         }

         if (this.getContainerByType("clothingdryer") == null) {
            var2 = new ItemContainer("clothingdryer", this.getSquare(), this);
            if (var1.Is("ContainerCapacity")) {
               var2.Capacity = PZMath.tryParseInt(var1.Val("ContainerCapacity"), 20);
            }

            if (this.getContainer() == null) {
               this.setContainer(var2);
            } else {
               this.addSecondaryContainer(var2);
            }
         }

      }
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.m_washer.load(var1, var2, var3);
      this.m_dryer.load(var1, var2, var3);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      this.m_washer.save(var1, var2);
      this.m_dryer.save(var1, var2);
   }

   public void update() {
      this.m_washer.update();
      this.m_dryer.update();
   }

   public void addToWorld() {
      IsoCell var1 = this.getCell();
      var1.addToProcessIsoObject(this);
   }

   public void removeFromWorld() {
      super.removeFromWorld();
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      this.m_washer.saveChange(var1, var2, var3);
      this.m_dryer.saveChange(var1, var2, var3);
   }

   public void loadChange(String var1, ByteBuffer var2) {
      this.m_washer.loadChange(var1, var2);
      this.m_dryer.loadChange(var1, var2);
   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_washer.isItemAllowedInContainer(var1, var2) || this.m_dryer.isItemAllowedInContainer(var1, var2);
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_washer.isRemoveItemAllowedFromContainer(var1, var2) || this.m_dryer.isRemoveItemAllowedFromContainer(var1, var2);
   }

   public boolean isWasherActivated() {
      return this.m_washer.isActivated();
   }

   public void setWasherActivated(boolean var1) {
      this.m_washer.setActivated(var1);
   }

   public boolean isDryerActivated() {
      return this.m_dryer.isActivated();
   }

   public void setDryerActivated(boolean var1) {
      this.m_dryer.setActivated(var1);
   }
}
