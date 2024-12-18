package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.ui.UiConfig;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.events.EntityEventType;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.BitSet;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.Item;
import zombie.vehicles.VehiclePart;

@DebugClassFields
public abstract class GameEntity {
   public static final String DEFAULT_ENTITY_DISPLAY_NAME = "EC_Entity_DisplayName_Default";
   private ComponentContainer components = null;
   private boolean addedToWorldOrEquipped = false;
   boolean addedToEntityManager = false;
   private static final BitSet dummyBits = new BitSet();
   boolean addedToEngine = false;
   boolean removingFromEngine = false;
   boolean scheduledDelayedAddToEngine = false;
   boolean scheduledForEngineRemoval = false;
   boolean scheduledForBucketUpdate = false;
   @DebugNonRecursive
   private IsoPlayer usingPlayer;
   private static final boolean IOverbose = false;

   public GameEntity() {
   }

   public static String getDefaultEntityDisplayName() {
      return "EC_Entity_DisplayName_Default";
   }

   @DebugMethod
   @DebugNonRecursive
   public abstract GameEntityType getGameEntityType();

   @DebugMethod
   @DebugNonRecursive
   public abstract IsoGridSquare getSquare();

   @DebugMethod
   public abstract long getEntityNetID();

   @DebugMethod
   public abstract float getX();

   @DebugMethod
   public abstract float getY();

   @DebugMethod
   public abstract float getZ();

   @DebugMethod
   public abstract boolean isEntityValid();

   public boolean isValidEngineEntity() {
      return this.addedToEngine && !this.scheduledForEngineRemoval && !this.removingFromEngine;
   }

   @DebugMethod
   public boolean isMeta() {
      return false;
   }

   @DebugMethod
   public String getEntityDisplayName() {
      return this.hasComponent(ComponentType.UiConfig) ? ((UiConfig)this.getComponent(ComponentType.UiConfig)).getEntityDisplayName() : Translator.getEntityText("EC_Entity_DisplayName_Default");
   }

   @DebugMethod
   public String getEntityFullTypeDebug() {
      if (this.hasComponent(ComponentType.Script)) {
         EntityScriptInfo var1 = (EntityScriptInfo)this.getComponent(ComponentType.Script);
         if (var1 != null && var1.getOriginalScript() != null) {
            return var1.getOriginalScript();
         }
      }

      return "<anonymous>";
   }

   public final String getExceptionCompatibleString() {
      if (this instanceof InventoryItem var1) {
         if (var1.getScriptItem() != null) {
            Item var2 = var1.getScriptItem();
            short var10000 = var2.getRegistry_id();
            return "ITEM_ENTITY[id=" + var10000 + ", name=" + var2.getName() + ", type=" + var2.getScriptObjectFullType() + ", vanilla=" + var1.isVanilla() + "]";
         }
      } else {
         if (this instanceof IsoObject) {
            return "ISO_ENTITY[anonymous]";
         }

         if (this instanceof VehiclePart) {
            return "V_PART_ENTITY[anonymous]";
         }
      }

      return "ENTITY[anonymous]";
   }

   public final AttributeContainer attrib() {
      return (AttributeContainer)this.getComponent(ComponentType.Attributes);
   }

   public final AttributeContainer getAttributes() {
      return (AttributeContainer)this.getComponent(ComponentType.Attributes);
   }

   public final FluidContainer getFluidContainer() {
      return (FluidContainer)this.getComponent(ComponentType.FluidContainer);
   }

   public final SpriteConfig getSpriteConfig() {
      return (SpriteConfig)this.getComponent(ComponentType.SpriteConfig);
   }

   public final boolean isAddedToEngine() {
      return this.addedToEngine;
   }

   public final boolean isRemovingFromEngine() {
      return this.removingFromEngine;
   }

   public final boolean isScheduledForEngineRemoval() {
      return this.scheduledForEngineRemoval;
   }

   public final boolean isScheduledForBucketUpdate() {
      return this.scheduledForBucketUpdate;
   }

   BitSet getBucketBits() {
      if (this.components != null) {
         return this.components.getBucketBits();
      } else {
         DebugLog.General.error("Getting bucketBits while no Component data");
         if (Core.bDebug) {
            throw new RuntimeException();
         } else {
            dummyBits.clear();
            return dummyBits;
         }
      }
   }

   BitSet getComponentBits() {
      return this.components != null ? this.components.getComponentBits() : null;
   }

   public boolean hasRenderers() {
      return this.components != null && this.components.hasRenderers();
   }

