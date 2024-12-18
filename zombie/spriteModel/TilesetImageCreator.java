package zombie.spriteModel;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.iso.IsoMovingObject;
import zombie.iso.SpriteModel;
import zombie.iso.SpriteModelsFile;
import zombie.iso.Vector2;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;

public final class TilesetImageCreator {
   final int TILE_WIDTH = 128;
   final int TILE_HEIGHT = 256;
   TextureFBO m_fbo;
   final Matrix4f PROJECTION = new Matrix4f();
   final Matrix4f MODELVIEW = new Matrix4f();

   public TilesetImageCreator() {
   }

   public void createImage(String var1, String var2, String var3) {
      SpriteModelsFile.Tileset var4 = SpriteModelManager.getInstance().findTileset(var1, var2);
      if (var4 != null) {
         int var5 = this.getTilesetRows(var2);
         Model[] var6 = new Model[8 * var5];
         Texture[] var7 = new Texture[8 * var5];
         FloatBuffer[] var8 = new FloatBuffer[8 * var5];
         this.loadModelsEtc(8, var5, var4, var6, var7, var8);
         this.m_fbo = this.createFBO(1024, var5 * 256);
         RenderThread.invokeOnRenderContext(() -> {
            this.renderTilesetToFBO(var4, 8, var5, var6, var7, var8);
            ((Texture)this.m_fbo.getTexture()).saveOnRenderThread(var3);
         });
         this.m_fbo.destroy();
      }
   }

   int getTilesetRows(String var1) {
      byte var2 = 8;

      for(int var3 = 63; var3 >= 0; --var3) {
         for(int var4 = 0; var4 < var2; ++var4) {
            int var5 = var4 + var3 * var2;
            Texture var6 = Texture.getSharedTexture(var1 + "_" + var5);
            if (var6 != null) {
               return var3 + 1;
            }
         }
      }

      return 0;
   }

   void loadModelsEtc(int var1, int var2, SpriteModelsFile.Tileset var3, Model[] var4, Texture[] var5, FloatBuffer[] var6) {
      for(int var7 = 0; var7 < var2; ++var7) {
         for(int var8 = 0; var8 < var1; ++var8) {
            SpriteModelsFile.Tile var9 = var3.getTile(var8, var7);
            if (var9 == null) {
               var5[var8 + var7 * var1] = Texture.getSharedTexture(String.format("%s_%d", var3.getName(), var8 + var7 * var1));
            } else {
               this.loadModelsEtc(var8, var7, var1, var9.spriteModel, var4, var5, var6);
               if (var4[var8 + var7 * var1] == null) {
                  var5[var8 + var7 * var1] = Texture.getSharedTexture(String.format("%s_%d", var3.getName(), var8 + var7 * var1));
               }
            }
         }
      }

      while(GameWindow.fileSystem.hasWork()) {
         GameWindow.fileSystem.updateAsyncTransactions();
      }

   }

   void loadModelsEtc(int var1, int var2, int var3, SpriteModel var4, Model[] var5, Texture[] var6, FloatBuffer[] var7) {
      ModelScript var8 = ScriptManager.instance.getModelScript(var4.modelScriptName);
      if (var8 != null) {
         String var9 = var8.getMeshName();
         String var10 = var8.getTextureName();
         String var11 = var8.getShaderName();
         boolean var12 = var8.bStatic;
         Model var13 = ModelManager.instance.tryGetLoadedModel(var9, var10, var12, var11, true);
         if (var13 == null && !var12 && var8.animationsMesh != null) {
            AnimationsMesh var14 = ScriptManager.instance.getAnimationsMesh(var8.animationsMesh);
            if (var14 != null && var14.modelMesh != null) {
               var13 = ModelManager.instance.loadModel(var9, var10, var14.modelMesh, var11);
            }
         }

         if (var13 == null) {
            ModelManager.instance.loadAdditionalModel(var9, var10, var12, var11);
            var13 = ModelManager.instance.getLoadedModel(var9, var10, var12, var11);
         }

         if (var13 != null) {
            if (!var13.isFailure()) {
               Texture var16 = null;
               if (var4.getTextureName() != null) {
                  if (var4.getTextureName().contains("media/")) {
                     var16 = Texture.getSharedTexture(var4.getTextureName());
                  } else {
                     var16 = Texture.getSharedTexture("media/textures/" + var4.getTextureName() + ".png");
                  }
               } else if (var13.tex != null) {
                  var16 = var13.tex;
               }

               if (var16 != null && !var16.isFailure()) {
                  int var15 = var1 + var2 * var3;
                  var5[var15] = var13;
                  var6[var15] = var16;
                  var7[var15] = null;
                  if (!var12 && var4.getAnimationName() != null) {
                     var7[var15] = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(var13, var4.getAnimationName(), var4.getAnimationTime());
                  }

               }
            }
         }
      }
   }

