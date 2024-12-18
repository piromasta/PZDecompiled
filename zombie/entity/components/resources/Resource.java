package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.GameEntity;
import zombie.entity.util.enums.EnumBitStore;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
public abstract class Resource {
   @DebugNonRecursive
   protected Resources resourcesComponent;
   protected ResourceGroup group;
   private boolean isLocked = false;
   private double progress = 0.0;
   private String id = null;
   private ResourceType resourceType;
   private ResourceIO resourceIO;
   private ResourceChannel channel;
   private EnumBitStore<ResourceFlag> flags;
   private String filterName;
   private boolean dirty;

   protected Resource() {
      this.resourceType = ResourceType.Any;
      this.resourceIO = ResourceIO.Any;
      this.channel = ResourceChannel.NO_CHANNEL;
      this.flags = EnumBitStore.noneOf(ResourceFlag.class);
      this.filterName = null;
      this.dirty = true;
   }

   void setGroup(ResourceGroup var1) {
      this.group = var1;
   }

   ResourceGroup getGroup() {
      return this.group;
   }

   void setResourcesComponent(Resources var1) {
      this.resourcesComponent = var1;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void setDirty() {
      if (!GameClient.bClient) {
         if (this.resourcesComponent != null) {
            this.dirty = true;
            this.resourcesComponent.setDirty();
            if (this.group != null) {
               this.group.setDirty();
            }
         } else if (Core.bDebug) {
            throw new IllegalStateException("ResourceComponent (currently) not set, cannot perform.");
         }

      }
   }

   protected void resetDirty() {
      this.dirty = false;
   }

   public Resources getResourcesComponent() {
      if (this.resourcesComponent == null) {
         DebugLog.General.warn("ResourcesComponent (currently) not set!");
      }

      return this.resourcesComponent;
   }

   public GameEntity getGameEntity() {
      if (this.resourcesComponent != null) {
         return this.resourcesComponent.getGameEntity();
      } else if (Core.bDebug) {
         throw new IllegalStateException("ResourceComponent (currently) not set, cannot perform.");
      } else {
         return null;
      }
   }

   public void DoTooltip(ObjectTooltip var1) {
      ObjectTooltip.Layout var2 = var1.beginLayout();
      var2.setMinLabelWidth(80);
      int var3 = var1.padTop;
      this.DoTooltip(var1, var2);
      var3 = var2.render(var1.padLeft, var3, var1);
      var1.endLayout(var2);
      var3 += var1.padBottom;
      var1.setHeight((double)var3);
      if (var1.getWidth() < 150.0) {
         var1.setWidth(150.0);
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         ObjectTooltip.LayoutItem var3;
         if (this.isLocked) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getEntityText("EC_Locked") + ":", 0.9F, 0.3F, 0.3F, 1.0F);
         }

         if (this.progress > 0.0) {
            var3 = var2.addItem();
            var3.setLabel(Translator.getEntityText("EC_Progress") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            var3.setProgress((float)this.progress, 0.0F, 1.0F, 0.0F, 1.0F);
         }

         if (!this.isEmpty()) {
            ;
         }
      }
   }

