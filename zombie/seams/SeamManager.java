package zombie.seams;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.StringUtils;

public final class SeamManager {
   private static SeamManager instance;
   private final ArrayList<ModData> m_modData = new ArrayList();

   public static SeamManager getInstance() {
      if (instance == null) {
         instance = new SeamManager();
      }

      return instance;
   }

   private SeamManager() {
   }

   public void init() {
      ArrayList var1 = ZomboidFileSystem.instance.getModIDs();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         ChooseGameInfo.Mod var4 = ChooseGameInfo.getAvailableModDetails(var3);
         if (var4 != null) {
            File var5 = new File(var4.mediaFile.common.absoluteFile, "seams.txt");
            if (var5.exists()) {
               this.initModData(var4);
            }
         }
      }

      this.initGameData();
   }

   public void initGameData() {
      ModData var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
      var1.m_data.init();
      this.m_modData.add(var1);
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
      var2.m_data.init();
      this.m_modData.add(var2);
   }

   public ArrayList<String> getModIDs() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         var1.add(var3.m_modID);
      }

      return var1;
   }

   ModData getModData(String var1) {
      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         if (StringUtils.equals(var1, var3.m_modID)) {
            return var3;
         }
      }

      return null;
   }

   public SeamFile.Tile getHighestPriorityTile(String var1, int var2, int var3) {
      for(int var4 = 0; var4 < this.m_modData.size(); ++var4) {
         ModData var5 = (ModData)this.m_modData.get(var4);
         SeamFile.Tile var6 = var5.m_data.getTile(var1, var2, var3);
         if (var6 != null) {
            return var6;
         }
      }

      return null;
   }

   public SeamFile.Tile getHighestPriorityTileFromName(String var1) {
      int var2 = var1.lastIndexOf("_");
      String var3 = var1.substring(0, var2);
      int var4 = PZMath.tryParseInt(var1.substring(var2 + 1), -1);
      return var4 < 0 ? null : this.getHighestPriorityTile(var3, var4 % 8, var4 / 8);
   }

   public String getTileProperty(String var1, String var2, int var3, int var4, String var5) {
      return this.getModData(var1).m_data.getProperty(var2, var3, var4, var5);
   }

   public void setTileProperty(String var1, String var2, int var3, int var4, String var5, String var6) {
      this.getModData(var1).m_data.setProperty(var2, var3, var4, var5, var6);
   }

   public ArrayList<String> getTileJoinE(String var1, String var2, int var3, int var4, boolean var5) {
      return this.getModData(var1).m_data.getTileJoinE(var2, var3, var4, var5);
   }

   public ArrayList<String> getTileJoinS(String var1, String var2, int var3, int var4, boolean var5) {
      return this.getModData(var1).m_data.getTileJoinS(var2, var3, var4, var5);
   }

   public ArrayList<String> getTileJoinBelowE(String var1, String var2, int var3, int var4, boolean var5) {
      return this.getModData(var1).m_data.getTileJoinBelowE(var2, var3, var4, var5);
   }

   public ArrayList<String> getTileJoinBelowS(String var1, String var2, int var3, int var4, boolean var5) {
      return this.getModData(var1).m_data.getTileJoinBelowS(var2, var3, var4, var5);
   }

   public SeamFile.Tile getTile(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_data.getTile(var2, var3, var4);
   }

   public SeamFile.Tile getTileFromName(String var1, String var2) {
      int var3 = var2.lastIndexOf("_");
      String var4 = var2.substring(0, var3);
      int var5 = PZMath.tryParseInt(var2.substring(var3 + 1), -1);
      return var5 < 0 ? null : this.getModData(var1).m_data.getTile(var4, var5 % 8, var5 / 8);
   }

   public SeamFile.Tile getOrCreateTile(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_data.getOrCreateTile(var2, var3, var4);
   }

   public boolean isMasterTile(String var1, String var2, int var3, int var4) {
      SeamFile.Tile var5 = this.getTile(var1, var2, var3, var4);
      return var5 != null && var5.isMasterTile();
   }

   public String getMasterTileName(String var1, String var2, int var3, int var4) {
      SeamFile.Tile var5 = this.getTile(var1, var2, var3, var4);
      return var5 == null ? null : var5.getMasterTileName();
   }

   public void write(String var1) {
      this.getModData(var1).m_data.write();
   }

   public void Reset() {
      Iterator var1 = this.m_modData.iterator();

      while(var1.hasNext()) {
         ModData var2 = (ModData)var1.next();
         var2.m_data.Reset();
      }

      this.m_modData.clear();
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final SeamData m_data;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_data = new SeamData(var2);
      }
   }
}
