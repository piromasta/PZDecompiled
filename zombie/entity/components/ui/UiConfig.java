package zombie.entity.components.ui;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

@DebugClassFields
public class UiConfig extends Component {
   private String xuiSkinName;
   private XuiSkin skin;
   private String entityStyleName = null;
   private boolean uiEnabled = true;

   private UiConfig() {
      super(ComponentType.UiConfig);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      UiConfigScript var2 = (UiConfigScript)var1;
      this.setSkin(var2.getXuiSkinName());
      this.entityStyleName = var2.getEntityStyle();
      this.uiEnabled = var2.isUiEnabled();
   }

   private void setSkin(String var1) {
      this.xuiSkinName = var1;
      if (this.xuiSkinName != null) {
         this.skin = XuiManager.GetSkin(var1);
         if (this.skin == null) {
            DebugLog.General.warn("Could not find skin: " + var1);
            this.skin = XuiManager.GetDefaultSkin();
         }
      } else {
         this.skin = XuiManager.GetDefaultSkin();
      }

   }

   public XuiSkin getSkin() {
      return this.getSkin(false);
   }

   public XuiSkin getSkinOrDefault() {
      return this.getSkin(true);
   }

   public XuiSkin getSkin(boolean var1) {
      if (this.skin == null) {
         return XuiManager.GetDefaultSkin();
      } else {
         if (this.skin.isInvalidated()) {
            this.setSkin(this.xuiSkinName);
         }

         return this.skin;
      }
   }

   public XuiSkin.EntityUiStyle getEntityUiStyle() {
      XuiSkin var1 = this.getSkinOrDefault();
      return var1.getEntityUiStyle(this.entityStyleName);
   }

   public String getEntityStyleName() {
      return this.entityStyleName;
   }

   public boolean isUiEnabled() {
      return this.uiEnabled;
   }

   public String getEntityDisplayName() {
      XuiSkin var1 = this.getSkinOrDefault();
      return var1 != null ? var1.getEntityDisplayName(this.entityStyleName) : GameEntity.getDefaultEntityDisplayName();
   }

   protected void reset() {
      super.reset();
      this.skin = null;
      this.xuiSkinName = null;
      this.entityStyleName = null;
      this.uiEnabled = true;
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
      var1.put((byte)(this.xuiSkinName != null ? 1 : 0));
      if (this.xuiSkinName != null) {
         GameWindow.WriteString(var1, this.xuiSkinName);
      }

      var1.put((byte)(this.entityStyleName != null ? 1 : 0));
      if (this.entityStyleName != null) {
         GameWindow.WriteString(var1, this.entityStyleName);
      }

      var1.put((byte)(this.uiEnabled ? 1 : 0));
   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.xuiSkinName = null;
      this.entityStyleName = null;
      if (var1.get() == 1) {
         this.xuiSkinName = GameWindow.ReadString(var1);
      }

      if (var1.get() == 1) {
         this.entityStyleName = GameWindow.ReadString(var1);
      }

      this.setSkin(this.xuiSkinName);
      this.uiEnabled = var1.get() == 1;
   }
}
