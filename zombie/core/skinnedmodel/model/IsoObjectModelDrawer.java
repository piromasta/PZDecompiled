package zombie.core.skinnedmodel.model;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjglx.BufferUtils;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.IModelCamera;
import zombie.core.opengl.PZGLUtil;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.vehicles.BaseVehicle;

public final class IsoObjectModelDrawer extends TextureDraw.GenericDrawer {
   private static final ObjectPool<IsoObjectModelDrawer> s_modelDrawerPool = new ObjectPool(IsoObjectModelDrawer::new);
   private static final Vector3f tempVector3f = new Vector3f(0.0F, 5.0F, -2.0F);
   private static final Vector2 tempVector2_1 = new Vector2();
   private SpriteModel m_spriteModel;
   private Model m_model;
   private ModelScript m_modelScript;
   private Texture m_texture;
   private AnimationPlayer m_animationPlayer;
   private FloatBuffer m_matrixPalette;
   private float m_hue;
   private float m_tintR;
   private float m_tintG;
   private float m_tintB;
   private float m_x;
   private float m_y;
   private float m_z;
   private final Vector3f m_angle = new Vector3f();
   private final Matrix4f m_transform = new Matrix4f();
   private float m_ambientR;
   private float m_ambientG;
   private float m_ambientB;
   private float alpha = 1.0F;
   private boolean bRenderToChunkTexture = false;
   private float squareDepth = 0.0F;
   private boolean bOutline = false;
   private final ColorInfo outlineColor = new ColorInfo();
   private boolean bOutlineBehindPlayer = true;
   static final WorldModelCamera s_worldModelCamera = new WorldModelCamera();

   public IsoObjectModelDrawer() {
   }

   public static RenderStatus renderMain(SpriteModel var0, float var1, float var2, float var3, ColorInfo var4, float var5) {
      return renderMain(var0, var1, var2, var3, var4, var5, false);
   }

   public static RenderStatus renderMainOutline(SpriteModel var0, float var1, float var2, float var3, ColorInfo var4, float var5) {
      return renderMain(var0, var1, var2, var3, var4, var5, true);
   }

