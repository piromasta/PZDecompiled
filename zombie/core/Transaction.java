package zombie.core;

import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.PlayerID;
import zombie.network.packets.IDescriptor;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

public abstract class Transaction implements IDescriptor {
   protected static byte lastId = 0;
   @JSONField
   protected byte id = 0;
   @JSONField
   protected TransactionState state;
   @JSONField
   protected int itemId = -1;
   @JSONField
   protected final PlayerID playerID = new PlayerID();
   @JSONField
   protected final ContainerID sourceId = new ContainerID();
   @JSONField
   protected final ContainerID destinationId = new ContainerID();
   @JSONField
   protected String extra = null;
   @JSONField
   protected IsoDirections direction = null;
   protected int duration = -1;
   protected float xoff = 0.0F;
   protected float yoff = 0.0F;
   protected float zoff = 0.0F;
   protected long startTime;
   protected long endTime;
   public IsoGridSquare square;

   public Transaction() {
   }

   public void set(IsoPlayer var1, InventoryItem var2, ItemContainer var3, ItemContainer var4, String var5, IsoDirections var6, float var7, float var8, float var9) {
      this.state = GameServer.bServer ? Transaction.TransactionState.Accept : Transaction.TransactionState.Request;
      if (var2 != null) {
         this.itemId = var2.id;
      } else {
         this.itemId = -1;
      }

      this.playerID.set(var1);
      if (var3.getType().equals("object")) {
         this.sourceId.set(var3, var3.getParent());
      } else if (var3.getType().equals("floor")) {
         if (var2.getWorldItem() != null) {
            this.sourceId.set(var3, var2.getWorldItem());
            this.itemId = -1;
         } else {
            this.sourceId.set(var3, var2.deadBodyObject);
         }
      } else {
         this.sourceId.set(var3);
      }

      if (var4 == null) {
         this.destinationId.set((ItemContainer)null);
      } else if (var4.getType().equals("object")) {
         if (var3 != null && var4.getParent() == var3.getParent()) {
            this.destinationId.set(var4, var4.getParent());
         } else {
            this.destinationId.setObject(var4, var4.getParent(), var4.SourceGrid);
         }

         this.direction = var6;
         this.xoff = var7;
         this.yoff = var8;
         this.zoff = var9;
      } else if (var4.getType().equals("floor")) {
         this.destinationId.setFloor(var4, var1.getCurrentSquare());
      } else {
         this.destinationId.set(var4);
      }

      this.extra = var5;
      this.startTime = GameTime.getServerTimeMills();
      this.endTime = GameServer.bServer ? this.startTime + (long)this.getDuration() : this.startTime;
      if (lastId == 0) {
         ++lastId;
      }

      byte var10001 = lastId;
      lastId = (byte)(var10001 + 1);
      this.id = var10001;
   }

   public void setTimeData() {
      this.startTime = GameTime.getServerTimeMills();
      this.endTime = this.startTime + (long)this.getDuration();
   }

