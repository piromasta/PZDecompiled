package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.chat.ChatServer;
import zombie.network.packets.SyncSafehousePacket;

public class SafeHouse {
   private int x = 0;
   private int y = 0;
   private int w = 0;
   private int h = 0;
   private static int diffError = 2;
   private String owner = null;
   private ArrayList<String> players = new ArrayList();
   private long lastVisited = 0L;
   private String title = "Safehouse";
   private int playerConnected = 0;
   private int openTimer = 0;
   private final String id;
   public ArrayList<String> playersRespawn = new ArrayList();
   private static final ArrayList<SafeHouse> safehouseList = new ArrayList();
   private static final ArrayList<IsoPlayer> tempPlayers = new ArrayList();

   public static void init() {
      safehouseList.clear();
   }

   public static SafeHouse addSafeHouse(int var0, int var1, int var2, int var3, String var4, boolean var5) {
      SafeHouse var6 = new SafeHouse(var0, var1, var2, var3, var4);
      var6.setOwner(var4);
      var6.setLastVisited(Calendar.getInstance().getTimeInMillis());
      var6.addPlayer(var4);
      safehouseList.add(var6);
      if (GameServer.bServer) {
         DebugLog.log("safehouse: added " + var0 + "," + var1 + "," + var2 + "," + var3 + " owner=" + var4);
      }

      if (GameClient.bClient && !var5) {
         GameClient.sendSafehouse(var6, false);
      }

      updateSafehousePlayersConnected();
      if (GameClient.bClient) {
         LuaEventManager.triggerEvent("OnSafehousesChanged");
      }

      return var6;
   }

   public static SafeHouse addSafeHouse(IsoGridSquare var0, IsoPlayer var1) {
      String var2 = canBeSafehouse(var0, var1);
      return var2 != null && !"".equals(var2) ? null : addSafeHouse(var0.getBuilding().def.getX() - diffError, var0.getBuilding().def.getY() - diffError, var0.getBuilding().def.getW() + diffError * 2, var0.getBuilding().def.getH() + diffError * 2, var1.getUsername(), false);
   }

   public static SafeHouse hasSafehouse(String var0) {
      for(int var1 = 0; var1 < safehouseList.size(); ++var1) {
         SafeHouse var2 = (SafeHouse)safehouseList.get(var1);
         if (var2.getPlayers().contains(var0) || var2.getOwner().equals(var0)) {
            return var2;
         }
      }

      return null;
   }

   public static SafeHouse hasSafehouse(IsoPlayer var0) {
      return hasSafehouse(var0.getUsername());
   }

   public static void updateSafehousePlayersConnected() {
      SafeHouse var0 = null;

      label51:
      for(int var1 = 0; var1 < safehouseList.size(); ++var1) {
         var0 = (SafeHouse)safehouseList.get(var1);
         var0.setPlayerConnected(0);
         Iterator var2;
         IsoPlayer var3;
         if (GameClient.bClient) {
            var2 = GameClient.IDToPlayerMap.values().iterator();

            while(true) {
               do {
                  if (!var2.hasNext()) {
                     continue label51;
                  }

                  var3 = (IsoPlayer)var2.next();
               } while(!var0.getPlayers().contains(var3.getUsername()) && !var0.getOwner().equals(var3.getUsername()));

               var0.setPlayerConnected(var0.getPlayerConnected() + 1);
            }
         } else if (GameServer.bServer) {
            var2 = GameServer.IDToPlayerMap.values().iterator();

            while(true) {
               do {
                  if (!var2.hasNext()) {
                     continue label51;
                  }

                  var3 = (IsoPlayer)var2.next();
               } while(!var0.getPlayers().contains(var3.getUsername()) && !var0.getOwner().equals(var3.getUsername()));

               var0.setPlayerConnected(var0.getPlayerConnected() + 1);
            }
         }
      }

   }

