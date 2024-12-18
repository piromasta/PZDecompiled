package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public class ResourceEnergy extends Resource {
   private float storedEnergy = 0.0F;
   private Energy energy = null;
   private float capacity = 0.0F;

   protected ResourceEnergy() {
   }

   void loadBlueprint(ResourceBlueprint var1) {
      super.loadBlueprint(var1);
      this.capacity = var1.getCapacity();
      this.setEnergyType(this.getFilterName());
   }

   private void setEnergyType(String var1) {
      if (var1 != null) {
         this.energy = Energy.Get(var1);
         if (this.energy == null) {
            DebugLog.General.warn("Energy not found: " + var1);
         }
      } else {
         DebugLog.General.warn("Energy Type is null!");
         this.energy = Energy.VoidEnergy;
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         super.DoTooltip(var1, var2);
         ObjectTooltip.LayoutItem var3 = var2.addItem();
         var3.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue((int)(this.getEnergyRatio() * 100.0F) + " %", 1.0F, 1.0F, 1.0F, 1.0F);
         if (this.energy != null) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getEntityText("EC_Energy") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setValue(this.energy.getDisplayName(), 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            var3 = var2.addItem();
            var3.setLabel(Translator.getEntityText("EC_Energy_Not_Set"), 1.0F, 1.0F, 0.8F, 1.0F);
         }

         if (Core.bDebug && DebugOptions.instance.EntityDebugUI.getValue()) {
            this.DoDebugTooltip(var1, var2);
         }

      }
   }

   protected void DoDebugTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null && Core.bDebug) {
         super.DoDebugTooltip(var1, var2);
         float var3 = 0.7F;
         ObjectTooltip.LayoutItem var4 = var2.addItem();
         var4.setLabel("EnergyStored:", 1.0F * var3, 1.0F * var3, 0.8F * var3, 1.0F);
         var4.setValue("" + this.storedEnergy, 1.0F * var3, 1.0F * var3, 1.0F * var3, 1.0F);
         var4 = var2.addItem();
         var4.setLabel("EnergyCapacity:", 1.0F * var3, 1.0F * var3, 0.8F * var3, 1.0F);
         var4.setValue("" + this.capacity, 1.0F * var3, 1.0F * var3, 1.0F * var3, 1.0F);
      }
   }

   public boolean isFull() {
      return this.storedEnergy >= this.capacity;
   }

   public boolean isEmpty() {
      return this.storedEnergy <= 0.0F;
   }

   public Energy getEnergy() {
      return this.energy;
   }

   public float getEnergyAmount() {
      return this.storedEnergy;
   }

   public float getEnergyCapacity() {
      return this.capacity;
   }

   public float getFreeEnergyCapacity() {
      return this.getEnergyCapacity() - this.getEnergyAmount();
   }

   public float getEnergyRatio() {
      return this.capacity <= 0.0F ? 0.0F : PZMath.clamp_01(this.storedEnergy / this.capacity);
   }

   public boolean setEnergyAmount(float var1) {
      if (GameClient.bClient) {
         return false;
      } else {
         var1 = PZMath.min(PZMath.max(0.0F, var1), this.capacity);
         if (this.storedEnergy != var1) {
            this.storedEnergy = var1;
            this.setDirty();
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean canDrainToItem(InventoryItem var1) {
      if (!GameClient.bClient && !this.isEmpty()) {
         if (var1 != null && var1 instanceof DrainableComboItem) {
            DrainableComboItem var2 = (DrainableComboItem)var1;
            if (!var2.isFullUses() && var2.isEnergy()) {
               return this.getEnergy().equals(var2.getEnergy());
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean drainToItem(InventoryItem var1) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return false;
      } else if (this.canDrainToItem(var1)) {
         DrainableComboItem var2 = (DrainableComboItem)var1;
         float var3 = PZMath.min(1.0F - var2.getCurrentUsesFloat(), this.getEnergyAmount());
         this.setEnergyAmount(this.getEnergyAmount() - var3);
         int var4 = (int)((float)var2.getMaxUses() * var3);
         var2.setCurrentUses(var2.getCurrentUses() + var4);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean canDrainFromItem(InventoryItem var1) {
      if (!GameClient.bClient && !this.isFull()) {
         if (var1 != null && var1 instanceof DrainableComboItem) {
            DrainableComboItem var2 = (DrainableComboItem)var1;
            if (!var2.isEmptyUses() && var2.isEnergy()) {
               return this.getEnergy().equals(var2.getEnergy());
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean drainFromItem(InventoryItem var1) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return false;
      } else if (this.canDrainFromItem(var1)) {
         DrainableComboItem var2 = (DrainableComboItem)var1;
         float var3 = PZMath.min(this.getFreeEnergyCapacity(), var2.getCurrentUsesFloat());
         int var4 = (int)((float)var2.getMaxUses() * var3);
         var2.setCurrentUses(var2.getCurrentUses() - var4);
         this.setEnergyAmount(this.getEnergyAmount() + var3);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public void tryTransferTo(Resource var1) {
      this.tryTransferTo(var1, this.getEnergyAmount());
   }

   public void tryTransferTo(Resource var1, float var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         if (var1 instanceof ResourceEnergy) {
            this.transferTo((ResourceEnergy)var1, var2);
         }

      }
   }

   public void transferTo(ResourceEnergy var1, float var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         float var3 = PZMath.min(PZMath.min(var2, this.getEnergyAmount()), var1.getFreeEnergyCapacity());
         if (!(var3 <= 0.0F)) {
            this.setEnergyAmount(this.getEnergyAmount() - var3);
            var1.setEnergyAmount(var1.getEnergyAmount() + var3);
            this.setDirty();
         }
      }
   }

   public void clear() {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
      } else {
         this.storedEnergy = 0.0F;
         this.setDirty();
      }
   }

   protected void reset() {
      super.reset();
      this.storedEnergy = 0.0F;
      this.capacity = 0.0F;
      this.energy = null;
   }

   public void saveSync(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   public void loadSync(ByteBuffer var1, int var2) throws IOException {
      this.load(var1, var2);
   }

   public void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      Energy.saveEnergy(this.energy, var1);
      var1.putFloat(this.capacity);
      var1.putFloat(this.storedEnergy);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.storedEnergy = 0.0F;
      this.energy = Energy.loadEnergy(var1, var2);
      this.capacity = var1.getFloat();
      this.storedEnergy = var1.getFloat();
   }
}
