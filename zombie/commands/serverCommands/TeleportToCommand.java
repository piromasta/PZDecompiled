package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

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
@RequiredCapability(
   requiredCapability = Capability.TeleportToCoordinates
)
public class TeleportToCommand extends CommandBase {
   public static final String teleportMe = "teleport me";
   public static final String teleportUser = "Teleport user";
   private String username;
   private Float[] coords;

   public TeleportToCommand(String var1, Role var2, String var3, UdpConnection var4) {
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
      if (this.connection != null) {
         if (!this.connection.role.haveCapability(Capability.TeleportToCoordinates)) {
            return "An Observer can only teleport himself";
         } else {
            GameServer.sendTeleport(this.connection.players[0], var1, var2, var3);
            ZLogger var10000 = LoggerManager.getLogger("admin");
            String var10001 = this.getExecutorUsername();
            var10000.write(var10001 + " teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3);
            return "teleported to " + (int)var1 + "," + (int)var2 + "," + (int)var3 + " please wait two seconds to show the map around you.";
         }
      } else {
         return "Error";
      }
   }

   private String TeleportUserToCoords() {
      float var1 = this.coords[0];
      float var2 = this.coords[1];
      float var3 = this.coords[2];
      if (this.connection != null && !this.connection.role.haveCapability(Capability.TeleportPlayerToAnotherPlayer) && !this.username.equals(this.getExecutorUsername())) {
         return "An Observer can only teleport himself";
      } else {
         IsoPlayer var4 = GameServer.getPlayerByUserNameForCommand(this.username);
         if (var4 == null) {
            return "Can't find player " + this.username;
         } else {
            GameServer.sendTeleport(var4, var1, var2, var3);
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