   public void updatePlayersConnected() {
      this.setPlayerConnected(0);
      Iterator var1;
      IsoPlayer var2;
      if (GameClient.bClient) {
         var1 = GameClient.IDToPlayerMap.values().iterator();

         while(true) {
            do {
               if (!var1.hasNext()) {
                  return;
               }

               var2 = (IsoPlayer)var1.next();
            } while(!this.getPlayers().contains(var2.getUsername()) && !this.getOwner().equals(var2.getUsername()));

            this.setPlayerConnected(this.getPlayerConnected() + 1);
         }
      } else if (GameServer.bServer) {
         var1 = GameServer.IDToPlayerMap.values().iterator();

         while(true) {
            do {
               if (!var1.hasNext()) {
                  return;
               }

               var2 = (IsoPlayer)var1.next();
            } while(!this.getPlayers().contains(var2.getUsername()) && !this.getOwner().equals(var2.getUsername()));

            this.setPlayerConnected(this.getPlayerConnected() + 1);
         }
      }
   }

   public static SafeHouse getSafeHouse(IsoGridSquare var0) {
      return isSafeHouse(var0, (String)null, false);
   }

   public static SafeHouse getSafeHouse(int var0, int var1, int var2, int var3) {
      SafeHouse var4 = null;

      for(int var5 = 0; var5 < safehouseList.size(); ++var5) {
         var4 = (SafeHouse)safehouseList.get(var5);
         if (var0 == var4.getX() && var2 == var4.getW() && var1 == var4.getY() && var3 == var4.getH()) {
            return var4;
         }
      }

      return null;
   }

   public static SafeHouse isSafeHouse(IsoGridSquare var0, String var1, boolean var2) {
      if (var0 == null) {
         return null;
      } else {
         if (GameClient.bClient) {
            IsoPlayer var3 = GameClient.instance.getPlayerFromUsername(var1);
            if (var3 != null && !var3.accessLevel.equals("")) {
               return null;
            }
         }

         SafeHouse var6 = null;
         boolean var4 = false;

         for(int var5 = 0; var5 < safehouseList.size(); ++var5) {
            var6 = (SafeHouse)safehouseList.get(var5);
            if (var0.getX() >= var6.getX() && var0.getX() < var6.getX2() && var0.getY() >= var6.getY() && var0.getY() < var6.getY2()) {
               var4 = true;
               break;
            }
         }

         if (var4 && var2 && ServerOptions.instance.DisableSafehouseWhenPlayerConnected.getValue() && (var6.getPlayerConnected() > 0 || var6.getOpenTimer() > 0)) {
            return null;
         } else {
            return !var4 || (var1 == null || var6 == null || var6.getPlayers().contains(var1) || var6.getOwner().equals(var1)) && var1 != null ? null : var6;
         }
      }
   }

   public static void clearSafehouseList() {
      safehouseList.clear();
   }

   public boolean playerAllowed(IsoPlayer var1) {
      return this.players.contains(var1.getUsername()) || this.owner.equals(var1.getUsername()) || !var1.accessLevel.equals("");
   }

   public boolean playerAllowed(String var1) {
      return this.players.contains(var1) || this.owner.equals(var1);
   }

   public void addPlayer(String var1) {
      if (!this.players.contains(var1)) {
         this.players.add(var1);
         updateSafehousePlayersConnected();
      }

   }

   public void removePlayer(String var1) {
      if (this.players.contains(var1)) {
         this.players.remove(var1);
         this.playersRespawn.remove(var1);
         if (GameClient.bClient) {
            GameClient.sendSafehouse(this, false);
         }
      }

   }

   public void syncSafehouse() {
      if (GameClient.bClient) {
         GameClient.sendSafehouse(this, false);
      }

   }

   public void removeSafeHouse(IsoPlayer var1) {
      this.removeSafeHouse(var1, false);
   }

   public void removeSafeHouse(IsoPlayer var1, boolean var2) {
      if (var1 == null || var1.getUsername().equals(this.getOwner()) || !var1.accessLevel.equals("admin") && !var1.accessLevel.equals("moderator") || var2) {
         if (GameClient.bClient) {
            GameClient.sendSafehouse(this, true);
         }

         if (GameServer.bServer) {
            SyncSafehousePacket var3 = new SyncSafehousePacket();
            var3.set(this, true);
            GameServer.sendSafehouse(var3, (UdpConnection)null);
         }

         getSafehouseList().remove(this);
         int var10000 = this.x;
         DebugLog.log("safehouse: removed " + var10000 + "," + this.y + "," + this.w + "," + this.h + " owner=" + this.getOwner());
         if (GameClient.bClient) {
            LuaEventManager.triggerEvent("OnSafehousesChanged");
         }

      }
   }