   ComponentOperationHandler getComponentOperationHandler() {
      return this.components != null ? this.components.getComponentOperationHandler() : null;
   }

   void setComponentOperationHandler(ComponentOperationHandler var1) {
      if (this.components != null) {
         this.components.setComponentOperationHandler(var1);
      }

   }

   private void ensureComponents() {
      if (this.components == null) {
         this.components = ComponentContainer.Alloc(this);
      }

   }

   public final boolean hasComponents() {
      return this.components != null && this.components.size() > 0;
   }

   final boolean addComponent(Component var1) {
      if (var1 == null) {
         DebugLog.General.error("Trying to add 'null' component.");
         return false;
      } else if (this.components != null && this.components.size() >= 127) {
         DebugLog.General.error("Maximum component size '127' for entity reached.");
         return false;
      } else if (!this.hasComponent(var1.getComponentType())) {
         this.ensureComponents();
         if (var1.getOwner() != null) {
            DebugLog.General.warn("Component added has not been removed from previous Entity.");
            var1.getOwner().removeComponent(var1);
         }

         this.components.add(var1);
         var1.setOwner(this);
         var1.onAddedToOwner();
         if (this.addedToWorldOrEquipped && !this.addedToEntityManager) {
            GameEntityManager.RegisterEntity(this);
         }

         return true;
      } else {
         DebugLog.General.error("Trying to add component but component type already exists: " + var1.getComponentType());
         return false;
      }
   }

   final boolean releaseComponent(ComponentType var1) {
      Component var2 = this.removeComponent(var1);
      if (var2 != null) {
         ComponentType.ReleaseComponent(var2);
         return true;
      } else {
         return false;
      }
   }

   final boolean releaseComponent(Component var1) {
      Component var2 = this.removeComponent(var1);
      if (var2 != null) {
         ComponentType.ReleaseComponent(var2);
         return true;
      } else {
         return false;
      }
   }

   final Component removeComponent(ComponentType var1) {
      if (!this.hasComponents()) {
         DebugLog.General.error("Trying to remove component but has no components: " + var1);
         return null;
      } else {
         Component var2 = this.components.remove(var1);
         if (var2 != null) {
            var2.onRemovedFromOwner();
            var2.setOwner((GameEntity)null);
            return var2;
         } else {
            return null;
         }
      }
   }

   final Component removeComponent(Component var1) {
      if (var1 == null) {
         DebugLog.General.error("Trying to remove 'null' component.");
         return null;
      } else if (!this.hasComponents()) {
         DebugLog.General.error("Trying to remove component but has no components: " + var1.getComponentType());
         return null;
      } else if (this.components.removeComponent(var1)) {
         var1.onRemovedFromOwner();
         var1.setOwner((GameEntity)null);
         return var1;
      } else {
         return null;
      }
   }

   public final boolean hasComponent(ComponentType var1) {
      return var1 != null && this.hasComponents() ? this.components.contains(var1) : false;
   }

   public final int componentSize() {
      return this.hasComponents() ? this.components.size() : 0;
   }

   public final Component getComponentForIndex(int var1) {
      return this.hasComponents() && var1 >= 0 && var1 < this.components.size() ? this.components.getForIndex(var1) : null;
   }

   public final <T extends Component> T getComponent(ComponentType var1) {
      return var1 != null && this.hasComponents() ? this.components.get(var1) : null;
   }

   public final Component getComponentFromID(short var1) {
      return this.getComponent(ComponentType.FromId(var1));
   }

   public final boolean containsComponent(Component var1) {
      return this.hasComponents() ? this.components.contains(var1) : false;
   }

   protected final void sendComponentEvent(Component var1, ComponentEventType var2) {
      ComponentEvent var3 = ComponentEvent.Alloc(var2, var1);
      this.sendComponentEvent(var1, var3);
   }

   protected final void sendComponentEvent(Component var1, ComponentEvent var2) {
      for(int var3 = 0; var3 < this.components.size(); ++var3) {
         if (var1 == null || this.components.getForIndex(var3) != var1) {
            this.components.getForIndex(var3).onComponentEvent(var2);
         }
      }

      var2.release();
   }

   protected final void sendEntityEvent(EntityEventType var1) {
      EntityEvent var2 = EntityEvent.Alloc(var1, this);
      this.sendEntityEvent(var2);
   }

   protected final void sendEntityEvent(EntityEvent var1) {
      for(int var2 = 0; var2 < this.components.size(); ++var2) {
         this.components.getForIndex(var2).onEntityEvent(var1);
      }

      var1.release();
   }

