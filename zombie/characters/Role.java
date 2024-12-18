package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

public class Role {
   private String name;
   private String description;
   private Color color;
   private boolean isReadOnly = false;
   private HashSet<Capability> capabilities = new HashSet();

   public Role(String var1) {
      this.name = var1;
      this.description = "";
      this.color = Color.white;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String var1) {
      this.description = var1;
   }

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color var1) {
      this.color = var1;
   }

   public boolean isReadOnly() {
      return this.isReadOnly;
   }

   public void setReadOnly() {
      this.isReadOnly = true;
   }

   public ArrayList<String> getDefaults() {
      ArrayList var1 = new ArrayList();
      if (Roles.getDefaultForBanned() == this) {
         var1.add("Banned");
      }

      if (Roles.getDefaultForUser() == this) {
         var1.add("User");
      }

      if (Roles.getDefaultForNewUser() == this) {
         var1.add("NewUser");
      }

      if (Roles.getDefaultForPriorityUser() == this) {
         var1.add("PriorityUser");
      }

      if (Roles.getDefaultForObserver() == this) {
         var1.add("Observer");
      }

      if (Roles.getDefaultForGM() == this) {
         var1.add("GM");
      }

      if (Roles.getDefaultForOverseer() == this) {
         var1.add("Oversee");
      }

      if (Roles.getDefaultForModerator() == this) {
         var1.add("Moderator");
      }

      if (Roles.getDefaultForAdmin() == this) {
         var1.add("Admin");
      }

      return var1;
   }

   public boolean haveCapability(Capability var1) {
      return isUsingDebugMode() || this.capabilities.contains(var1);
   }

   public boolean addCapability(Capability var1) {
      return this.isReadOnly ? false : this.capabilities.add(var1);
   }

   public boolean removeCapability(Capability var1) {
      return this.isReadOnly ? false : this.capabilities.remove(var1);
   }

   public void cleanCapability() {
      if (!this.isReadOnly) {
         this.capabilities.clear();
      }
   }

   public void send(ByteBuffer var1) {
      GameWindow.WriteStringUTF(var1, this.name);
      GameWindow.WriteStringUTF(var1, this.description);
      var1.putFloat(this.color.r);
      var1.putFloat(this.color.g);
      var1.putFloat(this.color.b);
      var1.putFloat(this.color.a);
      var1.put((byte)this.capabilities.size());
      Iterator var2 = this.capabilities.iterator();

      while(var2.hasNext()) {
         Capability var3 = (Capability)var2.next();
         var1.put((byte)var3.ordinal());
      }

      var1.put((byte)(this.isReadOnly ? 1 : 0));
   }

   public void parse(ByteBuffer var1) {
      this.name = GameWindow.ReadString(var1);
      this.description = GameWindow.ReadString(var1);
      this.color = new Color();
      this.color.r = var1.getFloat();
      this.color.g = var1.getFloat();
      this.color.b = var1.getFloat();
      this.color.a = var1.getFloat();
      this.capabilities.clear();
      byte var2 = var1.get();

      for(int var3 = 0; var3 < var2; ++var3) {
         byte var4 = var1.get();
         if (var4 > Capability.values().length) {
            DebugLog.General.printStackTrace("Role.load error. id=" + var4);
         }

         Capability var5 = Capability.values()[var4];
         if (var5 != null) {
            this.capabilities.add(var5);
         }
      }

      this.isReadOnly = var1.get() > 0;
   }

   public void save(ByteBuffer var1) {
      GameWindow.WriteStringUTF(var1, this.name);
      GameWindow.WriteStringUTF(var1, this.description);
      var1.putFloat(this.color.r);
      var1.putFloat(this.color.g);
      var1.putFloat(this.color.b);
      var1.putFloat(this.color.a);
      var1.putShort((short)((byte)this.capabilities.size()));
      Iterator var2 = this.capabilities.iterator();

      while(var2.hasNext()) {
         Capability var3 = (Capability)var2.next();
         GameWindow.WriteStringUTF(var1, var3.name());
      }

   }

   public void load(ByteBuffer var1, int var2) {
      this.name = GameWindow.ReadString(var1);
      this.description = GameWindow.ReadString(var1);
      this.color.r = var1.getFloat();
      this.color.g = var1.getFloat();
      this.color.b = var1.getFloat();
      this.color.a = var1.getFloat();
      this.capabilities.clear();
      short var3 = var1.getShort();

      for(int var4 = 0; var4 < var3; ++var4) {
         String var5 = GameWindow.ReadString(var1);

         try {
            Capability var6 = Capability.valueOf(var5);
            this.capabilities.add(var6);
         } catch (Exception var7) {
            DebugLog.General.printStackTrace("capabilityName=" + var5);
         }
      }

   }

   public short rightLevel() {
      return !this.capabilities.contains(Capability.LoginOnServer) ? -1 : (short)this.capabilities.size();
   }

   public static boolean haveCapability(IsoMovingObject var0, Capability var1) {
      if (isUsingDebugMode()) {
         return true;
      } else {
         IsoPlayer var2 = (IsoPlayer)Type.tryCastTo(var0, IsoPlayer.class);
         return var2 != null && var2.getRole() != null ? var2.getRole().haveCapability(var1) : false;
      }
   }

   public static boolean isUsingDebugMode() {
      return Core.bDebug && !GameClient.bClient && !GameServer.bServer;
   }
}
