package zombie.entity.components.script;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.raknet.UdpConnection;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;

@DebugClassFields
public class EntityScriptInfo extends Component {
   private GameEntityScript script;
   private String originalScript;
   private boolean originalIsItem = false;

   private EntityScriptInfo() {
      super(ComponentType.Script);
   }

   public void setOriginalScript(GameEntityScript var1) {
      this.originalIsItem = var1 instanceof Item;
      this.originalScript = var1.getScriptObjectFullType();
      this.script = var1;
   }

   public boolean isOriginalIsItem() {
      return this.originalIsItem;
   }

   public String getOriginalScript() {
      return this.originalScript;
   }

   public GameEntityScript getScript() {
      return this.script;
   }

   protected void reset() {
      super.reset();
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
      var1.put((byte)(this.originalIsItem ? 1 : 0));
      var1.put((byte)(this.originalScript != null ? 1 : 0));
      if (this.originalScript != null) {
         GameWindow.WriteString(var1, this.originalScript);
      }

   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.originalIsItem = var1.get() == 1;
      if (var1.get() == 1) {
         this.originalScript = GameWindow.ReadString(var1);
      } else {
         this.originalScript = null;
      }

      if (this.originalScript != null) {
         if (this.originalIsItem) {
            this.script = ScriptManager.instance.getItem(this.originalScript);
         } else {
            this.script = ScriptManager.instance.getGameEntityScript(this.originalScript);
         }
      }

   }
}