   protected void DoDebugTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null && Core.bDebug) {
         float var4 = 0.7F;
         ObjectTooltip.LayoutItem var3 = var2.addItem();
         var3.setLabel("[DEBUG_INFO]", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("id:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(this.id, 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("ResourceType:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(this.resourceType.toString(), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("ResourceIO:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(this.resourceIO.toString(), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("Channel-#:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(this.channel.toString(), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("Flags:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(this.flags.toString(), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         var3 = var2.addItem();
         var3.setLabel("Dirty:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
         var3.setValue(Boolean.toString(this.dirty), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         if (this.group != null) {
            var3 = var2.addItem();
            var3.setLabel("Group:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
            var3.setValue(this.group.getName(), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         }

         if (this.progress > 0.0) {
            var3 = var2.addItem();
            var3.setLabel("Progress:", 1.0F * var4, 1.0F * var4, 0.8F * var4, 1.0F);
            var3.setValue(Double.toString(this.progress), 1.0F * var4, 1.0F * var4, 1.0F * var4, 1.0F);
         }

      }
   }

   public String getId() {
      return this.id;
   }

   public ResourceType getType() {
      return this.resourceType;
   }

   public ResourceIO getIO() {
      return this.resourceIO;
   }

   public ResourceChannel getChannel() {
      return this.channel;
   }

   public boolean isAutoDecay() {
      return this.flags.contains(ResourceFlag.AutoDecay);
   }

   public boolean hasFlag(ResourceFlag var1) {
      return this.flags.contains(var1);
   }

   public String getDebugFlagsString() {
      return this.flags.toString();
   }

   public String getFilterName() {
      return this.filterName;
   }

   void loadBlueprint(ResourceBlueprint var1) {
      this.id = var1.getId();
      this.resourceType = var1.getType();
      this.resourceIO = var1.getIO();
      this.channel = var1.getChannel();
      this.flags.setBits(var1.getFlagBits());
      this.filterName = var1.getFilter();
   }

   public void setProgress(double var1) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
      } else {
         var1 = PZMath.clampDouble_01(var1);
         if (this.progress != var1) {
            this.progress = PZMath.clampDouble_01(var1);
            this.setDirty();
         }

      }
   }

   public double getProgress() {
      return this.progress;
   }

   public boolean isLocked() {
      return this.isLocked;
   }

   public void setLocked(boolean var1) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
      } else {
         if (this.isLocked != var1) {
            this.isLocked = var1;
            this.setDirty();
         }

      }
   }

   public abstract boolean isFull();

   public abstract boolean isEmpty();

   public int getItemAmount() {
      return 0;
   }

   public float getFluidAmount() {
      return 0.0F;
   }

   public float getEnergyAmount() {
      return 0.0F;
   }

   public float getItemUsesAmount() {
      return 0.0F;
   }

   public int getItemCapacity() {
      return 0;
   }

   public float getFluidCapacity() {
      return 0.0F;
   }

   public float getEnergyCapacity() {
      return 0.0F;
   }

   public float getItemUsesCapacity() {
      return 0.0F;
   }

   public int getFreeItemCapacity() {
      return 0;
   }

   public float getFreeFluidCapacity() {
      return 0.0F;
   }

   public float getFreeEnergyCapacity() {
      return 0.0F;
   }

   public float getFreeItemUsesCapacity() {
      return 0.0F;
   }

   public boolean canMoveItemsToOutput() {
      return true;
   }

   public boolean containsItem(InventoryItem var1) {
      return false;
   }

   public final boolean acceptsItem(InventoryItem var1) {
      return this.acceptsItem(var1, false);
   }

   public boolean acceptsItem(InventoryItem var1, boolean var2) {
      return false;
   }

   public boolean canStackItem(InventoryItem var1) {
      return false;
   }

   public boolean canStackItem(Item var1) {
      return false;
   }

   public final InventoryItem offerItem(InventoryItem var1) {
      return this.offerItem(var1, false);
   }

   public InventoryItem offerItem(InventoryItem var1, boolean var2) {
      return var1;
   }

   public InventoryItem offerItem(InventoryItem var1, boolean var2, boolean var3, boolean var4) {
      return var1;
   }

   public InventoryItem pollItem() {
      return null;
   }

   public InventoryItem pollItem(boolean var1, boolean var2) {
      return null;
   }

   public InventoryItem peekItem() {
      return null;
   }

   public InventoryItem peekItem(int var1) {
      return null;
   }

   public boolean canDrainToItem(InventoryItem var1) {
      return false;
   }

   public boolean drainToItem(InventoryItem var1) {
      return false;
   }

   public boolean canDrainFromItem(InventoryItem var1) {
      return false;
   }

   public boolean drainFromItem(InventoryItem var1) {
      return false;
   }

   public void tryTransferTo(Resource var1) {
   }

   public void tryTransferTo(Resource var1, float var2) {
   }

   public abstract void clear();

   protected void reset() {
      this.resourcesComponent = null;
      this.id = null;
      this.resourceType = ResourceType.Any;
      this.resourceIO = ResourceIO.Any;
      this.channel = ResourceChannel.NO_CHANNEL;
      this.flags.clear();
      this.filterName = null;
      this.progress = 0.0;
      this.isLocked = false;
      this.dirty = false;
   }

   public void saveSync(ByteBuffer var1) throws IOException {
      var1.put((byte)(this.isLocked ? 1 : 0));
      var1.putDouble(this.progress);
   }

   public void loadSync(ByteBuffer var1, int var2) throws IOException {
      this.isLocked = var1.get() == 1;
      this.progress = var1.getDouble();
   }

   public void save(ByteBuffer var1) throws IOException {
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      GameWindow.WriteString(var1, this.id);
      var1.put(this.resourceType.getId());
      var1.put(this.resourceIO.getId());
      if (this.isLocked) {
         var2.addFlags(1);
      }

      if (this.progress > 0.0) {
         var2.addFlags(2);
         var1.putDouble(this.progress);
      }

      if (this.channel != ResourceChannel.NO_CHANNEL) {
         var2.addFlags(4);
         var1.put(this.channel.getByteId());
      }

      if (!this.flags.isEmpty()) {
         var2.addFlags(8);
         this.flags.save(var1);
      }

      var2.write();
      var2.release();
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      BitHeaderRead var3 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.isLocked = false;
      this.progress = 0.0;
      this.id = null;
      this.flags.clear();
      this.id = GameWindow.ReadString(var1);
      this.resourceType = ResourceType.fromId(var1.get());
      this.resourceIO = ResourceIO.fromId(var1.get());
      if (var3.hasFlags(1)) {
         this.isLocked = true;
      }

      if (var3.hasFlags(2)) {
         this.progress = var1.getDouble();
      }

      if (var3.hasFlags(4)) {
         this.channel = ResourceChannel.fromId(var1.get());
      }

      if (var3.hasFlags(8)) {
         this.flags.load(var1);
      }

      var3.release();
   }
}
