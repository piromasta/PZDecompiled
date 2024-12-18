package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public class ResourceFluid extends Resource {
   private final FluidContainer fluidContainer = FluidContainer.CreateContainer();
   private final FluidFilter fluidFilter = new FluidFilter();

   protected ResourceFluid() {
   }

   void loadBlueprint(ResourceBlueprint var1) {
      super.loadBlueprint(var1);
      this.fluidContainer.setCapacity(var1.getCapacity());
      if (this.getFilterName() != null) {
         this.fluidFilter.setFilterScript(this.getFilterName());
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         super.DoTooltip(var1, var2);
         ObjectTooltip.LayoutItem var3 = var2.addItem();
         var3.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue((int)(this.getFluidRatio() * 100.0F) + " %", 1.0F, 1.0F, 1.0F, 1.0F);
         if (this.fluidContainer != null) {
            this.fluidContainer.DoTooltip(var1, var2);
         }

         if (Core.bDebug && DebugOptions.instance.EntityDebugUI.getValue()) {
            this.DoDebugTooltip(var1, var2);
         }

      }
   }

   protected void DoDebugTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null && Core.bDebug) {
         super.DoDebugTooltip(var1, var2);
      }
   }

   public FluidContainer getFluidContainer() {
      return this.fluidContainer;
   }

   public boolean isFull() {
      return this.fluidContainer.isFull();
   }

   public boolean isEmpty() {
      return this.fluidContainer.isEmpty();
   }

   public float getFluidAmount() {
      return this.fluidContainer.getAmount();
   }

   public float getFluidCapacity() {
      return this.fluidContainer.getCapacity();
   }

   public float getFreeFluidCapacity() {
      return this.fluidContainer.getFreeCapacity();
   }

   public float getFluidRatio() {
      return this.getFluidCapacity() <= 0.0F ? 0.0F : PZMath.clamp_01(this.getFluidAmount() / this.getFluidCapacity());
   }

   public boolean canDrainToItem(InventoryItem var1) {
      if (!GameClient.bClient && !this.isEmpty()) {
         if (var1 != null && var1.getFluidContainer() != null) {
            FluidContainer var2 = var1.getFluidContainer();
            if (!var2.isFull()) {
               return FluidContainer.CanTransfer(this.fluidContainer, var2);
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
         FluidContainer.Transfer(this.fluidContainer, var1.getFluidContainer());
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean canDrainFromItem(InventoryItem var1) {
      if (!GameClient.bClient && !this.isFull()) {
         if (var1 != null && var1.getFluidContainer() != null) {
            FluidContainer var2 = var1.getFluidContainer();
            if (!var2.isEmpty()) {
               return FluidContainer.CanTransfer(var2, this.fluidContainer);
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
         FluidContainer.Transfer(var1.getFluidContainer(), this.fluidContainer);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public void tryTransferTo(Resource var1) {
      this.tryTransferTo(var1, this.getFluidAmount());
   }

   public void tryTransferTo(Resource var1, float var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         if (var1 instanceof ResourceFluid) {
            this.transferTo((ResourceFluid)var1, var2);
         }

      }
   }

   public void transferTo(ResourceFluid var1, float var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         float var3 = PZMath.min(PZMath.min(var2, this.getFluidAmount()), var1.getFreeFluidCapacity());
         if (!(var3 <= 0.0F)) {
            if (FluidContainer.CanTransfer(this.fluidContainer, var1.fluidContainer)) {
               FluidContainer.Transfer(this.fluidContainer, var1.fluidContainer, var3);
               this.setDirty();
            }

         }
      }
   }

   public void clear() {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
      } else {
         this.fluidContainer.Empty();
         this.setDirty();
      }
   }

   protected void reset() {
      super.reset();
      this.fluidContainer.Empty();
      this.fluidFilter.setFilterScript((String)null);
   }

   public void saveSync(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   public void loadSync(ByteBuffer var1, int var2) throws IOException {
      this.load(var1, var2);
   }

   public void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      this.fluidContainer.save(var1);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      if (this.fluidContainer != null && !this.fluidContainer.isEmpty()) {
         this.fluidContainer.Empty();
      }

      this.fluidContainer.load(var1, var2);
      if (this.getFilterName() != null) {
         this.fluidFilter.setFilterScript(this.getFilterName());
      } else {
         this.fluidFilter.setFilterScript((String)null);
      }

   }
}
