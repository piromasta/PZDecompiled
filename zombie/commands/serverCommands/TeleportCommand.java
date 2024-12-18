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
   name = "teleport"
), @CommandName(
   name = "tp"
)})
@AltCommandArgs({@CommandArgs(
   required = {"(.+)"},
   argName = "just port to user"
), @CommandArgs(
   required = {"(.+)", "(.+)"},
   argName = "teleport user1 to user 2"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_Teleport"
)
@RequiredCapability(
   requiredCapability = Capability.TeleportToPlayer
)
public class TeleportCommand extends CommandBase {
   public static final String justToUser = "just port to user";
   public static final String portUserToUser = "teleport user1 to user 2";
   private String username1;
   private String username2;

   public TeleportCommand(String var1, Role var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      switch (this.argsName) {
         case "just port to user":
            this.username1 = this.getCommandArg(0);
            return this.TeleportMeToUser();
         case "teleport user1 to user 2":
            this.username1 = this.getCommandArg(0);
            this.username2 = this.getCommandArg(1);
            return this.TeleportUser1ToUser2();
         default:
            return this.CommandArgumentsNotMatch();
      }
   }

   private String TeleportMeToUser() {
      if (this.connection == null) {
         return "Need player to teleport to, ex /teleport user1 user2";
      } else {
         IsoPlayer var1 = GameServer.getPlayerByUserNameForCommand(this.username1);
         if (var1 != null) {
            GameServer.sendTeleport(this.connection.players[0], var1.getX(), var1.getY(), var1.getZ());
            this.username1 = var1.getDisplayName();
            ZLogger var10000 = LoggerManager.getLogger("admin");
            String var10001 = this.getExecutorUsername();
            var10000.write(var10001 + " teleport to " + this.username1);
            return "teleported to " + this.username1 + " please wait two seconds to show the map around you.";
         } else {
            return "Can't find player " + this.username1;
         }
      }
   }

   private String TeleportUser1ToUser2() {
      if (!this.getRole().haveCapability(Capability.TeleportPlayerToAnotherPlayer) && !this.username1.equals(this.getExecutorUsername())) {
         return "An Observer can only teleport himself";
      } else {
         IsoPlayer var1 = GameServer.getPlayerByUserNameForCommand(this.username1);
         IsoPlayer var2 = GameServer.getPlayerByUserNameForCommand(this.username2);
         if (var1 == null) {
            return "Can't find player " + this.username1;
         } else if (var2 == null) {
            return "Can't find player " + this.username2;
         } else {
            this.username1 = var1.getDisplayName();
            this.username2 = var2.getDisplayName();
            UdpConnection var3 = GameServer.getConnectionFromPlayer(var1);
            if (var3 == null) {
               return "No connection for player " + this.username1;
            } else {
               GameServer.sendTeleport(var1, var2.getX(), var2.getY(), var2.getZ());
               ZLogger var10000 = LoggerManager.getLogger("admin");
               String var10001 = this.getExecutorUsername();
               var10000.write(var10001 + " teleported " + this.username1 + " to " + this.username2);
               return "teleported " + this.username1 + " to " + this.username2;
            }
         }
      }
   }

   private String CommandArgumentsNotMatch() {
      return this.getHelp();
   }
}
