package zombie.entity.components.lua;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.entity.ComponentScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
public class LuaComponent extends Component {
   private LuaComponent() {
      super(ComponentType.Lua);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
   }

   protected void renderlast() {
   }

   protected void reset() {
      super.reset();
   }

   public boolean isValid() {
      return super.isValid();
   }

   protected void onAddedToOwner() {
   }

   protected void onRemovedFromOwner() {
   }

   protected void onConnectComponents() {
   }

   protected void onFirstCreation() {
   }

   protected void onComponentEvent(ComponentEvent var1) {
   }

   protected void onEntityEvent(EntityEvent var1) {
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         default:
            return false;
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
      this.save(var1);
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
      this.load(var1, 219);
   }

   protected void save(ByteBuffer var1) throws IOException {
      super.save(var1);
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
   }

   public static enum LuaCall {
      OnCanStartReason,
      OnCanStart,
      OnStart,
      OnUpdate,
      OnCancel,
      OnFinish,
      OnEvent,
      OnInfoUI,
      OnInitUI,
      OnUpdateUI,
      OnButtonClickUI;

      private LuaCall() {
      }
   }
}
