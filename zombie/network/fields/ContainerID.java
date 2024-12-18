package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.Objects;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.ServerMap;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;

public class ContainerID implements INetworkPacketField {
   @JSONField
   public final PlayerID playerID = new PlayerID();
   @JSONField
   public ContainerType containerType;
   @JSONField
   public int x;
   @JSONField
   public int y;
   @JSONField
   public byte z;
   @JSONField
   short index;
   @JSONField
   short containerIndex;
   @JSONField
   short vid;
   @JSONField
   int worldItemID;
   ItemContainer container;
   IsoObject object;

   public ContainerID() {
      this.containerType = ContainerID.ContainerType.Undefined;
      this.x = 0;
      this.y = 0;
      this.z = 0;
      this.index = -1;
      this.containerIndex = -1;
      this.vid = -1;
      this.worldItemID = -1;
      this.container = null;
      this.object = null;
   }

   public ContainerType getContainerType() {
      return this.containerType;
   }

   public void set(ItemContainer var1) {
      if (var1 == null) {
         this.containerType = ContainerID.ContainerType.Undefined;
      } else {
         Object var2 = var1.getParent();
         if (!(var2 instanceof IsoPlayer)) {
            if (var1.getContainingItem() != null && var1.getContainingItem().getWorldItem() != null) {
               var2 = var1.getContainingItem().getWorldItem();
            }

            if (var1.containingItem != null && var1.containingItem.getOutermostContainer() != null) {
               IsoObject var4 = var1.containingItem.getOutermostContainer().getParent();
               if (var4 instanceof IsoPlayer) {
                  IsoPlayer var3 = (IsoPlayer)var4;
                  this.setInventoryContainer(var1, var3);
               } else {
                  this.setObject(var1, var4, var4.square);
               }

               return;
            }
         }

         if (var2 == null) {
            if (!var1.getType().equals("floor")) {
               throw new RuntimeException();
            }
         } else {
            this.set(var1, (IsoObject)var2);
         }
      }
   }

   public void copy(ContainerID var1) {
      this.containerType = var1.containerType;
      this.x = var1.x;
      this.y = var1.y;
      this.z = var1.z;
      this.index = var1.index;
      this.containerIndex = var1.containerIndex;
      this.vid = var1.vid;
      this.worldItemID = var1.worldItemID;
      this.playerID.copy(var1.playerID);
      this.container = var1.container;
      this.object = var1.object;
   }

   public void setFloor(ItemContainer var1, IsoGridSquare var2) {
      this.x = var2.getX();
      this.y = var2.getY();
      this.z = (byte)var2.getZ();
      this.containerType = ContainerID.ContainerType.Floor;
      this.container = var1;
   }

   public void setObject(ItemContainer var1, IsoObject var2, IsoGridSquare var3) {
      this.x = var3.getX();
      this.y = var3.getY();
      this.z = (byte)var3.getZ();
      this.container = var1;
      if (var2 != null) {
         this.containerType = ContainerID.ContainerType.ObjectContainer;
         this.index = (short)var2.square.getObjects().indexOf(var2);
         this.containerIndex = (short)var2.getContainerIndex(var1);
      } else {
         this.containerType = ContainerID.ContainerType.IsoObject;
         this.index = -1;
         this.containerIndex = -1;
      }

   }

   public void setInventoryContainer(ItemContainer var1, IsoPlayer var2) {
      this.x = var2.square.getX();
      this.y = var2.square.getY();
      this.z = (byte)var2.square.getZ();
      this.containerType = ContainerID.ContainerType.InventoryContainer;
      this.playerID.set(var2);
      this.worldItemID = var1.containingItem.id;
      this.container = var1;
   }

