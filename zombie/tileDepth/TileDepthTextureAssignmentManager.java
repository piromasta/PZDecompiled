package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class TileDepthTextureAssignmentManager {
   private static TileDepthTextureAssignmentManager instance;
   private final ArrayList<ModData> m_modData = new ArrayList();

   public static TileDepthTextureAssignmentManager getInstance() {
      if (instance == null) {
         instance = new TileDepthTextureAssignmentManager();
      }

      return instance;
   }

   private TileDepthTextureAssignmentManager() {
   }

   public void init() {
      ArrayList var1 = ZomboidFileSystem.instance.getModIDs();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         ChooseGameInfo.Mod var4 = ChooseGameInfo.getAvailableModDetails(var3);
         if (var4 != null) {
            File var5 = new File(var4.mediaFile.common.absoluteFile, "tileDepthTextureAssignments.txt");
            if (var5.exists()) {
               this.initModData(var4);
            }
         }
      }

      this.initGameData();
   }

   public void initGameData() {
      ModData var1 = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
      var1.m_assignments.load();
      this.m_modData.add(var1);
   }

   public void initModData(ChooseGameInfo.Mod var1) {
      ModData var2 = new ModData(var1.getId(), var1.mediaFile.common.absoluteFile.getAbsolutePath());
      var2.m_assignments.load();
      this.m_modData.add(var2);
   }

   public void save(String var1) {
      this.getModData(var1).m_assignments.save();
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

   public void initSprites() {
      Iterator var1 = IsoSpriteManager.instance.NamedMap.values().iterator();

      while(var1.hasNext()) {
         IsoSprite var2 = (IsoSprite)var1.next();
         if (var2.depthTexture == null && var2.tilesetName != null) {
            String var3 = this.getAssignedTileName(var2.name);
            if (var3 != null) {
               TileDepthTexture var4 = TileDepthTextureManager.getInstance().getTextureFromTileName(var3);
               if (var4 != null && !var4.isEmpty()) {
                  var2.depthTexture = var4;
               }
            }
         }
      }

   }

   public void assignTileName(String var1, String var2, String var3) {
      this.getModData(var1).m_assignments.assignTileName(var2, var3);
   }

   public String getAssignedTileName(String var1, String var2) {
      return this.getModData(var1).m_assignments.getAssignedTileName(var2);
   }

   public void clearAssignedTileName(String var1, String var2) {
      this.getModData(var1).m_assignments.clearAssignedTileName(var2);
   }

   public void assignDepthTextureToSprite(String var1, String var2) {
      this.getModData(var1).m_assignments.assignDepthTextureToSprite(var2);
   }

   String getAssignedTileName(String var1) {
      for(int var2 = 0; var2 < this.m_modData.size(); ++var2) {
         ModData var3 = (ModData)this.m_modData.get(var2);
         String var4 = var3.m_assignments.getAssignedTileName(var1);
         if (var4 != null) {
            return var4;
         }
      }

      return null;
   }

   static final class ModData {
      final String m_modID;
      final String m_mediaAbsPath;
      final TileDepthTextureAssignments m_assignments;

      ModData(String var1, String var2) {
         this.m_modID = var1;
         this.m_mediaAbsPath = var2;
         this.m_assignments = new TileDepthTextureAssignments(var2);
      }
   }
}
