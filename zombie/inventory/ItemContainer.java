package zombie.inventory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Predicate;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoStove;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class ItemContainer {
   private static final ArrayList<InventoryItem> tempList = new ArrayList();
   private static final ThreadLocal<ArrayList<IsoObject>> s_tempObjects = ThreadLocal.withInitial(ArrayList::new);
   public boolean active = false;
   private boolean dirty = true;
   public boolean IsDevice = false;
   public float ageFactor = 1.0F;
   public float CookingFactor = 1.0F;
   public int Capacity = 50;
   public InventoryItem containingItem = null;
   public ArrayList<InventoryItem> Items = new ArrayList();
   public ArrayList<InventoryItem> IncludingObsoleteItems = new ArrayList();
   public IsoObject parent = null;
   public IsoGridSquare SourceGrid = null;
   public VehiclePart vehiclePart = null;
   public InventoryContainer inventoryContainer = null;
   public boolean bExplored = false;
   public String type = "none";
   public int ID = 0;
   private boolean drawDirty = true;
   private float customTemperature = 0.0F;
   private boolean hasBeenLooted = false;
   private String openSound = null;
   private String closeSound = null;
   private String putSound = null;
   private String OnlyAcceptCategory = null;
   private String AcceptItemFunction = null;
   private int weightReduction = 0;
   private String containerPosition = null;
   private String freezerPosition = null;
   private int MAX_CAPACITY = 100;
   private int MAX_CAPACITY_BAG = 50;
   private int MAX_CAPACITY_VEHICLE = 1000;
   private static final ThreadLocal<Comparators> TL_comparators = ThreadLocal.withInitial(Comparators::new);
   private static final ThreadLocal<InventoryItemListPool> TL_itemListPool = ThreadLocal.withInitial(InventoryItemListPool::new);
   private static final ThreadLocal<Predicates> TL_predicates = ThreadLocal.withInitial(Predicates::new);

   public ItemContainer(int var1, String var2, IsoGridSquare var3, IsoObject var4) {
      this.ID = var1;
      this.parent = var4;
      this.type = var2;
      this.SourceGrid = var3;
      if (var2.equals("fridge")) {
         this.ageFactor = 0.02F;
         this.CookingFactor = 0.0F;
      }

   }

   public ItemContainer(String var1, IsoGridSquare var2, IsoObject var3) {
      this.ID = -1;
      this.parent = var3;
      this.type = var1;
      this.SourceGrid = var2;
      if (var1.equals("fridge")) {
         this.ageFactor = 0.02F;
         this.CookingFactor = 0.0F;
      }

   }

   public ItemContainer(int var1) {
      this.ID = var1;
   }

   public ItemContainer() {
      this.ID = -1;
   }

   public static float floatingPointCorrection(float var0) {
      byte var1 = 100;
      float var2 = var0 * (float)var1;
      return (float)((int)(var2 - (float)((int)var2) >= 0.5F ? var2 + 1.0F : var2)) / (float)var1;
   }

   public int getCapacity() {
      if (this.parent instanceof BaseVehicle) {
         return Math.min(this.Capacity, this.MAX_CAPACITY_VEHICLE);
      } else {
         return this.containingItem != null && this.containingItem instanceof InventoryItem ? Math.min(this.Capacity, this.MAX_CAPACITY_BAG) : Math.min(this.Capacity, this.MAX_CAPACITY);
      }
   }

   public void setCapacity(int var1) {
      if (this.parent instanceof BaseVehicle && this.Capacity > this.MAX_CAPACITY_VEHICLE) {
         DebugLog.General.warn("Attempting to set capacity of " + this + "over maximum capacity of " + this.MAX_CAPACITY_VEHICLE);
      } else if (this.containingItem != null && this.containingItem instanceof InventoryItem && this.Capacity > this.MAX_CAPACITY_BAG) {
         DebugLog.General.warn("Attempting to set capacity of " + this.containingItem + "over maximum capacity of " + this.MAX_CAPACITY_BAG);
      } else if (var1 > this.MAX_CAPACITY) {
         DebugLog.General.warn("Attempting to set capacity of " + this + "over maximum capacity of " + this.MAX_CAPACITY);
      }

      this.Capacity = var1;
   }

   public InventoryItem FindAndReturnWaterItem(int var1) {
      for(int var2 = 0; var2 < this.getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.getItems().get(var2);
         if (var3 instanceof DrainableComboItem var4 && var3.isWaterSource()) {
            if (var4.getCurrentUses() >= var1) {
               return var3;
            }
         }
      }

      return null;
   }

   public InventoryItem getItemFromTypeRecurse(String var1) {
      return this.getFirstTypeRecurse(var1);
   }

   public int getEffectiveCapacity(IsoGameCharacter var1) {
      int var2;
      if (this.parent instanceof BaseVehicle) {
         var2 = Math.min(this.Capacity, this.MAX_CAPACITY_VEHICLE);
      } else if (this.containingItem != null && this.containingItem instanceof InventoryItem) {
         var2 = Math.min(this.Capacity, this.MAX_CAPACITY_BAG);
      } else {
         var2 = Math.min(this.Capacity, this.MAX_CAPACITY);
      }

      if (var1 != null && !(this.parent instanceof IsoGameCharacter) && !(this.parent instanceof IsoDeadBody) && !"floor".equals(this.getType())) {
         if (var1.Traits.Organized.isSet()) {
            return (int)Math.max((float)var2 * 1.3F, (float)(this.Capacity + 1));
         }

         if (var1.Traits.Disorganized.isSet()) {
            return (int)Math.max((float)var2 * 0.7F, 1.0F);
         }
      }

      return var2;
   }

   public boolean hasRoomFor(IsoGameCharacter var1, InventoryItem var2) {
      if (this.inventoryContainer != null && this.inventoryContainer.getMaxItemSize() > 0.0F && var2.getUnequippedWeight() > this.inventoryContainer.getMaxItemSize()) {
         return false;
      } else if (var1.getVehicle() != null && var2.hasTag("HeavyItem") && this.parent instanceof IsoGameCharacter) {
         return false;
      } else {
         if (!this.isInCharacterInventory(var1) && this.containingItem != null && this.containingItem.getWorldItem() != null && this.containingItem.getWorldItem().getSquare() != null) {
            IsoGridSquare var3 = this.containingItem.getWorldItem().getSquare();
            float var4 = var3.getTotalWeightOfItemsOnFloor();
            if (var4 + var2.getUnequippedWeight() > 50.0F) {
               return false;
            }
         }

         if (this.vehiclePart != null && this.vehiclePart.getId().contains("Seat") && this.Items.isEmpty() && floatingPointCorrection(var2.getUnequippedWeight()) <= 50.0F) {
            return true;
         } else if (this.containingItem != null && this.containingItem.getContainer() != null && this.containingItem.getContainer().getVehiclePart() != null && floatingPointCorrection(this.containingItem.getContainer().getCapacityWeight() + var2.getUnequippedWeight()) > (float)this.getContainingItem().getContainer().getEffectiveCapacity(var1)) {
            return false;
         } else if (floatingPointCorrection(this.getCapacityWeight()) + var2.getUnequippedWeight() <= (float)this.getEffectiveCapacity(var1)) {
            if (this.getContainingItem() != null && this.getContainingItem().getEquipParent() != null && this.getContainingItem().getEquipParent().getInventory() != null && !this.getContainingItem().getEquipParent().getInventory().contains(var2)) {
               return floatingPointCorrection(this.getContainingItem().getEquipParent().getInventory().getCapacityWeight()) + var2.getUnequippedWeight() <= (float)this.getContainingItem().getEquipParent().getInventory().getEffectiveCapacity(var1);
            } else {
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public boolean hasRoomFor(IsoGameCharacter var1, float var2) {
      return floatingPointCorrection(this.getCapacityWeight()) + var2 <= (float)this.getEffectiveCapacity(var1);
   }

   public boolean isItemAllowed(InventoryItem var1) {
      if (var1 == null) {
         return false;
      } else if (var1 instanceof AnimalInventoryItem && !"floor".equals(this.type)) {
         return false;
      } else if (var1.getType().contains("Corpse") && this.parent instanceof IsoDeadBody) {
         return false;
      } else {
         String var2 = this.getOnlyAcceptCategory();
         if (var2 != null && !var2.equalsIgnoreCase(var1.getCategory())) {
            return false;
         } else {
            String var3 = this.getAcceptItemFunction();
            if (var3 != null) {
               Object var4 = LuaManager.getFunctionObject(var3);
               if (var4 != null) {
                  Boolean var5 = LuaManager.caller.protectedCallBoolean(LuaManager.thread, var4, this, var1);
                  if (var5 != Boolean.TRUE) {
                     return false;
                  }
               }
            }

            if (this.parent != null && !this.parent.isItemAllowedInContainer(this, var1)) {
               return false;
            } else if (this.getType().equals("clothingrack") && !(var1 instanceof Clothing) && !var1.hasTag("FitsClothingRack")) {
               return false;
            } else if (this.getParent() != null && this.getParent().getProperties() != null && this.getParent().getProperties().Val("CustomName") != null && this.getParent().getProperties().Val("CustomName").equals("Toaster") && !var1.hasTag("FitsToaster")) {
               return false;
            } else {
               if (this.getParent() != null && this.getParent().getProperties() != null && this.getParent().getProperties().Val("GroupName") != null) {
                  boolean var6 = this.getParent().getProperties().Val("GroupName").equals("Coffee") || this.getParent().getProperties().Val("GroupName").equals("Espresso");
                  if (var6 && !var1.hasTag("CoffeeMaker")) {
                     return false;
                  }
               }

               if (this.vehiclePart != null && this.vehiclePart.getId().contains("Seat")) {
                  Boolean var7 = this.vehiclePart.getVehicle().getCharacter(this.vehiclePart.getContainerSeatNumber()) != null;
                  if (var7 && var1.getWeight() + this.getCapacityWeight() > (float)(this.getCapacity() / 4)) {
                     return false;
                  }
               }

               return true;
            }
         }
      }
   }

   public boolean isRemoveItemAllowed(InventoryItem var1) {
      if (var1 == null) {
         return false;
      } else {
         return this.parent == null || this.parent.isRemoveItemAllowedFromContainer(this, var1);
      }
   }

   public boolean isExplored() {
      return this.bExplored;
   }

   public void setExplored(boolean var1) {
      this.bExplored = var1;
   }

   public boolean isInCharacterInventory(IsoGameCharacter var1) {
      if (var1.getInventory() == this) {
         return true;
      } else {
         if (this.containingItem != null) {
            if (var1.getInventory().contains(this.containingItem, true)) {
               return true;
            }

            if (this.containingItem.getContainer() != null) {
               return this.containingItem.getContainer().isInCharacterInventory(var1);
            }
         }

         return false;
      }
   }

   public boolean isInside(InventoryItem var1) {
      if (this.containingItem == null) {
         return false;
      } else if (this.containingItem == var1) {
         return true;
      } else {
         return this.containingItem.getContainer() != null && this.containingItem.getContainer().isInside(var1);
      }
   }

   public InventoryItem getContainingItem() {
      return this.containingItem;
   }

   public InventoryItem DoAddItem(InventoryItem var1) {
      return this.AddItem(var1);
   }

   public InventoryItem DoAddItemBlind(InventoryItem var1) {
      return this.AddItem(var1);
   }

   public ArrayList<InventoryItem> AddItems(String var1, int var2) {
      ArrayList var3 = new ArrayList();

      for(int var4 = 0; var4 < var2; ++var4) {
         InventoryItem var5 = this.AddItem(var1);
         if (var5 != null) {
            var3.add(var5);
         }
      }

      return var3;
   }

   public ArrayList<InventoryItem> AddItems(InventoryItem var1, int var2) {
      return this.AddItems(var1.getFullType(), var2);
   }

   public ArrayList<InventoryItem> AddItems(ArrayList<InventoryItem> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         InventoryItem var3 = (InventoryItem)var2.next();
         this.AddItem(var3);
      }

      return var1;
   }

   public int getNumberOfItem(String var1, boolean var2) {
      return this.getNumberOfItem(var1, var2, false);
   }

   public int getNumberOfItem(String var1) {
      return this.getNumberOfItem(var1, false);
   }

   public int getNumberOfItem(String var1, boolean var2, ArrayList<ItemContainer> var3) {
      int var4 = this.getNumberOfItem(var1, var2);
      if (var3 != null) {
         Iterator var5 = var3.iterator();

         while(var5.hasNext()) {
            ItemContainer var6 = (ItemContainer)var5.next();
            if (var6 != this) {
               var4 += var6.getNumberOfItem(var1, var2);
            }
         }
      }

      return var4;
   }

   public int getNumberOfItem(String var1, boolean var2, boolean var3) {
      int var4 = 0;

      for(int var5 = 0; var5 < this.Items.size(); ++var5) {
         InventoryItem var6 = (InventoryItem)this.Items.get(var5);
         if (!var6.getFullType().equals(var1) && !var6.getType().equals(var1)) {
            if (var3 && var6 instanceof InventoryContainer) {
               var4 += ((InventoryContainer)var6).getItemContainer().getNumberOfItem(var1);
            } else if (var2 && var6 instanceof DrainableComboItem && ((DrainableComboItem)var6).getReplaceOnDeplete() != null) {
               DrainableComboItem var7 = (DrainableComboItem)var6;
               if (var7.getReplaceOnDepleteFullType().equals(var1) || var7.getReplaceOnDeplete().equals(var1)) {
                  ++var4;
               }
            }
         } else {
            ++var4;
         }
      }

      return var4;
   }

   public InventoryItem addItem(InventoryItem var1) {
      return this.AddItem(var1);
   }

   public InventoryItem AddItem(InventoryItem var1) {
      if (var1 == null) {
         return null;
      } else if (this.containsID(var1.id)) {
         System.out.println("Error, container already has id");
         return this.getItemWithID(var1.id);
      } else {
         this.drawDirty = true;
         if (this.parent != null) {
            this.dirty = true;
         }

         if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.parent.DirtySlice();
         }

         if (var1.container != null) {
            var1.container.Remove(var1);
         }

         var1.container = this;
         this.Items.add(var1);
         if (IsoWorld.instance.CurrentCell != null) {
            IsoWorld.instance.CurrentCell.addToProcessItems(var1);
         }

         if (this.getParent() instanceof IsoFeedingTrough) {
            ((IsoFeedingTrough)this.getParent()).onFoodAdded();
         }

         var1.OnAddedToContainer(this);
         return var1;
      }
   }

   public InventoryItem AddItemBlind(InventoryItem var1) {
      if (var1 == null) {
         return null;
      } else if (var1.getWeight() + this.getCapacityWeight() > (float)this.getCapacity()) {
         return null;
      } else {
         if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.parent.DirtySlice();
         }

         this.Items.add(var1);
         return var1;
      }
   }

   public InventoryItem AddItem(String var1) {
      this.drawDirty = true;
      if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
         this.dirty = true;
      }

      Item var2 = ScriptManager.instance.FindItem(var1);
      if (var2 == null) {
         DebugLog.log("ERROR: ItemContainer.AddItem: can't find " + var1);
         return null;
      } else if (var2.OBSOLETE) {
         return null;
      } else {
         InventoryItem var3 = InventoryItemFactory.CreateItem(var1);
         if (var3 == null) {
            return null;
         } else {
            var3.container = this;
            this.Items.add(var3);
            if (var3 instanceof Food) {
               ((Food)var3).setHeat(this.getTemprature());
            }

            if (var3.hasComponent(ComponentType.FluidContainer) && var3.isCookable()) {
               var3.setItemHeat(this.getTemprature());
            }

            if (IsoWorld.instance.CurrentCell != null) {
               IsoWorld.instance.CurrentCell.addToProcessItems(var3);
            }

            return var3;
         }
      }
   }

   public boolean AddItem(String var1, float var2) {
      this.drawDirty = true;
      if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
         this.dirty = true;
      }

      InventoryItem var3 = InventoryItemFactory.CreateItem(var1);
      if (var3 == null) {
         return false;
      } else {
         if (var3 instanceof Drainable) {
            var3.setCurrentUses((int)((float)var3.getMaxUses() * var2));
         }

         var3.container = this;
         this.Items.add(var3);
         return true;
      }
   }

   public boolean contains(InventoryItem var1) {
      return this.Items.contains(var1);
   }

   public boolean containsWithModule(String var1) {
      return this.containsWithModule(var1, false);
   }

   public boolean containsWithModule(String var1, boolean var2) {
      String var3 = var1;
      String var4 = "Base";
      if (var1.contains(".")) {
         var4 = var1.split("\\.")[0];
         var3 = var1.split("\\.")[1];
      }

      for(int var5 = 0; var5 < this.Items.size(); ++var5) {
         InventoryItem var6 = (InventoryItem)this.Items.get(var5);
         if (var6 == null) {
            this.Items.remove(var5);
            --var5;
         } else if (var6.type.equals(var3.trim()) && var4.equals(var6.getModule()) && (!var2 || !(var6 instanceof DrainableComboItem) || var6.getCurrentUses() > 0)) {
            return true;
         }
      }

      return false;
   }

   /** @deprecated */
   @Deprecated
   public void removeItemOnServer(InventoryItem var1) {
      if (GameClient.bClient) {
         if (this.containingItem != null && this.containingItem.getWorldItem() != null) {
            GameClient.instance.addToItemRemoveSendBuffer(this.containingItem.getWorldItem(), this, var1);
         } else {
            GameClient.instance.addToItemRemoveSendBuffer(this.parent, this, var1);
         }
      }

   }

   public void addItemOnServer(InventoryItem var1) {
      if (GameClient.bClient) {
         if (this.containingItem != null && this.containingItem.getWorldItem() != null) {
            GameClient.instance.addToItemSendBuffer(this.containingItem.getWorldItem(), this, var1);
         } else {
            GameClient.instance.addToItemSendBuffer(this.parent, this, var1);
         }
      }

   }

   public boolean contains(InventoryItem var1, boolean var2) {
      InventoryItemList var3 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();

      int var4;
      for(var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         if (var5 == null) {
            this.Items.remove(var4);
            --var4;
         } else {
            if (var5 == var1) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
               return true;
            }

            if (var2 && var5 instanceof InventoryContainer && ((InventoryContainer)var5).getInventory() != null && !var3.contains(var5)) {
               var3.add(var5);
            }
         }
      }

      for(var4 = 0; var4 < var3.size(); ++var4) {
         ItemContainer var6 = ((InventoryContainer)var3.get(var4)).getInventory();
         if (var6.contains(var1, var2)) {
            ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
            return true;
         }
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
      return false;
   }

   public boolean contains(String var1, boolean var2) {
      return this.contains(var1, var2, false);
   }

   public boolean containsType(String var1) {
      return this.contains(var1, false, false);
   }

   public boolean containsTypeRecurse(String var1) {
      return this.contains(var1, true, false);
   }

   private boolean testBroken(boolean var1, InventoryItem var2) {
      if (!var1) {
         return true;
      } else {
         return !var2.isBroken();
      }
   }

   public boolean contains(String var1, boolean var2, boolean var3) {
      InventoryItemList var4 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      int var5;
      InventoryItem var6;
      if (var1.contains("Type:")) {
         for(var5 = 0; var5 < this.Items.size(); ++var5) {
            var6 = (InventoryItem)this.Items.get(var5);
            if (var1.contains("Food") && var6 instanceof Food) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
               return true;
            }

            if (var1.contains("Weapon") && var6 instanceof HandWeapon && this.testBroken(var3, var6)) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
               return true;
            }

            if (var1.contains("AlarmClock") && var6 instanceof AlarmClock) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
               return true;
            }

            if (var1.contains("AlarmClockClothing") && var6 instanceof AlarmClockClothing) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
               return true;
            }

            if (var2 && var6 instanceof InventoryContainer && ((InventoryContainer)var6).getInventory() != null && !var4.contains(var6)) {
               var4.add(var6);
            }
         }
      } else if (var1.contains("/")) {
         String[] var12 = var1.split("/");
         String[] var13 = var12;
         int var7 = var12.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            String var9 = var13[var8];

            for(int var10 = 0; var10 < this.Items.size(); ++var10) {
               InventoryItem var11 = (InventoryItem)this.Items.get(var10);
               if (compareType(var9.trim(), var11) && this.testBroken(var3, var11)) {
                  ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
                  return true;
               }

               if (var2 && var11 instanceof InventoryContainer && ((InventoryContainer)var11).getInventory() != null && !var4.contains(var11)) {
                  var4.add(var11);
               }
            }
         }
      } else {
         for(var5 = 0; var5 < this.Items.size(); ++var5) {
            var6 = (InventoryItem)this.Items.get(var5);
            if (var6 == null) {
               this.Items.remove(var5);
               --var5;
            } else {
               if (compareType(var1.trim(), var6) && this.testBroken(var3, var6)) {
                  ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
                  return true;
               }

               if (var2 && var6 instanceof InventoryContainer && ((InventoryContainer)var6).getInventory() != null && !var4.contains(var6)) {
                  var4.add(var6);
               }
            }
         }
      }

      for(var5 = 0; var5 < var4.size(); ++var5) {
         ItemContainer var14 = ((InventoryContainer)var4.get(var5)).getInventory();
         if (var14.contains(var1, var2, var3)) {
            ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
            return true;
         }
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
      return false;
   }

   public boolean contains(String var1) {
      return this.contains(var1, false);
   }

   public AnimalInventoryItem getAnimalInventoryItem(IsoAnimal var1) {
      Iterator var2 = this.Items.iterator();

      AnimalInventoryItem var4;
      label29:
      do {
         do {
            do {
               if (!var2.hasNext()) {
                  return null;
               }

               InventoryItem var3 = (InventoryItem)var2.next();
               var4 = (AnimalInventoryItem)Type.tryCastTo(var3, AnimalInventoryItem.class);
            } while(var4 == null);

            if (!GameServer.bServer && !GameClient.bClient) {
               continue label29;
            }
         } while(var4.getAnimal().getOnlineID() != var1.getOnlineID());

         return var4;
      } while(var4.getAnimal().getAnimalID() != var1.getAnimalID());

      return var4;
   }

   private static InventoryItem getBestOf(InventoryItemList var0, Comparator<InventoryItem> var1) {
      if (var0 != null && !var0.isEmpty()) {
         InventoryItem var2 = (InventoryItem)var0.get(0);

         for(int var3 = 1; var3 < var0.size(); ++var3) {
            InventoryItem var4 = (InventoryItem)var0.get(var3);
            if (var1.compare(var4, var2) > 0) {
               var2 = var4;
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   public InventoryItem getBest(Predicate<InventoryItem> var1, Comparator<InventoryItem> var2) {
      InventoryItemList var3 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAll(var1, var3);
      InventoryItem var4 = getBestOf(var3, var2);
      ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
      return var4;
   }

   public InventoryItem getBestRecurse(Predicate<InventoryItem> var1, Comparator<InventoryItem> var2) {
      InventoryItemList var3 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAllRecurse(var1, var3);
      InventoryItem var4 = getBestOf(var3, var2);
      ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
      return var4;
   }

   public InventoryItem getBestType(String var1, Comparator<InventoryItem> var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);

      InventoryItem var4;
      try {
         var4 = this.getBest(var3, var2);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var3);
      }

      return var4;
   }

   public InventoryItem getBestTypeRecurse(String var1, Comparator<InventoryItem> var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);

      InventoryItem var4;
      try {
         var4 = this.getBestRecurse(var3, var2);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var3);
      }

      return var4;
   }

   public InventoryItem getBestEval(LuaClosure var1, LuaClosure var2) {
      EvalPredicate var3 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      EvalComparator var4 = ((EvalComparator)((Comparators)TL_comparators.get()).eval.alloc()).init(var2);

      InventoryItem var5;
      try {
         var5 = this.getBest(var3, var4);
      } finally {
         ((Predicates)TL_predicates.get()).eval.release((Object)var3);
         ((Comparators)TL_comparators.get()).eval.release((Object)var4);
      }

      return var5;
   }

   public InventoryItem getBestEvalRecurse(LuaClosure var1, LuaClosure var2) {
      EvalPredicate var3 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      EvalComparator var4 = ((EvalComparator)((Comparators)TL_comparators.get()).eval.alloc()).init(var2);

      InventoryItem var5;
      try {
         var5 = this.getBestRecurse(var3, var4);
      } finally {
         ((Predicates)TL_predicates.get()).eval.release((Object)var3);
         ((Comparators)TL_comparators.get()).eval.release((Object)var4);
      }

      return var5;
   }

   public InventoryItem getBestEvalArg(LuaClosure var1, LuaClosure var2, Object var3) {
      EvalArgPredicate var4 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var3);
      EvalArgComparator var5 = ((EvalArgComparator)((Comparators)TL_comparators.get()).evalArg.alloc()).init(var2, var3);

      InventoryItem var6;
      try {
         var6 = this.getBest(var4, var5);
      } finally {
         ((Predicates)TL_predicates.get()).evalArg.release((Object)var4);
         ((Comparators)TL_comparators.get()).evalArg.release((Object)var5);
      }

      return var6;
   }

   public InventoryItem getBestEvalArgRecurse(LuaClosure var1, LuaClosure var2, Object var3) {
      EvalArgPredicate var4 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var3);
      EvalArgComparator var5 = ((EvalArgComparator)((Comparators)TL_comparators.get()).evalArg.alloc()).init(var2, var3);

      InventoryItem var6;
      try {
         var6 = this.getBestRecurse(var4, var5);
      } finally {
         ((Predicates)TL_predicates.get()).evalArg.release((Object)var4);
         ((Comparators)TL_comparators.get()).evalArg.release((Object)var5);
      }

      return var6;
   }

   public InventoryItem getBestTypeEval(String var1, LuaClosure var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      EvalComparator var4 = ((EvalComparator)((Comparators)TL_comparators.get()).eval.alloc()).init(var2);

      InventoryItem var5;
      try {
         var5 = this.getBest(var3, var4);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var3);
         ((Comparators)TL_comparators.get()).eval.release((Object)var4);
      }

      return var5;
   }

   public InventoryItem getBestTypeEvalRecurse(String var1, LuaClosure var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      EvalComparator var4 = ((EvalComparator)((Comparators)TL_comparators.get()).eval.alloc()).init(var2);

      InventoryItem var5;
      try {
         var5 = this.getBestRecurse(var3, var4);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var3);
         ((Comparators)TL_comparators.get()).eval.release((Object)var4);
      }

      return var5;
   }

   public InventoryItem getBestTypeEvalArg(String var1, LuaClosure var2, Object var3) {
      TypePredicate var4 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      EvalArgComparator var5 = ((EvalArgComparator)((Comparators)TL_comparators.get()).evalArg.alloc()).init(var2, var3);

      InventoryItem var6;
      try {
         var6 = this.getBest(var4, var5);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var4);
         ((Comparators)TL_comparators.get()).evalArg.release((Object)var5);
      }

      return var6;
   }

   public InventoryItem getBestTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      TypePredicate var4 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      EvalArgComparator var5 = ((EvalArgComparator)((Comparators)TL_comparators.get()).evalArg.alloc()).init(var2, var3);

      InventoryItem var6;
      try {
         var6 = this.getBestRecurse(var4, var5);
      } finally {
         ((Predicates)TL_predicates.get()).type.release((Object)var4);
         ((Comparators)TL_comparators.get()).evalArg.release((Object)var5);
      }

      return var6;
   }

   public InventoryItem getBestCondition(Predicate<InventoryItem> var1) {
      ConditionComparator var2 = (ConditionComparator)((Comparators)TL_comparators.get()).condition.alloc();
      InventoryItem var3 = this.getBest(var1, var2);
      ((Comparators)TL_comparators.get()).condition.release((Object)var2);
      if (var3 != null && var3.getCondition() <= 0) {
         var3 = null;
      }

      return var3;
   }

   public InventoryItem getBestConditionRecurse(Predicate<InventoryItem> var1) {
      ConditionComparator var2 = (ConditionComparator)((Comparators)TL_comparators.get()).condition.alloc();
      InventoryItem var3 = this.getBestRecurse(var1, var2);
      ((Comparators)TL_comparators.get()).condition.release((Object)var2);
      if (var3 != null && var3.getCondition() <= 0) {
         var3 = null;
      }

      return var3;
   }

   public InventoryItem getBestCondition(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      InventoryItem var3 = this.getBestCondition((Predicate)var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public InventoryItem getBestConditionRecurse(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      InventoryItem var3 = this.getBestConditionRecurse((Predicate)var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public InventoryItem getBestConditionEval(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      InventoryItem var3 = this.getBestCondition((Predicate)var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public InventoryItem getBestConditionEvalRecurse(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      InventoryItem var3 = this.getBestConditionRecurse((Predicate)var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public InventoryItem getBestConditionEvalArg(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      InventoryItem var4 = this.getBestCondition((Predicate)var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public InventoryItem getBestConditionEvalArgRecurse(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      InventoryItem var4 = this.getBestConditionRecurse((Predicate)var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstEval(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      InventoryItem var3 = this.getFirst(var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstEvalArg(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirst(var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public boolean containsEval(LuaClosure var1) {
      return this.getFirstEval(var1) != null;
   }

   public boolean containsEvalArg(LuaClosure var1, Object var2) {
      return this.getFirstEvalArg(var1, var2) != null;
   }

   public boolean containsEvalRecurse(LuaClosure var1) {
      return this.getFirstEvalRecurse(var1) != null;
   }

   public boolean containsEvalArgRecurse(LuaClosure var1, Object var2) {
      return this.getFirstEvalArgRecurse(var1, var2) != null;
   }

   public boolean containsTag(String var1) {
      return this.getFirstTag(var1) != null;
   }

   public boolean containsTagEval(String var1, LuaClosure var2) {
      return this.getFirstTagEval(var1, var2) != null;
   }

   public boolean containsTagRecurse(String var1) {
      return this.getFirstTagRecurse(var1) != null;
   }

   public boolean containsTagEvalRecurse(String var1, LuaClosure var2) {
      return this.getFirstTagEvalRecurse(var1, var2) != null;
   }

   public boolean containsTagEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      return this.getFirstTagEvalArgRecurse(var1, var2, var3) != null;
   }

   public boolean containsTypeEvalRecurse(String var1, LuaClosure var2) {
      return this.getFirstTypeEvalRecurse(var1, var2) != null;
   }

   public boolean containsTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      return this.getFirstTypeEvalArgRecurse(var1, var2, var3) != null;
   }

   private static boolean compareType(String var0, String var1) {
      if (var0 != null && var0.contains("/")) {
         int var2 = var0.indexOf(var1);
         if (var2 == -1) {
            return false;
         } else {
            char var3 = var2 > 0 ? var0.charAt(var2 - 1) : 0;
            char var4 = var2 + var1.length() < var0.length() ? var0.charAt(var2 + var1.length()) : 0;
            return var3 == 0 && var4 == '/' || var3 == '/' && var4 == 0 || var3 == '/' && var4 == '/';
         }
      } else {
         return var0.equals(var1);
      }
   }

   private static boolean compareType(String var0, InventoryItem var1) {
      if (var0 != null && var0.indexOf(46) == -1) {
         return compareType(var0, var1.getType());
      } else {
         return compareType(var0, var1.getFullType()) || compareType(var0, var1.getType());
      }
   }

   public InventoryItem getFirst(Predicate<InventoryItem> var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3 == null) {
            this.Items.remove(var2);
            --var2;
         } else if (var1.test(var3)) {
            return var3;
         }
      }

      return null;
   }

   public InventoryItem getFirstRecurse(Predicate<InventoryItem> var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();

      int var3;
      for(var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4 == null) {
            this.Items.remove(var3);
            --var3;
         } else {
            if (var1.test(var4)) {
               ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
               return var4;
            }

            if (var4 instanceof InventoryContainer) {
               var2.add(var4);
            }
         }
      }

      for(var3 = 0; var3 < var2.size(); ++var3) {
         ItemContainer var6 = ((InventoryContainer)var2.get(var3)).getInventory();
         InventoryItem var5 = var6.getFirstRecurse(var1);
         if (var5 != null) {
            ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
            return var5;
         }
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return null;
   }

   public ArrayList<InventoryItem> getSome(Predicate<InventoryItem> var1, int var2, ArrayList<InventoryItem> var3) {
      for(int var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         if (var5 == null) {
            this.Items.remove(var4);
            --var4;
         } else if (var1.test(var5)) {
            var3.add(var5);
            if (var3.size() >= var2) {
               break;
            }
         }
      }

      return var3;
   }

   public ArrayList<InventoryItem> getSomeRecurse(Predicate<InventoryItem> var1, int var2, ArrayList<InventoryItem> var3) {
      InventoryItemList var4 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();

      int var5;
      for(var5 = 0; var5 < this.Items.size(); ++var5) {
         InventoryItem var6 = (InventoryItem)this.Items.get(var5);
         if (var6 == null) {
            this.Items.remove(var5);
            --var5;
         } else {
            if (var1.test(var6)) {
               var3.add(var6);
               if (var3.size() >= var2) {
                  ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
                  return var3;
               }
            }

            if (var6 instanceof InventoryContainer) {
               var4.add(var6);
            }
         }
      }

      for(var5 = 0; var5 < var4.size(); ++var5) {
         ItemContainer var7 = ((InventoryContainer)var4.get(var5)).getInventory();
         var7.getSomeRecurse(var1, var2, var3);
         if (var3.size() >= var2) {
            break;
         }
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var4);
      return var3;
   }

   public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> var1, ArrayList<InventoryItem> var2) {
      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4 == null) {
            this.Items.remove(var3);
            --var3;
         } else if (var1.test(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public ArrayList<InventoryItem> getAllRecurse(Predicate<InventoryItem> var1, ArrayList<InventoryItem> var2) {
      InventoryItemList var3 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();

      int var4;
      for(var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         if (var5 == null) {
            this.Items.remove(var4);
            --var4;
         } else {
            if (var1.test(var5)) {
               var2.add(var5);
            }

            if (var5 instanceof InventoryContainer) {
               var3.add(var5);
            }
         }
      }

      for(var4 = 0; var4 < var3.size(); ++var4) {
         ItemContainer var6 = ((InventoryContainer)var3.get(var4)).getInventory();
         var6.getAllRecurse(var1, var2);
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var3);
      return var2;
   }

   public int getCount(Predicate<InventoryItem> var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAll(var1, var2);
      int var3 = var2.size();
      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return var3;
   }

   public int getCountRecurse(Predicate<InventoryItem> var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAllRecurse(var1, var2);
      int var3 = var2.size();
      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return var3;
   }

   public int getCountTag(String var1) {
      TagPredicate var2 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      int var3 = this.getCount(var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var2);
      return var3;
   }

   public int getCountTagEval(String var1, LuaClosure var2) {
      TagEvalPredicate var3 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      int var4 = this.getCount(var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var3);
      return var4;
   }

   public int getCountTagEvalArg(String var1, LuaClosure var2, Object var3) {
      TagEvalArgPredicate var4 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      int var5 = this.getCount(var4);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var4);
      return var5;
   }

   public int getCountTagRecurse(String var1) {
      TagPredicate var2 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      int var3 = this.getCountRecurse(var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var2);
      return var3;
   }

   public int getCountTagEvalRecurse(String var1, LuaClosure var2) {
      TagEvalPredicate var3 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      int var4 = this.getCountRecurse(var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var3);
      return var4;
   }

   public int getCountTagEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      TagEvalArgPredicate var4 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      int var5 = this.getCountRecurse(var4);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var4);
      return var5;
   }

   public int getCountType(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      int var3 = this.getCount(var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public int getCountTypeEval(String var1, LuaClosure var2) {
      TypeEvalPredicate var3 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      int var4 = this.getCount(var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var3);
      return var4;
   }

   public int getCountTypeEvalArg(String var1, LuaClosure var2, Object var3) {
      TypeEvalArgPredicate var4 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      int var5 = this.getCount(var4);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var4);
      return var5;
   }

   public int getCountTypeRecurse(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      int var3 = this.getCountRecurse(var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public int getCountTypeEvalRecurse(String var1, LuaClosure var2) {
      TypeEvalPredicate var3 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      int var4 = this.getCountRecurse(var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var3);
      return var4;
   }

   public int getCountTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      TypeEvalArgPredicate var4 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      int var5 = this.getCountRecurse(var4);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var4);
      return var5;
   }

   public int getCountEval(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      int var3 = this.getCount(var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public int getCountEvalArg(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      int var4 = this.getCount(var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public int getCountEvalRecurse(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      int var3 = this.getCountRecurse(var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public int getCountEvalArgRecurse(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      int var4 = this.getCountRecurse(var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstCategory(String var1) {
      CategoryPredicate var2 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      InventoryItem var3 = this.getFirst(var2);
      ((Predicates)TL_predicates.get()).category.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstCategoryRecurse(String var1) {
      CategoryPredicate var2 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      InventoryItem var3 = this.getFirstRecurse(var2);
      ((Predicates)TL_predicates.get()).category.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstEvalRecurse(LuaClosure var1) {
      EvalPredicate var2 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      InventoryItem var3 = this.getFirstRecurse(var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstEvalArgRecurse(LuaClosure var1, Object var2) {
      EvalArgPredicate var3 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirstRecurse(var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstTag(String var1) {
      TagPredicate var2 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      InventoryItem var3 = this.getFirst(var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstTagRecurse(String var1) {
      TagPredicate var2 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      InventoryItem var3 = this.getFirstRecurse(var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstTagEval(String var1, LuaClosure var2) {
      TagEvalPredicate var3 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirstRecurse(var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstTagEvalRecurse(String var1, LuaClosure var2) {
      TagEvalPredicate var3 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirstRecurse(var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstTagEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      TagEvalArgPredicate var4 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      InventoryItem var5 = this.getFirstRecurse(var4);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var4);
      return var5;
   }

   public InventoryItem getFirstType(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      InventoryItem var3 = this.getFirst(var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstTypeRecurse(String var1) {
      TypePredicate var2 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      InventoryItem var3 = this.getFirstRecurse(var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var2);
      return var3;
   }

   public InventoryItem getFirstTypeEval(String var1, LuaClosure var2) {
      TypeEvalPredicate var3 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirstRecurse(var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstTypeEvalRecurse(String var1, LuaClosure var2) {
      TypeEvalPredicate var3 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      InventoryItem var4 = this.getFirstRecurse(var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var3);
      return var4;
   }

   public InventoryItem getFirstTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      TypeEvalArgPredicate var4 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      InventoryItem var5 = this.getFirstRecurse(var4);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeCategory(String var1, int var2, ArrayList<InventoryItem> var3) {
      CategoryPredicate var4 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      ArrayList var5 = this.getSome(var4, var2, var3);
      ((Predicates)TL_predicates.get()).category.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeCategoryRecurse(String var1, int var2, ArrayList<InventoryItem> var3) {
      CategoryPredicate var4 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      ArrayList var5 = this.getSomeRecurse(var4, var2, var3);
      ((Predicates)TL_predicates.get()).category.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeTag(String var1, int var2, ArrayList<InventoryItem> var3) {
      TagPredicate var4 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      ArrayList var5 = this.getSome(var4, var2, var3);
      ((Predicates)TL_predicates.get()).tag.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeTagEval(String var1, LuaClosure var2, int var3, ArrayList<InventoryItem> var4) {
      TagEvalPredicate var5 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      ArrayList var6 = this.getSome(var5, var3, var4);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getSomeTagEvalArg(String var1, LuaClosure var2, Object var3, int var4, ArrayList<InventoryItem> var5) {
      TagEvalArgPredicate var6 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var7 = this.getSome(var6, var4, var5);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var6);
      return var7;
   }

   public ArrayList<InventoryItem> getSomeTagRecurse(String var1, int var2, ArrayList<InventoryItem> var3) {
      TagPredicate var4 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      ArrayList var5 = this.getSomeRecurse(var4, var2, var3);
      ((Predicates)TL_predicates.get()).tag.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeTagEvalRecurse(String var1, LuaClosure var2, int var3, ArrayList<InventoryItem> var4) {
      TagEvalPredicate var5 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      ArrayList var6 = this.getSomeRecurse(var5, var3, var4);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(String var1, LuaClosure var2, Object var3, int var4, ArrayList<InventoryItem> var5) {
      TagEvalArgPredicate var6 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var7 = this.getSomeRecurse(var6, var4, var5);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var6);
      return var7;
   }

   public ArrayList<InventoryItem> getSomeType(String var1, int var2, ArrayList<InventoryItem> var3) {
      TypePredicate var4 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      ArrayList var5 = this.getSome(var4, var2, var3);
      ((Predicates)TL_predicates.get()).type.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeTypeEval(String var1, LuaClosure var2, int var3, ArrayList<InventoryItem> var4) {
      TypeEvalPredicate var5 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      ArrayList var6 = this.getSome(var5, var3, var4);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getSomeTypeEvalArg(String var1, LuaClosure var2, Object var3, int var4, ArrayList<InventoryItem> var5) {
      TypeEvalArgPredicate var6 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var7 = this.getSome(var6, var4, var5);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var6);
      return var7;
   }

   public ArrayList<InventoryItem> getSomeTypeRecurse(String var1, int var2, ArrayList<InventoryItem> var3) {
      TypePredicate var4 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      ArrayList var5 = this.getSomeRecurse(var4, var2, var3);
      ((Predicates)TL_predicates.get()).type.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String var1, LuaClosure var2, int var3, ArrayList<InventoryItem> var4) {
      TypeEvalPredicate var5 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      ArrayList var6 = this.getSomeRecurse(var5, var3, var4);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3, int var4, ArrayList<InventoryItem> var5) {
      TypeEvalArgPredicate var6 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var7 = this.getSomeRecurse(var6, var4, var5);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var6);
      return var7;
   }

   public ArrayList<InventoryItem> getSomeEval(LuaClosure var1, int var2, ArrayList<InventoryItem> var3) {
      EvalPredicate var4 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      ArrayList var5 = this.getSome(var4, var2, var3);
      ((Predicates)TL_predicates.get()).eval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure var1, Object var2, int var3, ArrayList<InventoryItem> var4) {
      EvalArgPredicate var5 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      ArrayList var6 = this.getSome(var5, var3, var4);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure var1, int var2, ArrayList<InventoryItem> var3) {
      EvalPredicate var4 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      ArrayList var5 = this.getSomeRecurse(var4, var2, var3);
      ((Predicates)TL_predicates.get()).eval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure var1, Object var2, int var3, ArrayList<InventoryItem> var4) {
      EvalArgPredicate var5 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      ArrayList var6 = this.getSomeRecurse(var5, var3, var4);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getAllCategory(String var1, ArrayList<InventoryItem> var2) {
      CategoryPredicate var3 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      ArrayList var4 = this.getAll(var3, var2);
      ((Predicates)TL_predicates.get()).category.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllCategoryRecurse(String var1, ArrayList<InventoryItem> var2) {
      CategoryPredicate var3 = ((CategoryPredicate)((Predicates)TL_predicates.get()).category.alloc()).init(var1);
      ArrayList var4 = this.getAllRecurse(var3, var2);
      ((Predicates)TL_predicates.get()).category.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllTag(String var1, ArrayList<InventoryItem> var2) {
      TagPredicate var3 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      ArrayList var4 = this.getAll(var3, var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllTagEval(String var1, LuaClosure var2, ArrayList<InventoryItem> var3) {
      if (var3 == null) {
         var3 = new ArrayList();
      }

      TagEvalPredicate var4 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      ArrayList var5 = this.getAll(var4, var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getAllTagEvalArg(String var1, LuaClosure var2, Object var3, ArrayList<InventoryItem> var4) {
      TagEvalArgPredicate var5 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var6 = this.getAll(var5, var4);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getAllTagRecurse(String var1, ArrayList<InventoryItem> var2) {
      TagPredicate var3 = ((TagPredicate)((Predicates)TL_predicates.get()).tag.alloc()).init(var1);
      ArrayList var4 = this.getAllRecurse(var3, var2);
      ((Predicates)TL_predicates.get()).tag.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllTagEvalRecurse(String var1, LuaClosure var2, ArrayList<InventoryItem> var3) {
      TagEvalPredicate var4 = ((TagEvalPredicate)((Predicates)TL_predicates.get()).tagEval.alloc()).init(var1, var2);
      ArrayList var5 = this.getAllRecurse(var4, var3);
      ((Predicates)TL_predicates.get()).tagEval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getAllTagEvalArgRecurse(String var1, LuaClosure var2, Object var3, ArrayList<InventoryItem> var4) {
      TagEvalArgPredicate var5 = ((TagEvalArgPredicate)((Predicates)TL_predicates.get()).tagEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var6 = this.getAllRecurse(var5, var4);
      ((Predicates)TL_predicates.get()).tagEvalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getAllType(String var1, ArrayList<InventoryItem> var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      ArrayList var4 = this.getAll(var3, var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllTypeEval(String var1, LuaClosure var2, ArrayList<InventoryItem> var3) {
      TypeEvalPredicate var4 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      ArrayList var5 = this.getAll(var4, var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getAllTypeEvalArg(String var1, LuaClosure var2, Object var3, ArrayList<InventoryItem> var4) {
      TypeEvalArgPredicate var5 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var6 = this.getAll(var5, var4);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getAllTypeRecurse(String var1, ArrayList<InventoryItem> var2) {
      TypePredicate var3 = ((TypePredicate)((Predicates)TL_predicates.get()).type.alloc()).init(var1);
      ArrayList var4 = this.getAllRecurse(var3, var2);
      ((Predicates)TL_predicates.get()).type.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllTypeEvalRecurse(String var1, LuaClosure var2, ArrayList<InventoryItem> var3) {
      TypeEvalPredicate var4 = ((TypeEvalPredicate)((Predicates)TL_predicates.get()).typeEval.alloc()).init(var1, var2);
      ArrayList var5 = this.getAllRecurse(var4, var3);
      ((Predicates)TL_predicates.get()).typeEval.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3, ArrayList<InventoryItem> var4) {
      TypeEvalArgPredicate var5 = ((TypeEvalArgPredicate)((Predicates)TL_predicates.get()).typeEvalArg.alloc()).init(var1, var2, var3);
      ArrayList var6 = this.getAllRecurse(var5, var4);
      ((Predicates)TL_predicates.get()).typeEvalArg.release((Object)var5);
      return var6;
   }

   public ArrayList<InventoryItem> getAllEval(LuaClosure var1, ArrayList<InventoryItem> var2) {
      EvalPredicate var3 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      ArrayList var4 = this.getAll(var3, var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllEvalArg(LuaClosure var1, Object var2, ArrayList<InventoryItem> var3) {
      EvalArgPredicate var4 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      ArrayList var5 = this.getAll(var4, var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure var1, ArrayList<InventoryItem> var2) {
      EvalPredicate var3 = ((EvalPredicate)((Predicates)TL_predicates.get()).eval.alloc()).init(var1);
      ArrayList var4 = this.getAllRecurse(var3, var2);
      ((Predicates)TL_predicates.get()).eval.release((Object)var3);
      return var4;
   }

   public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure var1, Object var2, ArrayList<InventoryItem> var3) {
      EvalArgPredicate var4 = ((EvalArgPredicate)((Predicates)TL_predicates.get()).evalArg.alloc()).init(var1, var2);
      ArrayList var5 = this.getAllRecurse(var4, var3);
      ((Predicates)TL_predicates.get()).evalArg.release((Object)var4);
      return var5;
   }

   public ArrayList<InventoryItem> getSomeCategory(String var1, int var2) {
      return this.getSomeCategory(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeEval(LuaClosure var1, int var2) {
      return this.getSomeEval(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure var1, Object var2, int var3) {
      return this.getSomeEvalArg(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTypeEval(String var1, LuaClosure var2, int var3) {
      return this.getSomeTypeEval(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTypeEvalArg(String var1, LuaClosure var2, Object var3, int var4) {
      return this.getSomeTypeEvalArg(var1, var2, var3, var4, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure var1, int var2) {
      return this.getSomeEvalRecurse(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure var1, Object var2, int var3) {
      return this.getSomeEvalArgRecurse(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTag(String var1, int var2) {
      return this.getSomeTag(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTagRecurse(String var1, int var2) {
      return this.getSomeTagRecurse(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTagEvalRecurse(String var1, LuaClosure var2, int var3) {
      return this.getSomeTagEvalRecurse(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(String var1, LuaClosure var2, Object var3, int var4) {
      return this.getSomeTagEvalArgRecurse(var1, var2, var3, var4, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeType(String var1, int var2) {
      return this.getSomeType(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTypeRecurse(String var1, int var2) {
      return this.getSomeTypeRecurse(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String var1, LuaClosure var2, int var3) {
      return this.getSomeTypeEvalRecurse(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3, int var4) {
      return this.getSomeTypeEvalArgRecurse(var1, var2, var3, var4, new ArrayList());
   }

   public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> var1) {
      return this.getAll(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllCategory(String var1) {
      return this.getAllCategory(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllEval(LuaClosure var1) {
      return this.getAllEval(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllEvalArg(LuaClosure var1, Object var2) {
      return this.getAllEvalArg(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTagEval(String var1, LuaClosure var2) {
      return this.getAllTagEval(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTagEvalArg(String var1, LuaClosure var2, Object var3) {
      return this.getAllTagEvalArg(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTypeEval(String var1, LuaClosure var2) {
      return this.getAllTypeEval(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTypeEvalArg(String var1, LuaClosure var2, Object var3) {
      return this.getAllTypeEvalArg(var1, var2, var3, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure var1) {
      return this.getAllEvalRecurse(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure var1, Object var2) {
      return this.getAllEvalArgRecurse(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllType(String var1) {
      return this.getAllType(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTypeRecurse(String var1) {
      return this.getAllTypeRecurse(var1, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTypeEvalRecurse(String var1, LuaClosure var2) {
      return this.getAllTypeEvalRecurse(var1, var2, new ArrayList());
   }

   public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String var1, LuaClosure var2, Object var3) {
      return this.getAllTypeEvalArgRecurse(var1, var2, var3, new ArrayList());
   }

   public InventoryItem FindAndReturnCategory(String var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.getCategory().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public ArrayList<InventoryItem> FindAndReturn(String var1, int var2) {
      return this.getSomeType(var1, var2);
   }

   public InventoryItem FindAndReturn(String var1, ArrayList<InventoryItem> var2) {
      if (var1 == null) {
         return null;
      } else {
         for(int var3 = 0; var3 < this.Items.size(); ++var3) {
            InventoryItem var4 = (InventoryItem)this.Items.get(var3);
            if (var4.type != null && compareType(var1, var4) && !var2.contains(var4)) {
               return var4;
            }
         }

         return null;
      }
   }

   public InventoryItem FindAndReturn(String var1) {
      return this.getFirstType(var1);
   }

   public ArrayList<InventoryItem> FindAll(String var1) {
      return this.getAllType(var1);
   }

   public InventoryItem FindAndReturnStack(String var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (compareType(var1, var3)) {
            InventoryItem var4 = InventoryItemFactory.CreateItem(var3.module + "." + var1);
            if (var3.CanStack(var4)) {
               return var3;
            }
         }
      }

      return null;
   }

   public InventoryItem FindAndReturnStack(InventoryItem var1) {
      String var2 = var1.type;

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4.type == null) {
            if (var2 != null) {
               continue;
            }
         } else if (!var4.type.equals(var2)) {
            continue;
         }

         if (var4.CanStack(var1)) {
            return var4;
         }
      }

      return null;
   }

   public boolean HasType(ItemType var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.cat == var1) {
            return true;
         }
      }

      return false;
   }

   public void Remove(InventoryItem var1) {
      if (this.getCharacter() != null) {
         this.getCharacter().removeFromHands(var1);
      }

      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3 == var1) {
            var1.OnBeforeRemoveFromContainer(this);
            this.Items.remove(var1);
            var1.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
               this.dirty = true;
            }

            if (this.parent instanceof IsoDeadBody) {
               ((IsoDeadBody)this.parent).checkClothing(var1);
            }

            if (this.parent instanceof IsoMannequin) {
               ((IsoMannequin)this.parent).checkClothing(var1);
            }

            return;
         }
      }

   }

   public void DoRemoveItem(InventoryItem var1) {
      this.drawDirty = true;
      if (this.parent != null) {
         this.dirty = true;
      }

      this.Items.remove(var1);
      var1.container = null;
      if (this.parent instanceof IsoDeadBody) {
         ((IsoDeadBody)this.parent).checkClothing(var1);
      }

      if (this.parent instanceof IsoMannequin) {
         ((IsoMannequin)this.parent).checkClothing(var1);
      }

      if (this.parent instanceof IsoFeedingTrough) {
         ((IsoFeedingTrough)this.parent).onRemoveFood();
      }

   }

   public void Remove(String var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.type.equals(var1)) {
            if (var3.uses > 1) {
               --var3.uses;
            } else {
               this.Items.remove(var3);
            }

            var3.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
               this.dirty = true;
            }

            return;
         }
      }

   }

   public InventoryItem Remove(ItemType var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.cat == var1) {
            this.Items.remove(var3);
            var3.container = null;
            this.drawDirty = true;
            this.dirty = true;
            if (this.parent != null) {
               this.dirty = true;
            }

            return var3;
         }
      }

      return null;
   }

   public InventoryItem Find(String var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.type.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public InventoryItem Find(ItemType var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.cat == var1) {
            return var3;
         }
      }

      return null;
   }

   public ArrayList<InventoryItem> RemoveAll(String var1) {
      return this.RemoveAll(var1, this.Items.size());
   }

   public ArrayList<InventoryItem> RemoveAll(String var1, int var2) {
      this.drawDirty = true;
      if (this.parent != null) {
         this.dirty = true;
      }

      ArrayList var3 = new ArrayList();

      InventoryItem var5;
      for(int var4 = 0; var4 < this.Items.size(); ++var4) {
         var5 = (InventoryItem)this.Items.get(var4);
         if (var5.type.equals(var1) || var5.fullType.equals(var1)) {
            var5.container = null;
            var3.add(var5);
            this.dirty = true;
            if (var3.size() >= var2) {
               break;
            }
         }
      }

      Iterator var6 = var3.iterator();

      while(var6.hasNext()) {
         var5 = (InventoryItem)var6.next();
         this.Items.remove(var5);
      }

      return var3;
   }

   public InventoryItem RemoveOneOf(String var1, boolean var2) {
      this.drawDirty = true;
      if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
         this.dirty = true;
      }

      int var3;
      InventoryItem var4;
      for(var3 = 0; var3 < this.Items.size(); ++var3) {
         var4 = (InventoryItem)this.Items.get(var3);
         if (var4.getFullType().equals(var1) || var4.type.equals(var1)) {
            if (var4.uses > 1) {
               --var4.uses;
            } else {
               var4.container = null;
               this.Items.remove(var4);
            }

            this.dirty = true;
            return var4;
         }
      }

      if (var2) {
         for(var3 = 0; var3 < this.Items.size(); ++var3) {
            var4 = (InventoryItem)this.Items.get(var3);
            if (var4 instanceof InventoryContainer && ((InventoryContainer)var4).getItemContainer() != null && ((InventoryContainer)var4).getItemContainer().RemoveOneOf(var1, var2) != null) {
               return var4;
            }
         }
      }

      return null;
   }

   public void RemoveOneOf(String var1) {
      this.RemoveOneOf(var1, true);
   }

   /** @deprecated */
   public int getWeight() {
      if (this.parent instanceof IsoPlayer && ((IsoPlayer)this.parent).isGhostMode()) {
         return 0;
      } else {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.Items.size(); ++var2) {
            InventoryItem var3 = (InventoryItem)this.Items.get(var2);
            var1 += var3.ActualWeight * (float)var3.uses;
         }

         return (int)(var1 * ((float)this.weightReduction / 0.01F));
      }
   }

   public float getContentsWeight() {
      float var1 = 0.0F;

      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         var1 += var3.getUnequippedWeight();
      }

      return var1;
   }

   public float getMaxWeight() {
      if (this.parent instanceof IsoGameCharacter) {
         return (float)((IsoGameCharacter)this.parent).getMaxWeight();
      } else if (this.parent instanceof BaseVehicle) {
         return (float)Math.min(this.Capacity, this.MAX_CAPACITY_VEHICLE);
      } else {
         return this.containingItem != null && this.containingItem instanceof InventoryItem ? (float)Math.min(this.Capacity, this.MAX_CAPACITY_BAG) : (float)Math.min(this.Capacity, this.MAX_CAPACITY);
      }
   }

   public float getCapacityWeight() {
      IsoObject var2 = this.parent;
      if (var2 instanceof IsoPlayer var1) {
         if (Core.bDebug && var1.isGhostMode() || !var1.isAccessLevel("None") && var1.isUnlimitedCarry()) {
            return 0.0F;
         }

         if (var1.isUnlimitedCarry()) {
            return 0.0F;
         }
      }

      var2 = this.parent;
      if (var2 instanceof IsoGameCharacter var4) {
         return var4.getInventoryWeight();
      } else {
         var2 = this.parent;
         if (var2 instanceof IsoDeadBody var3) {
            return var3.getInventoryWeight();
         } else {
            return this.getContentsWeight();
         }
      }
   }

   public boolean isEmpty() {
      return this.Items == null || this.Items.isEmpty();
   }

   public boolean isMicrowave() {
      return "microwave".equals(this.getType());
   }

   private static boolean isSquareInRoom(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else {
         return var0.getRoom() != null;
      }
   }

   private static boolean isSquarePowered(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else {
         boolean var1 = IsoWorld.instance.isHydroPowerOn();
         if (var1 && var0.getRoom() != null) {
            return true;
         } else if (var0.haveElectricity()) {
            return true;
         } else {
            if (var1 && var0.getRoom() == null) {
               IsoGridSquare var2 = var0.nav[IsoDirections.N.index()];
               IsoGridSquare var3 = var0.nav[IsoDirections.S.index()];
               IsoGridSquare var4 = var0.nav[IsoDirections.W.index()];
               IsoGridSquare var5 = var0.nav[IsoDirections.E.index()];
               if (isSquareInRoom(var2) || isSquareInRoom(var3) || isSquareInRoom(var4) || isSquareInRoom(var5)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public boolean isPowered() {
      return isObjectPowered(this.parent);
   }

   public static boolean isObjectPowered(IsoObject var0) {
      if (var0 != null && var0.getObjectIndex() != -1) {
         ArrayList var1 = (ArrayList)s_tempObjects.get();
         var0.getSpriteGridObjects(var1);
         IsoLightSwitch var2;
         if (var1.isEmpty()) {
            if (var0.getProperties() != null && var0.getProperties().Is("streetlight")) {
               if (var0 instanceof IsoLightSwitch) {
                  var2 = (IsoLightSwitch)var0;
                  if (!var2.isActivated()) {
                     return false;
                  }
               }

               return GameTime.getInstance().getNight() >= 0.5F && IsoWorld.instance.isHydroPowerOn();
            } else {
               IsoGridSquare var6 = var0.getSquare();
               return isSquarePowered(var6);
            }
         } else {
            var2 = null;

            int var3;
            IsoObject var4;
            for(var3 = 0; var3 < var1.size(); ++var3) {
               var4 = (IsoObject)var1.get(var3);
               if (var4 instanceof IsoLightSwitch) {
                  IsoLightSwitch var5 = (IsoLightSwitch)var4;
                  var2 = var5;
                  break;
               }
            }

            for(var3 = 0; var3 < var1.size(); ++var3) {
               var4 = (IsoObject)var1.get(var3);
               if (var4.getProperties() != null && var4.getProperties().Is("streetlight")) {
                  if (var2 != null && !var2.isActivated()) {
                     return false;
                  }

                  return GameTime.getInstance().getNight() >= 0.5F && IsoWorld.instance.isHydroPowerOn();
               }

               IsoGridSquare var7 = var4.getSquare();
               if (isSquarePowered(var7)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public float getTemprature() {
      if (this.customTemperature != 0.0F) {
         return this.customTemperature;
      } else {
         boolean var1 = false;
         if (this.getParent() != null && this.getParent().getSprite() != null) {
            var1 = this.getParent().getSprite().getProperties().Is("IsFridge");
         }

         if (this.isPowered()) {
            if (this.type.equals("fridge") || this.type.equals("freezer") || var1) {
               return 0.2F;
            }

            if ((this.isStove() || "microwave".equals(this.type)) && this.parent instanceof IsoStove) {
               return ((IsoStove)this.parent).getCurrentTemperature() / 100.0F;
            }
         }

         if (this.parent instanceof IsoBarbecue) {
            return ((IsoBarbecue)this.parent).getTemperature();
         } else if (this.parent instanceof IsoFireplace) {
            return ((IsoFireplace)this.parent).getTemperature();
         } else if ((this.type.equals("fridge") || this.type.equals("freezer") || var1) && (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) == (float)SandboxOptions.instance.getElecShutModifier() && GameTime.instance.getTimeOfDay() < 13.0F) {
            float var2 = (GameTime.instance.getTimeOfDay() - 7.0F) / 6.0F;
            return GameTime.instance.Lerp(0.2F, 1.0F, var2);
         } else {
            return 1.0F;
         }
      }
   }

   public boolean isTemperatureChanging() {
      return this.parent instanceof IsoStove ? ((IsoStove)this.parent).isTemperatureChanging() : false;
   }

   public ArrayList<InventoryItem> save(ByteBuffer var1, IsoGameCharacter var2) throws IOException {
      GameWindow.WriteString(var1, this.type);
      var1.put((byte)(this.bExplored ? 1 : 0));
      ArrayList var3 = CompressIdenticalItems.save(var1, this.Items, (IsoGameCharacter)null);
      var1.put((byte)(this.isHasBeenLooted() ? 1 : 0));
      var1.putInt(this.Capacity);
      return var3;
   }

   public ArrayList<InventoryItem> save(ByteBuffer var1) throws IOException {
      return this.save(var1, (IsoGameCharacter)null);
   }

   public ArrayList<InventoryItem> load(ByteBuffer var1, int var2) throws IOException {
      this.type = GameWindow.ReadString(var1);
      this.bExplored = var1.get() == 1;
      ArrayList var3 = CompressIdenticalItems.load(var1, var2, this.Items, this.IncludingObsoleteItems);

      for(int var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         var5.container = this;
      }

      this.setHasBeenLooted(var1.get() == 1);
      this.Capacity = var1.getInt();
      this.dirty = false;
      return var3;
   }

   public boolean isDrawDirty() {
      return this.drawDirty;
   }

   public void setDrawDirty(boolean var1) {
      this.drawDirty = var1;
   }

   public InventoryItem getBestWeapon(SurvivorDesc var1) {
      InventoryItem var2 = null;
      float var3 = -1.0E7F;

      for(int var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         if (var5 instanceof HandWeapon) {
            float var6 = var5.getScore(var1);
            if (var6 >= var3) {
               var3 = var6;
               var2 = var5;
            }
         }
      }

      return var2;
   }

   public InventoryItem getBestWeapon() {
      InventoryItem var1 = null;
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4 instanceof HandWeapon) {
            float var5 = var4.getScore((SurvivorDesc)null);
            if (var5 >= var2) {
               var2 = var5;
               var1 = var4;
            }
         }
      }

      return var1;
   }

   public float getTotalFoodScore(SurvivorDesc var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4 instanceof Food) {
            var2 += var4.getScore(var1);
         }
      }

      return var2;
   }

   public float getTotalWeaponScore(SurvivorDesc var1) {
      float var2 = 0.0F;

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4 instanceof HandWeapon) {
            var2 += var4.getScore(var1);
         }
      }

      return var2;
   }

   public InventoryItem getBestFood(SurvivorDesc var1) {
      InventoryItem var2 = null;
      float var3 = 0.0F;

      for(int var4 = 0; var4 < this.Items.size(); ++var4) {
         InventoryItem var5 = (InventoryItem)this.Items.get(var4);
         if (var5 instanceof Food) {
            float var6 = var5.getScore(var1);
            if (((Food)var5).isbDangerousUncooked() && !var5.isCooked()) {
               var6 *= 0.2F;
            }

            if (((Food)var5).Age > (float)var5.OffAge) {
               var6 *= 0.2F;
            }

            if (var6 >= var3) {
               var3 = var6;
               var2 = var5;
            }
         }
      }

      return var2;
   }

   public InventoryItem getBestBandage(SurvivorDesc var1) {
      InventoryItem var2 = null;

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4.isCanBandage()) {
            var2 = var4;
            break;
         }
      }

      return var2;
   }

   public int getNumItems(String var1) {
      int var2 = 0;
      int var3;
      InventoryItem var4;
      if (var1.contains("Type:")) {
         for(var3 = 0; var3 < this.Items.size(); ++var3) {
            var4 = (InventoryItem)this.Items.get(var3);
            if (var4 instanceof Food && var1.contains("Food")) {
               var2 += var4.uses;
            }

            if (var4 instanceof HandWeapon && var1.contains("Weapon")) {
               var2 += var4.uses;
            }
         }
      } else {
         for(var3 = 0; var3 < this.Items.size(); ++var3) {
            var4 = (InventoryItem)this.Items.get(var3);
            if (var4.type.equals(var1)) {
               var2 += var4.uses;
            }
         }
      }

      return var2;
   }

   public boolean isActive() {
      return this.active;
   }

   public void setActive(boolean var1) {
      this.active = var1;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void setDirty(boolean var1) {
      this.dirty = var1;
   }

   public boolean isIsDevice() {
      return this.IsDevice;
   }

   public void setIsDevice(boolean var1) {
      this.IsDevice = var1;
   }

   public float getAgeFactor() {
      return this.ageFactor;
   }

   public void setAgeFactor(float var1) {
      this.ageFactor = var1;
   }

   public float getCookingFactor() {
      return this.CookingFactor;
   }

   public void setCookingFactor(float var1) {
      this.CookingFactor = var1;
   }

   public ArrayList<InventoryItem> getItems() {
      return this.Items;
   }

   public void setItems(ArrayList<InventoryItem> var1) {
      this.Items = var1;
   }

   public IsoObject getParent() {
      return this.parent;
   }

   public void setParent(IsoObject var1) {
      this.parent = var1;
   }

   public IsoGridSquare getSourceGrid() {
      return this.SourceGrid;
   }

   public void setSourceGrid(IsoGridSquare var1) {
      this.SourceGrid = var1;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String var1) {
      this.type = var1;
   }

   public void clear() {
      this.Items.clear();
      this.dirty = true;
      this.drawDirty = true;
   }

   public int getWaterContainerCount() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.hasComponent(ComponentType.FluidContainer)) {
            ++var1;
         }
      }

      return var1;
   }

   public InventoryItem FindWaterSource() {
      for(int var1 = 0; var1 < this.Items.size(); ++var1) {
         InventoryItem var2 = (InventoryItem)this.Items.get(var1);
         if (var2.isWaterSource()) {
            if (!(var2 instanceof Drainable)) {
               return var2;
            }

            if (var2.getCurrentUses() > 0) {
               return var2;
            }
         }
      }

      return null;
   }

   public ArrayList<InventoryItem> getAllWaterFillables() {
      tempList.clear();

      for(int var1 = 0; var1 < this.Items.size(); ++var1) {
         InventoryItem var2 = (InventoryItem)this.Items.get(var1);
         if (var2.hasComponent(ComponentType.FluidContainer) && var2.getFluidContainer().isEmpty()) {
            tempList.add(var2);
         }
      }

      return tempList;
   }

   public InventoryItem getFirstWaterFluidSources(boolean var1) {
      ArrayList var2 = this.getAllWaterFluidSources(var1);
      return var2.isEmpty() ? null : (InventoryItem)var2.get(0);
   }

   public InventoryItem getFirstWaterFluidSources(boolean var1, boolean var2) {
      ArrayList var3 = this.getAllWaterFluidSources(var1);
      if (var2) {
         for(int var4 = 0; var4 < var3.size(); ++var4) {
            InventoryItem var5 = (InventoryItem)var3.get(var4);
            if (var5.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater")) {
               return var5;
            }
         }
      }

      return var3.isEmpty() ? null : (InventoryItem)var3.get(0);
   }

   public InventoryItem getFirstFluidContainer(String var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.hasComponent(ComponentType.FluidContainer)) {
            if (var3.getFluidContainer().isEmpty()) {
               return var3;
            }

            if (!StringUtils.isNullOrEmpty(var1) && var3.getFluidContainer().getPrimaryFluid() != null && var3.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(var1)) {
               return var3;
            }
         }
      }

      return null;
   }

   public ArrayList<InventoryItem> getAvailableFluidContainer(String var1) {
      tempList.clear();

      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.hasComponent(ComponentType.FluidContainer)) {
            if (var3.getFluidContainer().isEmpty()) {
               tempList.add(var3);
            }

            if (!StringUtils.isNullOrEmpty(var1) && var3.getFluidContainer().getPrimaryFluid() != null && var3.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(var1) && !var3.getFluidContainer().isFull()) {
               tempList.add(var3);
            }
         }
      }

      return tempList;
   }

   public float getAvailableFluidContainersCapacity(String var1) {
      float var2 = 0.0F;
      ArrayList var3 = this.getAvailableFluidContainer(var1);

      InventoryItem var5;
      for(Iterator var4 = var3.iterator(); var4.hasNext(); var2 += var5.getFluidContainer().getFreeCapacity()) {
         var5 = (InventoryItem)var4.next();
      }

      return var2;
   }

   public InventoryItem getFirstAvailableFluidContainer(String var1) {
      ArrayList var2 = this.getAvailableFluidContainer(var1);
      return var2.isEmpty() ? null : (InventoryItem)var2.get(0);
   }

   public ArrayList<InventoryItem> getAllWaterFluidSources(boolean var1) {
      ArrayList var2 = new ArrayList();

      for(int var3 = 0; var3 < this.Items.size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.Items.get(var3);
         if (var4.hasComponent(ComponentType.FluidContainer) && !var4.getFluidContainer().isEmpty() && (var4.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Water") || var4.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CarbonatedWater") || var1 && var4.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater"))) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public InventoryItem getFirstCleaningFluidSources() {
      ArrayList var1 = this.getAllCleaningFluidSources();
      return var1.isEmpty() ? null : (InventoryItem)var1.get(0);
   }

   public ArrayList<InventoryItem> getAllCleaningFluidSources() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.hasComponent(ComponentType.FluidContainer) && !var3.getFluidContainer().isEmpty() && (var3.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Bleach") || var3.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CleaningLiquid"))) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public int getItemCount(String var1) {
      return this.getCountType(var1);
   }

   public int getItemCountRecurse(String var1) {
      return this.getCountTypeRecurse(var1);
   }

   public int getItemCount(String var1, boolean var2) {
      return var2 ? this.getCountTypeRecurse(var1) : this.getCountType(var1);
   }

   private static int getUses(InventoryItemList var0) {
      int var1 = 0;

      for(int var2 = 0; var2 < var0.size(); ++var2) {
         DrainableComboItem var3 = (DrainableComboItem)Type.tryCastTo((InventoryItem)var0.get(var2), DrainableComboItem.class);
         if (var3 != null) {
            var1 += var3.getCurrentUses();
         } else {
            ++var1;
         }
      }

      return var1;
   }

   public int getUsesRecurse(Predicate<InventoryItem> var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAllRecurse(var1, var2);
      int var3 = getUses(var2);
      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return var3;
   }

   public int getUsesType(String var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAllType(var1, var2);
      int var3 = getUses(var2);
      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return var3;
   }

   public int getUsesTypeRecurse(String var1) {
      InventoryItemList var2 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      this.getAllTypeRecurse(var1, var2);
      int var3 = getUses(var2);
      ((InventoryItemListPool)TL_itemListPool.get()).release(var2);
      return var3;
   }

   public int getWeightReduction() {
      return this.weightReduction;
   }

   public void setWeightReduction(int var1) {
      var1 = Math.min(var1, 100);
      var1 = Math.max(var1, 0);
      this.weightReduction = var1;
   }

   public void removeAllItems() {
      this.drawDirty = true;
      if (this.parent != null) {
         this.dirty = true;
      }

      for(int var1 = 0; var1 < this.Items.size(); ++var1) {
         InventoryItem var2 = (InventoryItem)this.Items.get(var1);
         var2.container = null;
      }

      this.Items.clear();
      if (this.parent instanceof IsoDeadBody) {
         ((IsoDeadBody)this.parent).checkClothing((InventoryItem)null);
      }

      if (this.parent instanceof IsoMannequin) {
         ((IsoMannequin)this.parent).checkClothing((InventoryItem)null);
      }

   }

   public boolean containsRecursive(InventoryItem var1) {
      for(int var2 = 0; var2 < this.getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.getItems().get(var2);
         if (var3 == var1) {
            return true;
         }

         if (var3 instanceof InventoryContainer && ((InventoryContainer)var3).getInventory().containsRecursive(var1)) {
            return true;
         }
      }

      return false;
   }

   public int getItemCountFromTypeRecurse(String var1) {
      int var2 = 0;

      for(int var3 = 0; var3 < this.getItems().size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.getItems().get(var3);
         if (var4.getFullType().equals(var1)) {
            ++var2;
         }

         if (var4 instanceof InventoryContainer) {
            int var5 = ((InventoryContainer)var4).getInventory().getItemCountFromTypeRecurse(var1);
            var2 += var5;
         }
      }

      return var2;
   }

   public float getCustomTemperature() {
      return this.customTemperature;
   }

   public void setCustomTemperature(float var1) {
      this.customTemperature = var1;
   }

   public InventoryItem getItemFromType(String var1, IsoGameCharacter var2, boolean var3, boolean var4, boolean var5) {
      InventoryItemList var6 = (InventoryItemList)((InventoryItemListPool)TL_itemListPool.get()).alloc();
      if (var1.contains(".")) {
         var1 = var1.split("\\.")[1];
      }

      int var7;
      for(var7 = 0; var7 < this.getItems().size(); ++var7) {
         InventoryItem var8 = (InventoryItem)this.getItems().get(var7);
         if (!var8.getFullType().equals(var1) && !var8.getType().equals(var1)) {
            if (var5 && var8 instanceof InventoryContainer && ((InventoryContainer)var8).getInventory() != null && !var6.contains(var8)) {
               var6.add(var8);
            }
         } else if ((!var3 || var2 == null || !var2.isEquippedClothing(var8)) && this.testBroken(var4, var8)) {
            ((InventoryItemListPool)TL_itemListPool.get()).release(var6);
            return var8;
         }
      }

      for(var7 = 0; var7 < var6.size(); ++var7) {
         ItemContainer var10 = ((InventoryContainer)var6.get(var7)).getInventory();
         InventoryItem var9 = var10.getItemFromType(var1, var2, var3, var4, var5);
         if (var9 != null) {
            ((InventoryItemListPool)TL_itemListPool.get()).release(var6);
            return var9;
         }
      }

      ((InventoryItemListPool)TL_itemListPool.get()).release(var6);
      return null;
   }

   public InventoryItem getItemFromType(String var1, boolean var2, boolean var3) {
      return this.getItemFromType(var1, (IsoGameCharacter)null, false, var2, var3);
   }

   public InventoryItem getItemFromType(String var1) {
      return this.getFirstType(var1);
   }

   public ArrayList<InventoryItem> getItemsFromType(String var1) {
      return this.getAllType(var1);
   }

   public ArrayList<InventoryItem> getItemsFromFullType(String var1) {
      return var1 != null && var1.contains(".") ? this.getAllType(var1) : new ArrayList();
   }

   public ArrayList<InventoryItem> getItemsFromFullType(String var1, boolean var2) {
      if (var1 != null && var1.contains(".")) {
         return var2 ? this.getAllTypeRecurse(var1) : this.getAllType(var1);
      } else {
         return new ArrayList();
      }
   }

   public ArrayList<InventoryItem> getItemsFromType(String var1, boolean var2) {
      return var2 ? this.getAllTypeRecurse(var1) : this.getAllType(var1);
   }

   public ArrayList<InventoryItem> getItemsFromCategory(String var1) {
      return this.getAllCategory(var1);
   }

   public void requestSync() {
      if (GameClient.bClient && (this.parent == null || this.parent.square == null || this.parent.square.chunk == null)) {
         ;
      }
   }

   public void requestServerItemsForContainer() {
      if (this.parent != null && this.parent.square != null) {
         INetworkPacket.send(PacketTypes.PacketType.RequestItemsForContainer, this);
      }
   }

   public InventoryItem getItemWithIDRecursiv(int var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.id == var1) {
            return var3;
         }

         if (var3 instanceof InventoryContainer && ((InventoryContainer)var3).getItemContainer() != null && !((InventoryContainer)var3).getItemContainer().getItems().isEmpty()) {
            var3 = ((InventoryContainer)var3).getItemContainer().getItemWithIDRecursiv(var1);
            if (var3 != null) {
               return var3;
            }
         }
      }

      return null;
   }

   public InventoryItem getItemWithID(int var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.id == var1) {
            return var3;
         }
      }

      return null;
   }

   public boolean removeItemWithID(int var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.id == var1) {
            this.Remove(var3);
            return true;
         }
      }

      return false;
   }

   public boolean containsID(int var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.id == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean removeItemWithIDRecurse(int var1) {
      for(int var2 = 0; var2 < this.Items.size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.Items.get(var2);
         if (var3.id == var1) {
            this.Remove(var3);
            return true;
         }

         if (var3 instanceof InventoryContainer && ((InventoryContainer)var3).getInventory().removeItemWithIDRecurse(var1)) {
            return true;
         }
      }

      return false;
   }

   public boolean isHasBeenLooted() {
      return this.hasBeenLooted;
   }

   public void setHasBeenLooted(boolean var1) {
      this.hasBeenLooted = var1;
   }

   public String getOpenSound() {
      return this.openSound;
   }

   public void setOpenSound(String var1) {
      this.openSound = var1;
   }

   public String getCloseSound() {
      return this.closeSound;
   }

   public void setCloseSound(String var1) {
      this.closeSound = var1;
   }

   public String getPutSound() {
      return this.putSound;
   }

   public void setPutSound(String var1) {
      this.putSound = var1;
   }

   public InventoryItem haveThisKeyId(int var1) {
      for(int var2 = 0; var2 < this.getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.getItems().get(var2);
         if (var3 instanceof Key var4) {
            if (var4.getKeyId() == var1) {
               return var4;
            }
         } else if ((var3.getType().equals("KeyRing") || var3.hasTag("KeyRing")) && ((InventoryContainer)var3).getInventory().haveThisKeyId(var1) != null) {
            return ((InventoryContainer)var3).getInventory().haveThisKeyId(var1);
         }
      }

      return null;
   }

   public String getOnlyAcceptCategory() {
      return this.OnlyAcceptCategory;
   }

   public void setOnlyAcceptCategory(String var1) {
      this.OnlyAcceptCategory = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getAcceptItemFunction() {
      return this.AcceptItemFunction;
   }

   public void setAcceptItemFunction(String var1) {
      this.AcceptItemFunction = StringUtils.discardNullOrWhitespace(var1);
   }

   public String toString() {
      String var10000 = this.getType();
      return "ItemContainer:[type:" + var10000 + ", parent:" + this.getParent() + "]";
   }

   public IsoGameCharacter getCharacter() {
      if (this.getParent() instanceof IsoGameCharacter) {
         return (IsoGameCharacter)this.getParent();
      } else {
         return this.containingItem != null && this.containingItem.getContainer() != null ? this.containingItem.getContainer().getCharacter() : null;
      }
   }

   public void emptyIt() {
      this.Items = new ArrayList();
   }

   public LinkedHashMap<String, InventoryItem> getItems4Admin() {
      LinkedHashMap var1 = new LinkedHashMap();

      for(int var2 = 0; var2 < this.getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.getItems().get(var2);
         var3.setCount(1);
         if (var3.getCat() != ItemType.Drainable && var3.getCat() != ItemType.Weapon && var1.get(var3.getFullType()) != null && !(var3 instanceof InventoryContainer)) {
            ((InventoryItem)var1.get(var3.getFullType())).setCount(((InventoryItem)var1.get(var3.getFullType())).getCount() + 1);
         } else if (var1.get(var3.getFullType()) != null) {
            var1.put(var3.getFullType() + Rand.Next(100000), var3);
         } else {
            var1.put(var3.getFullType(), var3);
         }
      }

      return var1;
   }

   public ArrayList<InventoryItem> getAllFoodsForAnimals() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.getItems().size(); ++var2) {
         InventoryItem var3 = (InventoryItem)this.getItems().get(var2);
         if (var3.isAnimalFeed() || var3 instanceof Food && (((Food)var3).getFoodType() != null && (((Food)var3).getFoodType().equals("Fruits") || ((Food)var3).getFoodType().equals("Vegetables")) || ((Food)var3).getMilkType() != null) && ((Food)var3).getHungerChange() < 0.0F && !((Food)var3).isRotten() && !((Food)var3).isSpice()) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public LinkedHashMap<String, InventoryItem> getAllItems(LinkedHashMap<String, InventoryItem> var1, boolean var2) {
      if (var1 == null) {
         var1 = new LinkedHashMap();
      }

      for(int var3 = 0; var3 < this.getItems().size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.getItems().get(var3);
         if (var2) {
            var4.setWorker("inInv");
         }

         var4.setCount(1);
         if (var4.getCat() != ItemType.Drainable && var4.getCat() != ItemType.Weapon && var1.get(var4.getFullType()) != null) {
            ((InventoryItem)var1.get(var4.getFullType())).setCount(((InventoryItem)var1.get(var4.getFullType())).getCount() + 1);
         } else if (var1.get(var4.getFullType()) != null) {
            var1.put(var4.getFullType() + Rand.Next(100000), var4);
         } else {
            var1.put(var4.getFullType(), var4);
         }

         if (var4 instanceof InventoryContainer && ((InventoryContainer)var4).getItemContainer() != null && !((InventoryContainer)var4).getItemContainer().getItems().isEmpty()) {
            var1 = ((InventoryContainer)var4).getItemContainer().getAllItems(var1, true);
         }
      }

      return var1;
   }

   /** @deprecated */
   @Deprecated
   public InventoryItem getItemById(long var1) {
      for(int var3 = 0; var3 < this.getItems().size(); ++var3) {
         InventoryItem var4 = (InventoryItem)this.getItems().get(var3);
         if ((long)var4.getID() == var1) {
            return var4;
         }

         if (var4 instanceof InventoryContainer && ((InventoryContainer)var4).getItemContainer() != null && !((InventoryContainer)var4).getItemContainer().getItems().isEmpty()) {
            var4 = ((InventoryContainer)var4).getItemContainer().getItemById(var1);
            if (var4 != null) {
               return var4;
            }
         }
      }

      return null;
   }

   public void addItemsToProcessItems() {
      IsoWorld.instance.CurrentCell.addToProcessItems(this.Items);
   }

   public void removeItemsFromProcessItems() {
      IsoWorld.instance.CurrentCell.addToProcessItemsRemove(this.Items);
      if (!"floor".equals(this.type)) {
         ItemSoundManager.removeItems(this.Items);
      }

   }

   public boolean isExistYet() {
      if (!SystemDisabler.doWorldSyncEnable) {
         return true;
      } else if (this.getCharacter() != null) {
         return true;
      } else if (this.getParent() instanceof BaseVehicle) {
         return true;
      } else if (this.parent instanceof IsoDeadBody) {
         return this.parent.getStaticMovingObjectIndex() != -1;
      } else if (this.parent instanceof IsoCompost) {
         return this.parent.getObjectIndex() != -1;
      } else if (this.containingItem != null && this.containingItem.worldItem != null) {
         return this.containingItem.worldItem.getWorldObjectIndex() != -1;
      } else if (this.getType().equals("floor")) {
         return true;
      } else if (this.SourceGrid == null) {
         return false;
      } else {
         IsoGridSquare var1 = this.SourceGrid;
         if (!var1.getObjects().contains(this.parent)) {
            return false;
         } else {
            return this.parent.getContainerIndex(this) != -1;
         }
      }
   }

   public String getContainerPosition() {
      return this.containerPosition;
   }

   public void setContainerPosition(String var1) {
      this.containerPosition = var1;
   }

   public String getFreezerPosition() {
      return this.freezerPosition;
   }

   public void setFreezerPosition(String var1) {
      this.freezerPosition = var1;
   }

   public VehiclePart getVehiclePart() {
      return this.vehiclePart;
   }

   public void reset() {
      for(int var1 = 0; var1 < this.getItems().size(); ++var1) {
         ((InventoryItem)this.getItems().get(var1)).reset();
      }

   }

   public ItemContainer getOutermostContainer() {
      if (this.getContainingItem() != null && this.getContainingItem().getContainer() != null) {
         ItemContainer var1;
         for(var1 = this.getContainingItem().getContainer(); var1.getContainingItem() != null && var1.getContainingItem().getContainer() != null; var1 = var1.getContainingItem().getContainer()) {
         }

         return var1;
      } else {
         return this;
      }
   }

   public IsoGridSquare getSquare() {
      IsoGridSquare var1 = null;
      ItemContainer var2 = this.getOutermostContainer();
      if (var2.getVehiclePart() != null && var2.getVehiclePart().getVehicle() != null) {
         var1 = var2.getVehiclePart().getVehicle().getSquare();
      }

      if (var1 == null && var2.getSourceGrid() != null) {
         var1 = var2.getSourceGrid();
      } else if (var1 == null && var2.getParent() != null && var2.getParent().getSquare() != null) {
         var1 = var2.getParent().getSquare();
      } else if (var1 == null && var2.getContainingItem() != null && var2.getContainingItem().getWorldItem() != null && var2.getContainingItem().getWorldItem().getSquare() != null) {
         var1 = var2.getContainingItem().getWorldItem().getSquare();
      }

      return var1;
   }

   public boolean isStove() {
      return "stove".equals(this.type) || "toaster".equals(this.type) || "coffeemaker".equals(this.type);
   }

   private static final class InventoryItemListPool extends ObjectPool<InventoryItemList> {
      public InventoryItemListPool() {
         super(InventoryItemList::new);
      }

      public void release(InventoryItemList var1) {
         var1.clear();
         super.release((Object)var1);
      }
   }

   private static final class InventoryItemList extends ArrayList<InventoryItem> {
      private InventoryItemList() {
      }

      public boolean equals(Object var1) {
         return this == var1;
      }
   }

   private static final class Predicates {
      final ObjectPool<CategoryPredicate> category = new ObjectPool(CategoryPredicate::new);
      final ObjectPool<EvalPredicate> eval = new ObjectPool(EvalPredicate::new);
      final ObjectPool<EvalArgPredicate> evalArg = new ObjectPool(EvalArgPredicate::new);
      final ObjectPool<TagPredicate> tag = new ObjectPool(TagPredicate::new);
      final ObjectPool<TagEvalPredicate> tagEval = new ObjectPool(TagEvalPredicate::new);
      final ObjectPool<TagEvalArgPredicate> tagEvalArg = new ObjectPool(TagEvalArgPredicate::new);
      final ObjectPool<TypePredicate> type = new ObjectPool(TypePredicate::new);
      final ObjectPool<TypeEvalPredicate> typeEval = new ObjectPool(TypeEvalPredicate::new);
      final ObjectPool<TypeEvalArgPredicate> typeEvalArg = new ObjectPool(TypeEvalArgPredicate::new);

      private Predicates() {
      }
   }

   private static final class TypePredicate implements Predicate<InventoryItem> {
      String type;

      private TypePredicate() {
      }

      TypePredicate init(String var1) {
         this.type = (String)Objects.requireNonNull(var1);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return ItemContainer.compareType(this.type, var1);
      }
   }

   private static final class EvalPredicate implements Predicate<InventoryItem> {
      LuaClosure functionObj;

      private EvalPredicate() {
      }

      EvalPredicate init(LuaClosure var1) {
         this.functionObj = (LuaClosure)Objects.requireNonNull(var1);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1) == Boolean.TRUE;
      }
   }

   private static final class Comparators {
      ObjectPool<ConditionComparator> condition = new ObjectPool(ConditionComparator::new);
      ObjectPool<EvalComparator> eval = new ObjectPool(EvalComparator::new);
      ObjectPool<EvalArgComparator> evalArg = new ObjectPool(EvalArgComparator::new);

      private Comparators() {
      }
   }

   private static final class EvalComparator implements Comparator<InventoryItem> {
      LuaClosure functionObj;

      private EvalComparator() {
      }

      EvalComparator init(LuaClosure var1) {
         this.functionObj = (LuaClosure)Objects.requireNonNull(var1);
         return this;
      }

      public int compare(InventoryItem var1, InventoryItem var2) {
         LuaReturn var3 = LuaManager.caller.protectedCall(LuaManager.thread, this.functionObj, new Object[]{var1, var2});
         if (var3.isSuccess() && !var3.isEmpty() && var3.getFirst() instanceof Double) {
            double var4 = (Double)var3.getFirst();
            return Double.compare(var4, 0.0);
         } else {
            return 0;
         }
      }
   }

   private static final class EvalArgPredicate implements Predicate<InventoryItem> {
      LuaClosure functionObj;
      Object arg;

      private EvalArgPredicate() {
      }

      EvalArgPredicate init(LuaClosure var1, Object var2) {
         this.functionObj = (LuaClosure)Objects.requireNonNull(var1);
         this.arg = var2;
         return this;
      }

      public boolean test(InventoryItem var1) {
         return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1, this.arg) == Boolean.TRUE;
      }
   }

   private static final class EvalArgComparator implements Comparator<InventoryItem> {
      LuaClosure functionObj;
      Object arg;

      private EvalArgComparator() {
      }

      EvalArgComparator init(LuaClosure var1, Object var2) {
         this.functionObj = (LuaClosure)Objects.requireNonNull(var1);
         this.arg = var2;
         return this;
      }

      public int compare(InventoryItem var1, InventoryItem var2) {
         LuaReturn var3 = LuaManager.caller.protectedCall(LuaManager.thread, this.functionObj, new Object[]{var1, var2, this.arg});
         if (var3.isSuccess() && !var3.isEmpty() && var3.getFirst() instanceof Double) {
            double var4 = (Double)var3.getFirst();
            return Double.compare(var4, 0.0);
         } else {
            return 0;
         }
      }
   }

   private static final class ConditionComparator implements Comparator<InventoryItem> {
      private ConditionComparator() {
      }

      public int compare(InventoryItem var1, InventoryItem var2) {
         return var1.getCondition() - var2.getCondition();
      }
   }

   private static final class TagPredicate implements Predicate<InventoryItem> {
      String tag;

      private TagPredicate() {
      }

      TagPredicate init(String var1) {
         this.tag = (String)Objects.requireNonNull(var1);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return var1.hasTag(this.tag);
      }
   }

   private static final class TagEvalPredicate implements Predicate<InventoryItem> {
      String tag;
      LuaClosure functionObj;

      private TagEvalPredicate() {
      }

      TagEvalPredicate init(String var1, LuaClosure var2) {
         this.tag = var1;
         this.functionObj = (LuaClosure)Objects.requireNonNull(var2);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return var1.hasTag(this.tag) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1) == Boolean.TRUE;
      }
   }

   private static final class TagEvalArgPredicate implements Predicate<InventoryItem> {
      String tag;
      LuaClosure functionObj;
      Object arg;

      private TagEvalArgPredicate() {
      }

      TagEvalArgPredicate init(String var1, LuaClosure var2, Object var3) {
         this.tag = var1;
         this.functionObj = (LuaClosure)Objects.requireNonNull(var2);
         this.arg = var3;
         return this;
      }

      public boolean test(InventoryItem var1) {
         return var1.hasTag(this.tag) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1, this.arg) == Boolean.TRUE;
      }
   }

   private static final class TypeEvalPredicate implements Predicate<InventoryItem> {
      String type;
      LuaClosure functionObj;

      private TypeEvalPredicate() {
      }

      TypeEvalPredicate init(String var1, LuaClosure var2) {
         this.type = var1;
         this.functionObj = (LuaClosure)Objects.requireNonNull(var2);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return ItemContainer.compareType(this.type, var1) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1) == Boolean.TRUE;
      }
   }

   private static final class TypeEvalArgPredicate implements Predicate<InventoryItem> {
      String type;
      LuaClosure functionObj;
      Object arg;

      private TypeEvalArgPredicate() {
      }

      TypeEvalArgPredicate init(String var1, LuaClosure var2, Object var3) {
         this.type = var1;
         this.functionObj = (LuaClosure)Objects.requireNonNull(var2);
         this.arg = var3;
         return this;
      }

      public boolean test(InventoryItem var1) {
         return ItemContainer.compareType(this.type, var1) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, var1, this.arg) == Boolean.TRUE;
      }
   }

   private static final class CategoryPredicate implements Predicate<InventoryItem> {
      String category;

      private CategoryPredicate() {
      }

      CategoryPredicate init(String var1) {
         this.category = (String)Objects.requireNonNull(var1);
         return this;
      }

      public boolean test(InventoryItem var1) {
         return var1.getCategory().equals(this.category);
      }
   }
}
