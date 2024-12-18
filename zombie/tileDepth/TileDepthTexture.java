package zombie.tileDepth;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.vehicles.UI3DScene;

public final class TileDepthTexture {
   private final TilesetDepthTexture m_tileset;
   private final int m_index;
   private final int m_width;
   private final int m_height;
   private float[] m_pixels;
   private final String m_name;
   private Texture m_texture;
   private boolean m_bEmpty = true;
   private static final float[] s_clampedPixels = new float[8192];
   private static boolean s_bClampedPixelsInit = false;

   public TileDepthTexture(TilesetDepthTexture var1, int var2) {
      this.m_tileset = var1;
      this.m_index = var2;
      this.m_width = var1.getTileWidth();
      this.m_height = var1.getTileHeight();
      String var10001 = this.m_tileset.getName();
      this.m_name = var10001 + "_" + var2;
   }

   public TilesetDepthTexture getTileset() {
      return this.m_tileset;
   }

   public int getIndex() {
      return this.m_index;
   }

   public int getColumn() {
      return this.getIndex() % this.m_tileset.getColumns();
   }

   public int getRow() {
      return this.getIndex() / this.m_tileset.getColumns();
   }

   public String getName() {
      return this.m_name;
   }

   public int getWidth() {
      return this.m_width;
   }

   public int getHeight() {
      return this.m_height;
   }

   public boolean isEmpty() {
      return this.m_bEmpty;
   }

   public float[] getPixels() {
      return this.m_pixels;
   }

   public void setPixel(int var1, int var2, float var3) {
      this.allocPixelsIfNeeded();
      this.m_pixels[this.index(var1, var2)] = var3;
   }

   public float getPixel(int var1, int var2) {
      return this.m_pixels == null ? -1.0F : this.m_pixels[this.index(var1, var2)];
   }

   public void setMinPixel(int var1, int var2, float var3) {
      int var4 = this.index(var1, var2);
      this.allocPixelsIfNeeded();
      this.m_pixels[var4] = PZMath.min(this.m_pixels[var4], var3);
   }

   public void setPixels(int var1, int var2, int var3, int var4, float var5) {
      if (var3 > 0 && var4 > 0) {
         int var8 = var1 + var3 - 1;
         int var9 = var2 + var4 - 1;
         int var6 = PZMath.clamp(var1, 0, this.getWidth() - 1);
         int var7 = PZMath.clamp(var2, 0, this.getHeight() - 1);
         var8 = PZMath.clamp(var8, 0, this.getWidth() - 1);
         var9 = PZMath.clamp(var9, 0, this.getHeight() - 1);

         for(var2 = var7; var2 <= var9; ++var2) {
            for(var1 = var6; var1 <= var8; ++var1) {
               this.setPixel(var1, var2, var5);
            }
         }

      }
   }

   public void replacePixels(int var1, int var2, int var3, int var4, float var5, float var6) {
      if (var3 > 0 && var4 > 0) {
         int var9 = var1 + var3 - 1;
         int var10 = var2 + var4 - 1;
         int var7 = PZMath.clamp(var1, 0, this.getWidth() - 1);
         int var8 = PZMath.clamp(var2, 0, this.getHeight() - 1);
         var9 = PZMath.clamp(var9, 0, this.getWidth() - 1);
         var10 = PZMath.clamp(var10, 0, this.getHeight() - 1);

         for(var2 = var8; var2 <= var10; ++var2) {
            for(var1 = var7; var1 <= var9; ++var1) {
               if (this.getPixel(var1, var2) == var5) {
                  this.setPixel(var1, var2, var6);
               }
            }
         }

      }
   }

   public int index(int var1, int var2) {
      return var1 + var2 * this.m_width;
   }

   void allocPixelsIfNeeded() {
      if (this.m_pixels == null) {
         this.m_pixels = new float[this.m_width * this.m_height];
         Arrays.fill(this.m_pixels, -1.0F);
      }

   }

