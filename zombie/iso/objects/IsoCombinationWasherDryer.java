package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.iso.sprite.IsoSprite;

public class IsoCombinationWasherDryer extends IsoObject {
   private final ClothingWasherLogic m_washer = new ClothingWasherLogic(this);
   private final ClothingDryerLogic m_dryer = new ClothingDryerLogic(this);
   private IClothingWasherDryerLogic m_logic;

   public IsoCombinationWasherDryer(IsoCell var1) {
      super(var1);
      this.m_logic = this.m_washer;
   }

   public IsoCombinationWasherDryer(IsoCell var1, IsoGridSquare var2, IsoSprite var3) {
      super(var1, var2, var3);
      this.m_logic = this.m_washer;
   }

   public String getObjectName() {
      return "CombinationWasherDryer";
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.m_logic = (IClothingWasherDryerLogic)(var1.get() == 0 ? this.m_washer : this.m_dryer);
      this.m_washer.load(var1, var2, var3);
      this.m_dryer.load(var1, var2, var3);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.m_logic == this.m_washer ? 0 : 1));
      this.m_washer.save(var1, var2);
      this.m_dryer.save(var1, var2);
   }

   public void update() {
      this.m_logic.update();
   }

   public void addToWorld() {
      IsoCell var1 = this.getCell();
      var1.addToProcessIsoObject(this);
   }

   public void removeFromWorld() {
      super.removeFromWorld();
   }

   public void saveChange(String var1, KahluaTable var2, ByteBuffer var3) {
      if ("mode".equals(var1)) {
         var3.put((byte)(this.isModeWasher() ? 0 : 1));
      } else {
         this.m_logic.saveChange(var1, var2, var3);
      }

   }

   public void loadChange(String var1, ByteBuffer var2) {
      if ("mode".equals(var1)) {
         if (var2.get() == 0) {
            this.setModeWasher();
         } else {
            this.setModeDryer();
         }
      } else {
         this.m_logic.loadChange(var1, var2);
      }

   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_logic.isItemAllowedInContainer(var1, var2);
   }

   public boolean isRemoveItemAllowedFromContainer(ItemContainer var1, InventoryItem var2) {
      return this.m_logic.isRemoveItemAllowedFromContainer(var1, var2);
   }

   public boolean isActivated() {
      return this.m_logic.isActivated();
   }

   public void setActivated(boolean var1) {
      this.m_logic.setActivated(var1);
   }

   public void setModeWasher() {
      if (!this.isModeWasher()) {
         this.m_dryer.switchModeOff();
         this.m_logic = this.m_washer;
         this.getContainer().setType("clothingwasher");
         this.m_washer.switchModeOn();
         LuaEventManager.triggerEvent("OnContainerUpdate");
      }
   }

   public void setModeDryer() {
      if (!this.isModeDryer()) {
         this.m_washer.switchModeOff();
         this.m_logic = this.m_dryer;
         this.getContainer().setType("clothingdryer");
         this.m_dryer.switchModeOn();
         LuaEventManager.triggerEvent("OnContainerUpdate");
      }
   }

   public boolean isModeWasher() {
      return this.m_logic == this.m_washer;
   }

   public boolean isModeDryer() {
      return this.m_logic == this.m_dryer;
   }
}
