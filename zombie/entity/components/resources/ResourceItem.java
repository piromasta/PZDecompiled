package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.CraftUtil;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemFilter;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public class ResourceItem extends Resource {
   private final ItemFilter itemFilter = new ItemFilter();
   private final ArrayList<InventoryItem> storedItems = new ArrayList();
   private float capacity = 0.0F;

   protected ResourceItem() {
   }

   void loadBlueprint(ResourceBlueprint var1) {
      super.loadBlueprint(var1);
      this.capacity = var1.getCapacity();
      if (this.getFilterName() != null) {
         this.itemFilter.setFilterScript(this.getFilterName());
      }

   }

   public ItemFilter getItemFilter() {
      return this.itemFilter;
   }

   public int storedSize() {
      return this.storedItems.size();
   }

   public void DoTooltip(ObjectTooltip var1) {
      ObjectTooltip.Layout var2 = var1.beginLayout();
      var2.setMinLabelWidth(80);
      int var3 = var1.padTop;
      this.DoTooltip(var1, var2);
      var3 = var2.render(var1.padLeft, var3, var1);
      var1.endLayout(var2);
      var2 = var1.beginLayout();
      var2.setMinLabelWidth(80);
      if (this.peekItem() != null) {
         this.peekItem().DoTooltipEmbedded(var1, var2, var3);
      }

      if (Core.bDebug && DebugOptions.instance.EntityDebugUI.getValue()) {
         this.DoDebugTooltip(var1, var2);
      }

      var3 = var2.render(var1.padLeft, PZMath.max(var3, var2.offsetY), var1);
      var1.endLayout(var2);
      var3 += var1.padBottom;
      var1.setHeight((double)var3);
      if (var1.getWidth() < 150.0) {
         var1.setWidth(150.0);
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null) {
         super.DoTooltip(var1, var2);
         ObjectTooltip.LayoutItem var3 = var2.addItem();
         var3.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
         var3.setValue(this.getItemAmount() + "/" + this.getItemCapacity(), 1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   protected void DoDebugTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      if (var2 != null && Core.bDebug) {
         super.DoDebugTooltip(var1, var2);
         float var3 = 0.7F;
         ObjectTooltip.LayoutItem var4 = var2.addItem();
         var4.setLabel("UsesAmount:", 1.0F * var3, 1.0F * var3, 0.8F * var3, 1.0F);
         var4.setValue("" + this.getItemUsesAmount(), 1.0F * var3, 1.0F * var3, 1.0F * var3, 1.0F);
         var4 = var2.addItem();
         var4.setLabel("FluidAmount:", 1.0F * var3, 1.0F * var3, 0.8F * var3, 1.0F);
         var4.setValue("" + this.getFluidAmount(), 1.0F * var3, 1.0F * var3, 1.0F * var3, 1.0F);
         var4 = var2.addItem();
         var4.setLabel("EnergyAmount:", 1.0F * var3, 1.0F * var3, 0.8F * var3, 1.0F);
         var4.setValue("" + this.getEnergyAmount(), 1.0F * var3, 1.0F * var3, 1.0F * var3, 1.0F);
      }
   }

   public boolean isFull() {
      return (float)this.storedItems.size() >= this.capacity;
   }

   public boolean isEmpty() {
      return this.storedItems.size() == 0;
   }

   public int getItemAmount() {
      return this.storedItems.size();
   }

   public float getFluidAmount() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         return var1.getFluidContainer() != null ? var1.getFluidContainer().getAmount() : 0.0F;
      }
   }

   public float getEnergyAmount() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         if (var1 instanceof DrainableComboItem) {
            DrainableComboItem var2 = (DrainableComboItem)var1;
            if (var2.isEnergy()) {
               return var2.getCurrentUsesFloat();
            }
         }

         return 0.0F;
      }
   }

   public float getItemUsesAmount() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         return var1 instanceof DrainableComboItem ? ((DrainableComboItem)var1).getCurrentUsesFloat() : 0.0F;
      }
   }

   public int getItemCapacity() {
      return (int)this.capacity;
   }

   public float getFluidCapacity() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         return var1.getFluidContainer() != null ? var1.getFluidContainer().getCapacity() : 0.0F;
      }
   }

   public float getEnergyCapacity() {
      return this.getItemUsesCapacity();
   }

   public float getItemUsesCapacity() {
      return 1.0F;
   }

   public int getFreeItemCapacity() {
      return (int)this.capacity - this.storedItems.size();
   }

   public float getFreeFluidCapacity() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         return var1.getFluidContainer() != null ? var1.getFluidContainer().getFreeCapacity() : 0.0F;
      }
   }

   public float getFreeEnergyCapacity() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         if (var1 instanceof DrainableComboItem) {
            DrainableComboItem var2 = (DrainableComboItem)var1;
            if (var2.isEnergy()) {
               return this.getEnergyCapacity() - var2.getCurrentUsesFloat();
            }
         }

         return 0.0F;
      }
   }

   public float getFreeItemUsesCapacity() {
      InventoryItem var1 = this.peekItem();
      if (var1 == null) {
         return 0.0F;
      } else {
         return var1 instanceof DrainableComboItem ? this.getItemUsesCapacity() - ((DrainableComboItem)var1).getCurrentUsesFloat() : 0.0F;
      }
   }

   public boolean containsItem(InventoryItem var1) {
      return this.storedItems.contains(var1);
   }

   public boolean acceptsItem(InventoryItem var1, boolean var2) {
      if (var1 == null) {
         return false;
      } else if (!this.isLocked() && !this.isFull() && CraftUtil.canItemsStack(this.peekItem(), var1, true) && !this.containsItem(var1)) {
         return var2 || this.itemFilter.allows(var1);
      } else {
         return false;
      }
   }

   public boolean canStackItem(InventoryItem var1) {
      if (var1 == null) {
         return false;
      } else if (this.isEmpty()) {
         return this.itemFilter.allows(var1);
      } else {
         return !this.isFull() && CraftUtil.canItemsStack(this.peekItem(), var1, true) ? this.itemFilter.allows(var1) : false;
      }
   }

   public boolean canStackItem(Item var1) {
      if (var1 == null) {
         return false;
      } else if (this.isEmpty()) {
         return this.itemFilter.allows(var1);
      } else {
         Item var2 = this.peekItem().getScriptItem();
         return !this.isFull() && CraftUtil.canItemsStack(var2, var1, true) ? this.itemFilter.allows(var1) : false;
      }
   }

   public InventoryItem offerItem(InventoryItem var1, boolean var2) {
      return this.offerItem(var1, var2, false, true);
   }

   public InventoryItem offerItem(InventoryItem var1, boolean var2, boolean var3, boolean var4) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return var1;
      } else if (var3 && (var1 == null || this.containsItem(var1) || this.isFull())) {
         return var1;
      } else if (!var3 && !this.acceptsItem(var1, var2)) {
         return var1;
      } else {
         ItemContainer var5 = var1.getContainer();
         if (var5 != null) {
            if (!InventoryItem.RemoveFromContainer(var1)) {
               if (Core.bDebug) {
                  throw new RuntimeException("WARNING OfferItem -> item not removed from container.");
               }

               return var1;
            }

            if (GameServer.bServer) {
               GameServer.sendRemoveItemFromContainer(var5, var1);
            }
         }

         this.storedItems.add(var1);
         this.setDirty();
         if (var4 && GameServer.bServer) {
            this.getGameEntity().sendSyncEntity((UdpConnection)null);
         }

         return null;
      }
   }

   public ArrayList<InventoryItem> offerItems(ArrayList<InventoryItem> var1) {
      return this.offerItems(var1, false);
   }

   public ArrayList<InventoryItem> offerItems(ArrayList<InventoryItem> var1, boolean var2) {
      ArrayList var3 = new ArrayList();
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return var3;
      } else {
         Iterator var4 = var1.iterator();

         while(var4.hasNext()) {
            InventoryItem var5 = (InventoryItem)var4.next();
            if (this.isFull()) {
               break;
            }

            if (this.offerItem(var5, var2) == null) {
               var3.add(var5);
            }
         }

         return var3;
      }
   }

   public ArrayList<InventoryItem> removeAllItems(ArrayList<InventoryItem> var1) {
      if (var1 == null) {
         var1 = new ArrayList();
      }

      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return var1;
      } else {
         if (!this.isLocked() && this.storedItems.size() > 0) {
            var1.addAll(this.storedItems);
            this.storedItems.clear();
            this.setDirty();
         }

         return var1;
      }
   }

   public InventoryItem pollItem() {
      return this.pollItem(false, true);
   }

   public InventoryItem pollItem(boolean var1, boolean var2) {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
         return null;
      } else if ((var1 || !this.isLocked()) && this.storedItems.size() >= 1) {
         InventoryItem var3 = (InventoryItem)this.storedItems.remove(this.storedItems.size() - 1);
         this.setDirty();
         if (var2 && GameServer.bServer) {
            this.getGameEntity().sendSyncEntity((UdpConnection)null);
         }

         return var3;
      } else {
         return null;
      }
   }

   public InventoryItem peekItem() {
      return this.peekItem(0);
   }

   public InventoryItem peekItem(int var1) {
      if (this.storedItems.size() >= 1) {
         int var2 = this.storedItems.size() - 1;
         if (var1 > 0) {
            var2 -= var1;
            if (var2 < 0) {
               return null;
            }
         }

         return (InventoryItem)this.storedItems.get(var2);
      } else {
         return null;
      }
   }

   public ArrayList<InventoryItem> getStoredItems() {
      return this.storedItems;
   }

   public void tryTransferTo(Resource var1) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         if (var1 instanceof ResourceItem) {
            this.transferTo((ResourceItem)var1, this.getItemAmount());
         }

      }
   }

   public void tryTransferTo(Resource var1, float var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         if (var1 instanceof ResourceItem) {
            this.transferTo((ResourceItem)var1, (int)var2);
         }

      }
   }

   public void transferTo(ResourceItem var1, int var2) {
      if (!this.isEmpty() && var1 != null && !var1.isFull()) {
         int var3 = PZMath.min(PZMath.min(var2, this.getItemAmount()), var1.getFreeItemCapacity());
         if (var3 > 0) {
            for(int var5 = 0; var5 < var3; ++var5) {
               InventoryItem var4 = this.pollItem();
               if (var1.offerItem(var4) != null) {
                  this.storedItems.add(var4);
                  break;
               }
            }

            this.setDirty();
         }
      }
   }

   public void clear() {
      if (GameClient.bClient) {
         DebugLog.General.warn("Not allowed on client");
      } else {
         this.storedItems.clear();
         this.setDirty();
      }
   }

   protected void reset() {
      super.reset();
      this.storedItems.clear();
      this.capacity = 0.0F;
      this.itemFilter.setFilterScript((String)null);
   }

   public void saveSync(ByteBuffer var1) throws IOException {
      super.saveSync(var1);
      var1.putFloat(this.capacity);
      var1.putInt(this.storedItems.size());
      if (this.storedItems.size() > 0) {
         String var2 = ((InventoryItem)this.storedItems.get(0)).getFullType();
         GameWindow.WriteString(var1, var2);

         for(int var3 = 0; var3 < this.storedItems.size(); ++var3) {
            InventoryItem var4 = (InventoryItem)this.storedItems.get(var3);
            if (!var4.getFullType().equals(var2)) {
               String var10002 = var4.getFullType();
               throw new IOException("Type mismatch '" + var10002 + "' vs '" + var2 + "'");
            }

            var4.saveWithSize(var1, true);
         }
      }

   }

   public void loadSync(ByteBuffer var1, int var2) throws IOException {
      super.loadSync(var1, var2);
      this.capacity = var1.getFloat();
      int var3 = var1.getInt();
      if (var3 == 0) {
         this.storedItems.clear();
      } else {
         String var4 = GameWindow.ReadString(var1);
         int var5 = var1.position();
         boolean var6 = false;
         if (this.storedItems.size() == var3) {
            try {
               var6 = this.tryLoadSyncItems(var1, var2, var3, var4, false);
            } catch (Exception var9) {
               if (Core.bDebug) {
                  DebugLog.General.warn("Unable to load items (may be ignored)");
               }
            }
         }

         if (!var6) {
            try {
               var1.position(var5);
               this.tryLoadSyncItems(var1, var2, var3, var4, true);
            } catch (Exception var8) {
               var8.printStackTrace();
            }
         }

      }
   }

   public boolean tryLoadSyncItems(ByteBuffer var1, int var2, int var3, String var4, boolean var5) throws IOException {
      if (var5) {
         this.storedItems.clear();
      }

      for(int var7 = 0; var7 < var3; ++var7) {
         InventoryItem var6;
         if (var5) {
            var6 = InventoryItemFactory.CreateItem(var4);
         } else {
            var6 = (InventoryItem)this.storedItems.get(var7);
            if (!var6.getFullType().equals(var4)) {
               String var10002 = var6.getFullType();
               throw new IOException("Type mismatch '" + var10002 + "' vs '" + var4 + "'");
            }
         }

         InventoryItem.loadItem(var1, var2, true, var6);
      }

      return true;
   }

   public void save(ByteBuffer var1) throws IOException {
      super.save(var1);
      var1.putFloat(this.capacity);
      if (this.storedItems.size() == 1) {
         CompressIdenticalItems.save(var1, (InventoryItem)this.storedItems.get(0));
      } else {
         CompressIdenticalItems.save(var1, this.storedItems, (IsoGameCharacter)null);
      }

   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.capacity = var1.getFloat();
      this.storedItems.clear();
      CompressIdenticalItems.load(var1, var2, this.storedItems, (ArrayList)null);
      if (this.getFilterName() != null) {
         this.itemFilter.setFilterScript(this.getFilterName());
      } else {
         this.itemFilter.setFilterScript((String)null);
      }

   }
}
