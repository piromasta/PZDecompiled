package zombie.worldMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureID;
import zombie.core.utils.ImageUtils;
import zombie.iso.IsoCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.worldMap.styles.WorldMapStyleLayer;

public class WorldMapVisited {
   private static WorldMapVisited instance;
   private int m_minX;
   private int m_minY;
   private int m_maxX;
   private int m_maxY;
   byte[] m_visited;
   boolean m_changed = false;
   int m_changeX1 = 0;
   int m_changeY1 = 0;
   int m_changeX2 = 0;
   int m_changeY2 = 0;
   private final int[] m_updateMinX = new int[4];
   private final int[] m_updateMinY = new int[4];
   private final int[] m_updateMaxX = new int[4];
   private final int[] m_updateMaxY = new int[4];
   private static final int TEXTURE_BPP = 4;
   private TextureID m_textureID;
   private int m_textureW = 0;
   private int m_textureH = 0;
   private ByteBuffer m_textureBuffer;
   private boolean m_textureChanged = false;
   private final WorldMapStyleLayer.RGBAf m_color = (new WorldMapStyleLayer.RGBAf()).init(0.85882354F, 0.84313726F, 0.7529412F, 1.0F);
   private final WorldMapStyleLayer.RGBAf m_gridColor;
   private boolean m_mainMenu;
   private static ShaderProgram m_shaderProgram;
   private static ShaderProgram m_gridShaderProgram;
   static final int UNITS_PER_CELL = 8;
   static final int SQUARES_PER_CELL;
   static final int SQUARES_PER_UNIT;
   static final int TEXTURE_PAD = 1;
   static final int BIT_VISITED = 1;
   static final int BIT_KNOWN = 2;
   Vector2 m_vector2;

   public WorldMapVisited() {
      this.m_gridColor = (new WorldMapStyleLayer.RGBAf()).init(this.m_color.r * 0.85F, this.m_color.g * 0.85F, this.m_color.b * 0.85F, 1.0F);
      this.m_mainMenu = false;
      this.m_vector2 = new Vector2();
      Arrays.fill(this.m_updateMinX, -1);
      Arrays.fill(this.m_updateMinY, -1);
      Arrays.fill(this.m_updateMaxX, -1);
      Arrays.fill(this.m_updateMaxY, -1);
   }

   public void setBounds(int var1, int var2, int var3, int var4) {
      if (var1 > var3 || var2 > var4) {
         var4 = 0;
         var2 = 0;
         var3 = 0;
         var1 = 0;
         this.m_mainMenu = true;
      }

      this.m_minX = var1;
      this.m_minY = var2;
      this.m_maxX = var3;
      this.m_maxY = var4;
      this.m_changed = true;
      this.m_changeX1 = 0;
      this.m_changeY1 = 0;
      this.m_changeX2 = this.getWidthInCells() * 8 - 1;
      this.m_changeY2 = this.getHeightInCells() * 8 - 1;
      this.m_visited = new byte[this.getWidthInCells() * 8 * this.getHeightInCells() * 8];
      this.m_textureW = this.calcTextureWidth();
      this.m_textureH = this.calcTextureHeight();
      this.m_textureBuffer = BufferUtils.createByteBuffer(this.m_textureW * this.m_textureH * 4);
      this.m_textureBuffer.limit(this.m_textureBuffer.capacity());
      int var5 = SandboxOptions.getInstance().Map.MapAllKnown.getValue() ? 0 : -1;
      byte var6 = -1;
      byte var7 = -1;
      byte var8 = -1;

      for(int var9 = 0; var9 < this.m_textureBuffer.limit(); var9 += 4) {
         this.m_textureBuffer.put(var9, (byte)var5);
         this.m_textureBuffer.put(var9 + 1, var6);
         this.m_textureBuffer.put(var9 + 2, var7);
         this.m_textureBuffer.put(var9 + 3, var8);
      }

      if (!GameServer.bServer || ServerGUI.isCreated()) {
         this.m_textureID = new TextureID(this.m_textureW, this.m_textureH, 0);
      }

   }

   public int getMinX() {
      return this.m_minX;
   }