   /** @deprecated */
   @Deprecated
   void load(float[] var1, BufferedImage var2, int var3, int var4) {
      this.m_bEmpty = true;

      for(int var5 = 0; var5 < this.m_height; ++var5) {
         for(int var6 = 0; var6 < this.m_width; ++var6) {
            int var7 = var2.getRGB(var3 + var6, var4 + var5);
            int var8 = var7 >> 24 & 255;
            int var9 = var7 & 255;
            var1[var6 + var5 * this.m_width] = var8 == 0 ? -1.0F : (float)var9 / 255.0F;
            if (this.m_bEmpty && var8 != 0) {
               this.m_bEmpty = false;
            }
         }
      }

      if (this.m_bEmpty) {
         this.m_pixels = null;
      } else {
         this.allocPixelsIfNeeded();
         System.arraycopy(var1, 0, this.m_pixels, 0, this.m_pixels.length);
         if (TileDepthTextureManager.getInstance().isLoadingFinished()) {
            IsoSprite var10 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(this.getName());
            if (var10 != null) {
               var10.depthTexture = this;
            }
         }
      }

      this.updateGPUTexture();
   }

   void load(float[] var1, ByteBuffer var2, int var3, int var4, int var5) {
      byte var6 = 4;
      this.m_bEmpty = true;

      for(int var7 = 0; var7 < this.m_height; ++var7) {
         int var8 = (var5 + var7) * var3;

         for(int var9 = 0; var9 < this.m_width; ++var9) {
            int var10 = var8 + (var4 + var9) * var6;
            int var11 = var2.get(var10 + 3) & 255;
            int var12 = var2.get(var10 + 2) & 255;
            var1[var9 + var7 * this.m_width] = var11 == 0 ? -1.0F : (float)var12 / 255.0F;
            if (this.m_bEmpty && var11 != 0) {
               this.m_bEmpty = false;
            }
         }
      }

      if (this.m_bEmpty) {
         this.m_pixels = null;
      } else {
         this.allocPixelsIfNeeded();
         System.arraycopy(var1, 0, this.m_pixels, 0, this.m_pixels.length);
         if (TileDepthTextureManager.getInstance().isLoadingFinished()) {
            IsoSprite var13 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(this.getName());
            if (var13 != null) {
               var13.depthTexture = this;
            }
         }
      }

      this.updateGPUTexture();
   }

   BufferedImage setBufferedImage(BufferedImage var1, int var2, int var3) {
      int[] var4 = new int[this.m_width];

      for(int var5 = 0; var5 < this.m_height; ++var5) {
         for(int var6 = 0; var6 < this.m_width; ++var6) {
            float var7 = this.getPixel(var6, var5);
            if (var7 >= 0.0F) {
               var7 = PZMath.min(var7, 1.0F);
               int var8 = (int)Math.floor((double)(var7 * 255.0F)) & 255;
               short var9 = 255;
               var4[var6] = var9 << 24 | var8 << 16 | var8 << 8 | var8;
            } else {
               var4[var6] = 0;
            }
         }

         var1.setRGB(var2, var3 + var5, this.m_width, 1, var4, 0, this.m_width);
      }

      return var1;
   }

   private float clampPixelToUpperFloor(int var1, int var2, float var3) {
      initClampedPixels();
      return PZMath.max(var3, s_clampedPixels[var1 + var2 * 128]);
   }

   private static void initClampedPixels() {
      if (!s_bClampedPixelsInit) {
         s_bClampedPixelsInit = true;
         float var0 = 2.44949F;
         float var1 = 0.011764706F;
         Vector3f var2 = new Vector3f(0.0F, var0, 0.0F);

         for(int var3 = 0; var3 < 64; ++var3) {
            for(int var4 = 0; var4 < 128; ++var4) {
               float var5 = TileGeometryUtils.getNormalizedDepthOnPlaneAt((float)var4 + 0.5F, (float)var3 + 0.5F, UI3DScene.GridPlane.XZ, var2);
               if (var5 >= 0.0F) {
                  var5 += var1;
               }

               s_clampedPixels[var4 + var3 * 128] = var5;
            }
         }

      }
   }

   public void save() throws Exception {
      this.getTileset().save();
   }