   public void set(ItemContainer var1, IsoObject var2) {
      if (var2.square != null) {
         this.x = var2.square.getX();
         this.y = var2.square.getY();
         this.z = (byte)var2.square.getZ();
      }

      if (var2 instanceof IsoDeadBody) {
         this.containerType = ContainerID.ContainerType.DeadBody;
         this.index = (short)var2.getStaticMovingObjectIndex();
      } else if (var2 instanceof IsoWorldInventoryObject) {
         this.containerType = ContainerID.ContainerType.WorldObject;
         this.worldItemID = ((IsoWorldInventoryObject)var2).getItem().id;
      } else if (var2 instanceof BaseVehicle) {
         this.containerType = ContainerID.ContainerType.Vehicle;
         this.vid = ((BaseVehicle)var2).VehicleID;
         this.index = (short)var1.vehiclePart.getIndex();
      } else if (var2 instanceof IsoPlayer) {
         this.containerType = ContainerID.ContainerType.PlayerInventory;
         this.playerID.set((IsoPlayer)var2);
      } else if (var2.getContainerIndex(var1) != -1) {
         this.containerType = ContainerID.ContainerType.ObjectContainer;
         this.index = (short)var2.square.getObjects().indexOf(var2);
         this.containerIndex = (short)var2.getContainerIndex(var1);
      } else {
         this.containerType = ContainerID.ContainerType.IsoObject;
         this.index = (short)var2.square.getObjects().indexOf(var2);
         this.containerIndex = -1;
      }

      this.container = var1;
   }

   public boolean isContainerTheSame(int var1, ItemContainer var2) {
      if (!"floor".equals(var2.getType())) {
         return this.container == var2;
      } else {
         return ContainerID.ContainerType.WorldObject.equals(this.containerType) && var1 == this.worldItemID;
      }
   }

   public ItemContainer getContainer() {
      return this.container;
   }

   public IsoObject getObject() {
      return this.object;
   }