   public int getMinY() {
      return this.m_minY;
   }

   private int getWidthInCells() {
      return this.m_maxX - this.m_minX + 1;
   }

   private int getHeightInCells() {
      return this.m_maxY - this.m_minY + 1;
   }

   private int calcTextureWidth() {
      return ImageUtils.getNextPowerOfTwo(this.getWidthInCells() * 8 + 2);
   }

   private int calcTextureHeight() {
      return ImageUtils.getNextPowerOfTwo(this.getHeightInCells() * 8 + 2);
   }

   public void setKnownInCells(int var1, int var2, int var3, int var4) {
      this.setFlags(var1 * SQUARES_PER_CELL, var2 * SQUARES_PER_CELL, (var3 + 1) * SQUARES_PER_CELL, (var4 + 1) * SQUARES_PER_CELL, 2);
   }

   public void clearKnownInCells(int var1, int var2, int var3, int var4) {
      this.clearFlags(var1 * SQUARES_PER_CELL, var2 * SQUARES_PER_CELL, (var3 + 1) * SQUARES_PER_CELL, (var4 + 1) * SQUARES_PER_CELL, 2);
   }

   public void setVisitedInCells(int var1, int var2, int var3, int var4) {
      this.setFlags(var1 * SQUARES_PER_CELL, var2 * SQUARES_PER_CELL, var3 * SQUARES_PER_CELL, var4 * SQUARES_PER_CELL, 1);
   }

   public void clearVisitedInCells(int var1, int var2, int var3, int var4) {
      this.clearFlags(var1 * SQUARES_PER_CELL, var2 * SQUARES_PER_CELL, var3 * SQUARES_PER_CELL, var4 * SQUARES_PER_CELL, 1);
   }

   public void setKnownInSquares(int var1, int var2, int var3, int var4) {
      this.setFlags(var1, var2, var3, var4, 2);
   }

   public void clearKnownInSquares(int var1, int var2, int var3, int var4) {
      this.clearFlags(var1, var2, var3, var4, 2);
   }

   public void setVisitedInSquares(int var1, int var2, int var3, int var4) {
      this.setFlags(var1, var2, var3, var4, 1);
   }

   public void clearVisitedInSquares(int var1, int var2, int var3, int var4) {
      this.clearFlags(var1, var2, var3, var4, 1);
   }

   private void updateVisitedTexture() {
      this.m_textureID.bind();
      GL11.glTexImage2D(3553, 0, 6408, this.m_textureW, this.m_textureH, 0, 6408, 5121, this.m_textureBuffer);
   }

   public void renderMain() {
      this.m_textureChanged |= this.updateTextureData(this.m_textureBuffer, this.m_textureW);
   }

   private void initShader() {
      m_shaderProgram = ShaderProgram.createShaderProgram("worldMapVisited", false, false, true);
      if (m_shaderProgram.isCompiled()) {
      }

   }

   public void render(float var1, float var2, int var3, int var4, int var5, int var6, float var7, boolean var8) {
      if (!this.m_mainMenu) {
         GL13.glActiveTexture(33984);
         GL11.glEnable(3553);
         if (this.m_textureChanged) {
            this.m_textureChanged = false;
            this.updateVisitedTexture();
         }

         this.m_textureID.bind();
         int var9 = var8 ? 9729 : 9728;
         GL11.glTexParameteri(3553, 10241, var9);
         GL11.glTexParameteri(3553, 10240, var9);
         GL11.glEnable(3042);
         GL11.glTexEnvi(8960, 8704, 8448);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glColor4f(this.m_color.r, this.m_color.g, this.m_color.b, this.m_color.a);
         if (m_shaderProgram == null) {
            this.initShader();
         }

         if (m_shaderProgram.isCompiled()) {
            m_shaderProgram.Start();
            float var10 = (float)(1 + (var3 - this.m_minX) * 8) / (float)this.m_textureW;
            float var11 = (float)(1 + (var4 - this.m_minY) * 8) / (float)this.m_textureH;
            float var12 = (float)(1 + (var5 + 1 - this.m_minX) * 8) / (float)this.m_textureW;
            float var13 = (float)(1 + (var6 + 1 - this.m_minY) * 8) / (float)this.m_textureH;
            float var14 = (float)((var3 - this.m_minX) * SQUARES_PER_CELL) * var7;
            float var15 = (float)((var4 - this.m_minY) * SQUARES_PER_CELL) * var7;
            float var16 = (float)((var5 + 1 - this.m_minX) * SQUARES_PER_CELL) * var7;
            float var17 = (float)((var6 + 1 - this.m_minY) * SQUARES_PER_CELL) * var7;
            VBORenderer var18 = VBORenderer.getInstance();
            var18.startRun(var18.FORMAT_PositionColorUV);
            var18.setShaderProgram(m_shaderProgram);
            var18.setMode(7);
            var18.setTextureID(this.m_textureID);
            var18.addQuad(var1 + var14, var2 + var15, var10, var11, var1 + var16, var2 + var17, var12, var13, 0.0F, this.m_color.r, this.m_color.g, this.m_color.b, this.m_color.a);
            var18.endRun();
            var18.flush();
            m_shaderProgram.End();
            DefaultShader var10000 = SceneShaderStore.DefaultShader;
            DefaultShader.isActive = false;
            ShaderHelper.forgetCurrentlyBound();
            GL20.glUseProgram(0);
         }
      }
   }