   TextureFBO createFBO(int var1, int var2) {
      Texture var3 = new Texture(var1, var2, 16);
      TextureFBO var4 = new TextureFBO(var3, false);
      return var4;
   }

   void renderTilesetToFBO(SpriteModelsFile.Tileset var1, int var2, int var3, Model[] var4, Texture[] var5, FloatBuffer[] var6) {
      GL11.glPushAttrib(1048575);
      GL11.glPushClientAttrib(-1);
      GL11.glDepthMask(true);
      GL11.glColorMask(true, true, true, true);
      GL11.glDisable(3089);
      this.m_fbo.startDrawing(true, true);

      for(int var7 = 0; var7 < var3; ++var7) {
         for(int var8 = 0; var8 < var2; ++var8) {
            int var9 = var8 + var7 * var2;
            SpriteModelsFile.Tile var10 = var1.getTile(var8, var7);
            if (var10 == null) {
               this.renderTileToFBO(var8, var7, var5[var9]);
            } else {
               this.renderTileToFBO(var8, var7, var10.spriteModel, var4[var9], var5[var9], var6[var9]);
            }
         }
      }

      this.m_fbo.endDrawing();
      Texture.lastTextureID = -1;
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      SpriteRenderer.ringBuffer.restoreVBOs = true;
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
   }

