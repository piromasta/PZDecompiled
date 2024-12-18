package zombie.tileDepth;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryUtil;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.PNGDecoder;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class TilesetDepthTexture {
   private final TileDepthTextures m_owner;
   private final String m_name;
   private final int m_columns;
   private final int m_rows;
   private final TileDepthTexture[] m_tiles;
   private final boolean m_b2x;
   private int m_fileExists = -1;
   private boolean m_bKeepPixels = false;

   public TilesetDepthTexture(TileDepthTextures var1, String var2, int var3, int var4, boolean var5) {
      this.m_owner = var1;
      this.m_name = var2;
      this.m_columns = var3;
      this.m_rows = var4;
      this.m_tiles = new TileDepthTexture[this.m_columns * this.m_rows];
      this.m_b2x = var5;
   }

   public int getColumns() {
      return this.m_columns;
   }

   public int getRows() {
      return this.m_rows;
   }

   public boolean is2x() {
      return this.m_b2x;
   }

   public void setKeepPixels(boolean var1) {
      this.m_bKeepPixels = var1;
   }

   public boolean isKeepPixels() {
      return this.m_bKeepPixels;
   }

   public TileDepthTexture getOrCreateTile(int var1) {
      if (var1 >= 0 && var1 < this.m_tiles.length) {
         TileDepthTexture var2 = this.m_tiles[var1];
         if (var2 == null) {
            var2 = this.createTile(var1);
         }

         return var2;
      } else {
         return null;
      }
   }

   private TileDepthTexture createTile(int var1) {
      TileDepthTexture var2 = new TileDepthTexture(this, var1);
      this.m_tiles[var1] = var2;
      return var2;
   }

   public TileDepthTexture getOrCreateTile(int var1, int var2) {
      return this.getOrCreateTile(this.tileIndex(var1, var2));
   }

   public String getName() {
      return this.m_name;
   }

   public int getTileWidth() {
      return 64 * (this.m_b2x ? 2 : 1);
   }

   public int getTileHeight() {
      return 128 * (this.m_b2x ? 2 : 1);
   }

   public int getWidth() {
      return this.m_columns * this.getTileWidth();
   }

   public int getHeight() {
      return this.m_rows * this.getTileHeight();
   }

   public int getTileCount() {
      return this.m_columns * this.m_rows;
   }

   private int tileIndex(int var1, int var2) {
      return var1 + var2 * this.m_columns;
   }

   boolean isEmpty() {
      for(int var1 = 0; var1 < this.m_rows; ++var1) {
         for(int var2 = 0; var2 < this.m_columns; ++var2) {
            TileDepthTexture var3 = this.m_tiles[this.tileIndex(var2, var1)];
            if (var3 != null && !var3.isEmpty()) {
               return false;
            }
         }
      }

      return true;
   }

   BufferedImage getBufferedImage() {
      BufferedImage var1 = new BufferedImage(this.getWidth(), this.getHeight(), 2);

      for(int var2 = 0; var2 < this.m_rows; ++var2) {
         for(int var3 = 0; var3 < this.m_columns; ++var3) {
            TileDepthTexture var4 = this.m_tiles[this.tileIndex(var3, var2)];
            if (var4 != null) {
               var4.setBufferedImage(var1, var3 * this.getTileWidth(), var2 * this.getTileHeight());
            }
         }
      }

      return var1;
   }

   void writeImageToFile(BufferedImage var1, String var2) throws Exception {
      File var3 = new File(var2);
      ImageIO.write(var1, "png", var3);
   }

   public String getRelativeFileName() {
      return "media/depthmaps/DEPTH_" + this.getName() + ".png";
   }

   public String getAbsoluteFileName() {
      String var10000 = this.m_owner.m_mediaAbsPath;
      return var10000 + File.separator + "depthmaps" + File.separator + "DEPTH_" + this.getName() + ".png";
   }

   public void load() throws Exception {
      FileInputStream var1 = new FileInputStream(this.getAbsoluteFileName());

      try {
         BufferedInputStream var2 = new BufferedInputStream(var1);

         try {
            PNGDecoder var3 = new PNGDecoder(var2, false);
            byte var4 = 4;
            int var5 = var3.getWidth() * var4;
            ByteBuffer var6 = MemoryUtil.memAlloc(var5 * var3.getHeight());
            var3.decode(var6, var5, var3.getHeight(), PNGDecoder.Format.RGBA, 1229209940);
            float[] var7 = new float[this.getTileWidth() * this.getTileHeight()];
            int var8 = 0;

            while(true) {
               if (var8 >= this.m_rows) {
                  MemoryUtil.memFree(var6);
                  break;
               }

               for(int var9 = 0; var9 < this.m_columns; ++var9) {
                  TileDepthTexture var10 = this.getOrCreateTile(var9, var8);
                  if (var9 < var3.getWidth() / this.getTileWidth() && var8 < var3.getHeight() / this.getTileHeight()) {
                     var10.load(var7, var6, var5, var9 * this.getTileWidth(), var8 * this.getTileHeight());
                  }
               }

               ++var8;
            }
         } catch (Throwable var13) {
            try {
               var2.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         var2.close();
      } catch (Throwable var14) {
         try {
            var1.close();
         } catch (Throwable var11) {
            var14.addSuppressed(var11);
         }

         throw var14;
      }

      var1.close();
   }

   public void save() throws Exception {
      if (this.isEmpty()) {
         if (Files.exists(Paths.get(this.getAbsoluteFileName()), new LinkOption[0])) {
            this.removeFile();
         }

      } else {
         BufferedImage var1 = this.getBufferedImage();
         this.writeImageToFile(var1, this.getAbsoluteFileName());
         this.m_fileExists = -1;
      }
   }

   public boolean fileExists() {
      if (this.m_fileExists == -1) {
         this.m_fileExists = Files.exists(Paths.get(this.getAbsoluteFileName()), new LinkOption[0]) ? 1 : 0;
      }

      return this.m_fileExists == 1;
   }

   public void removeFile() {
      try {
         Files.delete(Paths.get(this.getAbsoluteFileName()));
      } catch (Exception var2) {
         ExceptionLogger.logException(var2);
      }

   }

   public Texture getTexture() {
      return Texture.getSharedTexture(this.getRelativeFileName());
   }

   public void reload() throws Exception {
      if (this.fileExists()) {
         this.load();
      }

   }

   public void mergeTileset(TilesetDepthTexture var1) {
      int var2 = PZMath.min(this.m_rows, var1.m_rows);
      int var3 = PZMath.min(this.m_columns, var1.m_columns);

      for(int var4 = 0; var4 < var2; ++var4) {
         for(int var5 = 0; var5 < var3; ++var5) {
            int var6 = var5 + var4 * this.m_columns;
            int var7 = var5 + var4 * var1.m_columns;
            if ((this.m_tiles[var6] == null || this.m_tiles[var6].isEmpty()) && var1.m_tiles[var7] != null && !var1.m_tiles[var7].isEmpty()) {
               this.m_tiles[var6] = var1.m_tiles[var7];
            }
         }
      }

   }

   public void initSprites() {
      for(int var1 = 0; var1 < this.getTileCount(); ++var1) {
         TileDepthTexture var2 = this.m_tiles[var1];
         if (var2 != null) {
            IsoSprite var3 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2.getName());
            if (var3 != null && var3.depthTexture == null && !var2.isEmpty()) {
               var3.depthTexture = var2;
            }
         }
      }

   }

   void initSpriteProperties(IsoSprite var1, TileDepthTexture var2) {
      var1.depthFlags = 0;
      TileGeometryFile.Tile var3 = TileGeometryManager.getInstance().getTile(this.m_owner.m_modID, this.m_name, var2.getColumn(), var2.getRow());
      if (var3 != null && var3.m_properties != null && !var3.m_properties.isEmpty()) {
         String var4 = (String)var3.m_properties.get("ItemHeight");
         if (var4 != null) {
            var1.getProperties().Set("ItemHeight", var4, false);
         }

         var4 = (String)var3.m_properties.get("Surface");
         if (var4 != null) {
            var1.getProperties().Set("Surface", var4, false);
         }

         var4 = (String)var3.m_properties.get("OpaquePixelsOnly");
         if (StringUtils.tryParseBoolean(var4)) {
            var1.depthFlags |= 4;
         }

         var4 = (String)var3.m_properties.get("Translucent");
         if (StringUtils.tryParseBoolean(var4)) {
            var1.depthFlags |= 2;
         }

         var4 = (String)var3.m_properties.get("UseObjectDepthTexture");
         if (StringUtils.tryParseBoolean(var4)) {
            var1.depthFlags |= 1;
         }

      }
   }

   void recalculateDepth() {
      for(int var1 = 0; var1 < this.m_rows; ++var1) {
         for(int var2 = 0; var2 < this.m_columns; ++var2) {
            TileDepthTexture var3 = this.m_tiles[this.tileIndex(var2, var1)];
            if (var3 != null && !var3.isEmpty()) {
               var3.recalculateDepth();
            }
         }
      }

   }

   public void clearTiles() {
      Arrays.fill(this.m_tiles, (Object)null);
   }

   public void Reset() {
      for(int var1 = 0; var1 < this.m_tiles.length; ++var1) {
         TileDepthTexture var2 = this.m_tiles[var1];
         if (var2 != null) {
            this.m_tiles[var1] = null;
            var2.Reset();
         }
      }

   }
}