   public static RenderStatus renderMain(SpriteModel var0, float var1, float var2, float var3, ColorInfo var4, float var5, boolean var6) {
      ModelScript var7 = ScriptManager.instance.getModelScript(var0.modelScriptName);
      if (var7 == null) {
         return IsoObjectModelDrawer.RenderStatus.NoModel;
      } else {
         String var8 = var7.getMeshName();
         String var9 = var7.getTextureName();
         String var10 = var7.getShaderName();
         ImmutableColor var11 = ImmutableColor.white;
         float var12 = 1.0F;
         boolean var13 = var7.bStatic;
         Model var14 = ModelManager.instance.tryGetLoadedModel(var8, var9, var13, var10, true);
         if (var14 == null && !var13 && var7.animationsMesh != null) {
            AnimationsMesh var15 = ScriptManager.instance.getAnimationsMesh(var7.animationsMesh);
            if (var15 != null && var15.modelMesh != null) {
               var14 = ModelManager.instance.loadModel(var8, var9, var15.modelMesh, var10);
            }
         }

         if (var14 == null) {
            ModelManager.instance.loadAdditionalModel(var8, var9, var13, var10);
            var14 = ModelManager.instance.getLoadedModel(var8, var9, var13, var10);
         }

         if (var14 == null) {
            return IsoObjectModelDrawer.RenderStatus.NoModel;
         } else if (var14.isFailure()) {
            return IsoObjectModelDrawer.RenderStatus.Failed;
         } else if (var14.isReady() && var14.Mesh != null && var14.Mesh.isReady()) {
            IsoObjectModelDrawer var24 = (IsoObjectModelDrawer)s_modelDrawerPool.alloc();
            var24.m_spriteModel = var0;
            var24.m_model = var14;
            var24.m_modelScript = var7;
            var24.m_texture = null;
            RenderStatus var16;
            if (var0.getTextureName() != null) {
               if (var0.getTextureName().contains("media/")) {
                  var24.m_texture = Texture.getSharedTexture(var0.getTextureName());
               } else {
                  var24.m_texture = Texture.getSharedTexture("media/textures/" + var0.getTextureName() + ".png");
               }

               var16 = checkTextureStatus(var24.m_texture);
               if (var16 != IsoObjectModelDrawer.RenderStatus.Ready) {
                  s_modelDrawerPool.release((Object)var24);
                  return var16;
               }
            } else if (var14.tex != null) {
               var16 = checkTextureStatus(var14.tex);
               if (var16 != IsoObjectModelDrawer.RenderStatus.Ready) {
                  s_modelDrawerPool.release((Object)var24);
                  return var16;
               }
            }

            var24.m_animationPlayer = null;
            var24.initMatrixPalette();
            var24.m_x = var1;
            var24.m_y = var2;
            var24.m_z = var3;
            var24.m_tintR = var11.r;
            var24.m_tintG = var11.g;
            var24.m_tintB = var11.b;
            var24.m_hue = var12;
            var24.m_transform.identity();
            float var25 = var5 / 96.0F * 2.44949F;
            float var17 = (float)((double)System.currentTimeMillis() % 1500.0 / 1500.0 * 1.0);
            var17 = 0.0F;
            var24.m_transform.translate(-var0.translate.x / 1.5F, (var0.translate.y + var25) / 1.5F, var0.translate.z / 1.5F);
            var24.m_transform.rotateXYZ(var0.rotate.x * 0.017453292F, -var0.rotate.y * 0.017453292F, var0.rotate.z * 0.017453292F);
            if (var7.scale != 1.0F) {
               var24.m_transform.scale(var7.scale);
            }

            if (var0.scale != 1.0F) {
               var24.m_transform.scale(var0.scale);
            }

            var24.m_angle.set(0.0F);
            ModelInstanceRenderData.postMultiplyMeshTransform(var24.m_transform, var14.Mesh);
            var24.m_ambientR = var4.r;
            var24.m_ambientG = var4.g;
            var24.m_ambientB = var4.b;
            if (Core.bDebug) {
            }

            IsoGridSquare var18 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1, (double)var2, (double)var3);
            var24.alpha = var18 == null ? 1.0F : IsoWorldInventoryObject.getSurfaceAlpha(var18, 0.02F, false);
            var24.alpha *= var4.a;
            if (DebugOptions.instance.FBORenderChunk.ForceAlphaAndTargetOne.getValue()) {
               var24.alpha = 1.0F;
            }

            float var19 = var1;
            float var20 = var2;
            float var21 = var3;
            if (!var7.bStatic) {
               var19 = var1 + var0.translate.x;
               var20 = var2 + var0.translate.z + var17;
               var21 = var3 + var0.translate.y / 2.44949F;
            }

            PlayerCamera var22 = IsoCamera.cameras[IsoCamera.frameState.playerIndex];
            if (PerformanceSettings.FBORenderChunk && !FBORenderChunkManager.instance.isCaching()) {
               var19 += var22.fixJigglyModelsSquareX;
               var20 += var22.fixJigglyModelsSquareY;
            }

            var24.bRenderToChunkTexture = !var6 && PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching();
            IsoDepthHelper.Results var23 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var19, var20, var21);
            if (var24.bRenderToChunkTexture) {
               var23.depthStart -= IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / 8.0F), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / 8.0F), PZMath.fastfloor(var1 / 8.0F), PZMath.fastfloor(var2 / 8.0F), PZMath.fastfloor(var3)).depthStart;
            }

            var24.squareDepth = var23.depthStart;
            if (FBORenderObjectHighlight.getInstance().isRendering()) {
               var24.squareDepth -= 5.0E-5F;
            }

            var24.bOutline = var6;
            var24.outlineColor.set(var4);
            var24.bOutlineBehindPlayer = var1 + var2 < IsoCamera.frameState.CamCharacterX + IsoCamera.frameState.CamCharacterY;
            SpriteRenderer.instance.drawGeneric(var24);
            return IsoObjectModelDrawer.RenderStatus.Ready;
         } else {
            return IsoObjectModelDrawer.RenderStatus.Loading;
         }
      }
   }

   public static RenderStatus renderMain(SpriteModel var0, float var1, float var2, float var3, ColorInfo var4, float var5, AnimationPlayer var6) {
      ModelScript var7 = ScriptManager.instance.getModelScript(var0.modelScriptName);
      if (var7 == null) {
         return IsoObjectModelDrawer.RenderStatus.NoModel;
      } else {
         ImmutableColor var8 = ImmutableColor.white;
         float var9 = 1.0F;
         Model var10 = var6.getModel();
         if (var10 == null) {
            return IsoObjectModelDrawer.RenderStatus.NoModel;
         } else if (var10.isFailure()) {
            return IsoObjectModelDrawer.RenderStatus.Failed;
         } else if (var10.isReady() && var10.Mesh != null && var10.Mesh.isReady()) {
            IsoObjectModelDrawer var11 = (IsoObjectModelDrawer)s_modelDrawerPool.alloc();
            var11.m_model = var10;
            var11.m_modelScript = var7;
            var11.m_texture = null;
            RenderStatus var12;
            if (var0.getTextureName() != null) {
               if (var0.getTextureName().contains("media/")) {
                  var11.m_texture = Texture.getSharedTexture(var0.getTextureName());
               } else {
                  var11.m_texture = Texture.getSharedTexture("media/textures/" + var0.getTextureName() + ".png");
               }

               var12 = checkTextureStatus(var11.m_texture);
               if (var12 != IsoObjectModelDrawer.RenderStatus.Ready) {
                  s_modelDrawerPool.release((Object)var11);
                  return var12;
               }
            } else if (var10.tex != null) {
               var12 = checkTextureStatus(var10.tex);
               if (var12 != IsoObjectModelDrawer.RenderStatus.Ready) {
                  s_modelDrawerPool.release((Object)var11);
                  return var12;
               }
            }

            var11.m_animationPlayer = var6;
            var11.initMatrixPalette();
            var11.m_x = var1;
            var11.m_y = var2;
            var11.m_z = var3;
            var11.m_tintR = var8.r;
            var11.m_tintG = var8.g;
            var11.m_tintB = var8.b;
            var11.m_hue = var9;
            var11.m_transform.identity();
            float var18 = var5 / 96.0F * 2.44949F;
            var11.m_transform.translate(-var0.translate.x / 1.5F, (var0.translate.y + var18) / 1.5F, var0.translate.z / 1.5F);
            var11.m_transform.rotateXYZ(var0.rotate.x * 0.017453292F, -var0.rotate.y * 0.017453292F, var0.rotate.z * 0.017453292F);
            if (var7.scale != 1.0F) {
               var11.m_transform.scale(var7.scale);
            }

            if (var0.scale != 1.0F) {
               var11.m_transform.scale(var0.scale);
            }

            var11.m_angle.set(0.0F);
            ModelAttachment var13 = var7.getAttachmentById("DoorPivot");
            if (var13 != null) {
            }

            ModelInstanceRenderData.postMultiplyMeshTransform(var11.m_transform, var10.Mesh);
            var11.m_ambientR = var4.r;
            var11.m_ambientG = var4.g;
            var11.m_ambientB = var4.b;
            if (Core.bDebug) {
            }

            IsoGridSquare var19 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1, (double)var2, (double)var3);
            var11.alpha = var19 == null ? 1.0F : IsoWorldInventoryObject.getSurfaceAlpha(var19, 0.02F, false);
            var11.alpha *= var4.a;
            if (DebugOptions.instance.FBORenderChunk.ForceAlphaAndTargetOne.getValue()) {
               var11.alpha = 1.0F;
            }

            var11.bRenderToChunkTexture = PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching();
            float var14 = var1;
            float var15 = var2;
            float var16 = var3;
            if (!var7.bStatic) {
               var14 = var1 + var0.translate.x;
               var15 = var2 + var0.translate.z;
               var16 = var3 + var0.translate.y / 2.44949F;
            }

            IsoDepthHelper.Results var17 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var14, var15, var16);
            if (var11.bRenderToChunkTexture) {
               var17.depthStart -= IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX / 8.0F), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY / 8.0F), PZMath.fastfloor(var1 / 8.0F), PZMath.fastfloor(var2 / 8.0F), PZMath.fastfloor(var3)).depthStart;
            }

            var11.squareDepth = var17.depthStart;
            var11.bOutline = false;
            SpriteRenderer.instance.drawGeneric(var11);
            return IsoObjectModelDrawer.RenderStatus.Ready;
         } else {
            return IsoObjectModelDrawer.RenderStatus.Loading;
         }
      }
   }

   private static RenderStatus checkTextureStatus(Texture var0) {
      if (var0 != null && !var0.isFailure()) {
         return !var0.isReady() ? IsoObjectModelDrawer.RenderStatus.Loading : IsoObjectModelDrawer.RenderStatus.Ready;
      } else {
         return IsoObjectModelDrawer.RenderStatus.Failed;
      }
   }

   private void initMatrixPalette() {
      SkinningData var1 = (SkinningData)this.m_model.Tag;
      if (var1 == null) {
         this.m_matrixPalette = null;
      } else if (this.m_animationPlayer == null) {
         if (!this.m_model.bStatic && this.m_spriteModel.getAnimationName() != null) {
            FloatBuffer var4 = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(this.m_model, this.m_spriteModel.getAnimationName(), this.m_spriteModel.getAnimationTime());
            if (var4 == null) {
               return;
            }

            var4.position(0);
            if (this.m_matrixPalette == null || this.m_matrixPalette.capacity() < var4.capacity()) {
               this.m_matrixPalette = BufferUtils.createFloatBuffer(var4.capacity());
            }

            this.m_matrixPalette.clear();
            this.m_matrixPalette.put(var4);
            this.m_matrixPalette.flip();
         } else {
            this.m_matrixPalette = null;
         }

      } else {
         org.lwjgl.util.vector.Matrix4f[] var2 = this.m_animationPlayer.getSkinTransforms(var1);
         if (this.m_matrixPalette == null || this.m_matrixPalette.capacity() < var2.length * 16) {
            this.m_matrixPalette = BufferUtils.createFloatBuffer(var2.length * 16);
         }

         this.m_matrixPalette.clear();

         for(int var3 = 0; var3 < var2.length; ++var3) {
            var2[var3].store(this.m_matrixPalette);
         }

         this.m_matrixPalette.flip();
      }
   }

   public void render() {
      FBORenderChunk var1 = FBORenderChunkManager.instance.renderThreadCurrent;
      if (PerformanceSettings.FBORenderChunk && var1 != null) {
         Vector3f var10000 = this.m_angle;
         var10000.y += 180.0F;
         FBORenderItems.getInstance().setCamera(var1, this.m_x, this.m_y, this.m_z, this.m_angle);
         var10000 = this.m_angle;
         var10000.y -= 180.0F;
         this.renderToChunkTexture(FBORenderItems.getInstance().getCamera(), var1.bHighRes);
      } else {
         this.renderToWorld();
      }
   }

   public void postRender() {
      s_modelDrawerPool.release((Object)this);
   }

   private void renderToChunkTexture(IModelCamera var1, boolean var2) {
      GL11.glPushAttrib(1048575);
      GL11.glPushClientAttrib(-1);
      var1.Begin();
      this.renderModel(var2);
      var1.End();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      SpriteRenderer.ringBuffer.restoreVBOs = true;
   }

   private void renderToWorld() {
      GL11.glPushAttrib(1048575);
      GL11.glPushClientAttrib(-1);
      if (this.bOutline) {
         boolean var1 = ModelOutlines.instance.beginRenderOutline(this.outlineColor, this.bOutlineBehindPlayer, false);
         GL11.glDepthMask(true);
         ModelOutlines.instance.m_fboA.startDrawing(var1, true);
         if (ModelSlotRenderData.solidColor == null) {
            ModelSlotRenderData.solidColor = new Shader("aim_outline_solid", false, false);
            ModelSlotRenderData.solidColorStatic = new Shader("aim_outline_solid", true, false);
         }

         ModelSlotRenderData.solidColor.Start();
         ModelSlotRenderData.solidColor.getShaderProgram().setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, 1.0F);
         ModelSlotRenderData.solidColor.End();
         ModelSlotRenderData.solidColorStatic.Start();
         ModelSlotRenderData.solidColorStatic.getShaderProgram().setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, 1.0F);
         ModelSlotRenderData.solidColorStatic.End();
      }

      s_worldModelCamera.m_x = this.m_x;
      s_worldModelCamera.m_y = this.m_y;
      s_worldModelCamera.m_z = this.m_z;
      s_worldModelCamera.Begin();
      this.renderModel(false);
      s_worldModelCamera.End();
      if (this.bOutline) {
         ModelOutlines.instance.m_fboA.endDrawing();
      }

      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      SpriteRenderer.ringBuffer.restoreVBOs = true;
   }

   private void renderModel(boolean var1) {
      Model var2 = this.m_model;
      if (var2.Effect == null) {
         var2.CreateShader("basicEffect");
      }

      Shader var3 = var2.Effect;
      if (this.bOutline) {
         var3 = var2.bStatic ? ModelSlotRenderData.solidColorStatic : ModelSlotRenderData.solidColor;
      }

      if (var3 != null && var2.Mesh != null && var2.Mesh.isReady()) {
         IndieGL.glDefaultBlendFuncA();
         GL11.glDepthFunc(513);
         GL11.glDepthMask(true);
         GL11.glDepthRange(0.0, 1.0);
         GL11.glEnable(2929);
         if (var3.getShaderProgram().getName().contains("door") && FBORenderChunkManager.instance.renderThreadCurrent == null) {
            GL11.glEnable(2884);
            GL11.glCullFace(1028);
         } else if (var3.getShaderProgram().getName().contains("door") && FBORenderChunkManager.instance.renderThreadCurrent != null) {
            GL11.glEnable(2884);
            GL11.glCullFace(1029);
         }

         GL11.glColor3f(1.0F, 1.0F, 1.0F);
         boolean var4 = false;
         if (!this.m_model.bStatic) {
            PZGLUtil.pushAndMultMatrix(5888, this.m_transform);
            var4 = true;
            if (this.m_modelScript.meshName.contains("door1")) {
            }
         }

         var3.Start();
         Texture var5 = var2.tex;
         if (this.m_texture != null) {
            var5 = this.m_texture;
         }

         int var6;
         int var7;
         if (var5 != null) {
            if (!var5.getTextureId().hasMipMaps()) {
               GL11.glBlendFunc(770, 771);
            }

            var3.setTexture(var5, "Texture", 0);
            if (var3.getShaderProgram().getName().equalsIgnoreCase("door")) {
               var6 = var5.getWidthHW();
               var7 = var5.getHeightHW();
               float var8 = var5.xStart * (float)var6 - var5.offsetX;
               float var9 = var5.yStart * (float)var7 - var5.offsetY;
               float var10 = var8 + (float)var5.getWidthOrig();
               float var11 = var9 + (float)var5.getHeightOrig();
               var3.getShaderProgram().setValue("UVOffset", tempVector2_1.set(var8 / (float)var6, var9 / (float)var7));
               var3.getShaderProgram().setValue("UVScale", tempVector2_1.set((var10 - var8) / (float)var6, (var11 - var9) / (float)var7));
            }
         }

         var3.setDepthBias(0.0F);
         float var12 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
         float var13 = this.squareDepth - (var12 + 1.0F) / 2.0F + 0.5F;
         if (!PerformanceSettings.FBORenderChunk) {
            var13 = 0.5F;
         }

         var3.setTargetDepth(var13);
         if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
            var3.setAmbient(1.0F, 1.0F, 1.0F);
         } else {
            var3.setAmbient(this.m_ambientR, this.m_ambientG, this.m_ambientB);
         }

         var3.setLightingAmount(1.0F);
         var3.setHueShift(this.m_hue);
         var3.setTint(this.m_tintR, this.m_tintG, this.m_tintB);
         var3.setAlpha(DebugOptions.instance.Model.Render.ForceAlphaOne.getValue() ? 1.0F : this.alpha);

         for(var6 = 0; var6 < 5; ++var6) {
            var3.setLight(var6, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
         }

         if (!DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
            Vector3f var14 = tempVector3f;
            var14.x = 0.0F;
            var14.y = 5.0F;
            var14.z = -2.0F;
            var14.rotateY(this.m_angle.y * 0.017453292F);
            var13 = 1.5F;
            var3.setLight(4, var14.x, var14.z, var14.y, this.m_ambientR / 4.0F * var13, this.m_ambientG / 4.0F * var13, this.m_ambientB / 4.0F * var13, 5000.0F, 0.0F / 0.0F, 0.0F, 0.0F, 0.0F, (IsoMovingObject)null);
         }

         if (var2.bStatic) {
            var3.setTransformMatrix(this.m_transform, false);
         } else if (this.m_matrixPalette != null) {
            var3.setMatrixPalette(this.m_matrixPalette, true);
         }

         if (this.bRenderToChunkTexture && var1) {
            var3.setHighResDepthMultiplier(0.5F);
         }

         var2.Mesh.Draw(var3);
         if (this.bRenderToChunkTexture && var1) {
            var3.setHighResDepthMultiplier(0.0F);
         }

         var3.End();
         if (var4) {
            PZGLUtil.popMatrix(5888);
         }

         Matrix4f var15 = BaseVehicle.allocMatrix4f();
         var15.set(this.m_transform);
         var15.mul(this.m_model.Mesh.m_transform);
         var15.scale(this.m_modelScript.scale);
         if (DebugOptions.instance.Model.Render.Attachments.getValue()) {
            for(var7 = 0; var7 < this.m_modelScript.getAttachmentCount(); ++var7) {
               ModelAttachment var16 = this.m_modelScript.getAttachment(var7);
               Matrix4f var17 = BaseVehicle.allocMatrix4f();
               ModelInstanceRenderData.makeAttachmentTransform(var16, var17);
               var15.mul(var17, var17);
               PZGLUtil.pushAndMultMatrix(5888, var17);
               BaseVehicle.releaseMatrix4f(var17);
               Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 1.0F);
               PZGLUtil.popMatrix(5888);
            }
         }

         if (Core.bDebug && DebugOptions.instance.Model.Render.Axis.getValue()) {
            PZGLUtil.pushAndMultMatrix(5888, var15);
            Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
            PZGLUtil.popMatrix(5888);
         }

         BaseVehicle.releaseMatrix4f(var15);
      }
   }

   public static enum RenderStatus {
      NoModel,
      Failed,
      Loading,
      Ready;

      private RenderStatus() {
      }
   }

   private static final class WorldModelCamera implements IModelCamera {
      float m_x;
      float m_y;
      float m_z;

      private WorldModelCamera() {
      }

      public void Begin() {
         float var1 = (Float)Core.getInstance().FloatParamMap.get(0);
         float var2 = (Float)Core.getInstance().FloatParamMap.get(1);
         float var3 = (Float)Core.getInstance().FloatParamMap.get(2);
         double var4 = (double)var1;
         double var6 = (double)var2;
         double var8 = (double)var3;
         SpriteRenderState var10 = SpriteRenderer.instance.getRenderingState();
         int var11 = var10.playerIndex;
         PlayerCamera var12 = var10.playerCamera[var11];
         float var13 = var12.RightClickX;
         float var14 = var12.RightClickY;
         float var15 = var12.DeferedX;
         float var16 = var12.DeferedY;
         float var17 = (var13 + 2.0F * var14) / (64.0F * (float)Core.TileScale);
         float var18 = (var13 - 2.0F * var14) / (-64.0F * (float)Core.TileScale);
         var4 += (double)var17;
         var6 += (double)var18;
         var4 += (double)var15;
         var6 += (double)var16;
         double var19 = (double)((float)var12.OffscreenWidth / 1920.0F);
         double var21 = (double)((float)var12.OffscreenHeight / 1920.0F);
         Matrix4f var23 = Core.getInstance().projectionMatrixStack.alloc();
         var23.setOrtho(-((float)var19) / 2.0F, (float)var19 / 2.0F, -((float)var21) / 2.0F, (float)var21 / 2.0F, -10.0F, 10.0F);
         Core.getInstance().projectionMatrixStack.push(var23);
         Matrix4f var24 = Core.getInstance().modelViewMatrixStack.alloc();
         float var25 = (float)(2.0 / Math.sqrt(2048.0));
         var24.scaling(Core.scale);
         var24.scale((float)Core.TileScale / 2.0F);
         var24.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var24.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         double var26 = (double)this.m_x - var4;
         double var28 = (double)this.m_y - var6;
         var24.translate(-((float)var26), (float)((double)this.m_z - var8) * 2.44949F, -((float)var28));
         var24.scale(-1.5F, 1.5F, 1.5F);
         var24.rotate(3.1415927F, 0.0F, 1.0F, 0.0F);
         var24.translate(0.0F, -0.48F, 0.0F);
         Core.getInstance().modelViewMatrixStack.push(var24);
      }

      public void End() {
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
      }
   }
}