   boolean update() {
      InventoryItem var1 = null;
      KahluaTable var9;
      if (this.sourceId.containerType == ContainerID.ContainerType.IsoObject) {
         if (this.destinationId.containerType == ContainerID.ContainerType.Undefined) {
            LuaEventManager.triggerEvent("OnProcessTransaction", "scrapMoveable", this.playerID.getPlayer(), (Object)null, this.sourceId, this.destinationId, (Object)null);
            return true;
         } else if (this.sourceId.hashCode() != this.destinationId.hashCode()) {
            LuaEventManager.triggerEvent("OnProcessTransaction", "pickUpMoveable", this.playerID.getPlayer(), (Object)null, this.sourceId, this.destinationId, (Object)null);
            return true;
         } else {
            var9 = LuaManager.platform.newTable();
            var9.rawset("direction", this.direction.name());
            LuaEventManager.triggerEvent("OnProcessTransaction", "rotateMoveable", this.playerID.getPlayer(), (Object)null, this.sourceId, this.destinationId, var9);
            return true;
         }
      } else {
         if (this.itemId == -1 && this.sourceId.containerType == ContainerID.ContainerType.DeadBody) {
            var1 = ((IsoDeadBody)this.sourceId.getObject()).getItem();
         } else {
            if (this.sourceId.getContainer() == null) {
               return false;
            }

            var1 = this.sourceId.getContainer().getItemWithID(this.itemId);
         }

         if (var1 != null) {
            var1.update();
            if (var1 instanceof Clothing) {
               ((Clothing)var1).updateWetness();
            }
         }

         if (this.destinationId.containerType == ContainerID.ContainerType.IsoObject) {
            var9 = LuaManager.platform.newTable();
            var9.rawset("direction", this.direction.name());
            LuaEventManager.triggerEvent("OnProcessTransaction", "placeMoveable", this.playerID.getPlayer(), var1, this.sourceId, this.destinationId, var9);
            return true;
         } else {
            IsoPlayer var2;
            if (ContainerID.ContainerType.Floor.equals(this.destinationId.containerType) && ContainerID.ContainerType.PlayerInventory.equals(this.sourceId.containerType)) {
               var2 = this.playerID.getPlayer();
               var2.removeAttachedItem(var1);
               if (var2.isEquipped(var1)) {
                  var2.removeFromHands(var1);
                  var2.removeWornItem(var1, false);
                  LuaEventManager.triggerEvent("OnClothingUpdated", var2);
                  GameServer.updateHandEquips(GameServer.getConnectionFromPlayer(var2), var2);
               }

               KahluaTable var8 = LuaManager.platform.newTable();
               var8.rawset("square", this.square);
               LuaEventManager.triggerEvent("OnProcessTransaction", "dropOnFloor", this.playerID.getPlayer(), var1, this.sourceId, this.destinationId, var8);
               return true;
            } else if (!StringUtils.isNullOrEmpty(this.extra)) {
               int var6 = this.itemId;
               if (var1 != null && this.sourceId.containerType == ContainerID.ContainerType.PlayerInventory && this.sourceId.getContainer() == this.destinationId.getContainer()) {
                  DebugLog.Objects.noise("Extra option for \"%s\"", this.extra);
                  IsoPlayer var7 = this.playerID.getPlayer();
                  if (var7 != null) {
                     var7.removeFromHands(var1);
                     var7.removeWornItem(var1, false);
                     var7.getInventory().Remove(var1);
                     InventoryItem var4 = InventoryItemFactory.CreateItem(var1, this.extra);
                     var7.getInventory().AddItem(var4);
                     if (var4 instanceof InventoryContainer && !StringUtils.isNullOrEmpty(((InventoryContainer)var4).canBeEquipped())) {
                        var7.setWornItem(((InventoryContainer)var4).canBeEquipped(), var4);
                     } else if (var4.IsClothing()) {
                        var7.setWornItem(var4.getBodyLocation(), var4);
                     }

                     var6 = var4.id;
                     GameServer.sendRemoveItemFromContainer(this.sourceId.getContainer(), var1);
                     GameServer.sendAddItemToContainer(this.destinationId.getContainer(), var4);
                     GameServer.updateHandEquips(GameServer.getConnectionFromPlayer(var7), var7);
                     GameServer.sendSyncClothing(var7, var4.getBodyLocation(), var4);
                  }
               }

               return this.destinationId.getContainer().getItemWithID(var6) != null && this.sourceId.getContainer().getItemWithID(this.itemId) == null;
            } else {
               if (this.sourceId.containerType == ContainerID.ContainerType.WorldObject && this.itemId == -1) {
                  var1 = ((IsoWorldInventoryObject)this.sourceId.getObject()).getItem();
               }

               if (this.destinationId.getContainer() == null) {
                  return false;
               } else if (var1 == null) {
                  return false;
               } else {
                  UdpConnection var3;
                  if (this.sourceId.containerType == ContainerID.ContainerType.WorldObject && this.itemId == -1) {
                     GameServer.RemoveItemFromMap(this.sourceId.getObject());
                  } else if (this.itemId == -1 && this.sourceId.containerType == ContainerID.ContainerType.DeadBody) {
                     GameServer.sendRemoveCorpseFromMap((IsoDeadBody)this.sourceId.getObject());
                     this.sourceId.getObject().getSquare().removeCorpse((IsoDeadBody)this.sourceId.getObject(), true);
                  } else {
                     var2 = this.playerID.getPlayer();
                     var2.removeAttachedItem(var1);
                     if (var2.isEquipped(var1)) {
                        var2.removeFromHands(var1);
                        var2.removeWornItem(var1, false);
                        LuaEventManager.triggerEvent("OnClothingUpdated", var2);
                        var3 = GameServer.getConnectionFromPlayer(var2);
                        GameServer.updateHandEquips(var3, var2);
                        GameServer.sendSyncClothing(var2, (String)null, var1);
                     }

                     if (this.sourceId.getContainer().getCharacter() instanceof IsoPlayer) {
                        INetworkPacket.send((IsoPlayer)this.sourceId.getContainer().getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, this.sourceId.getContainer(), var1);
                     } else {
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (float)this.sourceId.x, (float)this.sourceId.y, this.sourceId.getContainer(), var1);
                     }
                  }

                  this.sourceId.getContainer().Remove(var1);
                  if (!TransactionManager.lastItemFullType.equals(var1.getFullType())) {
                     TransactionManager.lightweightItemsCount = 0;
                  }

                  TransactionManager.lastItemFullType = var1.getFullType() == null ? "" : var1.getFullType();
                  TransactionManager.lightweightItemsLastTransactionTime = System.currentTimeMillis();
                  this.destinationId.getContainer().addItem(var1);
                  if (this.destinationId.containerType == ContainerID.ContainerType.Floor) {
                     IsoWorldInventoryObject var5 = (IsoWorldInventoryObject)this.destinationId.getObject();
                     var5.square.AddWorldInventoryItem(var1, var5.xoff, var5.yoff, var5.zoff, true);
                  } else {
                     if (this.sourceId.containerType == ContainerID.ContainerType.WorldObject || this.sourceId.containerType == ContainerID.ContainerType.Floor) {
                        var1.setWorldItem((IsoWorldInventoryObject)null);
                     }

                     if (this.destinationId.getContainer().getCharacter() instanceof IsoPlayer) {
                        INetworkPacket.send((IsoPlayer)this.destinationId.getContainer().getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, this.destinationId.getContainer(), var1);
                     } else {
                        INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (float)this.destinationId.x, (float)this.destinationId.y, this.destinationId.getContainer(), var1);
                     }
                  }

                  if (this.sourceId.containerType == ContainerID.ContainerType.DeadBody && this.itemId == -1) {
                     if (this.destinationId.containerType == ContainerID.ContainerType.PlayerInventory) {
                        var2 = this.playerID.getPlayer();
                        var2.setPrimaryHandItem(var1);
                        var2.setSecondaryHandItem(var1);
                        var3 = GameServer.getConnectionFromPlayer(var2);
                        GameServer.updateHandEquips(var3, var2);
                     } else {
                        DebugLogStream var10000 = DebugLog.Objects;
                        String var10001 = this.playerID.getDescription();
                        var10000.error("A player " + var10001 + " sent invalid transaction " + this.getDuration());
                     }
                  }

                  return true;
               }
            }
         }
      }
   }

   private float getDuration() {
      float var1 = 1000.0F;
      ItemContainer var2 = this.sourceId.getContainer();
      ItemContainer var3 = this.destinationId.getContainer();
      IsoPlayer var4 = this.playerID.getPlayer();
      InventoryItem var5;
      if (var2 != null && var3 != null && var4 != null) {
         var5 = this.sourceId.getContainer().getItemWithID(this.itemId);
         if (!var3.getType().equals("TradeUI") && !var2.getType().equals("TradeUI")) {
            var1 = 120.0F;
            float var6 = 1.0F;
            if (var2 == var4.getInventory()) {
               if (var3.isInCharacterInventory(var4)) {
                  var6 = var3.getCapacityWeight() / var3.getMaxWeight();
               } else {
                  var1 = 50.0F;
               }
            } else if (!var2.isInCharacterInventory(var4) && var3.isInCharacterInventory(var4)) {
               var1 = 50.0F;
            }

            if (var6 < 0.4F) {
               var6 = 0.4F;
            }

            if (var5 != null) {
               float var7 = var5.getActualWeight();
               if (var7 > 3.0F) {
                  var7 = 3.0F;
               }

               var1 = var1 * var7 * var6;
            }

            if (Core.getInstance().getGameMode().equals("LastStand")) {
               var1 *= 0.3F;
            }

            if (var3.getType().equals("floor")) {
               if (var2 == var4.getInventory()) {
                  var1 *= 0.1F;
               } else if (!var2.isInCharacterInventory(var4)) {
                  var1 *= 0.2F;
               }
            }

            if (var4.HasTrait("Dextrous")) {
               var1 *= 0.5F;
            }

            if (var4.HasTrait("AllThumbs") || var4.isWearingAwkwardGloves()) {
               var1 *= 2.0F;
            }
         } else {
            var1 = 0.0F;
         }
      } else if (this.sourceId.containerType == ContainerID.ContainerType.IsoObject || this.destinationId.containerType == ContainerID.ContainerType.IsoObject) {
         var1 = 50.0F;
      }

      var5 = null;
      if (this.sourceId.containerType == ContainerID.ContainerType.WorldObject && this.itemId == -1) {
         var5 = ((IsoWorldInventoryObject)this.sourceId.getObject()).getItem();
      }

      if (var5 != null && var5.getWeight() <= 0.1F && (TransactionManager.lightweightItemsLastTransactionTime == 0L || System.currentTimeMillis() - TransactionManager.lightweightItemsLastTransactionTime < 1000L)) {
         if (TransactionManager.lightweightItemsCount < 19 && TransactionManager.lastItemFullType.equals(var5.getFullType())) {
            var1 = 0.0F;
            ++TransactionManager.lightweightItemsCount;
         } else {
            TransactionManager.lightweightItemsCount = 0;
            TransactionManager.lastItemFullType = "";
         }
      }

      if (this.sourceId.getContainer() == this.destinationId.getContainer() && ContainerID.ContainerType.PlayerInventory.equals(this.sourceId.containerType) && this.itemId != -1 && !StringUtils.isNullOrEmpty(this.extra)) {
         var1 = 0.0F;
      }

      return var1 * 20.0F;
   }

   public void setState(TransactionState var1) {
      this.state = var1;
      DebugLog.Objects.noise(this);
   }

   public void setDuration(long var1) {
      this.endTime = this.startTime + var1;
   }

   public String toString() {
      return this.getDescription();
   }

   public static enum TransactionState {
      Reject,
      Request,
      Accept,
      Done;

      private TransactionState() {
      }
   }
}
