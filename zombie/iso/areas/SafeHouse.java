package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
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
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.WarManager;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public class SafeHouse {
   private int x = 0;
   private int y = 0;
   private int w = 0;
   private int h = 0;
   private static int diffError = 2;
   private String owner = null;
   private long lastVisited = 0L;
   private String title = "Safehouse";
   private int playerConnected = 0;
   private int openTimer = 0;
   private int hitPoints = 0;
   private final String id;
   private ArrayList<String> players = new ArrayList();
   private final ArrayList<String> playersRespawn = new ArrayList();
   private static final ArrayList<SafeHouse> safehouseList = new ArrayList();
   private int onlineID = -1;
   private static final ArrayList<IsoPlayer> tempPlayers = new ArrayList();
   private static final HashSet<String> invites = new HashSet();

   public static void init() {
      safehouseList.clear();
   }

   public static SafeHouse addSafeHouse(int var0, int var1, int var2, int var3, String var4) {
      SafeHouse var5 = new SafeHouse(var0, var1, var2, var3, var4);
      var5.setOwner(var4);
      var5.setLastVisited(Calendar.getInstance().getTimeInMillis());
      var5.setHitPoints(0);
      WarManager.removeWar(var5.getOnlineID(), (String)null);
      safehouseList.add(var5);
      DebugLog.Multiplayer.debugln("[%03d] Safehouse=%d added (%d;%d) owner=%s", safehouseList.size(), var5.getOnlineID(), var5.getX(), var5.getY(), var5.getOwner());
      updateSafehousePlayersConnected();
      if (GameClient.bClient) {
         LuaEventManager.triggerEvent("OnSafehousesChanged");
      }

      return var5;
   }

   public static SafeHouse addSafeHouse(IsoGridSquare var0, IsoPlayer var1) {
      String var2 = canBeSafehouse(var0, var1);
      if (!StringUtils.isNullOrEmpty(var2)) {
         return null;
      } else {
         return var0.getBuilding() == null ? null : addSafeHouse(var0.getBuilding().def.getX() - diffError, var0.getBuilding().def.getY() - diffError, var0.getBuilding().def.getW() + diffError * 2, var0.getBuilding().def.getH() + diffError * 2, var1.getUsername());
      }
   }

   public static SafeHouse hasSafehouse(String var0) {
      Iterator var1 = safehouseList.iterator();

      SafeHouse var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (SafeHouse)var1.next();
      } while(!var2.getPlayers().contains(var0) && !var2.getOwner().equals(var0));

      return var2;
   }

   public static SafeHouse getSafehouseByOwner(String var0) {
      Iterator var1 = safehouseList.iterator();

      SafeHouse var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (SafeHouse)var1.next();
      } while(!var2.getOwner().equals(var0));

      return var2;
   }

   public static SafeHouse hasSafehouse(IsoPlayer var0) {
      return hasSafehouse(var0.getUsername());
   }

   public static void updateSafehousePlayersConnected() {
      Iterator var0 = safehouseList.iterator();

      while(var0.hasNext()) {
         SafeHouse var1 = (SafeHouse)var0.next();
         var1.updatePlayersConnected();
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

   private static SafeHouse findSafeHouse(IsoGridSquare var0) {
      SafeHouse var1 = null;
      Iterator var2 = safehouseList.iterator();

      while(var2.hasNext()) {
         SafeHouse var3 = (SafeHouse)var2.next();
         if (var0.getX() >= var3.getX() && var0.getX() < var3.getX2() && var0.getY() >= var3.getY() && var0.getY() < var3.getY2()) {
            var1 = var3;
            break;
         }
      }

      return var1;
   }

   public static SafeHouse getSafeHouse(IsoGridSquare var0) {
      return isSafeHouse(var0, (String)null, false);
   }

   public static SafeHouse getSafeHouse(String var0) {
      Iterator var1 = safehouseList.iterator();

      SafeHouse var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (SafeHouse)var1.next();
      } while(!var2.getTitle().equals(var0));

      return var2;
   }

   public static SafeHouse getSafeHouse(int var0, int var1, int var2, int var3) {
      Iterator var4 = safehouseList.iterator();

      SafeHouse var5;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         var5 = (SafeHouse)var4.next();
      } while(var0 != var5.getX() || var2 != var5.getW() || var1 != var5.getY() || var3 != var5.getH());

      return var5;
   }

   public static SafeHouse getSafehouseOverlapping(int var0, int var1, int var2, int var3) {
      Iterator var4 = safehouseList.iterator();

      SafeHouse var5;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         var5 = (SafeHouse)var4.next();
      } while(var0 >= var5.getX2() || var2 <= var5.getX() || var1 >= var5.getY2() || var3 <= var5.getY());

      return var5;
   }

   public static SafeHouse getSafehouseOverlapping(int var0, int var1, int var2, int var3, SafeHouse var4) {
      Iterator var5 = safehouseList.iterator();

      SafeHouse var6;
      do {
         if (!var5.hasNext()) {
            return null;
         }

         var6 = (SafeHouse)var5.next();
      } while(var6 == var4 || var0 >= var6.getX2() || var2 <= var6.getX() || var1 >= var6.getY2() || var3 <= var6.getY());

      return var6;
   }

   public static SafeHouse isSafeHouse(IsoGridSquare var0, String var1, boolean var2) {
      if (var0 == null) {
         return null;
      } else {
         if (GameClient.bClient && var1 != null) {
            IsoPlayer var3 = GameClient.instance.getPlayerFromUsername(var1);
            if (var3 != null && var3.role.haveCapability(Capability.CanGoInsideSafehouses)) {
               return null;
            }
         }

         SafeHouse var4 = findSafeHouse(var0);
         if (var4 != null && var2 && ServerOptions.instance.DisableSafehouseWhenPlayerConnected.getValue() && (var4.getPlayerConnected() > 0 || var4.getOpenTimer() > 0)) {
            return null;
         } else {
            return var4 == null || var1 != null && (var4.getPlayers().contains(var1) || var4.getOwner().equals(var1)) ? null : var4;
         }
      }
   }

   public static boolean isSafehouseAllowTrepass(IsoGridSquare var0, IsoPlayer var1) {
      if (var0 == null) {
         return true;
      } else if (var1 == null) {
         return true;
      } else {
         SafeHouse var2 = findSafeHouse(var0);
         if (var2 == null) {
            return true;
         } else if (var1.role.haveCapability(Capability.CanGoInsideSafehouses)) {
            return true;
         } else if (ServerOptions.getInstance().SafehouseAllowTrepass.getValue()) {
            return true;
         } else if (ServerOptions.getInstance().DisableSafehouseWhenPlayerConnected.getValue() && (var2.getPlayerConnected() > 0 || var2.getOpenTimer() > 0)) {
            return true;
         } else {
            return WarManager.isWarStarted(var2.getOnlineID(), var1.getUsername()) ? true : var2.playerAllowed(var1.getUsername());
         }
      }
   }

   public static boolean isSafehouseAllowInteract(IsoGridSquare var0, IsoPlayer var1) {
      if (var0 == null) {
         return true;
      } else if (var1 == null) {
         return true;
      } else {
         SafeHouse var2 = findSafeHouse(var0);
         if (var2 == null) {
            return true;
         } else if (var1.role != null && var1.role.haveCapability(Capability.CanGoInsideSafehouses)) {
            return true;
         } else {
            return WarManager.isWarStarted(var2.getOnlineID(), var1.getUsername()) ? true : var2.playerAllowed(var1.getUsername());
         }
      }
   }

   public static boolean isSafehouseAllowLoot(IsoGridSquare var0, IsoPlayer var1) {
      return ServerOptions.getInstance().SafehouseAllowLoot.getValue() ? true : isSafehouseAllowInteract(var0, var1);
   }

   public static boolean isSafehouseAllowClaim(SafeHouse var0, IsoPlayer var1) {
      if (var0 == null) {
         return false;
      } else if (var1 == null) {
         return false;
      } else if (WarManager.isWarClaimed(var1.getUsername())) {
         return false;
      } else {
         return !var0.playerAllowed(var1.getUsername());
      }
   }

   public static void clearSafehouseList() {
      safehouseList.clear();
   }

   public boolean playerAllowed(IsoPlayer var1) {
      return this.players.contains(var1.getUsername()) || this.owner.equals(var1.getUsername()) || var1.role.haveCapability(Capability.CanGoInsideSafehouses);
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
      }

   }

   public static void removeSafeHouse(SafeHouse var0) {
      safehouseList.remove(var0);
      DebugLog.Multiplayer.debugln("[%03d] Safehouse=%d removed (%d;%d) owner=%s", safehouseList.size(), var0.getOnlineID(), var0.getX(), var0.getY(), var0.getOwner());
      if (GameClient.bClient) {
         LuaEventManager.triggerEvent("OnSafehousesChanged");
      }

   }

   public void save(ByteBuffer var1) {
      var1.putInt(this.getX());
      var1.putInt(this.getY());
      var1.putInt(this.getW());
      var1.putInt(this.getH());
      GameWindow.WriteString(var1, this.getOwner());
      var1.putInt(this.getHitPoints());
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
      if (var1 >= 216) {
         var2.setHitPoints(var0.getInt());
      }

      int var3 = var0.getInt();

      int var4;
      for(var4 = 0; var4 < var3; ++var4) {
         var2.addPlayer(GameWindow.ReadString(var0));
      }

      var2.setLastVisited(var0.getLong());
      var2.setTitle(GameWindow.ReadString(var0));
      if (ChatServer.isInited()) {
         ChatServer.getInstance().createSafehouseChat(var2.getId());
      }

      safehouseList.add(var2);
      var4 = var0.getInt();

      for(int var5 = 0; var5 < var4; ++var5) {
         var2.playersRespawn.add(GameWindow.ReadString(var0));
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
            if (!var1.role.haveCapability(Capability.CanSetupSafehouses)) {
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
                     Double var11;
                     Double var12;
                     if (var10.rawget("worldX") != null) {
                        var11 = (Double)var10.rawget("worldX");
                        var12 = (Double)var10.rawget("worldY");
                        Double var13 = (Double)var10.rawget("posX");
                        Double var14 = (Double)var10.rawget("posY");
                        var5 = IsoWorld.instance.getCell().getGridSquare(var13 + var11 * 300.0, var14 + var12 * 300.0, 0.0);
                     } else {
                        var11 = (Double)var10.rawget("posX");
                        var12 = (Double)var10.rawget("posY");
                        if (var11 != null && var12 != null) {
                           var5 = IsoWorld.instance.getCell().getGridSquare(var11, var12, 0.0);
                        } else {
                           var5 = null;
                        }
                     }

                     if (var5 != null && var5.getBuilding() != null && var5.getBuilding().getDef() != null) {
                        BuildingDef var23 = var5.getBuilding().getDef();
                        if (var0.getX() >= var23.getX() && var0.getX() < var23.getX2() && var0.getY() >= var23.getY() && var0.getY() < var23.getY2()) {
                           return Translator.getText("IGUI_Safehouse_IsSpawnPoint");
                        }
                     }
                  }
               }
            }
         }

         boolean var15 = true;
         boolean var17 = false;
         boolean var16 = false;
         boolean var18 = false;
         ArrayList var19 = new ArrayList();
         var19.add("bathroom");
         var19.add("bedroom");
         var19.add("closet");
         var19.add("fishingstorage");
         var19.add("garage");
         var19.add("hall");
         var19.add("kidsbedroom");
         var19.add("kitchen");
         var19.add("laundry");
         var19.add("livingroom");
         BuildingDef var20 = var0.getBuilding().getDef();
         if (var0.getBuilding().Rooms != null) {
            Iterator var21 = var0.getBuilding().Rooms.iterator();

            label155:
            while(true) {
               IsoRoom var24;
               String var26;
               do {
                  if (!var21.hasNext()) {
                     break label155;
                  }

                  var24 = (IsoRoom)var21.next();
                  var26 = var24.getName();
                  if (!var19.contains(var26)) {
                     var18 = true;
                     break label155;
                  }
               } while(!var26.equals("bedroom") && !var24.getName().equals("livingroom"));

               var16 = true;
            }
         }

         if (!var16) {
            var18 = true;
         }

         IsoCell var22 = IsoWorld.instance.getCell();

         for(int var25 = 0; var25 < var22.getObjectList().size(); ++var25) {
            IsoMovingObject var27 = (IsoMovingObject)var22.getObjectList().get(var25);
            if (var27 != var1 && var27 instanceof IsoGameCharacter && !(var27 instanceof IsoAnimal) && var27.getX() >= (float)(var20.getX() - diffError) && var27.getX() < (float)(var20.getX2() + diffError) && var27.getY() >= (float)(var20.getY() - diffError) && var27.getY() < (float)(var20.getY2() + diffError) && !(var27 instanceof BaseVehicle)) {
               var15 = false;
               break;
            }
         }

         if (var1.getX() >= (float)(var20.getX() - diffError) && var1.getX() < (float)(var20.getX2() + diffError) && var1.getY() >= (float)(var20.getY() - diffError) && var1.getY() < (float)(var20.getY2() + diffError) && var1.getCurrentSquare() != null && !var1.getCurrentSquare().Is(IsoFlagType.exterior)) {
            var17 = true;
         }

         if (!var15 || !var17) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_SomeoneInside") + System.lineSeparator();
         }

         if (var18 && !ServerOptions.instance.SafehouseAllowNonResidential.getValue()) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_NotHouse") + System.lineSeparator();
         }

         boolean var28 = intersects(var20.getX() - diffError, var20.getY() - diffError, var20.getX() - diffError + var20.getW() + diffError * 2, var20.getY() - diffError + var20.getH() + diffError * 2);
         if (var28) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_Intersects") + System.lineSeparator();
         }

         if (!WarManager.isWarClaimed(getOnlineID(var20.getX() - diffError, var20.getY() - diffError))) {
            var2 = var2 + Translator.getText("IGUI_Safehouse_War") + System.lineSeparator();
         }

         return var2;
      }
   }

   public void checkTrespass(IsoPlayer var1) {
      if (GameServer.bServer && var1.getVehicle() == null && !isSafehouseAllowTrepass(var1.getCurrentSquare(), var1)) {
         GameServer.sendTeleport(var1, (float)(this.x - 1), (float)(this.y - 1), 0.0F);
         var1.updateDisguisedState();
         if (var1.isAsleep()) {
            INetworkPacket.processPacketOnServer(PacketTypes.PacketType.WakeUpPlayer, (UdpConnection)null, var1);
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
      if ((GameClient.bClient || GameServer.bServer) && (ServerOptions.instance.PlayerSafehouse.getValue() || ServerOptions.instance.AdminSafehouse.getValue())) {
         if (ServerOptions.instance.PlayerSafehouse.getValue()) {
            var1 = hasSafehouse(var0) == null;
         }

         if (var1 && ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue() > 0 && var0.getHoursSurvived() < (double)(ServerOptions.instance.SafehouseDaySurvivedToClaim.getValue() * 24)) {
            var1 = false;
         }

         if (ServerOptions.instance.AdminSafehouse.getValue()) {
            var1 = var0.role.haveCapability(Capability.CanSetupSafehouses);
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
               if (this.containsLocation(var5.getX(), var5.getY()) && (this.getPlayers().contains(var5.getUsername()) || this.getOwner().equals(var5.getUsername()))) {
                  var2 = true;
                  break;
               }
            }

            if (var2) {
               this.setLastVisited(System.currentTimeMillis());
               return;
            }

            removeSafeHouse(this);
         }
      } else {
         this.setLastVisited(System.currentTimeMillis());
      }

   }

   public static int getOnlineID(int var0, int var1) {
      return (var0 + var1) * (var0 + var1 + 1) / 2 + var0;
   }

   public SafeHouse(int var1, int var2, int var3, int var4, String var5) {
      this.x = var1;
      this.y = var2;
      this.w = var3;
      this.h = var4;
      this.players.add(var5);
      this.owner = var5;
      this.id = "" + var1 + "," + var2 + " at " + Calendar.getInstance().getTimeInMillis();
      this.onlineID = getOnlineID(var1, var2);
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

   public ArrayList<String> getPlayersRespawn() {
      return this.playersRespawn;
   }

   public static ArrayList<SafeHouse> getSafehouseList() {
      return safehouseList;
   }

   public String getOwner() {
      return this.owner;
   }

   public void setOwner(String var1) {
      this.owner = var1;
      this.players.remove(var1);
   }

   public boolean isOwner(IsoPlayer var1) {
      return this.isOwner(var1.getUsername());
   }

   public boolean isOwner(String var1) {
      return this.getOwner().equals(var1);
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

   public int getHitPoints() {
      return this.hitPoints;
   }

   public void setHitPoints(int var1) {
      this.hitPoints = var1;
   }

   public void setRespawnInSafehouse(boolean var1, String var2) {
      if (var1) {
         if (!this.playersRespawn.contains(var2)) {
            this.playersRespawn.add(var2);
         }
      } else if (this.playersRespawn.contains(var2)) {
         this.playersRespawn.remove(var2);
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

   public int getOnlineID() {
      return this.onlineID;
   }

   public void setOnlineID(int var1) {
      this.onlineID = var1;
   }

   public static SafeHouse getSafeHouse(int var0) {
      Iterator var1 = safehouseList.iterator();

      SafeHouse var2;
      do {
         if (!var1.hasNext()) {
            return null;
         }

         var2 = (SafeHouse)var1.next();
      } while(var2.getOnlineID() != var0);

      return var2;
   }

   public static boolean isInSameSafehouse(String var0, String var1) {
      Iterator var2 = safehouseList.iterator();

      SafeHouse var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (SafeHouse)var2.next();
      } while(!var3.playerAllowed(var0) || !var3.playerAllowed(var1));

      return true;
   }

   public static boolean intersects(int var0, int var1, int var2, int var3) {
      for(int var5 = var0; var5 < var2; ++var5) {
         for(int var6 = var1; var6 < var3; ++var6) {
            IsoGridSquare var4 = IsoWorld.instance.CurrentCell.getOrCreateGridSquare((double)var5, (double)var6, 0.0);
            if (getSafeHouse(var4) != null) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean haveInvite(String var1) {
      return invites.contains(var1);
   }

   public void removeInvite(String var1) {
      invites.remove(var1);
   }

   public void addInvite(String var1) {
      invites.add(var1);
   }

   public static void hitPoint(int var0) {
      SafeHouse var1 = getSafeHouse(var0);
      if (var1 != null) {
         int var2 = var1.getHitPoints() + 1;
         if (var2 == ServerOptions.instance.WarSafehouseHitPoints.getValue()) {
            removeSafeHouse(var1);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, (UdpConnection)null, var1, true);
         } else {
            var1.setHitPoints(var2);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, (UdpConnection)null, var1, false);
         }
      }

   }
}