   protected final void connectComponents() {
      if (this.hasComponents()) {
         for(int var1 = 0; var1 < this.components.size(); ++var1) {
            this.components.getForIndex(var1).onConnectComponents();
         }

      }
   }

   protected final void onFirstCreation() {
      if (this.hasComponents()) {
         for(int var1 = 0; var1 < this.components.size(); ++var1) {
            this.components.getForIndex(var1).onFirstCreation();
         }

      }
   }

   public void reset() {
      if (this.addedToEngine || this.addedToEntityManager) {
         if (Core.bDebug) {
            throw new IllegalStateException("Entity should be removed from Engine and Manager at this point.");
         }

         if (GameEntityManager.isEngineProcessing()) {
            throw new IllegalStateException("Fatal entity state.");
         }

         GameEntityManager.UnregisterEntity(this);
      }

      this.usingPlayer = null;
      this.addedToEngine = false;
      this.removingFromEngine = false;
      this.scheduledDelayedAddToEngine = false;
      this.scheduledForEngineRemoval = false;
      this.scheduledForBucketUpdate = false;
      this.addedToEntityManager = false;
      this.addedToWorldOrEquipped = false;
      if (this.components != null) {
         this.components.release();
         this.components = null;
      }

   }

   public void onEquip() {
      this.onEquip(true);
   }

   public void onEquip(boolean var1) {
      if (this.getGameEntityType() == GameEntityType.InventoryItem) {
         if (var1) {
            GameEntityManager.RegisterEntity(this);
         }

         this.addedToWorldOrEquipped = true;
      }

   }

   public void onUnEquip() {
      if (this.getGameEntityType() == GameEntityType.InventoryItem) {
         GameEntityManager.UnregisterEntity(this);
         this.addedToWorldOrEquipped = false;
      }

   }

   public void addToWorld() {
      if (this.getGameEntityType() != GameEntityType.InventoryItem) {
         GameEntityManager.RegisterEntity(this);
         this.addedToWorldOrEquipped = true;
      }
   }

   public void removeFromWorld() {
      this.removeFromWorld(false);
   }

   public final void removeFromWorld(boolean var1) {
      if (this.getGameEntityType() != GameEntityType.InventoryItem) {
         GameEntityManager.UnregisterEntity(this, var1);
         this.addedToWorldOrEquipped = false;
      }
   }

   public void renderlast() {
   }

   public void renderlastComponents() {
      if (this.hasComponents()) {
         this.components.render();
      }
   }

   public final boolean requiresEntitySave() {
      return this.hasComponents();
   }

   public final void saveEntity(ByteBuffer var1) throws IOException {
      var1.put(this.hasComponents() ? (byte)this.components.size() : 0);
      if (this.hasComponents()) {
         for(int var3 = 0; var3 < this.components.size(); ++var3) {
            Component var2 = this.components.getForIndex(var3);
            ByteBlock var4 = ByteBlock.Start(var1, ByteBlock.Mode.Save);
            var1.putShort(var2.getComponentType().GetID());
            var2.save(var1);
            ByteBlock.End(var1, var4);
         }
      }

   }

   public final void loadEntity(ByteBuffer var1, int var2) throws IOException {
      byte var3 = var1.get();
      if (var3 > 0) {
         for(int var4 = 0; var4 < var3; ++var4) {
            ByteBlock var5 = ByteBlock.Start(var1, ByteBlock.Mode.Load);
            var5.safelyForceSkipOnEnd(true);

            try {
               short var6 = var1.getShort();
               ComponentType var7 = ComponentType.FromId(var6);
               Component var8 = this.getComponent(var7);
               boolean var9 = true;
               if (var8 == null) {
                  var8 = var7.CreateComponent();
                  var9 = false;
               }

               if (var8 != null) {
                  var8.load(var1, var2);
                  if (!var9) {
                     this.addComponent(var8);
                  }
               } else if (Core.bDebug) {
                  throw new IOException("Component with id '" + var6 + "' not found.");
               }
            } catch (Exception var10) {
               var10.printStackTrace();
            }

            ByteBlock.End(var1, var5);
         }

         this.connectComponents();
      }

   }

   public boolean isUsingPlayer(IsoPlayer var1) {
      if (this.getUsingPlayer() == null) {
         return false;
      } else if (var1 == null) {
         return false;
      } else {
         return this.getUsingPlayer() == var1;
      }
   }

   public IsoPlayer getUsingPlayer() {
      return this.usingPlayer;
   }

