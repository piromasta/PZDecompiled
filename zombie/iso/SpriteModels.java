package zombie.iso;

import java.util.Iterator;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptManager;

public final class SpriteModels {
   private SpriteModelsFile m_file;
   private final String m_mediaAbsPath;

   public SpriteModels(String var1) {
      this.m_mediaAbsPath = var1;
   }

   public void init() {
      this.m_file = new SpriteModelsFile();
      this.m_file.read(this.m_mediaAbsPath + "/spriteModels.txt");
      this.fromScriptManager();
      this.toScriptManager();
   }

   public void write() {
      this.m_file.write(this.m_mediaAbsPath + "/spriteModels.txt");
   }

   public void setTileProperties(String var1, int var2, int var3, SpriteModel var4) {
      SpriteModelsFile.Tileset var5 = this.findTileset(var1);
      if (var5 == null) {
         var5 = new SpriteModelsFile.Tileset();
         var5.name = var1;
         this.m_file.tilesets.add(var5);
      }

      SpriteModelsFile.Tile var6 = var5.getOrCreateTile(var2, var3);
      var6.spriteModel.set(var4);
   }

   public SpriteModel getTileProperties(String var1, int var2, int var3) {
      SpriteModelsFile.Tileset var4 = this.findTileset(var1);
      if (var4 == null) {
         return null;
      } else {
         SpriteModelsFile.Tile var5 = var4.getTile(var2, var3);
         return var5 == null ? null : var5.spriteModel;
      }
   }

   public void clearTileProperties(String var1, int var2, int var3) {
      SpriteModelsFile.Tileset var4 = this.findTileset(var1);
      if (var4 != null) {
         SpriteModelsFile.Tile var5 = var4.getTile(var2, var3);
         if (var5 != null) {
            var4.tiles.remove(var5);
         }
      }
   }

   public SpriteModelsFile.Tileset findTileset(String var1) {
      Iterator var2 = this.m_file.tilesets.iterator();

      SpriteModelsFile.Tileset var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (SpriteModelsFile.Tileset)var2.next();
      } while(!var1.equals(var3.name));

      return var3;
   }

   void fromScriptManager() {
      Iterator var1 = ScriptManager.instance.getAllSpriteModels().iterator();

      while(var1.hasNext()) {
         SpriteModel var2 = (SpriteModel)var1.next();
         String var3 = var2.getScriptObjectName();
         int var4 = var3.lastIndexOf(95);
         if (var4 != -1) {
            String var5 = var3.substring(0, var4);
            int var6 = PZMath.tryParseInt(var3.substring(var4 + 1), -1);
            if (var6 >= 0) {
               this.setTileProperties(var5, var6 % 8, var6 / 8, var2);
            }
         }
      }

   }

   public void toScriptManager() {
      Iterator var1 = this.m_file.tilesets.iterator();

      while(var1.hasNext()) {
         SpriteModelsFile.Tileset var2 = (SpriteModelsFile.Tileset)var1.next();
         Iterator var3 = var2.tiles.iterator();

         while(var3.hasNext()) {
            SpriteModelsFile.Tile var4 = (SpriteModelsFile.Tile)var3.next();
            String var5 = String.format("%s_%d", var2.name, var4.getIndex());
            SpriteModel var6 = ScriptManager.instance.getSpriteModel(var5);
            if (var6 == null) {
               var6 = new SpriteModel();
               var6.set(var4.spriteModel);
               var6.setModule(ScriptManager.instance.getModule("Base"));
               var6.InitLoadPP(var5);
               ScriptManager.instance.addSpriteModel(var6);
            } else {
               var6.set(var4.spriteModel);
            }
         }
      }

   }

   public void initSprites() {
      Iterator var1 = this.m_file.tilesets.iterator();

      while(var1.hasNext()) {
         SpriteModelsFile.Tileset var2 = (SpriteModelsFile.Tileset)var1.next();
         var2.initSprites();
      }

   }

   public void Reset() {
      if (this.m_file != null) {
         this.m_file.Reset();
         this.m_file = null;
      }
   }
}
