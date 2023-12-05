package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;
import zombie.GameWindow;
import zombie.core.Rand;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LogSeverity;
import zombie.gameStates.GameLoadingState;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketValidator;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Recipe;

public class ValidatePacket implements INetworkPacket {
   private long checksum;
   private long checksumFromClient;
   private int salt;
   private byte flags;

   public ValidatePacket() {
   }

   public void setSalt(int var1, boolean var2, boolean var3, boolean var4) {
      this.salt = var1;
      this.flags = 0;
      this.flags = (byte)(this.flags | (var2 ? 1 : 0));
      this.flags = (byte)(this.flags | (var3 ? 2 : 0));
      this.flags = (byte)(this.flags | (var4 ? 4 : 0));
   }

   public void process(UdpConnection var1) {
      if (GameClient.bClient) {
         this.checksum = this.calculateChecksum(var1, this.salt);
         GameClient.sendValidatePacket(this);
         if (DebugOptions.instance.MultiplayerFailChecksum.getValue() && (this.flags & 1) != 0) {
            ArrayList var2 = ScriptManager.instance.getAllRecipes();
            Recipe var3 = (Recipe)var2.get(Rand.Next(var2.size()));
            var3.TimeToMake = (float)Rand.Next(32767);
            DebugLog.Multiplayer.debugln("Failed recipe \"%s\"", var3.getOriginalname());
         }

         if ((this.flags & 2) != 0) {
            GameLoadingState.Done();
         }
      } else if (GameServer.bServer) {
         this.salt = var1.validator.getSalt();
         this.checksum = this.calculateChecksum(var1, this.salt);
         if ((this.flags & 4) == 0) {
            if (this.checksumFromClient != this.checksum) {
               var1.validator.failChecksum();
            }

            if (var1.validator.isFailed()) {
               var1.validator.sendChecksum(false, false, true);
            } else {
               var1.validator.successChecksum();
               if ((this.flags & 1) != 0) {
                  var1.validator.sendChecksum(false, true, false);
               }
            }
         }
      }

   }

   private long calculateChecksum(UdpConnection var1, int var2) {
      if ((this.flags & 4) != 0) {
         var1.validator.details.clear();
      }

      CRC32 var3 = new CRC32();
      CRC32 var4 = new CRC32();
      ByteBuffer var5 = ByteBuffer.allocate(8);
      var3.update(var2);
      ArrayList var6 = ScriptManager.instance.getAllRecipes();
      Iterator var7 = var6.iterator();

      while(var7.hasNext()) {
         Recipe var8 = (Recipe)var7.next();
         var4.reset();
         var5.clear();
         var4.update(var8.getOriginalname().getBytes());
         var4.update((int)var8.TimeToMake);
         Iterator var9;
         if (var8.skillRequired != null) {
            var9 = var8.skillRequired.iterator();

            while(var9.hasNext()) {
               Recipe.RequiredSkill var10 = (Recipe.RequiredSkill)var9.next();
               var4.update(var10.getPerk().index());
               var4.update(var10.getLevel());
            }
         }

         var9 = var8.getSource().iterator();

         while(var9.hasNext()) {
            Recipe.Source var14 = (Recipe.Source)var9.next();
            Iterator var11 = var14.getItems().iterator();

            while(var11.hasNext()) {
               String var12 = (String)var11.next();
               var4.update(var12.getBytes());
            }
         }

         var4.update(var8.getResult().getType().getBytes());
         var4.update(var8.getResult().getModule().getBytes());
         var4.update(var8.getResult().getCount());
         long var13 = var4.getValue();
         var5.putLong(var13);
         var5.position(0);
         var3.update(var5);
         if ((this.flags & 4) != 0) {
            var1.validator.details.put(var8.getOriginalname(), new PacketValidator.RecipeDetails(var8.getOriginalname(), var13, (int)var8.TimeToMake, var8.skillRequired, var8.getSource(), var8.getResult().getType(), var8.getResult().getModule(), var8.getResult().getCount()));
         }
      }

      return var3.getValue();
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      try {
         this.flags = var1.get();
         if (GameClient.bClient) {
            this.salt = var1.getInt();
         } else if (GameServer.bServer) {
            this.checksumFromClient = var1.getLong();
            if ((this.flags & 4) != 0) {
               var2.validator.detailsFromClient.clear();
               int var3 = var1.getInt();

               for(int var4 = 0; var4 < var3; ++var4) {
                  var2.validator.detailsFromClient.put(GameWindow.ReadString(var1), new PacketValidator.RecipeDetails(var1));
               }
            }
         }
      } catch (Exception var5) {
         DebugLog.Multiplayer.printException(var5, "Parse error. Probably, \"" + var2.username + "\" client is outdated", LogSeverity.Error);
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putByte(this.flags);
      if (GameServer.bServer) {
         var1.putInt(this.salt);
      } else if (GameClient.bClient) {
         var1.putLong(this.checksum);
         if ((this.flags & 4) != 0) {
            int var2 = GameClient.connection.validator.details.size();
            var1.putInt(var2);
            Iterator var3 = GameClient.connection.validator.details.entrySet().iterator();

            while(var3.hasNext()) {
               Map.Entry var4 = (Map.Entry)var3.next();
               var1.putUTF((String)var4.getKey());
               ((PacketValidator.RecipeDetails)var4.getValue()).write(var1);
            }
         }
      }

   }

   public void log(UdpConnection var1, String var2) {
      if (this.flags != 0) {
         ConnectionManager.log(var2, String.format("checksum-packet-%d", this.flags), var1);
      }

   }

   public static enum ValidateState {
      Request,
      Success;

      private ValidateState() {
      }
   }
}
