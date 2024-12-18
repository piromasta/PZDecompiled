package zombie.network;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

public class PVPLogTool {
   private static final int MAX_EVENTS = 10;
   private static final ArrayList<PVPEvent> events = new ArrayList();

   private PVPLogTool() {
   }

   public static void clearEvents() {
      Iterator var0 = events.iterator();

      while(var0.hasNext()) {
         PVPEvent var1 = (PVPEvent)var0.next();
         var1.reset((String)null, (String)null, (String)null, 0.0F, 0.0F, 0.0F);
      }

   }

   public static ArrayList<PVPEvent> getEvents() {
      return events;
   }

   public static void logSafety(IsoPlayer var0, String var1) {
      String var2 = String.format("Safety: \"%s\" %s %s", var0.getUsername(), var1, var0.getSafety().isEnabled());
      String var3 = String.format("Safety: \"%s\" %s %s %s", var0.getUsername(), LoggerManager.getPlayerCoords(var0), var1, var0.getSafety().isEnabled());
      log(var2, var3, "LOG");
   }

   public static void logKill(IsoPlayer var0, IsoPlayer var1) {
      String var2 = String.format("Kill: \"%s\" killed \"%s\"", var0.getUsername(), var1.getUsername());
      String var3 = String.format("Kill: \"%s\" %s killed \"%s\" %s", var0.getUsername(), LoggerManager.getPlayerCoords(var0), var1.getUsername(), LoggerManager.getPlayerCoords(var1));
      log(var2, var3, "IMPORTANT");
      if (ServerOptions.instance.AnnounceDeath.getValue()) {
         ChatServer.getInstance().sendMessageToServerChat(var2);
      }

   }

   public static void logCombat(String var0, String var1, String var2, String var3, float var4, float var5, float var6, String var7, float var8) {
      String var9 = String.format("Combat: \"%s\" hit \"%s\" \"%s\" %f", var0, var2, var7, var8);
      String var10 = String.format("Combat: \"%s\" %s hit \"%s\" %s weapon=\"%s\" damage=%f", var0, var1, var2, var3, var7, var8);
      log(var9, var10, "INFO");
      PVPEvent var11 = (PVPEvent)events.remove(9);
      var11.reset(var0, var2, var4, var5, var6);
      events.add(0, var11);
      Iterator var12 = GameServer.udpEngine.connections.iterator();

      while(var12.hasNext()) {
         UdpConnection var13 = (UdpConnection)var12.next();
         if (var13.role.haveCapability(Capability.CanSeeMessageForAdmin)) {
            INetworkPacket.send(var13, PacketTypes.PacketType.PVPEvents, false);
         }
      }

   }

   private static void log(String var0, String var1, String var2) {
      if (GameServer.bServer) {
         if (ServerOptions.getInstance().PVPLogToolChat.getValue()) {
            ChatServer.getInstance().sendMessageToAdminChat(var0);
         }

         if (ServerOptions.getInstance().PVPLogToolFile.getValue()) {
            LoggerManager.getLogger("pvp").write(var1, var2);
         }

         DebugLog.Multiplayer.debugln(var1);
      }

   }

   static {
      for(int var0 = 0; var0 < 10; ++var0) {
         events.add(new PVPEvent((String)null, (String)null, 0.0F, 0.0F, 0.0F));
      }

   }

   public static class PVPEvent {
      private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
      public String timestamp;
      public String wielder;
      public String target;
      public float x;
      public float y;
      public float z;

      public PVPEvent(String var1, String var2, float var3, float var4, float var5) {
         this.reset(var1, var2, var3, var4, var5);
      }

      public void reset(String var1, String var2, float var3, float var4, float var5) {
         this.reset(format.format(Calendar.getInstance().getTime()), var1, var2, var3, var4, var5);
      }

      public void reset(String var1, String var2, String var3, float var4, float var5, float var6) {
         this.x = var4;
         this.y = var5;
         this.z = var6;
         this.wielder = var2;
         this.target = var3;
         this.timestamp = var1;
      }

      public String getText() {
         return String.format("[%s] \"%s\" hit \"%s\"", this.timestamp, this.wielder, this.target);
      }

      public boolean isSet() {
         return !StringUtils.isNullOrEmpty(this.wielder) && !StringUtils.isNullOrEmpty(this.target);
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }

      public float getZ() {
         return this.z;
      }
   }
}
