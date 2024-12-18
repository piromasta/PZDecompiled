package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.StringUtils;

public final class TileGeometryManager {
   private static TileGeometryManager instance;
   public static final boolean ONE_PIXEL_OFFSET = false;
   private final ArrayList<ModData> m_modData = new ArrayList();

   public static TileGeometryManager getInstance() {
      if (instance == null) {
         instance = new TileGeometryManager();
      }

      return instance;
   }

   private TileGeometryManager() {
   }

   public void init() {
      this.initGameData();
      ArrayList var1 = ZomboidFileSystem.instance.getModIDs();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         ChooseGameInfo.Mod var4 = ChooseGameInfo.getAvailableModDetails(var3);
         if (var4 != null) {
            File var5 = new File(var4.mediaFile.common.absoluteFile, "tileGeometry.txt");
            if (var5.exists()) {
               this.initModData(var4);
            }
         }
      }

   }

   public void initGameData() {
      ModData var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
      var1.m_geometry.init();
      this.m_modData.add(var1);
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
      var2.m_geometry.init();
      this.m_modData.add(var2);
   }

   public void initSpriteProperties() {
      for(int var1 = 0; var1 < this.m_modData.size(); ++var1) {
         ModData var2 = (ModData)this.m_modData.get(var1);
         var2.m_geometry.initSpriteProperties();
      }

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

   public void setGeometry(String var1, String var2, int var3, int var4, ArrayList<TileGeometryFile.Geometry> var5) {
      this.getModData(var1).m_geometry.setGeometry(var2, var3, var4, var5);
   }

   public void copyGeometry(String var1, String var2, int var3, int var4, ArrayList<TileGeometryFile.Geometry> var5) {
      this.getModData(var1).m_geometry.copyGeometry(var2, var3, var4, var5);
   }

   public ArrayList<TileGeometryFile.Geometry> getGeometry(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_geometry.getGeometry(var2, var3, var4);
   }

   public String getTileProperty(String var1, String var2, int var3, int var4, String var5) {
      return this.getModData(var1).m_geometry.getProperty(var2, var3, var4, var5);
   }

   public void setTileProperty(String var1, String var2, int var3, int var4, String var5, String var6) {
      this.getModData(var1).m_geometry.setProperty(var2, var3, var4, var5, var6);
   }

   public TileGeometryFile.Tile getTile(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_geometry.getTile(var2, var3, var4);
   }

   public TileGeometryFile.Tile getOrCreateTile(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_geometry.getOrCreateTile(var2, var3, var4);
   }

   public void write(String var1) {
      this.getModData(var1).m_geometry.write();
   }

   public void Reset() {
      Iterator var1 = this.m_modData.iterator();

      while(var1.hasNext()) {
         ModData var2 = (ModData)var1.next();
         var2.m_geometry.Reset();
      }

      this.m_modData.clear();
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final TileGeometry m_geometry;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_geometry = new TileGeometry(var2);
      }
   }
}
