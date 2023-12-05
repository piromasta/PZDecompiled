package zombie.commands.serverCommands;

import zombie.characters.IsoPlayer;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredRight;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;

@CommandName(
   name = "addvehicle"
)
@AltCommandArgs({@CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)"},
   argName = "Script Only"
), @CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(\\d+),(\\d+),(\\d+)"},
   argName = "Script And Coordinate"
), @CommandArgs(
   required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(.+)"},
   argName = "Script And Player"
)})
@CommandHelp(
   helpText = "UI_ServerOptionDesc_AddVehicle"
)
@RequiredRight(
   requiredRights = 60
)
public class AddVehicleCommand extends CommandBase {
   public static final String scriptOnly = "Script Only";
   public static final String scriptPlayer = "Script And Player";
   public static final String scriptCoordinate = "Script And Coordinate";

   public AddVehicleCommand(String var1, String var2, String var3, UdpConnection var4) {
      super(var1, var2, var3, var4);
   }

   protected String Command() {
      String var1 = this.getCommandArg(0);
      VehicleScript var2 = ScriptManager.instance.getVehicle(var1);
      if (var2 == null) {
         return "Unknown vehicle script \"" + var1 + "\"";
      } else {
         String var10000 = var2.getModule().getName();
         var1 = var10000 + "." + var2.getName();
         String var3;
         int var4;
         int var5;
         int var6;
         IsoPlayer var7;
         if (this.argsName.equals("Script And Player")) {
            var3 = this.getCommandArg(1);
            var7 = GameServer.getPlayerByUserNameForCommand(var3);
            if (var7 == null) {
               return "User \"" + var3 + "\" not found";
            }

            var4 = PZMath.fastfloor(var7.getX());
            var5 = PZMath.fastfloor(var7.getY());
            var6 = PZMath.fastfloor(var7.getZ());
         } else if (this.argsName.equals("Script And Coordinate")) {
            var4 = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(1)));
            var5 = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(2)));
            var6 = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(3)));
         } else {
            if (this.connection == null) {
               return "Pass a username or coordinate";
            }

            var3 = this.getExecutorUsername();
            var7 = GameServer.getPlayerByUserNameForCommand(var3);
            if (var7 == null) {
               return "User \"" + var3 + "\" not found";
            }

            var4 = PZMath.fastfloor(var7.getX());
            var5 = PZMath.fastfloor(var7.getY());
            var6 = PZMath.fastfloor(var7.getZ());
         }

         if (var6 > 0) {
            return "Z coordinate must be 0 for now";
         } else {
            IsoGridSquare var9 = ServerMap.instance.getGridSquare(var4, var5, var6);
            if (var9 == null) {
               return "Invalid location " + var4 + "," + var5 + "," + var6;
            } else {
               BaseVehicle var8 = new BaseVehicle(IsoWorld.instance.CurrentCell);
               var8.setScriptName(var1);
               var8.setX((float)var4 - 1.0F);
               var8.setY((float)var5 - 0.1F);
               var8.setZ((float)var6 + 0.2F);
               if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var8)) {
                  var8.setSquare(var9);
                  var8.square.chunk.vehicles.add(var8);
                  var8.chunk = var8.square.chunk;
                  var8.addToWorld();
                  VehiclesDB2.instance.addVehicle(var8);
                  return "Vehicle spawned";
               } else {
                  return "ERROR: I can not spawn the vehicle. Invalid position. Try to change position.";
               }
            }
         }
      }
   }
}