   public VehiclePart getPart() {
      if (this.containerType != ContainerID.ContainerType.Vehicle) {
         return null;
      } else {
         BaseVehicle var1 = VehicleManager.instance.getVehicleByID(this.vid);
         return var1 == null ? null : var1.getPartByIndex(this.index);
      }
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.containerType = ContainerID.ContainerType.values()[var1.get()];
      if (this.containerType != ContainerID.ContainerType.Undefined) {
         if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
            this.playerID.parse(var1, var2);
            this.playerID.parsePlayer(var2);
         } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
            this.playerID.parse(var1, var2);
            this.playerID.parsePlayer(var2);
            this.worldItemID = var1.getInt();
         } else {
            this.x = var1.getInt();
            this.y = var1.getInt();
            this.z = var1.get();
            if (this.containerType == ContainerID.ContainerType.DeadBody) {
               this.index = var1.getShort();
            } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
               this.worldItemID = var1.getInt();
            } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
               this.index = var1.getShort();
               this.containerIndex = -1;
            } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
               this.index = var1.getShort();
               this.containerIndex = var1.getShort();
            } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
               this.vid = var1.getShort();
               this.index = var1.getShort();
            }
         }

         this.parse();
      }
   }

   public void write(ByteBufferWriter var1) {
      var1.putByte((byte)this.containerType.ordinal());
      if (this.containerType != ContainerID.ContainerType.Undefined) {
         if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
            this.playerID.write(var1);
         } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
            this.playerID.write(var1);
            var1.putInt(this.worldItemID);
         } else {
            var1.putInt(this.x);
            var1.putInt(this.y);
            var1.putByte(this.z);
            if (this.containerType == ContainerID.ContainerType.DeadBody) {
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
               var1.putInt(this.worldItemID);
            } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
               var1.putShort(this.vid);
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
               var1.putShort(this.index);
               var1.putShort(this.containerIndex);
            }

         }
      }
   }

   public void write(ByteBuffer var1) {
      var1.put((byte)this.containerType.ordinal());
      if (this.containerType != ContainerID.ContainerType.Undefined) {
         if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
            this.playerID.write(var1);
         } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
            this.playerID.write(var1);
            var1.putInt(this.worldItemID);
         } else {
            var1.putInt(this.x);
            var1.putInt(this.y);
            var1.put(this.z);
            if (this.containerType == ContainerID.ContainerType.DeadBody) {
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
               var1.putInt(this.worldItemID);
            } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
               var1.putShort(this.vid);
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
               var1.putShort(this.index);
            } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
               var1.putShort(this.index);
               var1.putShort(this.containerIndex);
            }

         }
      }
   }

   private void parse() {
      if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
         this.container = this.playerID.getPlayer().getInventory();
      } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
         ItemContainer var6 = this.playerID.getPlayer().getInventory();
         InventoryContainer var7 = (InventoryContainer)var6.getItemWithID(this.worldItemID);
         this.container = var7.getItemContainer();
      } else if (IsoWorld.instance.CurrentCell != null) {
         IsoGridSquare var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.x, this.y, this.z);
         if (GameServer.bServer && var1 == null) {
            var1 = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
         }

         if (var1 != null) {
            this.container = null;
            VehiclePart var2 = null;
            int var3;
            IsoWorldInventoryObject var4;
            if (this.containerType == ContainerID.ContainerType.Floor) {
               this.object = new IsoWorldInventoryObject((InventoryItem)null, var1, 0.0F, 0.0F, 0.0F);
               this.container = new ItemContainer("floor", var1, (IsoObject)null);

               for(var3 = 0; var3 < var1.getWorldObjects().size(); ++var3) {
                  var4 = (IsoWorldInventoryObject)var1.getWorldObjects().get(var3);
                  if (var4 != null && var4.getItem() != null) {
                     this.container.getItems().add(var4.getItem());
                  }
               }
            } else if (this.containerType == ContainerID.ContainerType.DeadBody) {
               if (this.index < 0 || this.index >= var1.getStaticMovingObjects().size()) {
                  DebugLog.log("ERROR: ContainerID: invalid corpse index");
                  return;
               }

               this.object = (IsoObject)var1.getStaticMovingObjects().get(this.index);
               if (this.object != null && this.object.getContainer() != null) {
                  this.container = this.object.getContainer();
               }
            } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
               for(var3 = 0; var3 < var1.getWorldObjects().size(); ++var3) {
                  var4 = (IsoWorldInventoryObject)var1.getWorldObjects().get(var3);
                  if (var4 != null && var4.getItem() != null && var4.getItem().id == this.worldItemID) {
                     this.object = var4;
                     InventoryItem var5 = var4.getItem();
                     if (var5 instanceof InventoryContainer) {
                        this.container = ((InventoryContainer)var5).getItemContainer();
                     } else {
                        this.container = new ItemContainer("floor", var1, (IsoObject)null);
                        this.container.getItems().add(var5);
                     }
                     break;
                  }
               }

               if (this.container == null) {
                  DebugLog.log("ERROR: sendItemsToContainer: can't find world item with id=" + this.worldItemID);
               }
            } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
               if (this.index >= 0 && this.index < var1.getObjects().size()) {
                  this.object = (IsoObject)var1.getObjects().get(this.index);
                  this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
               } else {
                  this.object = new IsoObject();
                  this.object.setSquare(var1);
                  this.container = new ItemContainer("object", var1, this.object);
               }
            } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
               if (this.index >= 0 && this.index < var1.getObjects().size()) {
                  this.object = (IsoObject)var1.getObjects().get(this.index);
                  this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
               } else {
                  this.object = new IsoObject();
                  this.object.setSquare(var1);
               }
            } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
               BaseVehicle var8 = VehicleManager.instance.getVehicleByID(this.vid);
               if (var8 == null) {
                  DebugLog.log("ERROR: sendItemsToContainer: invalid vehicle id");
               } else {
                  var2 = var8.getPartByIndex(this.index);
                  if (var2 == null) {
                     DebugLog.log("ERROR: sendItemsToContainer: invalid part index");
                  } else {
                     this.object = var8;
                     this.container = var2.getItemContainer();
                     if (this.container == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: part " + var2.getId() + " has no container");
                     }
                  }
               }
            } else {
               DebugLog.log("ERROR: sendItemsToContainer: unknown container type");
            }
         } else if (GameClient.bClient) {
            GameClient.instance.delayPacket(this.x, this.y, this.z);
         }

      }
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         ContainerID var2 = (ContainerID)var1;
         if (this.containerType != ContainerID.ContainerType.InventoryContainer && this.containerType != ContainerID.ContainerType.PlayerInventory) {
            return this.containerType == var2.containerType && this.x == var2.x && this.y == var2.y && this.z == var2.z && this.index == var2.index && this.containerIndex == var2.containerIndex && this.vid == var2.vid && this.worldItemID == var2.worldItemID;
         } else {
            return this.containerType == var2.containerType && this.playerID.getID() == var2.playerID.getID() && this.worldItemID == var2.worldItemID;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.containerType, this.x, this.y, this.z, this.index, this.containerIndex, this.vid, this.worldItemID});
   }

   public String toString() {
      ContainerType var10000 = this.containerType;
      return "" + var10000 + this.hashCode();
   }

   public static enum ContainerType {
      Undefined,
      DeadBody,
      WorldObject,
      IsoObject,
      ObjectContainer,
      Vehicle,
      PlayerInventory,
      InventoryContainer,
      Floor;

      private ContainerType() {
      }
   }
}