   public boolean fileExists() {
      return this.getTileset().fileExists();
   }

   public Texture getTexture() {
      if (this.m_texture == null) {
         this.m_texture = new Texture(this.m_width, this.m_height, "DEPTH_" + this.getName(), 0);
         this.updateGPUTexture();
      }

      return this.m_texture;
   }

   public void updateGPUTexture() {
      if (this.m_texture == null) {
         this.m_texture = new Texture(this.m_width, this.m_height, "DEPTH_" + this.getName(), 0);
      }

      RenderThread.queueInvokeOnRenderContext(() -> {
         GL11.glBindTexture(3553, Texture.lastTextureID = this.m_texture.getID());
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         byte var1 = 4;
         ByteBuffer var2 = MemoryUtil.memAlloc(this.getWidth() * this.getHeight() * var1);
         var2.position(this.getWidth() * this.getHeight() * var1);
         boolean var3 = true;
         if (this.m_tileset.getName().startsWith("roofs_")) {
            var3 = false;
         }

         this.m_bEmpty = true;

         for(int var4 = 0; var4 < this.getHeight(); ++var4) {
            for(int var5 = 0; var5 < this.getWidth(); ++var5) {
               float var6 = this.getPixel(var5, var4);
               if (var6 >= 0.0F && var4 < 64 && var3) {
                  var6 = this.clampPixelToUpperFloor(var5, var4, var6);
               }

               int var7 = var6 < 0.0F ? 0 : Color.colorToABGR(var6, var6, var6, 1.0F);
               var2.putInt(var5 * var1 + var4 * this.getWidth() * var1, var7);
               if (var6 >= 0.0F && this.m_bEmpty) {
                  this.m_bEmpty = false;
               }
            }
         }

         var2.flip();
         GL11.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 5121, var2);
         MemoryUtil.memFree(var2);
         if (this.m_tileset != null && !this.m_tileset.isKeepPixels()) {
            this.m_pixels = null;
         }

      });
   }

   void recalculateDepth() {
      ArrayList var1 = TileGeometryManager.getInstance().getGeometry("game", this.getTileset().getName(), this.getColumn(), this.getRow());
      if (var1 != null && !var1.isEmpty()) {
         float[] var2 = new float['耀'];
         Arrays.fill(var2, 1000.0F);

         int var3;
         for(var3 = 0; var3 < var1.size(); ++var3) {
            TileGeometryFile.Geometry var4 = (TileGeometryFile.Geometry)var1.get(var3);
            if (var4.isPolygon()) {
               ((TileGeometryFile.Polygon)var4).rasterize((var3x, var4x) -> {
                  if (var3x >= 0 && var3x < 128 && var4x >= 0 && var4x < 256) {
                     float var5 = var4.getNormalizedDepthAt((float)var3x, (float)var4x);
                     if (var5 >= 0.0F) {
                        var2[this.index(var3x, var4x)] = PZMath.min(var2[this.index(var3x, var4x)], var5);
                     }

                  }
               });
            } else {
               for(int var5 = 0; var5 < this.getHeight(); ++var5) {
                  for(int var6 = 0; var6 < this.getWidth(); ++var6) {
                     float var7 = this.getPixel(var6, var5);
                     if (!(var7 < 0.0F)) {
                        float var8 = var4.getNormalizedDepthAt((float)var6 + 0.5F, (float)var5 + 0.5F);
                        if (var8 >= 0.0F) {
                           var2[this.index(var6, var5)] = PZMath.min(var2[this.index(var6, var5)], var8);
                        }
                     }
                  }
               }
            }
         }

         for(var3 = 0; var3 < this.getHeight(); ++var3) {
            for(int var9 = 0; var9 < this.getWidth(); ++var9) {
               float var10 = var2[this.index(var9, var3)];
               if (var10 != 1000.0F) {
                  this.setPixel(var9, var3, var10);
               }
            }
         }

         this.updateGPUTexture();
      }
   }

   public void reload() throws Exception {
      this.getTileset().reload();
   }

   public void Reset() {
   }
}
