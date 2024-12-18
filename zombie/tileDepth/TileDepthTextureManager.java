package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class TileDepthTextureManager {
   private static TileDepthTextureManager instance;
   public static final boolean DELAYED_LOADING = true;
   private int m_remainingLoadTasks = 0;
   private boolean m_bLoadedTileDefinitions = false;
   private final ArrayList<ModData> m_modData = new ArrayList();
   private final ArrayList<ModData> m_previouslyLoadedModData = new ArrayList();
   private TilesetDepthTexture m_defaultDepthTextureTileset;
   private TilesetDepthTexture m_presetDepthTextureTileset;
   private TileDepthTextures m_mergedTilesets;
   private final HashSet<String> m_nullTilesets = new HashSet();

   public static TileDepthTextureManager getInstance() {
      if (instance == null) {
         instance = new TileDepthTextureManager();
      }

      return instance;
   }

   private TileDepthTextureManager() {
   }

   public void init() {
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

      this.initGameData();
   }

   public void initGameData() {
      ModData var1 = this.getModData("game", this.m_previouslyLoadedModData);
      if (var1 == null) {
         var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
         var1.m_textures.loadDepthTextureImages();
         this.m_modData.add(var1);
         var1.m_textures.hackAddPresetTilesetDepthTexture();
      } else {
         this.m_modData.add(var1);
      }
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = this.getModData(var1.getId(), this.m_previouslyLoadedModData);
      if (var2 == null) {
         var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
         var2.m_textures.loadDepthTextureImages();
      }

      this.m_modData.add(var2);
   }

   private void initMergedTilesets() {
      this.m_mergedTilesets = new TileDepthTextures((String)null, (String)null);

      for(int var1 = 0; var1 < this.m_modData.size(); ++var1) {
         ModData var2 = (ModData)this.m_modData.get(var1);
         this.m_mergedTilesets.mergeTilesets(var2.m_textures);
      }

   }

   public void mergeAfterEditing(String var1) {
      TilesetDepthTexture var2 = this.m_mergedTilesets.getExistingTileset(var1);
      if (var2 != null) {
         var2.clearTiles();
      }

      for(int var3 = 0; var3 < this.m_modData.size(); ++var3) {
         ModData var4 = (ModData)this.m_modData.get(var3);
         TilesetDepthTexture var5 = var4.m_textures.getExistingTileset(var1);
         if (var5 != null) {
            this.m_mergedTilesets.mergeTileset(var5);
         }
      }

   }

   public void reloadTileset(String var1, String var2) throws Exception {
      TilesetDepthTexture var3 = this.getModData(var1).m_textures.getExistingTileset(var2);
      if (var3 != null) {
         var3.reload();
      }
   }

   ModData getModData(String var1) {
      return this.getModData(var1, this.m_modData);
   }

   ModData getModData(String var1, ArrayList<ModData> var2) {
      for(int var3 = 0; var3 < var2.size(); ++var3) {
         ModData var4 = (ModData)var2.get(var3);
         if (StringUtils.equals(var1, var4.m_modID)) {
            return var4;
         }
      }

      return null;
   }

   public void loadTilesetPixelsIfNeeded(String var1, String var2) {
      TilesetDepthTexture var3 = this.getModData(var1).m_textures.getExistingTileset(var2);
      if (var3 != null) {
         if (!var3.isKeepPixels()) {
            var3.setKeepPixels(true);

            try {
               var3.reload();
            } catch (Exception var5) {
               ExceptionLogger.logException(var5);
            }

         }
      }
   }

   public void saveTileset(String var1, String var2) throws Exception {
      ModData var3 = this.getModData(var1);
      TilesetDepthTexture var4 = var3.m_textures.getExistingTileset(var2);
      if (var4 != null) {
         if (var4.isKeepPixels()) {
            var3.m_textures.saveTileset(var2);
         }
      }
   }

   public TileDepthTexture getTexture(String var1, String var2, int var3) {
      return this.getModData(var1).m_textures.getTexture(var2, var3);
   }

   public TileDepthTexture getTextureFromTileName(String var1, String var2) {
      return this.getModData(var1).m_textures.getTextureFromTileName(var2);
   }

   public TileDepthTexture getTexture(String var1, int var2) {
      if (this.m_nullTilesets.contains(var1)) {
         return null;
      } else {
         TilesetDepthTexture var3 = this.m_mergedTilesets.getExistingTileset(var1);
         if (var3 == null) {
            this.m_nullTilesets.add(var1);
            return null;
         } else {
            return var3.getOrCreateTile(var2);
         }
      }
   }

   public TileDepthTexture getTextureFromTileName(String var1) {
      int var2 = var1.lastIndexOf(95);
      if (var2 == -1) {
         return null;
      } else {
         String var3 = var1.substring(0, var2);
         if (this.m_nullTilesets.contains(var3)) {
            return null;
         } else {
            int var4 = PZMath.tryParseInt(var1.substring(var2 + 1), -1);
            if (var4 == -1) {
               return null;
            } else {
               TilesetDepthTexture var5 = this.m_mergedTilesets.getExistingTileset(var3);
               if (var5 == null) {
                  this.m_nullTilesets.add(var3);
                  return null;
               } else {
                  return var5.getOrCreateTile(var4);
               }
            }
         }
      }
   }

   private void initDefaultDepthTexture() {
      if (this.m_defaultDepthTextureTileset == null) {
         TilesetDepthTexture var1 = new TilesetDepthTexture(this.getModData("game").m_textures, "whole_tile", 1, 1, true);
         if (var1.fileExists()) {
            try {
               var1.load();
            } catch (Exception var3) {
               ExceptionLogger.logException(var3);
            }
         }

         this.m_defaultDepthTextureTileset = var1;
      }
   }

   public TileDepthTexture getDefaultDepthTexture() {
      this.initDefaultDepthTexture();
      return this.m_defaultDepthTextureTileset == null ? null : this.m_defaultDepthTextureTileset.getOrCreateTile(0, 0);
   }

   private void initPresetDepthTexture() {
      if (this.m_presetDepthTextureTileset == null) {
         TilesetDepthTexture var1 = new TilesetDepthTexture(this.getModData("game").m_textures, "preset_depthmaps_01", 8, 1, true);
         if (var1.fileExists()) {
            try {
               var1.load();
            } catch (Exception var3) {
               ExceptionLogger.logException(var3);
            }
         }

         this.m_presetDepthTextureTileset = var1;
      }
   }

   public TilesetDepthTexture getPresetTilesetDepthTexture() {
      this.initPresetDepthTexture();
      return this.m_presetDepthTextureTileset;
   }

   public TileDepthTexture getPresetDepthTexture(int var1, int var2) {
      this.initPresetDepthTexture();
      return this.m_presetDepthTextureTileset == null ? null : this.m_presetDepthTextureTileset.getOrCreateTile(var1, var2);
   }

   public void initSprites() {
      IsoSprite var2;
      for(Iterator var1 = IsoSpriteManager.instance.NamedMap.values().iterator(); var1.hasNext(); var2.depthFlags = 0) {
         var2 = (IsoSprite)var1.next();
         var2.depthTexture = null;
      }

      for(int var3 = 0; var3 < this.m_modData.size(); ++var3) {
         ModData var4 = (ModData)this.m_modData.get(var3);
         var4.m_textures.initSprites();
      }

   }

   public void initSprites(String var1) {
      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         var3.m_textures.initSprites(var1);
      }

   }

   public void Reset() {
      for(int var1 = 0; var1 < this.m_modData.size(); ++var1) {
         ModData var2 = (ModData)this.m_modData.get(var1);
         if (!this.m_previouslyLoadedModData.contains(var2)) {
            this.m_previouslyLoadedModData.add(var2);
         }
      }

      this.m_modData.clear();
      this.m_mergedTilesets = null;
      this.m_nullTilesets.clear();
      this.m_bLoadedTileDefinitions = false;
   }

   public void addedLoadTask() {
      ++this.m_remainingLoadTasks;
   }

   public void finishedLoadTask() {
      --this.m_remainingLoadTasks;
      if (this.m_remainingLoadTasks == 0) {
         this.initMergedTilesets();
         if (this.m_bLoadedTileDefinitions) {
            this.initSprites();
            TileDepthTextureAssignmentManager.getInstance().initSprites();
            TileGeometryManager.getInstance().initSpriteProperties();
         }
      }

   }

   public void loadedTileDefinitions() {
      this.m_bLoadedTileDefinitions = true;
      if (this.m_remainingLoadTasks <= 0) {
         if (this.m_mergedTilesets == null) {
            this.initMergedTilesets();
         }

         this.initSprites();
         TileDepthTextureAssignmentManager.getInstance().initSprites();
         TileGeometryManager.getInstance().initSpriteProperties();
      }
   }

   public boolean isLoadingFinished() {
      return this.m_bLoadedTileDefinitions && this.m_mergedTilesets != null;
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final TileDepthTextures m_textures;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_textures = new TileDepthTextures(var1, var2);
      }

      public void Reset() {
         this.m_textures.Reset();
      }
   }
}
