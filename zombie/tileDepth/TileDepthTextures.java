package zombie.tileDepth;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import zombie.GameWindow;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;

public final class TileDepthTextures {
   final String m_modID;
   final String m_mediaAbsPath;
   private final HashMap<String, Integer> m_tilesetRows = new HashMap();
   private final HashMap<String, TilesetDepthTexture> m_tilesets = new HashMap();
   private final HashSet<String> m_nullTilesets = new HashSet();

   public TileDepthTextures(String var1, String var2) {
      this.m_modID = var1;
      this.m_mediaAbsPath = var2;
   }

   public void saveTileset(String var1) throws Exception {
      TilesetDepthTexture var2 = (TilesetDepthTexture)this.m_tilesets.get(var1);
      if (var2 != null) {
         var2.save();
      }
   }

   public TileDepthTexture getTexture(String var1, int var2) {
      TilesetDepthTexture var3 = (TilesetDepthTexture)this.m_tilesets.get(var1);
      if (var3 == null) {
         var3 = this.createTileset(var1, false);
         if (var3 == null) {
            this.m_nullTilesets.add(var1);
            return null;
         }
      }

      return var3.getOrCreateTile(var2);
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
               TilesetDepthTexture var5 = (TilesetDepthTexture)this.m_tilesets.get(var3);
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

   private TilesetDepthTexture createTileset(String var1, boolean var2) {
      byte var3 = 8;
      int var4 = this.getTilesetRows(var1, var2);
      if (var4 == 0) {
         return null;
      } else {
         TilesetDepthTexture var5 = new TilesetDepthTexture(this, var1, var3, var4, true);
         if (var5.fileExists()) {
            try {
               var5.load();
            } catch (Exception var7) {
               ExceptionLogger.logException(var7);
            }
         }

         this.m_tilesets.put(var1, var5);
         return var5;
      }
   }

   public TilesetDepthTexture getExistingTileset(String var1) {
      return (TilesetDepthTexture)this.m_tilesets.get(var1);
   }

   private int getTilesetRows(String var1, boolean var2) {
      if (var2) {
         return (Integer)this.m_tilesetRows.getOrDefault(var1, 0);
      } else {
         byte var3 = 8;

         for(int var4 = 63; var4 >= 0; --var4) {
            for(int var5 = 0; var5 < var3; ++var5) {
               int var6 = var5 + var4 * var3;
               Texture var7 = Texture.getSharedTexture(var1 + "_" + var6);
               if (var7 != null) {
                  return var4 + 1;
               }
            }
         }

         return 0;
      }
   }

   public void loadDepthTextureImages() {
      Path var1 = FileSystems.getDefault().getPath(this.m_mediaAbsPath, "depthmaps");
      if (Files.exists(var1, new LinkOption[0])) {
         DirectoryStream.Filter var2 = (var0) -> {
            return Files.isRegularFile(var0, new LinkOption[0]) && var0.toString().endsWith(".png");
         };

         try {
            DirectoryStream var3 = Files.newDirectoryStream(var1, var2);

            try {
               Iterator var4 = var3.iterator();

               while(var4.hasNext()) {
                  Path var5 = (Path)var4.next();
                  String var6 = var5.toFile().getName();
                  if (var6.startsWith("DEPTH_") && var6.endsWith(".png") && !"DEPTH_whole_tile.png".equalsIgnoreCase(var6) && !"DEPTH_preset_depthmaps_01.png".equalsIgnoreCase(var6)) {
                     String var7 = var6.substring(6, var6.length() - 4);
                     this.m_tilesetRows.put(var7, this.getTilesetRows(var7, false));
                     TileDepthTextureManager.getInstance().addedLoadTask();
                     FileSystem var8 = GameWindow.fileSystem;
                     LoadTask var9 = new LoadTask(this, var5, var8);
                     var9.setPriority(4);
                     var8.runAsync(var9);
                  }
               }
            } catch (Throwable var11) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (var3 != null) {
               var3.close();
            }
         } catch (Exception var12) {
            ExceptionLogger.logException(var12);
         }

      }
   }

   protected void hackAddPresetTilesetDepthTexture() {
      TilesetDepthTexture var1 = TileDepthTextureManager.getInstance().getPresetTilesetDepthTexture();
      if (var1 != null) {
         this.m_tilesets.put(var1.getName(), var1);
      }

   }

   public void mergeTilesets(TileDepthTextures var1) {
      Iterator var2 = var1.m_tilesets.values().iterator();

      while(var2.hasNext()) {
         TilesetDepthTexture var3 = (TilesetDepthTexture)var2.next();
         this.mergeTileset(var3);
      }

   }

   public void mergeTileset(TilesetDepthTexture var1) {
      TilesetDepthTexture var2 = (TilesetDepthTexture)this.m_tilesets.get(var1.getName());
      if (var2 == null) {
         var2 = new TilesetDepthTexture(this, var1.getName(), var1.getWidth() / var1.getTileWidth(), var1.getHeight() / var1.getTileHeight(), var1.is2x());
         this.m_tilesets.put(var2.getName(), var2);
      }

      var2.mergeTileset(var1);
   }

   public void initSprites() {
      Iterator var1 = this.m_tilesets.values().iterator();

      while(var1.hasNext()) {
         TilesetDepthTexture var2 = (TilesetDepthTexture)var1.next();
         var2.initSprites();
      }

   }

   public void initSprites(String var1) {
      TilesetDepthTexture var2 = (TilesetDepthTexture)this.m_tilesets.get(var1);
      if (var2 != null) {
         var2.initSprites();
      }
   }

   public void Reset() {
      Iterator var1 = this.m_tilesets.values().iterator();

      while(var1.hasNext()) {
         TilesetDepthTexture var2 = (TilesetDepthTexture)var1.next();
         var2.Reset();
      }

      this.m_tilesets.clear();
      this.m_nullTilesets.clear();
   }

   static final class LoadTask extends FileTask {
      final TileDepthTextures textures;
      final Path path;

      public LoadTask(TileDepthTextures var1, Path var2, FileSystem var3) {
         super(var3);
         this.textures = var1;
         this.path = var2;
      }

      public void done() {
         TileDepthTextureManager.getInstance().finishedLoadTask();
      }

      public Object call() throws Exception {
         String var1 = this.path.toFile().getName();
         String var2 = var1.replaceFirst("DEPTH_", "").replace(".png", "");
         synchronized(this.textures) {
            TilesetDepthTexture var4 = (TilesetDepthTexture)this.textures.m_tilesets.get(var2);
            if (var4 == null) {
               this.textures.createTileset(var2, true);
            }

            return null;
         }
      }
   }
}
