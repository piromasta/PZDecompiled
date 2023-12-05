package zombie.commands.serverCommands;

import zombie.characters.IsoPlayer;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredRight;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketTypes;

@CommandNames({@CommandName(
   name = "teleportto"
), @CommandName(
   name = "tpto"
)})
@AltCommandArgs({@CommandArgs(
   required = {"(.+)", "(\\d+),(\\d+),(\\d+)"},
   argName = "Teleport user"
), @CommandArgs(
   required = {"(\\d+),(\\d+),(\\d+)"},
   argName = "teleport me"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_TeleportTo"
)
@RequiredRight(
   requiredRights = 62
)
public class TeleportToCommand extends CommandBase {
   public static final String teleportMe = "teleport me";
   public static final String teleportUser = "Teleport user";
   private String username;
   private Float[] coords;

   public TeleportToCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      int var3;
      switch (this.argsName) {
         case "teleport me":
            this.coords = new Float[3];

            for(var3 = 0; var3 < 3; ++var3) {
               this.coords[var3] = Float.parseFloat(this.getCommandArg(var3));
            }

            return this.TeleportMeToCoords();
         case "Teleport user":
            this.username = this.getCommandArg(0);
            this.coords = new Float[3];

            for(var3 = 0; var3 < 3; ++var3) {
               this.coords[var3] = Float.parseFloat(this.getCommandArg(var3 + 1));
            }

            return this.TeleportUserToCoords();
         default:
            return this.CommandArgumentsNotMatch();
      }
   }

   private String TeleportMeToCoords() {
      float var1 = this.coords[0];
      float var2 = this.coords[1];
      float var3 = this.coords[2];
      if (this.connection == null) {
         return "Error";
      } else {
         ByteBufferWriter var4 = this.connection.startPacket();
         PacketTypes.PacketType.Teleport.doPacket(var4);
         var4.putByte((byte)0);
         var4.putFloat(var1);
         var4.putFloat(var2);
         var4.putFloat(var3);
         PacketTypes.PacketType.Teleport.send(this.connection);
         if (this.connection.players[0] != null && this.connection.players[0].getNetworkCharacterAI() != null) {
            this.connection.players[0].getNetworkCharacterAI().resetSpeedLimiter();
         }

         ZLogger var10000 = LoggerManager.getLogger("admin");
         String var10001 = this.getExecutorUsername();
         var10000.write(var10001 + " teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3);
         return "teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3 + " please wait two seconds to show the map around you.";
      }
   }

   private String TeleportUserToCoords() {
      float var1 = this.coords[0];
      float var2 = this.coords[1];
      float var3 = this.coords[2];
      if (this.connection != null && this.connection.accessLevel == 2 && !this.username.equals(this.getExecutorUsername())) {
         return "An Observer can only teleport himself";
      } else {
         IsoPlayer var4 = GameServer.getPlayerByUserNameForCommand(this.username);
         if (var4 == null) {
            return "Can't find player " + this.username;
         } else {
            UdpConnection var5 = GameServer.getConnectionFromPlayer(var4);
            ByteBufferWriter var6 = var5.startPacket();
            PacketTypes.PacketType.Teleport.doPacket(var6);
            var6.putByte((byte)0);
            var6.putFloat(var1);
            var6.putFloat(var2);
            var6.putFloat(var3);
            PacketTypes.PacketType.Teleport.send(var5);
            if (var4.getNetworkCharacterAI() != null) {
               var4.getNetworkCharacterAI().resetSpeedLimiter();
            }

            ZLogger var10000 = LoggerManager.getLogger("admin");
            String var10001 = this.getExecutorUsername();
            var10000.write(var10001 + " teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3);
            return this.username + " teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3 + " please wait two seconds to show the map around you.";
         }
      }
   }

   private String CommandArgumentsNotMatch() {
      return this.getHelp();
   }
}