   public void setUsingPlayer(IsoPlayer var1) {
      if (this.usingPlayer != var1) {
         this.usingPlayer = var1;
         if ((GameClient.bClient || GameServer.bServer) && this.getGameEntityType() == GameEntityType.IsoObject) {
            this.sendUpdateUsingPlayer();
         }
      }

   }

   protected final void sendServerEntityPacketTo(IsoPlayer var1, EntityPacketData var2) {
      GameEntityNetwork.sendPacketDataTo(var1, var2, this, (Object)null);
   }

   protected final void sendClientEntityPacket(EntityPacketData var1) {
      GameEntityNetwork.sendPacketData(var1, this, (Object)null, (Object)null, true);
   }

   protected final void sendServerEntityPacket(EntityPacketData var1, UdpConnection var2) {
      GameEntityNetwork.sendPacketData(var1, this, (Object)null, var2, true);
   }

   protected final boolean onReceiveEntityPacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         case UpdateUsingPlayer:
            this.receiveUpdateUsingPlayer(var1, var3);
            return true;
         case SyncGameEntity:
            this.receiveSyncEntity(var1, var3);
            return true;
         default:
            throw new IOException("Packet type not found: " + var2);
      }
   }

   protected final void sendUpdateUsingPlayer() {
      EntityPacketData var1 = GameEntityNetwork.createPacketData(EntityPacketType.UpdateUsingPlayer);
      var1.bb.put((byte)(this.usingPlayer != null ? 1 : 0));
      if (this.usingPlayer != null) {
         GameWindow.WriteString(var1.bb, this.usingPlayer.getUsername());
         var1.bb.putShort(this.usingPlayer.getOnlineID());
      }

      if (GameClient.bClient) {
         this.sendClientEntityPacket(var1);
      } else {
         this.sendServerEntityPacket(var1, (UdpConnection)null);
      }

   }

   protected final void receiveUpdateUsingPlayer(ByteBuffer var1, UdpConnection var2) {
      if (var1.get() == 1) {
         String var3 = GameWindow.ReadString(var1);
         short var4 = var1.getShort();
         if (GameClient.bClient) {
            this.usingPlayer = GameClient.instance.getPlayerByOnlineID(var4);
         } else if (GameServer.bServer) {
            IsoPlayer var5 = GameServer.getPlayerByUserName(var3);
            if (this.usingPlayer == null) {
               this.usingPlayer = var5;
            }
         }
      } else {
         this.usingPlayer = null;
      }

      if (GameServer.bServer) {
         this.sendUpdateUsingPlayer();
      }

   }

   public final void sendSyncEntity(UdpConnection var1) {
      if (GameServer.bServer) {
         EntityPacketData var2 = GameEntityNetwork.createPacketData(EntityPacketType.SyncGameEntity);

         try {
            var2.bb.put((byte)(this.usingPlayer != null ? 1 : 0));
            if (this.usingPlayer != null) {
               var2.bb.putShort(this.usingPlayer.getOnlineID());
            }

            var2.bb.put(this.hasComponents() ? (byte)this.components.size() : 0);
            if (this.hasComponents()) {
               for(int var4 = 0; var4 < this.components.size(); ++var4) {
                  Component var3 = this.components.getForIndex(var4);
                  var2.bb.putShort(var3.getComponentType().GetID());
                  var3.saveSyncData(var2.bb);
               }
            }
         } catch (Exception var5) {
            var5.printStackTrace();
            EntityPacketData.release(var2);
            return;
         }

         if (GameClient.bClient) {
            this.sendClientEntityPacket(var2);
         } else {
            this.sendServerEntityPacket(var2, var1);
         }

      }
   }

   protected final void receiveSyncEntity(ByteBuffer var1, UdpConnection var2) {
      if (!GameClient.bClient) {
         DebugLog.General.warn("Can only call on client.");
      } else {
         try {
            if (var1.get() == 1) {
               short var3 = var1.getShort();
               this.usingPlayer = GameClient.instance.getPlayerByOnlineID(var3);
            } else {
               this.usingPlayer = null;
            }

            byte var9 = var1.get();
            if (var9 > 0) {
               for(int var5 = 0; var5 < var9; ++var5) {
                  short var6 = var1.getShort();
                  ComponentType var7 = ComponentType.FromId(var6);
                  Component var4 = this.getComponent(var7);
                  if (var4 == null) {
                     var4 = var7.CreateComponent();
                     this.addComponent(var4);
                  }

                  var4.loadSyncData(var1);
               }
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }

      }
   }
}
