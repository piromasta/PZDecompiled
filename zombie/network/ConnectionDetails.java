package zombie.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionMain;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.ConnectToServerState;
import zombie.gameStates.MainScreenState;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.Vector3;
import zombie.world.WorldDictionary;

public class ConnectionDetails {
   public ConnectionDetails() {
   }

   public static void write(UdpConnection var0, ServerWorldDatabase.LogonResult var1, ByteBuffer var2) {
      try {
         writeServerDetails(var2, var0, var1);
         writeGameMap(var2);
         if (SteamUtils.isSteamModeEnabled()) {
            writeWorkshopItems(var2);
         }

         writeMods(var2);
         writeStartLocation(var2);
         writeServerOptions(var2);
         writeSandboxOptions(var2);
         writeGameTime(var2);
         writeErosionMain(var2);
         writeGlobalObjects(var2);
         writeResetID(var2);
         writeBerries(var2);
         writeWorldDictionary(var2);
      } catch (Throwable var4) {
         throw new RuntimeException(var4);
      }
   }

   public static void parse(ByteBuffer var0) {
      ConnectionManager.log("receive-packet", "connection-details", (UdpConnection)null);
      Calendar var1 = Calendar.getInstance();
      ConnectToServerState var2 = new ConnectToServerState(var0);
      var2.enter();
      MainScreenState.getInstance().setConnectToServerState(var2);
      DebugLog.General.println("LOGGED INTO : %d millisecond", var1.getTimeInMillis() - GameClient.startAuth.getTimeInMillis());
   }

   private static void writeServerDetails(ByteBuffer var0, UdpConnection var1, ServerWorldDatabase.LogonResult var2) {
      var0.put((byte)(var1.isCoopHost ? 1 : 0));
      var0.putInt(ServerOptions.getInstance().getMaxPlayers());
      if (SteamUtils.isSteamModeEnabled() && CoopSlave.instance != null && !var1.isCoopHost) {
         var0.put((byte)1);
         var0.putLong(CoopSlave.instance.hostSteamID);
         GameWindow.WriteString(var0, GameServer.ServerName);
      } else {
         var0.put((byte)0);
      }

      int var3 = var1.playerIDs[0] / 4;
      var0.put((byte)var3);
      GameWindow.WriteString(var0, var2.accessLevel);
   }

   private static void writeGameMap(ByteBuffer var0) {
      GameWindow.WriteString(var0, GameServer.GameMap);
   }

   private static void writeWorkshopItems(ByteBuffer var0) {
      var0.putShort((short)GameServer.WorkshopItems.size());

      for(int var1 = 0; var1 < GameServer.WorkshopItems.size(); ++var1) {
         var0.putLong((Long)GameServer.WorkshopItems.get(var1));
         var0.putLong(GameServer.WorkshopTimeStamps[var1]);
      }

   }

   private static void writeMods(ByteBuffer var0) {
      ArrayList var1 = new ArrayList();

      ChooseGameInfo.Mod var2;
      Iterator var3;
      for(var3 = GameServer.ServerMods.iterator(); var3.hasNext(); var1.add(var2)) {
         String var4 = (String)var3.next();
         String var5 = ZomboidFileSystem.instance.getModDir(var4);
         if (var5 != null) {
            try {
               var2 = ChooseGameInfo.readModInfo(var5);
            } catch (Exception var7) {
               ExceptionLogger.logException(var7);
               var2 = new ChooseGameInfo.Mod(var4);
               var2.setId(var4);
               var2.setName(var4);
            }
         } else {
            var2 = new ChooseGameInfo.Mod(var4);
            var2.setId(var4);
            var2.setName(var4);
         }
      }

      var0.putInt(var1.size());
      var3 = var1.iterator();

      while(var3.hasNext()) {
         ChooseGameInfo.Mod var8 = (ChooseGameInfo.Mod)var3.next();
         GameWindow.WriteString(var0, var8.getId());
         GameWindow.WriteString(var0, var8.getUrl());
         GameWindow.WriteString(var0, var8.getName());
      }

   }

   private static void writeStartLocation(ByteBuffer var0) {
      Object var1 = null;
      Vector3 var2 = ServerMap.instance.getStartLocation((ServerWorldDatabase.LogonResult)var1);
      var0.putInt((int)var2.x);
      var0.putInt((int)var2.y);
      var0.putInt((int)var2.z);
   }

   private static void writeServerOptions(ByteBuffer var0) {
      var0.putInt(ServerOptions.instance.getPublicOptions().size());
      Iterator var1 = ServerOptions.instance.getPublicOptions().iterator();

      while(var1.hasNext()) {
         String var2 = (String)var1.next();
         GameWindow.WriteString(var0, var2);
         GameWindow.WriteString(var0, ServerOptions.instance.getOption(var2));
      }

   }

   private static void writeSandboxOptions(ByteBuffer var0) throws IOException {
      SandboxOptions.instance.save(var0);
   }

   private static void writeGameTime(ByteBuffer var0) throws IOException {
      GameTime.getInstance().saveToPacket(var0);
   }

   private static void writeErosionMain(ByteBuffer var0) {
      ErosionMain.getInstance().getConfig().save(var0);
   }

   private static void writeGlobalObjects(ByteBuffer var0) throws IOException {
      SGlobalObjects.saveInitialStateForClient(var0);
   }

   private static void writeResetID(ByteBuffer var0) {
      var0.putInt(GameServer.ResetID);
   }

   private static void writeBerries(ByteBuffer var0) {
      GameWindow.WriteString(var0, Core.getInstance().getPoisonousBerry());
      GameWindow.WriteString(var0, Core.getInstance().getPoisonousMushroom());
   }

   private static void writeWorldDictionary(ByteBuffer var0) throws IOException {
      WorldDictionary.saveDataForClient(var0);
   }
}