   void renderTileToFBO(int var1, int var2, Texture var3) {
      if (var3 != null) {
         GL11.glViewport(0, 0, this.m_fbo.getWidth(), this.m_fbo.getHeight());
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         this.PROJECTION.setOrtho2D(0.0F, (float)this.m_fbo.getWidth(), 0.0F, (float)this.m_fbo.getHeight());
         this.MODELVIEW.identity();
         VBORenderer var4 = VBORenderer.getInstance();
         var4.cmdPushAndLoadMatrix(5889, this.PROJECTION);
         var4.cmdPushAndLoadMatrix(5888, this.MODELVIEW);
         var4.startRun(var4.FORMAT_PositionColorUV);
         var4.setMode(7);
         var4.setTextureID(var3.getTextureId());
         int var5 = var1 * 128;
         int var6 = var2 * 256;
         var4.addQuad((float)var5 + var3.offsetX, (float)var6 + var3.offsetY, var3.xStart, var3.yStart, (float)var5 + var3.offsetX + (float)var3.getWidth(), (float)var6 + var3.offsetY + (float)var3.getHeight(), var3.xEnd, var3.yEnd, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
         var4.endRun();
         var4.cmdPopMatrix(5889);
         var4.cmdPopMatrix(5888);
         var4.flush();
      }
   }

   void renderTileToFBO(int var1, int var2, SpriteModel var3, Model var4, Texture var5, FloatBuffer var6) {
      if (var4.bStatic || var6 != null) {
         if (var4.isReady()) {
            if (var5.isReady()) {
               Shader var7 = var4.Effect;
               if (var7 != null && var4.Mesh != null && var4.Mesh.isReady()) {
                  int var8 = var1 * 128 - (this.m_fbo.getWidth() - 128) / 2;
                  int var9 = var2 * 256 - (this.m_fbo.getHeight() - 256) / 2;
                  var9 += 96;
                  GL11.glViewport(var8, var9, this.m_fbo.getWidth(), this.m_fbo.getHeight());
                  this.calcMatrices(this.PROJECTION, this.MODELVIEW, var3, var4);
                  PZGLUtil.pushAndLoadMatrix(5889, this.PROJECTION);
                  PZGLUtil.pushAndLoadMatrix(5888, this.MODELVIEW);
                  IndieGL.glDefaultBlendFuncA();
                  GL11.glDepthFunc(513);
                  GL11.glDepthMask(true);
                  GL11.glDepthRange(0.0, 1.0);
                  GL11.glEnable(2929);
                  if (var7.getShaderProgram().getName().contains("door")) {
                     GL11.glEnable(2884);
                     GL11.glCullFace(1029);
                  } else {
                     GL11.glDisable(2884);
                  }

                  var7.Start();
                  int var10;
                  if (var5 == null) {
                     var7.setTexture(Texture.getErrorTexture(), "Texture", 0);
                  } else {
                     if (!var5.getTextureId().hasMipMaps()) {
                        GL11.glBlendFunc(770, 771);
                     }

                     var7.setTexture(var5, "Texture", 0);
                     if (var7.getShaderProgram().getName().equalsIgnoreCase("door")) {
                        var10 = var5.getWidthHW();
                        int var11 = var5.getHeightHW();
                        float var12 = var5.xStart * (float)var10 - var5.offsetX;
                        float var13 = var5.yStart * (float)var11 - var5.offsetY;
                        float var14 = var12 + (float)var5.getWidthOrig();
                        float var15 = var13 + (float)var5.getHeightOrig();
                        Vector2 var16 = new Vector2();
                        var7.getShaderProgram().setValue("UVOffset", var16.set(var12 / (float)var10, var13 / (float)var11));
                        var7.getShaderProgram().setValue("UVScale", var16.set((var14 - var12) / (float)var10, (var15 - var13) / (float)var11));
                     }
                  }

                  var7.setDepthBias(0.0F);
                  var7.setTargetDepth(0.5F);
                  var7.setAmbient(1.0F, 1.0F, 1.0F);
                  var7.setLightingAmount(1.0F);
                  var7.setHueShift(1.0F);
                  var7.setTint(1.0F, 1.0F, 1.0F);
                  var7.setAlpha(1.0F);

                  for(var10 = 0; var10 < 5; ++var10) {
                     var7.setLight(var10, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
                  }

                  if (var4.bStatic) {
                     var7.setTransformMatrix(new Matrix4f(), false);
                  } else {
                     var6.position(0);
                     var7.setMatrixPalette(var6, true);
                  }

                  var4.Mesh.Draw(var7);
                  var7.End();
                  PZGLUtil.popMatrix(5889);
                  PZGLUtil.popMatrix(5888);
               }
            }
         }
      }
   }

   void calcMatrices(Matrix4f var1, Matrix4f var2, SpriteModel var3, Model var4) {
      double var5 = (double)((float)this.m_fbo.getWidth() / 1920.0F);
      double var7 = (double)((float)this.m_fbo.getHeight() / 1920.0F);
      var1.setOrtho(-((float)var5) / 2.0F, (float)var5 / 2.0F, (float)var7 / 2.0F, (float)(-var7) / 2.0F, -1.0F, 1.0F);
      var2.identity();
      var2.scale(Core.scale * (float)Core.TileScale / 2.0F);
      var2.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
      var2.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
      var2.rotateY(3.1415927F);
      var2.scale(-1.5F, 1.5F, 1.5F);
      var2.translate(-var3.translate.x / 1.5F, var3.translate.y / 1.5F, var3.translate.z / 1.5F);
      var2.rotateXYZ(var3.rotate.x * 0.017453292F, -var3.rotate.y * 0.017453292F, var3.rotate.z * 0.017453292F);
      var2.scale(var3.scale);
      ModelScript var9 = ScriptManager.instance.getModelScript(var3.modelScriptName);
      var2.scale(var9.scale);
      ModelInstanceRenderData.postMultiplyMeshTransform(var2, var4.Mesh);
   }
}