   public void renderGrid(float var1, float var2, int var3, int var4, int var5, int var6, float var7, float var8) {
      if (!(var8 < 11.0F)) {
         if (m_gridShaderProgram == null) {
            m_gridShaderProgram = ShaderProgram.createShaderProgram("worldMapGrid", false, false, true);
         }

         if (m_gridShaderProgram.isCompiled()) {
            float var9 = this.m_gridColor.r;
            float var10 = this.m_gridColor.g;
            float var11 = this.m_gridColor.b;
            float var12 = this.m_gridColor.a;
            byte var13 = 1;
            if (var8 < 13.0F) {
               var13 = 8;
            } else if (var8 < 14.0F) {
               var13 = 4;
            } else if (var8 < 15.0F) {
               var13 = 2;
            }

            VBORenderer var14 = VBORenderer.getInstance();
            var14.startRun(var14.FORMAT_PositionColor);
            var14.setMode(1);
            var14.setLineWidth(0.5F);
            this.renderGridLinesVertical(var1, var2, var3 * 8, var4 * 8, this.m_minX * 8, (var6 + 1) * 8, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesVertical(var1, var2, this.m_minX * 8, var4 * 8, (this.m_maxX + 1) * 8 + var13, this.m_minY * 8, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesVertical(var1, var2, this.m_minX * 8, (this.m_maxY + 1) * 8, (this.m_maxX + 1) * 8 + var13, (var6 + 1) * 8, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesVertical(var1, var2, (this.m_maxX + 1) * 8 + var13, var4 * 8, (var5 + 1) * 8, (var6 + 1) * 8, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesHorizontal(var1, var2, var3 * 8, var4 * 8, (var5 + 1) * 8, this.m_minY * 8, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesHorizontal(var1, var2, var3 * 8, this.m_minY * 8, this.m_minX * 8, (this.m_maxY + 1) * 8 + var13, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesHorizontal(var1, var2, (this.m_maxX + 1) * 8, this.m_minY * 8, (var5 + 1) * 8, (this.m_maxY + 1) * 8 + var13, var7, var13, var9, var10, var11, var12);
            this.renderGridLinesHorizontal(var1, var2, var3 * 8, (this.m_maxY + 1) * 8 + var13, (var5 + 1) * 8, (var6 + 1) * 8, var7, var13, var9, var10, var11, var12);
            var14.endRun();
            var14.flush();
            var10 = this.m_gridColor.g;
            var3 = PZMath.clamp(var3, this.m_minX, this.m_maxX);
            var4 = PZMath.clamp(var4, this.m_minY, this.m_maxY);
            var5 = PZMath.clamp(var5, this.m_minX, this.m_maxX);
            var6 = PZMath.clamp(var6, this.m_minY, this.m_maxY);
            m_gridShaderProgram.Start();
            float var15 = var1 + (float)(var3 * SQUARES_PER_CELL - this.m_minX * SQUARES_PER_CELL) * var7;
            float var16 = var2 + (float)(var4 * SQUARES_PER_CELL - this.m_minY * SQUARES_PER_CELL) * var7;
            float var17 = var15 + (float)((var5 - var3 + 1) * SQUARES_PER_CELL) * var7;
            float var18 = var16 + (float)((var6 - var4 + 1) * SQUARES_PER_CELL) * var7;
            var14.startRun(var14.FORMAT_PositionColorUV);
            var14.setMode(1);
            var14.setLineWidth(0.5F);
            var14.setShaderProgram(m_gridShaderProgram);
            var14.setTextureID(this.m_textureID);
            var14.cmdShader2f("UVOffset", 0.5F / (float)this.m_textureW, 0.0F);

            int var19;
            for(var19 = var3 * 8; var19 <= (var5 + 1) * 8; var19 += var13) {
               var14.reserve(2);
               var14.addElement(var1 + (float)(var19 * SQUARES_PER_UNIT - this.m_minX * SQUARES_PER_CELL) * var7, var16, 0.0F, (float)(1 + var19 - this.m_minX * 8) / (float)this.m_textureW, 1.0F / (float)this.m_textureH, var9, var10, var11, var12);
               var14.addElement(var1 + (float)(var19 * SQUARES_PER_UNIT - this.m_minX * SQUARES_PER_CELL) * var7, var18, 0.0F, (float)(1 + var19 - this.m_minX * 8) / (float)this.m_textureW, (float)(1 + this.getHeightInCells() * 8) / (float)this.m_textureH, var9, var10, var11, var12);
            }

            var14.endRun();
            var14.startRun(var14.FORMAT_PositionColorUV);
            var14.setMode(1);
            var14.setLineWidth(0.5F);
            var14.setShaderProgram(m_gridShaderProgram);
            var14.setTextureID(this.m_textureID);
            var14.cmdShader2f("UVOffset", 0.0F, 0.5F / (float)this.m_textureH);

            for(var19 = var4 * 8; var19 <= (var6 + 1) * 8; var19 += var13) {
               var14.reserve(2);
               var14.addElement(var15, var2 + (float)(var19 * SQUARES_PER_UNIT - this.m_minY * SQUARES_PER_CELL) * var7, 0.0F, 1.0F / (float)this.m_textureW, (float)(1 + var19 - this.m_minY * 8) / (float)this.m_textureH, var9, var10, var11, var12);
               var14.addElement(var17, var2 + (float)(var19 * SQUARES_PER_UNIT - this.m_minY * SQUARES_PER_CELL) * var7, 0.0F, (float)(1 + this.getWidthInCells() * 8) / (float)this.m_textureW, (float)(1 + var19 - this.m_minY * 8) / (float)this.m_textureH, var9, var10, var11, var12);
            }

            var14.endRun();
            var14.flush();
            m_gridShaderProgram.End();
            DefaultShader var10000 = SceneShaderStore.DefaultShader;
            DefaultShader.isActive = false;
            ShaderHelper.forgetCurrentlyBound();
            GL20.glUseProgram(0);
         }
      }
   }

   private void renderGridLinesHorizontal(float var1, float var2, int var3, int var4, int var5, int var6, float var7, int var8, float var9, float var10, float var11, float var12) {
      VBORenderer var13 = VBORenderer.getInstance();

      for(int var14 = var4; var14 < var6; var14 += var8) {
         var13.reserve(2);
         var13.addElement(var1 + (float)(var3 * SQUARES_PER_UNIT) * var7, var2 + (float)(var14 * SQUARES_PER_UNIT - this.m_minY * SQUARES_PER_CELL) * var7, 0.0F, var9, var10, var11, var12);
         var13.addElement(var1 + (float)(var5 * SQUARES_PER_UNIT) * var7, var2 + (float)(var14 * SQUARES_PER_UNIT - this.m_minY * SQUARES_PER_CELL) * var7, 0.0F, var9, var10, var11, var12);
      }

   }

   private void renderGridLinesVertical(float var1, float var2, int var3, int var4, int var5, int var6, float var7, int var8, float var9, float var10, float var11, float var12) {
      VBORenderer var13 = VBORenderer.getInstance();

      for(int var14 = var3; var14 < var5; var14 += var8) {
         var13.reserve(2);
         var13.addElement(var1 + (float)(var14 * SQUARES_PER_UNIT - this.m_minX * SQUARES_PER_CELL) * var7, var2 + (float)(var4 * SQUARES_PER_UNIT) * var7, 0.0F, var9, var10, var11, var12);
         var13.addElement(var1 + (float)(var14 * SQUARES_PER_UNIT - this.m_minX * SQUARES_PER_CELL) * var7, var2 + (float)(var6 * SQUARES_PER_UNIT) * var7, 0.0F, var9, var10, var11, var12);
      }

   }

   private void destroy() {
      if (this.m_textureID != null) {
         TextureID var10000 = this.m_textureID;
         Objects.requireNonNull(var10000);
         RenderThread.invokeOnRenderContext(var10000::destroy);
      }

      this.m_textureBuffer = null;
      this.m_visited = null;
   }

   private void saveHeader(ByteBuffer var1) {
      var1.putInt(219);
      var1.putInt(this.m_minX);
      var1.putInt(this.m_minY);
      var1.putInt(this.m_maxX);
      var1.putInt(this.m_maxY);
      var1.putInt(8);
   }

   public void save() throws IOException {
      File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_visited.bin"));
      FileOutputStream var2 = new FileOutputStream(var1);

      try {
         BufferedOutputStream var3 = new BufferedOutputStream(var2);

         try {
            ByteBuffer var4 = SliceY.SliceBuffer;
            var4.clear();
            this.saveHeader(var4);
            var3.write(var4.array(), 0, var4.position());

            for(int var5 = 0; var5 < this.m_visited.length; var5 += var4.capacity()) {
               var4.clear();
               var4.put(Arrays.copyOfRange(this.m_visited, var5, PZMath.min(var5 + var4.capacity(), this.m_visited.length)));
               var3.write(var4.array(), 0, var4.position());
            }
         } catch (Throwable var8) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         var3.close();
      } catch (Throwable var9) {
         try {
            var2.close();
         } catch (Throwable var6) {
            var9.addSuppressed(var6);
         }

         throw var9;
      }

      var2.close();
   }

   public void load() throws IOException {
      File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_visited.bin"));

      try {
         FileInputStream var2 = new FileInputStream(var1);

         label102: {
            try {
               BufferedInputStream var3 = new BufferedInputStream(var2);

               label82: {
                  try {
                     long var4 = var1.length();
                     ByteBuffer var6 = SliceY.SliceBuffer;
                     var6.clear();
                     byte var7 = 24;
                     var3.read(var6.array(), 0, var7);
                     int var8 = var6.getInt();
                     int var9 = var6.getInt();
                     int var10 = var6.getInt();
                     int var11 = var6.getInt();
                     int var12 = var6.getInt();
                     int var13 = var6.getInt();
                     int var14;
                     if (var9 == this.m_minX && var10 == this.m_minY && var11 == this.m_maxX && var12 == this.m_maxY && var13 == 8) {
                        var14 = 0;

                        while(true) {
                           if (var14 >= this.m_visited.length) {
                              break label82;
                           }

                           var6.clear();
                           int var27 = var3.read(var6.array(), 0, var6.capacity());
                           System.arraycopy(var6.array(), 0, this.m_visited, var14, var27);
                           var14 += var6.capacity();
                        }
                     }

                     var6.clear();
                     var14 = var3.read(var6.array());
                     var6.limit(var14);
                     byte[] var15 = new byte[(var11 - var9 + 1) * var13 * (var12 - var10 + 1) * var13];
                     var6.get(var15);
                     int var16 = SQUARES_PER_CELL / var13;
                     int var17 = (var11 - var9 + 1) * var13;

                     for(int var18 = var10 * var13; var18 <= var12 * var13; ++var18) {
                        int var19 = var18 - var10 * var13;

                        for(int var20 = var9 * var13; var20 <= var11 * var13; ++var20) {
                           int var21 = var20 - var9 * var13;
                           this.setFlags(var20 * var16, var18 * var16, var20 * var16 + var16 - 1, var18 * var16 + var16 - 1, var15[var21 + var19 * var17]);
                        }
                     }
                  } catch (Throwable var24) {
                     try {
                        var3.close();
                     } catch (Throwable var23) {
                        var24.addSuppressed(var23);
                     }

                     throw var24;
                  }

                  var3.close();
                  break label102;
               }

               var3.close();
            } catch (Throwable var25) {
               try {
                  var2.close();
               } catch (Throwable var22) {
                  var25.addSuppressed(var22);
               }

               throw var25;
            }

            var2.close();
            return;
         }

         var2.close();
      } catch (FileNotFoundException var26) {
      }
   }

   private void setFlags(int var1, int var2, int var3, int var4, int var5) {
      var1 -= this.m_minX * SQUARES_PER_CELL;
      var2 -= this.m_minY * SQUARES_PER_CELL;
      var3 -= this.m_minX * SQUARES_PER_CELL;
      var4 -= this.m_minY * SQUARES_PER_CELL;
      int var6 = this.getWidthInCells();
      int var7 = this.getHeightInCells();
      var1 = PZMath.clamp(var1, 0, var6 * SQUARES_PER_CELL - 1);
      var2 = PZMath.clamp(var2, 0, var7 * SQUARES_PER_CELL - 1);
      var3 = PZMath.clamp(var3, 0, var6 * SQUARES_PER_CELL - 1);
      var4 = PZMath.clamp(var4, 0, var7 * SQUARES_PER_CELL - 1);
      if (var1 != var3 && var2 != var4) {
         int var8 = var1 / SQUARES_PER_UNIT;
         int var9 = var3 / SQUARES_PER_UNIT;
         int var10 = var2 / SQUARES_PER_UNIT;
         int var11 = var4 / SQUARES_PER_UNIT;
         if (var3 % SQUARES_PER_UNIT == 0) {
            --var9;
         }

         if (var4 % SQUARES_PER_UNIT == 0) {
            --var11;
         }

         boolean var12 = false;
         int var13 = var6 * 8;

         for(int var14 = var10; var14 <= var11; ++var14) {
            for(int var15 = var8; var15 <= var9; ++var15) {
               byte var16 = this.m_visited[var15 + var14 * var13];
               if ((var16 & var5) != var5) {
                  this.m_visited[var15 + var14 * var13] = (byte)(var16 | var5);
                  var12 = true;
               }
            }
         }

         if (var12) {
            this.m_changed = true;
            this.m_changeX1 = PZMath.min(this.m_changeX1, var8);
            this.m_changeY1 = PZMath.min(this.m_changeY1, var10);
            this.m_changeX2 = PZMath.max(this.m_changeX2, var9);
            this.m_changeY2 = PZMath.max(this.m_changeY2, var11);
         }

      }
   }

   private void clearFlags(int var1, int var2, int var3, int var4, int var5) {
      var1 -= this.m_minX * SQUARES_PER_CELL;
      var2 -= this.m_minY * SQUARES_PER_CELL;
      var3 -= this.m_minX * SQUARES_PER_CELL;
      var4 -= this.m_minY * SQUARES_PER_CELL;
      int var6 = this.getWidthInCells();
      int var7 = this.getHeightInCells();
      var1 = PZMath.clamp(var1, 0, var6 * SQUARES_PER_CELL - 1);
      var2 = PZMath.clamp(var2, 0, var7 * SQUARES_PER_CELL - 1);
      var3 = PZMath.clamp(var3, 0, var6 * SQUARES_PER_CELL - 1);
      var4 = PZMath.clamp(var4, 0, var7 * SQUARES_PER_CELL - 1);
      if (var1 != var3 && var2 != var4) {
         int var8 = var1 / SQUARES_PER_UNIT;
         int var9 = var3 / SQUARES_PER_UNIT;
         int var10 = var2 / SQUARES_PER_UNIT;
         int var11 = var4 / SQUARES_PER_UNIT;
         if (var3 % SQUARES_PER_UNIT == 0) {
            --var9;
         }

         if (var4 % SQUARES_PER_UNIT == 0) {
            --var11;
         }

         boolean var12 = false;
         int var13 = var6 * 8;

         for(int var14 = var10; var14 <= var11; ++var14) {
            for(int var15 = var8; var15 <= var9; ++var15) {
               byte var16 = this.m_visited[var15 + var14 * var13];
               if ((var16 & var5) != 0) {
                  this.m_visited[var15 + var14 * var13] = (byte)(var16 & ~var5);
                  var12 = true;
               }
            }
         }

         if (var12) {
            this.m_changed = true;
            this.m_changeX1 = PZMath.min(this.m_changeX1, var8);
            this.m_changeY1 = PZMath.min(this.m_changeY1, var10);
            this.m_changeX2 = PZMath.max(this.m_changeX2, var9);
            this.m_changeY2 = PZMath.max(this.m_changeY2, var11);
         }

      }
   }

   private boolean updateTextureData(ByteBuffer var1, int var2) {
      if (!this.m_changed) {
         return false;
      } else {
         this.m_changed = false;
         byte var3 = 4;
         int var4 = this.getWidthInCells() * 8;

         for(int var5 = this.m_changeY1; var5 <= this.m_changeY2; ++var5) {
            var1.position((1 + this.m_changeX1) * var3 + (1 + var5) * var2 * var3);

            for(int var6 = this.m_changeX1; var6 <= this.m_changeX2; ++var6) {
               byte var7 = this.m_visited[var6 + var5 * var4];
               var1.put((byte)((var7 & 2) != 0 ? 0 : -1));
               var1.put((byte)((var7 & 1) != 0 ? 0 : -1));
               var1.put((byte)-1);
               var1.put((byte)-1);
            }
         }

         var1.position(0);
         this.m_changeX1 = 2147483647;
         this.m_changeY1 = 2147483647;
         this.m_changeX2 = -2147483648;
         this.m_changeY2 = -2147483648;
         return true;
      }
   }

   void setUnvisitedRGBA(float var1, float var2, float var3, float var4) {
      this.m_color.init(var1, var2, var3, var4);
   }

   void setUnvisitedGridRGBA(float var1, float var2, float var3, float var4) {
      this.m_gridColor.init(var1, var2, var3, var4);
   }

   boolean hasFlags(int var1, int var2, int var3, int var4, int var5, boolean var6) {
      var1 -= this.m_minX * SQUARES_PER_CELL;
      var2 -= this.m_minY * SQUARES_PER_CELL;
      var3 -= this.m_minX * SQUARES_PER_CELL;
      var4 -= this.m_minY * SQUARES_PER_CELL;
      int var7 = this.getWidthInCells();
      int var8 = this.getHeightInCells();
      var1 = PZMath.clamp(var1, 0, var7 * SQUARES_PER_CELL - 1);
      var2 = PZMath.clamp(var2, 0, var8 * SQUARES_PER_CELL - 1);
      var3 = PZMath.clamp(var3, 0, var7 * SQUARES_PER_CELL - 1);
      var4 = PZMath.clamp(var4, 0, var8 * SQUARES_PER_CELL - 1);
      if (var1 != var3 && var2 != var4) {
         int var9 = var1 / SQUARES_PER_UNIT;
         int var10 = var3 / SQUARES_PER_UNIT;
         int var11 = var2 / SQUARES_PER_UNIT;
         int var12 = var4 / SQUARES_PER_UNIT;
         if (var3 % SQUARES_PER_UNIT == 0) {
            --var10;
         }

         if (var4 % SQUARES_PER_UNIT == 0) {
            --var12;
         }

         int var13 = var7 * 8;

         for(int var14 = var11; var14 <= var12; ++var14) {
            for(int var15 = var9; var15 <= var10; ++var15) {
               byte var16 = this.m_visited[var15 + var14 * var13];
               if (var6) {
                  if ((var16 & var5) != 0) {
                     return true;
                  }
               } else if ((var16 & var5) != var5) {
                  return false;
               }
            }
         }

         return !var6;
      } else {
         return false;
      }
   }

   boolean isCellVisible(int var1, int var2) {
      boolean var3 = true;
      int var4 = var3 ? 1 : 0;
      return this.hasFlags(var1 * SQUARES_PER_CELL - var4, var2 * SQUARES_PER_CELL - var4, (var1 + 1) * SQUARES_PER_CELL + var4, (var2 + 1) * SQUARES_PER_CELL + var4, 3, true);
   }

   public boolean isVisited(int var1, int var2) {
      byte var3 = 1;
      return this.hasFlags(var1 - var3, var2 - var3, var1 + var3, var2 + var3, 1, true);
   }

   public boolean isVisited(int var1, int var2, int var3, int var4) {
      byte var5 = 1;
      return this.hasFlags(var1 - var5, var2 - var5, var3 + var5, var4 + var5, 1, true);
   }

   public static WorldMapVisited getInstance() {
      IsoMetaGrid var0 = IsoWorld.instance.getMetaGrid();
      if (var0 == null) {
         throw new NullPointerException("IsoWorld.instance.MetaGrid is null");
      } else {
         if (instance == null) {
            instance = new WorldMapVisited();
            instance.setBounds(var0.minX, var0.minY, var0.maxX, var0.maxY);
            if (instance.m_mainMenu) {
               return instance;
            }

            try {
               instance.load();
               if (SandboxOptions.getInstance().Map.MapAllKnown.getValue()) {
                  instance.setKnownInCells(var0.minX, var0.minY, var0.maxX, var0.maxY);
               }
            } catch (Throwable var2) {
               ExceptionLogger.logException(var2);
            }
         }

         return instance;
      }
   }

   public static void update() {
      if (IsoWorld.instance != null) {
         WorldMapVisited var0 = getInstance();
         if (var0 != null) {
            for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
               IsoPlayer var2 = IsoPlayer.players[var1];
               if (var2 != null && !var2.isDead()) {
                  byte var3 = 25;
                  int var4 = ((int)var2.getX() - var3) / SQUARES_PER_UNIT;
                  int var5 = ((int)var2.getY() - var3) / SQUARES_PER_UNIT;
                  int var6 = ((int)var2.getX() + var3) / SQUARES_PER_UNIT;
                  int var7 = ((int)var2.getY() + var3) / SQUARES_PER_UNIT;
                  if (((int)var2.getX() + var3) % SQUARES_PER_UNIT == 0) {
                     --var6;
                  }

                  if (((int)var2.getY() + var3) % SQUARES_PER_UNIT == 0) {
                     --var7;
                  }

                  if (var4 != var0.m_updateMinX[var1] || var5 != var0.m_updateMinY[var1] || var6 != var0.m_updateMaxX[var1] || var7 != var0.m_updateMaxY[var1]) {
                     var0.m_updateMinX[var1] = var4;
                     var0.m_updateMinY[var1] = var5;
                     var0.m_updateMaxX[var1] = var6;
                     var0.m_updateMaxY[var1] = var7;
                     var0.setFlags((int)var2.getX() - var3, (int)var2.getY() - var3, (int)var2.getX() + var3, (int)var2.getY() + var3, 3);
                  }
               }
            }

         }
      }
   }

   public void forget() {
      this.clearKnownInCells(this.m_minX, this.m_minY, this.m_maxX, this.m_maxY);
      this.clearVisitedInCells(this.m_minX, this.m_minY, this.m_maxX, this.m_maxY);
      Arrays.fill(this.m_updateMinX, -1);
      Arrays.fill(this.m_updateMinY, -1);
      Arrays.fill(this.m_updateMaxX, -1);
      Arrays.fill(this.m_updateMaxY, -1);
   }

   public static void SaveAll() {
      WorldMapVisited var0 = instance;
      if (var0 != null) {
         try {
            var0.save();
         } catch (Exception var2) {
            ExceptionLogger.logException(var2);
         }
      }

   }

   public static void Reset() {
      WorldMapVisited var0 = instance;
      if (var0 != null) {
         var0.destroy();
         instance = null;
      }

   }

   static {
      SQUARES_PER_CELL = IsoCell.CellSizeInSquares;
      SQUARES_PER_UNIT = SQUARES_PER_CELL / 8;
   }
}