   public void save(ByteBuffer var1) {
      var1.putInt(this.getX());
      var1.putInt(this.getY());
      var1.putInt(this.getW());
      var1.putInt(this.getH());
      GameWindow.WriteString(var1, this.getOwner());
      var1.putInt(this.getPlayers().size());
      Iterator var2 = this.getPlayers().iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         GameWindow.WriteString(var1, var3);
      }

      var1.putLong(this.getLastVisited());
      GameWindow.WriteString(var1, this.getTitle());
      var1.putInt(this.playersRespawn.size());

      for(int var4 = 0; var4 < this.playersRespawn.size(); ++var4) {
         GameWindow.WriteString(var1, (String)this.playersRespawn.get(var4));
      }

   }

   public static SafeHouse load(ByteBuffer var0, int var1) {
      SafeHouse var2 = new SafeHouse(var0.getInt(), var0.getInt(), var0.getInt(), var0.getInt(), GameWindow.ReadString(var0));
      int var3 = var0.getInt();

      int var4;
      for(var4 = 0; var4 < var3; ++var4) {
         var2.addPlayer(GameWindow.ReadString(var0));
      }

      var2.setLastVisited(var0.getLong());
      if (var1 >= 101) {
         var2.setTitle(GameWindow.ReadString(var0));
      }

      if (ChatServer.isInited()) {
         ChatServer.getInstance().createSafehouseChat(var2.getId());
      }

      safehouseList.add(var2);
      if (var1 >= 177) {
         var4 = var0.getInt();

         for(int var5 = 0; var5 < var4; ++var5) {
            var2.playersRespawn.add(GameWindow.ReadString(var0));
         }
      }

      return var2;
   }

   public static String canBeSafehouse(IsoGridSquare var0, IsoPlayer var1) {
      if (!GameClient.bClient && !GameServer.bServer) {
         return null;
      } else if (!ServerOptions.instance.PlayerSafehouse.getValue() && !ServerOptions.instance.AdminSafehouse.getValue()) {
         return null;
      } else {
         String var2 = "";
         if (ServerOptions.instance.PlayerSafehouse.getValue() && hasSafehouse(var1) != null) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_AlreadyHaveSafehouse") + System.lineSeparator();
         }

         int var3 = ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue();
         if (!ServerOptions.instance.PlayerSafehouse.getValue() && ServerOptions.instance.AdminSafehouse.getValue() && GameClient.bClient) {
            if (!var1.accessLevel.equals("admin") && !var1.accessLevel.equals("moderator")) {
               return null;
            }

            var3 = 0;
         }

         if (var3 > 0 && var1.getHoursSurvived() < (double)(var3 * 24)) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_DaysSurvivedToClaim", var3) + System.lineSeparator();
         }

         if (GameClient.bClient) {
            KahluaTableIterator var4 = GameClient.instance.getServerSpawnRegions().iterator();
            IsoGridSquare var5 = null;

            while(var4.advance()) {
               KahluaTable var6 = (KahluaTable)var4.getValue();
               KahluaTableIterator var7 = ((KahluaTableImpl)var6.rawget("points")).iterator();

               while(var7.advance()) {
                  KahluaTable var8 = (KahluaTable)var7.getValue();
                  KahluaTableIterator var9 = var8.iterator();

                  while(var9.advance()) {
                     KahluaTable var10 = (KahluaTable)var9.getValue();
                     Double var11 = (Double)var10.rawget("worldX");
                     Double var12 = (Double)var10.rawget("worldY");
                     Double var13 = (Double)var10.rawget("posX");
                     Double var14 = (Double)var10.rawget("posY");
                     var5 = IsoWorld.instance.getCell().getGridSquare(var13 + var11 * 300.0, var14 + var12 * 300.0, 0.0);
                     if (var5 != null && var5.getBuilding() != null && var5.getBuilding().getDef() != null) {
                        BuildingDef var15 = var5.getBuilding().getDef();
                        if (var0.getX() >= var15.getX() && var0.getX() < var15.getX2() && var0.getY() >= var15.getY() && var0.getY() < var15.getY2()) {
                           return Translator.getText("IGUI_Safehouse_IsSpawnPoint");
                        }
                     }
                  }
               }
            }
         }

         boolean var16 = true;
         boolean var17 = false;
         boolean var18 = false;
         boolean var19 = false;
         boolean var20 = false;
         BuildingDef var21 = var0.getBuilding().getDef();
         if (var0.getBuilding().Rooms != null) {
            Iterator var22 = var0.getBuilding().Rooms.iterator();

            while(var22.hasNext()) {
               IsoRoom var24 = (IsoRoom)var22.next();
               if (var24.getName().equals("kitchen")) {
                  var18 = true;
               }

               if (var24.getName().equals("bedroom") || var24.getName().equals("livingroom")) {
                  var19 = true;
               }

               if (var24.getName().equals("bathroom")) {
                  var20 = true;
               }
            }
         }

         IsoCell var23 = IsoWorld.instance.getCell();

         for(int var25 = 0; var25 < var23.getObjectList().size(); ++var25) {
            IsoMovingObject var26 = (IsoMovingObject)var23.getObjectList().get(var25);
            if (var26 != var1 && var26 instanceof IsoGameCharacter && var26.getX() >= (float)(var21.getX() - diffError) && var26.getX() < (float)(var21.getX2() + diffError) && var26.getY() >= (float)(var21.getY() - diffError) && var26.getY() < (float)(var21.getY2() + diffError)) {
               var16 = false;
               break;
            }
         }

         if (var1.getX() >= (float)(var21.getX() - diffError) && var1.getX() < (float)(var21.getX2() + diffError) && var1.getY() >= (float)(var21.getY() - diffError) && var1.getY() < (float)(var21.getY2() + diffError) && var1.getCurrentSquare() != null && !var1.getCurrentSquare().Is(IsoFlagType.exterior)) {
            var17 = true;
         }

         if (!var16 || !var17) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_SomeoneInside") + System.lineSeparator();
         }

         if (!var19 && !ServerOptions.instance.SafehouseAllowNonResidential.getValue()) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_NotHouse") + System.lineSeparator();
         }

         return var2;
      }
   }

   public void kickOutOfSafehouse(IsoPlayer var1) {
      if (var1.isAccessLevel("None")) {
         GameClient.sendKickOutOfSafehouse(var1);
      }

   }

   public void checkTrespass(IsoPlayer var1) {
      if (GameServer.bServer && !ServerOptions.instance.SafehouseAllowTrepass.getValue() && var1.getVehicle() == null && !var1.isAccessLevel("admin")) {
         SafeHouse var2 = isSafeHouse(var1.getCurrentSquare(), var1.getUsername(), true);
         if (var2 != null) {
            GameServer.sendTeleport(var1, (float)(this.x - 1), (float)(this.y - 1), 0.0F);
            if (var1.isAsleep()) {
               var1.setAsleep(false);
               var1.setAsleepTime(0.0F);
               GameServer.sendWakeUpPlayer(var1, (UdpConnection)null);
            }
         }
      }

   }

   public SafeHouse alreadyHaveSafehouse(String var1) {
      return ServerOptions.instance.PlayerSafehouse.getValue() ? hasSafehouse(var1) : null;
   }

   public SafeHouse alreadyHaveSafehouse(IsoPlayer var1) {
      return ServerOptions.instance.PlayerSafehouse.getValue() ? hasSafehouse(var1) : null;
   }

   public static boolean allowSafeHouse(IsoPlayer var0) {
      boolean var1 = false;
      boolean var2 = (GameClient.bClient || GameServer.bServer) && (ServerOptions.instance.PlayerSafehouse.getValue() || ServerOptions.instance.AdminSafehouse.getValue());
      if (var2) {
         if (ServerOptions.instance.PlayerSafehouse.getValue()) {
            var1 = hasSafehouse(var0) == null;
         }

         if (var1 && ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue() > 0 && var0.getHoursSurvived() / 24.0 < (double)ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue()) {
            var1 = false;
         }

         if (ServerOptions.instance.AdminSafehouse.getValue() && GameClient.bClient) {
            var1 = var0.accessLevel.equals("admin") || var0.accessLevel.equals("moderator");
         }
      }

      return var1;
   }

   public void updateSafehouse(IsoPlayer var1) {
      this.updatePlayersConnected();
      if (var1 == null || !this.getPlayers().contains(var1.getUsername()) && !this.getOwner().equals(var1.getUsername())) {
         if (ServerOptions.instance.SafeHouseRemovalTime.getValue() > 0 && System.currentTimeMillis() - this.getLastVisited() > 3600000L * (long)ServerOptions.instance.SafeHouseRemovalTime.getValue()) {
            boolean var2 = false;
            ArrayList var3 = GameServer.getPlayers(tempPlayers);

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               IsoPlayer var5 = (IsoPlayer)var3.get(var4);
               if (this.containsLocation(var5.x, var5.y) && (this.getPlayers().contains(var5.getUsername()) || this.getOwner().equals(var5.getUsername()))) {
                  var2 = true;
                  break;
               }
            }

            if (var2) {
               this.setLastVisited(System.currentTimeMillis());
               return;
            }

            this.removeSafeHouse(var1, true);
         }
      } else {
         this.setLastVisited(System.currentTimeMillis());
      }

   }

   public SafeHouse(int var1, int var2, int var3, int var4, String var5) {
      this.x = var1;
      this.y = var2;
      this.w = var3;
      this.h = var4;
      this.players.add(var5);
      this.owner = var5;
      this.id = "" + var1 + "," + var2 + " at " + Calendar.getInstance().getTimeInMillis();
   }

   public String getId() {
      return this.id;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int var1) {
      this.x = var1;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int var1) {
      this.y = var1;
   }

   public int getW() {
      return this.w;
   }

   public void setW(int var1) {
      this.w = var1;
   }

   public int getH() {
      return this.h;
   }

   public void setH(int var1) {
      this.h = var1;
   }

   public int getX2() {
      return this.x + this.w;
   }

   public int getY2() {
      return this.y + this.h;
   }

   public boolean containsLocation(float var1, float var2) {
      return var1 >= (float)this.getX() && var1 < (float)this.getX2() && var2 >= (float)this.getY() && var2 < (float)this.getY2();
   }

   public ArrayList<String> getPlayers() {
      return this.players;
   }

   public void setPlayers(ArrayList<String> var1) {
      this.players = var1;
   }

   public static ArrayList<SafeHouse> getSafehouseList() {
      return safehouseList;
   }

   public String getOwner() {
      return this.owner;
   }

   public void setOwner(String var1) {
      this.owner = var1;
      if (this.players.contains(var1)) {
         this.players.remove(var1);
      }

   }

   public boolean isOwner(IsoPlayer var1) {
      return this.getOwner().equals(var1.getUsername());
   }

   public long getLastVisited() {
      return this.lastVisited;
   }

   public void setLastVisited(long var1) {
      this.lastVisited = var1;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String var1) {
      this.title = var1;
   }

   public int getPlayerConnected() {
      return this.playerConnected;
   }

   public void setPlayerConnected(int var1) {
      this.playerConnected = var1;
   }

   public int getOpenTimer() {
      return this.openTimer;
   }

   public void setOpenTimer(int var1) {
      this.openTimer = var1;
   }

   public void setRespawnInSafehouse(boolean var1, String var2) {
      if (var1) {
         this.playersRespawn.add(var2);
      } else {
         this.playersRespawn.remove(var2);
      }

      if (GameClient.bClient) {
         GameClient.sendSafehouse(this, false);
      }

   }

   public boolean isRespawnInSafehouse(String var1) {
      return this.playersRespawn.contains(var1);
   }

   public static boolean isPlayerAllowedOnSquare(IsoPlayer var0, IsoGridSquare var1) {
      if (!ServerOptions.instance.SafehouseAllowTrepass.getValue()) {
         return isSafeHouse(var1, var0.getUsername(), true) == null;
      } else {
         return true;
      }
   }
}
