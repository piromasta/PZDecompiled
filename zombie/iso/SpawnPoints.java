package zombie.iso;

import java.util.ArrayList;
import java.util.Random;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.Type;

public final class SpawnPoints {
   public static final SpawnPoints instance = new SpawnPoints();
   private KahluaTable SpawnRegions;
   private final ArrayList<IsoGameCharacter.Location> SpawnPoints = new ArrayList();
   private final ArrayList<BuildingDef> SpawnBuildings = new ArrayList();
   private final IsoGameCharacter.Location m_tempLocation = new IsoGameCharacter.Location(-1, -1, -1);

   public SpawnPoints() {
   }

   public void init() {
      this.SpawnRegions = LuaManager.platform.newTable();
      this.SpawnPoints.clear();
      this.SpawnBuildings.clear();
   }

   public void initServer1() {
      this.init();
      this.initSpawnRegions();
   }

   public void initServer2(IsoMetaGrid var1) {
      if (!this.parseServerSpawnPoint()) {
         this.parseSpawnRegions(var1);
         this.initSpawnBuildings();
      }
   }

   public void initSinglePlayer(IsoMetaGrid var1) {
      this.init();
      this.initSpawnRegions();
      this.parseSpawnRegions(var1);
      this.initSpawnBuildings();
   }

   private void initSpawnRegions() {
      KahluaTable var1 = (KahluaTable)LuaManager.env.rawget("SpawnRegionMgr");
      if (var1 == null) {
         DebugLog.General.error("SpawnRegionMgr is undefined");
      } else {
         Object[] var2 = LuaManager.caller.pcall(LuaManager.thread, var1.rawget("getSpawnRegions"), new Object[0]);
         if (var2.length > 1 && var2[1] instanceof KahluaTable) {
            this.SpawnRegions = (KahluaTable)var2[1];
         }

      }
   }

   private boolean parseServerSpawnPoint() {
      if (!GameServer.bServer) {
         return false;
      } else if (ServerOptions.instance.SpawnPoint.getValue().isEmpty()) {
         return false;
      } else {
         String[] var1 = ServerOptions.instance.SpawnPoint.getValue().split(",");
         if (var1.length == 3) {
            try {
               int var2 = Integer.parseInt(var1[0].trim());
               int var3 = Integer.parseInt(var1[1].trim());
               int var4 = Integer.parseInt(var1[2].trim());
               if (var2 != 0 || var3 != 0) {
                  this.SpawnPoints.add(new IsoGameCharacter.Location(var2, var3, var4));
                  return true;
               }
            } catch (NumberFormatException var5) {
               DebugLog.General.error("SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
            }
         } else {
            DebugLog.General.error("SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.SpawnPoint.getValue() + "\"");
         }

         return false;
      }
   }

   private void parseSpawnRegions(IsoMetaGrid var1) {
      KahluaTableIterator var2 = this.SpawnRegions.iterator();

      while(var2.advance()) {
         KahluaTable var3 = (KahluaTable)Type.tryCastTo(var2.getValue(), KahluaTable.class);
         if (var3 != null) {
            this.parseRegion(var3, var1);
         }
      }

   }

   private void parseRegion(KahluaTable var1, IsoMetaGrid var2) {
      KahluaTable var3 = (KahluaTable)Type.tryCastTo(var1.rawget("points"), KahluaTable.class);
      if (var3 != null) {
         KahluaTableIterator var4 = var3.iterator();

         while(var4.advance()) {
            KahluaTable var5 = (KahluaTable)Type.tryCastTo(var4.getValue(), KahluaTable.class);
            if (var5 != null) {
               this.parseProfession(var5, var2);
            }
         }
      }

   }

   private void parseProfession(KahluaTable var1, IsoMetaGrid var2) {
      KahluaTableIterator var3 = var1.iterator();

      while(var3.advance()) {
         KahluaTable var4 = (KahluaTable)Type.tryCastTo(var3.getValue(), KahluaTable.class);
         if (var4 != null) {
            this.parsePoint(var4, var2);
         }
      }

   }

   private void parsePoint(KahluaTable var1, IsoMetaGrid var2) {
      String var3 = (String)Type.tryCastTo(var1.rawget("position"), String.class);
      if (var3 != null) {
         switch (var3.toLowerCase()) {
            case "center":
               Random var12 = new Random();
               this.m_tempLocation.x = ((var2.maxX - var2.minX) / 2 + var2.minX) * IsoCell.CellSizeInSquares + var12.nextInt(32);
               this.m_tempLocation.y = ((var2.maxY - var2.minY) / 2 + var2.minY) * IsoCell.CellSizeInSquares + var12.nextInt(32);
               this.m_tempLocation.z = 0;
               break;
            default:
               return;
         }
      } else {
         Double var4;
         Double var5;
         Double var6;
         if (var1.rawget("worldX") != null) {
            var4 = (Double)Type.tryCastTo(var1.rawget("worldX"), Double.class);
            var5 = (Double)Type.tryCastTo(var1.rawget("worldY"), Double.class);
            var6 = (Double)Type.tryCastTo(var1.rawget("posX"), Double.class);
            Double var7 = (Double)Type.tryCastTo(var1.rawget("posY"), Double.class);
            Double var8 = (Double)Type.tryCastTo(var1.rawget("posZ"), Double.class);
            if (var4 == null || var5 == null || var6 == null || var7 == null) {
               return;
            }

            this.m_tempLocation.x = var4.intValue() * 300 + var6.intValue();
            this.m_tempLocation.y = var5.intValue() * 300 + var7.intValue();
            this.m_tempLocation.z = var8 == null ? 0 : var8.intValue();
         } else {
            var4 = (Double)Type.tryCastTo(var1.rawget("posX"), Double.class);
            var5 = (Double)Type.tryCastTo(var1.rawget("posY"), Double.class);
            var6 = (Double)Type.tryCastTo(var1.rawget("posZ"), Double.class);
            if (var4 == null || var5 == null) {
               return;
            }

            this.m_tempLocation.x = var4.intValue();
            this.m_tempLocation.y = var5.intValue();
            this.m_tempLocation.z = var6 == null ? 0 : var6.intValue();
         }
      }

      if (!this.SpawnPoints.contains(this.m_tempLocation)) {
         IsoGameCharacter.Location var10 = new IsoGameCharacter.Location(this.m_tempLocation.x, this.m_tempLocation.y, this.m_tempLocation.z);
         this.SpawnPoints.add(var10);
      }

   }

   private void initSpawnBuildings() {
      for(int var1 = 0; var1 < this.SpawnPoints.size(); ++var1) {
         IsoGameCharacter.Location var2 = (IsoGameCharacter.Location)this.SpawnPoints.get(var1);
         RoomDef var3 = IsoWorld.instance.MetaGrid.getRoomAt(var2.x, var2.y, var2.z);
         if (var3 != null && var3.getBuilding() != null) {
            this.SpawnBuildings.add(var3.getBuilding());
         } else {
            DebugLog.General.warn("initSpawnBuildings: no room or building at %d,%d,%d", var2.x, var2.y, var2.z);
         }
      }

   }

   public boolean isSpawnBuilding(BuildingDef var1) {
      return this.SpawnBuildings.contains(var1);
   }

   public KahluaTable getSpawnRegions() {
      return this.SpawnRegions;
   }

   public ArrayList<IsoGameCharacter.Location> getSpawnPoints() {
      return this.SpawnPoints;
   }
}
