package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.ComponentScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public abstract class Component {
   @DebugNonRecursive
   protected GameEntity owner;
   private final ComponentType componentType;

   protected Component(ComponentType var1) {
      this.componentType = (ComponentType)Objects.requireNonNull(var1);
      if (this.componentType.equals(ComponentType.Undefined)) {
         throw new IllegalArgumentException("ComponentType error in GameEntity Component.");
      }
   }

   public String toString() {
      return "[component: " + this.componentType.toString() + "]";
   }

   public boolean isRunningInMeta() {
      return this.owner != null && this.owner.isMeta();
   }

   public boolean isQualifiesForMetaStorage() {
      return true;
   }

   public final boolean isAddedToEngine() {
      return this.getGameEntity() != null && this.getGameEntity().isValidEngineEntity();
   }

   public final GameEntity getOwner() {
      return this.owner;
   }

   public final GameEntity getGameEntity() {
      return this.owner;
   }

   public final boolean isUsingPlayer(IsoPlayer var1) {
      return this.owner != null ? this.owner.isUsingPlayer(var1) : false;
   }

   public final IsoPlayer getUsingPlayer() {
      return this.owner != null ? this.owner.getUsingPlayer() : null;
   }

   public final ComponentType getComponentType() {
      return this.componentType;
   }

   protected final void setOwner(GameEntity var1) {
      if (this.owner != null && var1 != null) {
         DebugLog.General.error("Setting owner while owner exists.");
      }

      this.owner = var1;
   }

   public final <T extends Component> T getComponent(ComponentType var1) {
      if (this.owner != null) {
         return this.owner.getComponent(var1);
      } else {
         DebugLog.General.warn("GetComponent owner == null");
         return null;
      }
   }

   public boolean isValid() {
      return this.owner != null && this.componentType.isValidGameEntityType(this.owner.getGameEntityType());
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
   }

   public final boolean isRenderLast() {
      return this.componentType.isRenderLast();
   }

   public int getRenderLastPriority() {
      return 1000;
   }

   protected void renderlast() {
   }

   protected void reset() {
   }

   protected <T extends ComponentScript> void readFromScript(T var1) {
   }

   public boolean isValidOwnerType(GameEntityType var1) {
      return this.componentType.isValidGameEntityType(var1);
   }

   protected void onAddedToOwner() {
   }

   protected void onRemovedFromOwner() {
   }

   protected final void sendComponentEvent(ComponentEventType var1) {
      if (this.owner != null) {
         this.owner.sendComponentEvent(this, var1);
      } else if (Core.bDebug) {
         DebugLog.General.warn("Cannot send component event, no owner.");
      }

   }

   protected final void sendComponentEvent(ComponentEvent var1) {
      if (this.owner != null) {
         this.owner.sendComponentEvent(this, var1);
      } else if (Core.bDebug) {
         DebugLog.General.warn("Cannot send component event, no owner.");
      }

   }

   protected void onConnectComponents() {
   }

   protected void onFirstCreation() {
   }

   protected void onComponentEvent(ComponentEvent var1) {
   }

   protected void onEntityEvent(EntityEvent var1) {
   }

   public final void sendServerPacketTo(IsoPlayer var1, EntityPacketData var2) {
      if (!GameServer.bServer) {
         DebugLog.General.warn("Can only send server.");
      }

      GameEntityNetwork.sendPacketDataTo(var1, var2, this.owner, this);
   }

   protected final void sendClientPacket(EntityPacketData var1) {
      if (!GameClient.bClient) {
         DebugLog.General.warn("Can only send client.");
      }

      GameEntityNetwork.sendPacketData(var1, this.owner, this, (Object)null, true);
   }

   protected final void sendServerPacket(EntityPacketData var1, UdpConnection var2) {
      if (!GameServer.bServer) {
         DebugLog.General.warn("Can only send server.");
      }

      GameEntityNetwork.sendPacketData(var1, this.owner, this, var2, true);
   }

   protected abstract boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException;

   protected abstract void saveSyncData(ByteBuffer var1) throws IOException;

   protected abstract void loadSyncData(ByteBuffer var1) throws IOException;

   protected void save(ByteBuffer var1) throws IOException {
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
   }
}
