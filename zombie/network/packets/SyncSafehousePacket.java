package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.PacketValidator;
import zombie.network.ServerOptions;

public class SyncSafehousePacket implements INetworkPacket {
   final byte requiredManagerAccessLevel = 56;
   int x;
   int y;
   short w;
   short h;
   public String ownerUsername;
   ArrayList<String> members = new ArrayList();
   ArrayList<String> membersRespawn = new ArrayList();
   public boolean remove = false;
   String title = "";
   public SafeHouse safehouse;
   public boolean shouldCreateChat;

   public SyncSafehousePacket() {
   }

   public void set(SafeHouse var1, boolean var2) {
      this.x = var1.getX();
      this.y = var1.getY();
      this.w = (short)var1.getW();
      this.h = (short)var1.getH();
      this.ownerUsername = var1.getOwner();
      this.members.clear();
      this.members.addAll(var1.getPlayers());
      this.membersRespawn.clear();
      this.membersRespawn.addAll(var1.playersRespawn);
      this.remove = var2;
      this.title = var1.getTitle();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.x = var1.getInt();
      this.y = var1.getInt();
      this.w = var1.getShort();
      this.h = var1.getShort();
      this.ownerUsername = GameWindow.ReadString(var1);
      short var3 = var1.getShort();
      this.members.clear();

      for(int var4 = 0; var4 < var3; ++var4) {
         this.members.add(GameWindow.ReadString(var1));
      }

      short var6 = var1.getShort();

      for(int var5 = 0; var5 < var6; ++var5) {
         this.membersRespawn.add(GameWindow.ReadString(var1));
      }

      this.remove = var1.get() == 1;
      this.title = GameWindow.ReadString(var1);
   }

   public void write(ByteBufferWriter var1) {
      var1.putInt(this.x);
      var1.putInt(this.y);
      var1.putShort(this.w);
      var1.putShort(this.h);
      var1.putUTF(this.ownerUsername);
      var1.putShort((short)this.members.size());
      Iterator var2 = this.members.iterator();

      String var3;
      while(var2.hasNext()) {
         var3 = (String)var2.next();
         var1.putUTF(var3);
      }

      var1.putShort((short)this.membersRespawn.size());
      var2 = this.membersRespawn.iterator();

      while(var2.hasNext()) {
         var3 = (String)var2.next();
         var1.putUTF(var3);
      }

      var1.putByte((byte)(this.remove ? 1 : 0));
      var1.putUTF(this.title);
   }

   public void process() {
      this.safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
      this.shouldCreateChat = false;
      if (this.safehouse == null) {
         this.safehouse = SafeHouse.addSafeHouse(this.x, this.y, this.w, this.h, this.ownerUsername, GameClient.bClient);
         this.shouldCreateChat = true;
      }

      if (this.safehouse != null) {
         this.safehouse.getPlayers().clear();
         this.safehouse.getPlayers().addAll(this.members);
         this.safehouse.playersRespawn.clear();
         this.safehouse.playersRespawn.addAll(this.membersRespawn);
         this.safehouse.setTitle(this.title);
         this.safehouse.setOwner(this.ownerUsername);
         if (this.remove) {
            SafeHouse.getSafehouseList().remove(this.safehouse);
            int var10000 = this.x;
            DebugLog.log("safehouse: removed " + var10000 + "," + this.y + "," + this.w + "," + this.h + " owner=" + this.safehouse.getOwner());
         }

      }
   }

   public boolean validate(UdpConnection var1) {
      boolean var2 = (var1.accessLevel & 56) != 0;
      this.safehouse = SafeHouse.getSafeHouse(this.x, this.y, this.w, this.h);
      if (this.safehouse == null) {
         if (var1.accessLevel == 1 && SafeHouse.hasSafehouse(this.ownerUsername) != null) {
            if (ServerOptions.instance.AntiCheatProtectionType19.getValue() && PacketValidator.checkUser(var1)) {
               PacketValidator.doKickUser(var1, this.getClass().getSimpleName(), "Type19", this.getDescription());
            }

            return false;
         } else {
            double var3 = 100.0 * ServerOptions.instance.AntiCheatProtectionType20ThresholdMultiplier.getValue();
            if (var1.accessLevel != 1 || !((double)this.h > var3) && !((double)this.w > var3)) {
               return true;
            } else {
               if (ServerOptions.instance.AntiCheatProtectionType20.getValue() && PacketValidator.checkUser(var1)) {
                  PacketValidator.doKickUser(var1, this.getClass().getSimpleName(), "Type20", this.getDescription());
               }

               return false;
            }
         }
      } else if (!var2) {
         return true;
      } else {
         return PacketValidator.checkSafehouseAuth(var1, this.safehouse.getOwner(), this.getClass().getSimpleName());
      }
   }

   public String getDescription() {
      String var1 = "\n\t" + this.getClass().getSimpleName() + " [";
      var1 = var1 + "position=(" + this.x + ", " + this.y + ", " + this.w + ", " + this.h + ") | ";
      var1 = var1 + "ownerUsername=" + this.ownerUsername + " | ";
      var1 = var1 + "members=" + Arrays.toString(this.members.toArray()) + " | ";
      var1 = var1 + "membersRespawn=" + Arrays.toString(this.membersRespawn.toArray()) + " | ";
      var1 = var1 + "remove=" + this.remove + " | ";
      var1 = var1 + "title=" + this.title + "] ";
      return var1;
   }
}
