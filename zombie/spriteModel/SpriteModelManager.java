package zombie.spriteModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.SpriteModel;
import zombie.iso.SpriteModels;
import zombie.iso.SpriteModelsFile;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class SpriteModelManager {
   private static SpriteModelManager instance = null;
   private boolean m_bLoadedTileDefinitions = false;
   private final ArrayList<ModData> m_modData = new ArrayList();

   public SpriteModelManager() {
   }

   public static SpriteModelManager getInstance() {
      if (instance == null) {
         instance = new SpriteModelManager();
      }

      return instance;
   }

   public void init() {
      this.initGameData();
      ArrayList var1 = ZomboidFileSystem.instance.getModIDs();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         ChooseGameInfo.Mod var4 = ChooseGameInfo.getAvailableModDetails(var3);
         if (var4 != null) {
            File var5 = new File(var4.mediaFile.common.absoluteFile, "spriteModels.txt");
            if (var5.exists()) {
               this.initModData(var4);
            }
         }
      }

   }

   public void initGameData() {
      ModData var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
      var1.m_spriteModels.init();
      this.m_modData.add(var1);
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
      var2.m_spriteModels.init();
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

   public void setTileProperties(String var1, String var2, int var3, int var4, SpriteModel var5) {
      this.getModData(var1).m_spriteModels.setTileProperties(var2, var3, var4, var5);
   }

   public SpriteModel getTileProperties(String var1, String var2, int var3, int var4) {
      return this.getModData(var1).m_spriteModels.getTileProperties(var2, var3, var4);
   }

   public void clearTileProperties(String var1, String var2, int var3, int var4) {
      this.getModData(var1).m_spriteModels.clearTileProperties(var2, var3, var4);
   }

   public SpriteModelsFile.Tileset findTileset(String var1, String var2) {
      return this.getModData(var1).m_spriteModels.findTileset(var2);
   }

   public void toScriptManager(String var1) {
      this.getModData(var1).m_spriteModels.toScriptManager();
   }

   public void toScriptManager() {
      for(int var1 = 0; var1 < this.m_modData.size(); ++var1) {
         ((ModData)this.m_modData.get(var1)).m_spriteModels.toScriptManager();
      }

   }

   public void loadedTileDefinitions() {
      this.m_bLoadedTileDefinitions = true;
      this.initSprites();
   }

   public void initSprites() {
      IsoSprite var2;
      for(Iterator var1 = IsoSpriteManager.instance.NamedMap.values().iterator(); var1.hasNext(); var2.spriteModel = null) {
         var2 = (IsoSprite)var1.next();
      }

      for(int var3 = 0; var3 < this.m_modData.size(); ++var3) {
         ModData var4 = (ModData)this.m_modData.get(var3);
         var4.m_spriteModels.initSprites();
      }

   }

   public void write(String var1) {
      this.getModData(var1).m_spriteModels.write();
   }

   public void Reset() {
      Iterator var1 = this.m_modData.iterator();

      while(var1.hasNext()) {
         ModData var2 = (ModData)var1.next();
         var2.m_spriteModels.Reset();
      }

      this.m_modData.clear();
      this.m_bLoadedTileDefinitions = false;
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final SpriteModels m_spriteModels;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_spriteModels = new SpriteModels(var2);
      }
   }
}
