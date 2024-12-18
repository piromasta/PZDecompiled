package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import zombie.GameProfiler;
import zombie.ai.states.PlayerGetUpState;
import zombie.characters.Imposter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.ShaderProgram;
import zombie.core.rendering.RenderTarget;
import zombie.core.rendering.RenderTexture;
import zombie.core.rendering.ShaderPropertyBlock;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector3;
import zombie.iso.fboRenderChunk.FBORenderTracerEffects;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.seating.SeatingManager;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelSlotRenderData extends TextureDraw.GenericDrawer {
   private int playerIndex;
   private float CamCharacterX;
   private float CamCharacterY;
   private float CamCharacterZ;
   private float fixJigglyModelsSquareX;
   private float fixJigglyModelsSquareY;
   public IsoGameCharacter character;
   public IsoMovingObject object;
   public ModelManager.ModelSlot modelSlot;
   public final ModelInstanceRenderDataList modelData = new ModelInstanceRenderDataList();
   private final ModelInstanceRenderDataList readyModelData = new ModelInstanceRenderDataList();
   public ModelInstanceTextureCreator textureCreator;
   public AnimationPlayer animPlayer;
   public float animPlayerAngle;
   public float x;
   public float y;
   public float z;
   public float ambientR;
   public float ambientG;
   public float ambientB;
   public boolean bOutside;
   public float FinalScale = 1.0F;
   public final Matrix4f vehicleTransform = new Matrix4f();
   public boolean bInVehicle;
   public float inVehicleX;
   public float inVehicleY;
   public float inVehicleZ;
   public float vehicleAngleX;
   public float vehicleAngleY;
   public float vehicleAngleZ;
   public float alpha;
   private boolean bRendered;
   private boolean bReady;
   public final ModelInstance.EffectLight[] effectLights = new ModelInstance.EffectLight[5];
   public float centerOfMassY;
   public boolean RENDER_TO_TEXTURE;
   public static Shader solidColor;
   public static Shader solidColorStatic;
   private boolean bCharacterOutline = false;
   private final ColorInfo outlineColor = new ColorInfo(1.0F, 0.0F, 0.0F, 1.0F);
   private boolean bOutlineBehindPlayer = true;
   public float squareDepth;
   public final ShaderPropertyBlock properties = new ShaderPropertyBlock();
   private ModelSlotDebugRenderData m_debugRenderData;
   private boolean renderingToCard = false;
   private static VertexBufferObject CardQuad;
   private Boolean hasSlotData = false;
   static final int[] vp = new int[4];
   private static final ObjectPool<ModelSlotRenderData> pool = new ObjectPool(ModelSlotRenderData::new);

   public boolean IsRenderingToCard() {
      return this.renderingToCard;
   }

   public ModelSlotRenderData() {
      for(int var1 = 0; var1 < this.effectLights.length; ++var1) {
         this.effectLights[var1] = new ModelInstance.EffectLight();
      }

   }

   public void initModel(ModelManager.ModelSlot var1) {
      int var2 = this.playerIndex = IsoCamera.frameState.playerIndex;
      this.CamCharacterX = IsoCamera.frameState.CamCharacterX;
      this.CamCharacterY = IsoCamera.frameState.CamCharacterY;
      this.CamCharacterZ = IsoCamera.frameState.CamCharacterZ;
      PlayerCamera var3 = IsoCamera.cameras[var2];
      this.fixJigglyModelsSquareX = var3.fixJigglyModelsSquareX;
      this.fixJigglyModelsSquareY = var3.fixJigglyModelsSquareY;
      this.object = var1.model.object;
      this.character = var1.character;
      this.modelData.clear();
      ModelInstanceRenderData var4 = null;
      if (var1.model.model.isReady() && (var1.model.AnimPlayer == null || var1.model.AnimPlayer.isReady())) {
         var4 = ModelInstanceRenderData.alloc();
         var4.initModel(var1.model, (AnimatedModel.AnimatedModelInstanceRenderData)null);
         this.modelData.add(var4);
      }

      this.hasSlotData = var4 != null;
      this.initModelInst(var1.sub, var4);
      if (Core.bDebug) {
         this.m_debugRenderData = ModelSlotDebugRenderData.alloc();
         this.m_debugRenderData.initModel(this);
      }

      boolean var5 = false;
      Iterator var6 = this.modelData.iterator();

      while(var6.hasNext()) {
         ModelInstanceRenderData var7 = (ModelInstanceRenderData)var6.next();
         if (var7.modelInstance != null && var7.modelInstance.hasTextureCreator()) {
            var5 = true;
            break;
         }
      }

      if (this.object instanceof BaseVehicle) {
         this.textureCreator = null;
      } else {
         this.textureCreator = this.character.getTextureCreator();
         if (this.textureCreator != null) {
            if (this.textureCreator.isRendered()) {
               this.textureCreator = null;
            } else {
               ++this.textureCreator.renderRefCount;
            }
         }

         this.bCharacterOutline = this.character.isOutlineHighlight(var2);
      }

      if (this.character != null && (this.textureCreator != null || var5)) {
         assert this.readyModelData.isEmpty();

         ModelInstanceRenderData.release(this.readyModelData);
         this.readyModelData.clear();

         for(int var9 = 0; var9 < this.character.getReadyModelData().size(); ++var9) {
            ModelInstance var10 = (ModelInstance)this.character.getReadyModelData().get(var9);
            ModelInstanceRenderData var8 = ModelInstanceRenderData.alloc();
            var8.initModel(var10, this.getParentData(var10));
            this.readyModelData.add(var8);
         }
      }

   }

   private void initModelInst(ArrayList<ModelInstance> var1, AnimatedModel.AnimatedModelInstanceRenderData var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         ModelInstance var4 = (ModelInstance)var1.get(var3);
         if (var4.model.isReady() && (var4.AnimPlayer == null || var4.AnimPlayer.isReady())) {
            ModelInstanceRenderData var5 = ModelInstanceRenderData.alloc();
            var5.initModel(var4, var2);
            this.modelData.add(var5);
            this.initModelInst(var4.sub, var5);
         }
      }

   }

   public synchronized ModelSlotRenderData init(ModelManager.ModelSlot var1) {
      int var2 = this.playerIndex;
      this.modelSlot = var1;
      this.x = this.object.getX();
      this.y = this.object.getY();
      this.z = this.object.getZ();
      this.FinalScale = 1.0F;
      IsoDepthHelper.Results var3 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.CamCharacterX), PZMath.fastfloor(this.CamCharacterY), this.x + this.fixJigglyModelsSquareX, this.y + this.fixJigglyModelsSquareY, this.z);
      this.squareDepth = var3.depthStart;
      BaseVehicle var4 = (BaseVehicle)Type.tryCastTo(this.object, BaseVehicle.class);
      VehicleModelInstance var6;
      int var15;
      Vector3f var17;
      if (var4 != null) {
         this.animPlayer = var4.getAnimationPlayer();
         this.animPlayerAngle = 0.0F / 0.0F;
         BaseVehicle.CENTER_OF_MASS_MAGIC = 0.7F;
         this.centerOfMassY = var4.jniTransform.origin.y - BaseVehicle.CENTER_OF_MASS_MAGIC;
         this.centerOfMassY -= var4.getZ() * 3.0F * 0.8164967F;
         if (BaseVehicle.RENDER_TO_TEXTURE) {
            this.centerOfMassY = 0.0F - BaseVehicle.CENTER_OF_MASS_MAGIC;
         }

         this.squareDepth = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.CamCharacterX), PZMath.fastfloor(this.CamCharacterY), this.x, this.y, var4.jniTransform.origin.y / 2.44949F).depthStart;
         this.alpha = this.object.getAlpha(var2);
         float var5 = RenderSettings.getInstance().getAmbientForPlayer(var2);
         var6 = (VehicleModelInstance)var1.model;
         ModelInstance.EffectLight[] var7 = var6.getLights();

         for(int var8 = 0; var8 < this.effectLights.length; ++var8) {
            this.effectLights[var8].clear();
         }

         var17 = (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();

         for(int var9 = 0; var9 < var7.length; ++var9) {
            ModelInstance.EffectLight var10 = var7[var9];
            if (var10 != null && var10.radius > 0) {
               Vector3f var11 = var4.getLocalPos(var10.x, var10.y, var10.z + 0.75F, var17);
               this.effectLights[var9].set(var11.x, var11.y, var11.z, var10.r, var10.g, var10.b, var10.radius);
            }
         }

         ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var17);
         if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
            var5 = 1.0F;
         }

         this.ambientR = this.ambientG = this.ambientB = var5;
         this.vehicleTransform.set(var4.vehicleTransform);
      } else {
         IsoAnimal var12 = (IsoAnimal)Type.tryCastTo(this.character, IsoAnimal.class);
         if (var12 != null && var12.getData() != null) {
            this.FinalScale = var12.getData().getSize();
         }

         ModelInstance.PlayerData var13 = var1.model.playerData[var2];
         this.animPlayer = this.character.getAnimationPlayer();
         this.animPlayerAngle = this.animPlayer.getRenderedAngle();

         for(var15 = 0; var15 < this.effectLights.length; ++var15) {
            ModelInstance.EffectLight var18 = var13.effectLightsMain[var15];
            this.effectLights[var15].set(var18.x, var18.y, var18.z, var18.r, var18.g, var18.b, var18.radius);
         }

         this.bOutside = this.character.getCurrentSquare() != null && this.character.getCurrentSquare().isOutside();
         this.alpha = this.character.getAlpha(var2);
         if ((!Core.bDebug || !DebugOptions.instance.DebugDraw_SkipWorldShading.getValue()) && (!GameServer.bServer || !ServerGUI.isCreated())) {
            this.ambientR = var13.currentAmbient.x;
            this.ambientG = var13.currentAmbient.y;
            this.ambientB = var13.currentAmbient.z;
         } else {
            this.ambientR = this.ambientG = this.ambientB = 1.0F;
         }

         if (this.bCharacterOutline) {
            this.outlineColor.setABGR(this.character.getOutlineHighlightCol(var2));
            this.bOutlineBehindPlayer = this.character.isProne() || this.x + this.y < this.CamCharacterX + this.CamCharacterY;
         }

         this.adjustForSittingOnFurniture();
         this.bInVehicle = this.character.isSeatedInVehicle();
         if (this.bInVehicle) {
            this.animPlayerAngle = 0.0F;
            BaseVehicle var16 = this.character.getVehicle();
            this.centerOfMassY = var16.jniTransform.origin.y - BaseVehicle.CENTER_OF_MASS_MAGIC;
            this.x = var16.getX();
            this.y = var16.getY();
            this.z = var16.getZ();
            var17 = (Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc();
            var16.getPassengerLocalPos(var16.getSeat(this.character), var17);
            this.inVehicleX = var17.x;
            this.inVehicleY = var17.y;
            this.inVehicleZ = var17.z;
            ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var17);
            Vector3f var20 = var16.vehicleTransform.getEulerAnglesZYX((Vector3f)((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).alloc());
            this.vehicleAngleZ = (float)Math.toDegrees((double)var20.z);
            this.vehicleAngleY = (float)Math.toDegrees((double)var20.y);
            this.vehicleAngleX = (float)Math.toDegrees((double)var20.x);
            ((BaseVehicle.Vector3fObjectPool)BaseVehicle.TL_vector3f_pool.get()).release(var20);
         }

         FBORenderTracerEffects.getInstance().releaseWeaponTransform(this.character);
      }

      this.RENDER_TO_TEXTURE = BaseVehicle.RENDER_TO_TEXTURE;
      boolean var14 = false;
      var6 = null;
      GameProfiler.getInstance().invokeAndMeasure("Init Inst", this::initRenderData);
      GameProfiler.getInstance().invokeAndMeasure("Set Lights", var1.model.model, this, 5, Model::setLightsInst);

      ModelInstanceRenderData var21;
      for(var15 = 0; var15 < this.modelData.size(); ++var15) {
         var21 = (ModelInstanceRenderData)this.modelData.get(var15);
         if (this.character != null && var21.modelInstance == this.character.primaryHandModel) {
            if (this.character.isMuzzleFlash()) {
               var21.m_muzzleFlash = true;
            }

            FBORenderTracerEffects.getInstance().storeWeaponTransform(this.character, var21.xfrm);
         }

         if (var21.modelInstance != null && var21.modelInstance.hasTextureCreator()) {
            var14 = true;
         }

         this.UpdateCharacter(var21.properties);
      }

      Iterator var19 = this.readyModelData.iterator();

      while(var19.hasNext()) {
         var21 = (ModelInstanceRenderData)var19.next();
         var21.init();
         var21.transformToParent(var21.parent);
      }

      if (Core.bDebug) {
         this.m_debugRenderData.init(this);
      }

      this.bRendered = false;
      return this;
   }

   private void adjustForSittingOnFurniture() {
      if (this.character.isSittingOnFurniture()) {
         IsoObject var1 = this.character.getSitOnFurnitureObject();
         if (var1 != null && var1.getSprite() != null && var1.getSprite().tilesetName != null) {
            IsoDirections var2 = this.character.getSitOnFurnitureDirection();
            Vector3f var3 = SeatingManager.getInstance().getTranslation(var1.getSprite(), var2.name(), new Vector3f());
            float var4 = var3.x;
            float var5 = var3.y;
            float var6 = var3.z;
            float var7 = 1.0F;
            String var8 = this.character.getVariableString("SitOnFurnitureDirection");
            String var9 = "SitOnFurniture" + var8;
            if (this.character.isCurrentState(PlayerGetUpState.instance())) {
               String var10 = "";
               if (this.character.getVariableBoolean("getUpQuick")) {
                  var10 = "Quick";
               }

               if ("Left".equalsIgnoreCase(var8)) {
                  var10 = var10 + "_L";
               } else if ("Right".equalsIgnoreCase(var8)) {
                  var10 = var10 + "_R";
               }

               var9 = "fromSitOnFurniture" + var10;
               var7 = 0.0F;
            }

            float var14 = SeatingManager.getInstance().getAnimationTrackFraction(this.character, var9);
            if (var14 < 0.0F && !this.character.getVariableBoolean("SitOnFurnitureStarted")) {
               var7 = 1.0F - var7;
            }

            if (var14 >= 0.0F) {
               float var11;
               float var12;
               if (this.character.isCurrentState(PlayerGetUpState.instance())) {
                  var11 = 0.48F;
                  var12 = 0.63F;
                  if (var14 >= var12) {
                     var7 = 1.0F;
                  } else if (var14 >= var11) {
                     var7 = (var14 - var11) / (var12 - var11);
                  } else {
                     var7 = 0.0F;
                  }

                  var7 = 1.0F - var7;
               } else {
                  var11 = 0.27F;
                  var12 = 0.43F;
                  if (var14 >= var11 && var14 <= var12) {
                     var7 = (var14 - var11) / (var12 - var11);
                  } else if (var14 >= var12) {
                     var7 = 1.0F;
                  } else {
                     var7 = 0.0F;
                  }
               }
            }

            this.z = PZMath.lerp(this.z, (float)var1.square.z + var6 / 2.44949F, var7);
            IsoDepthHelper.Results var13 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.CamCharacterX), PZMath.fastfloor(this.CamCharacterY), this.x + this.fixJigglyModelsSquareX, this.y + this.fixJigglyModelsSquareY, this.z);
            this.squareDepth = var13.depthStart;
         }
      }
   }

   public void UpdateCharacter(ShaderPropertyBlock var1) {
      var1.SetVector3("AmbientColour", this.ambientR * 0.45F, this.ambientG * 0.45F, this.ambientB * 0.45F);
      var1.SetFloat("Alpha", this.alpha);
      if (var1.GetParameter("LightDirection") == null) {
         var1.SetVector3Array("LightDirection", new org.lwjgl.util.vector.Vector3f[5]);
      }

      if (var1.GetParameter("LightColour") == null) {
         var1.SetVector3Array("LightColour", new org.lwjgl.util.vector.Vector3f[5]);
      }

      var1.CopyParameters(this.properties);
   }

   private ModelInstanceRenderData getParentData(ModelInstance var1) {
      for(int var2 = 0; var2 < this.readyModelData.size(); ++var2) {
         ModelInstanceRenderData var3 = (ModelInstanceRenderData)this.readyModelData.get(var2);
         if (var3.modelInstance == var1.parent) {
            return var3;
         }
      }

      return null;
   }

   private void initRenderData() {
      Iterator var1 = this.modelData.iterator();

      while(var1.hasNext()) {
         ModelInstanceRenderData var2 = (ModelInstanceRenderData)var1.next();
         var2.init();
         var2.transformToParent(var2.parent);
      }

   }

   public synchronized void render() {
      if (this.character == null) {
         this.renderVehicle();
      } else {
         this.renderCharacter();
      }

   }

   public void renderDebug() {
      if (this.m_debugRenderData != null) {
         this.m_debugRenderData.render();
      }

   }

   private static VertexBufferObject GetCardQuad() {
      if (CardQuad == null) {
         VertexBufferObject.VertexFormat var0 = new VertexBufferObject.VertexFormat(2);
         var0.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
         var0.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
         var0.calculate();
         VertexBufferObject.VertexArray var1 = new VertexBufferObject.VertexArray(var0, 4);
         var1.setElement(0, 0, -0.5F, 1.0F, 0.0F);
         var1.setElement(0, 1, 0.0F, 1.0F);
         var1.setElement(1, 0, 0.5F, 1.0F, 0.0F);
         var1.setElement(1, 1, 1.0F, 1.0F);
         var1.setElement(2, 0, -0.5F, 0.0F, 0.0F);
         var1.setElement(2, 1, 0.0F, 0.0F);
         var1.setElement(3, 0, 0.5F, 0.0F, 0.0F);
         var1.setElement(3, 1, 1.0F, 0.0F);
         CardQuad = new VertexBufferObject(var1, new int[]{0, 1, 2, 1, 3, 2});
      }

      return CardQuad;
   }

   private void bindAndClearBlend() {
      Imposter.CreateBlend();
      Imposter.BlendTexture.BindDraw();
      GL43.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL43.glClearDepthf(1.0F);
      GL43.glClearStencil(0);
      GL11.glClear(17664);
   }

   private void setupImposterShader(Shader var1, RenderTexture var2) {
      ShaderProgram var3 = var1.getShaderProgram();
      ShaderProgram.Uniform var4 = var3.getUniform("tex", 35678);
      ShaderProgram.Uniform var5 = var3.getUniform("depth", 35678);
      var1.Start();
      if (var4 != null) {
         GL43.glActiveTexture('蓀' + var4.sampler);
         var2.BindTexture();
         GL43.glUniform1i(var4.loc, var4.sampler);
      }

      if (var5 != null) {
         GL43.glActiveTexture('蓀' + var5.sampler);
         var2.BindDepth();
         GL43.glUniform1i(var5.loc, var5.sampler);
      }

   }

   private void drawCardToBlend(Imposter var1) {
      GL43.glStencilFunc(514, 0, 255);
      GL11.glStencilOp(7680, 7680, 7681);
      Imposter.BlendTexture.BindDraw();
      Shader var2 = ShaderManager.instance.getOrCreateShader("imposterBlend", true, false);
      this.setupImposterShader(var2, var1.card);
      RenderTarget.DrawFullScreenTri();
      var2.End();
   }

   private void drawBlendToCard(Imposter var1) {
      GL43.glStencilFunc(519, 0, 0);
      var1.card.BindDraw();
      GL43.glClear(16640);
      Shader var2 = ShaderManager.instance.getOrCreateShader("imposterUnstencil", true, false);
      ShaderProgram var3 = var2.getShaderProgram();
      ShaderProgram.Uniform var4 = var3.getUniform("stencil", 36306);
      this.setupImposterShader(var2, Imposter.BlendTexture);
      if (var4 != null) {
         GL43.glActiveTexture('蓀' + var4.sampler);
         Imposter.BlendTexture.BindStencil();
         GL43.glUniform1i(var4.loc, var4.sampler);
      }

      RenderTarget.DrawFullScreenTri();
      var2.End();
   }

   private void blendImposter(Imposter var1) {
      this.bindAndClearBlend();
      GL43.glEnable(2960);
      GL43.glStencilFunc(519, 1, 255);
      GL43.glStencilMask(255);
      GL11.glStencilOp(7681, 7681, 7681);
      this.renderCharacter();
      this.drawBlendToCard(var1);
      GL43.glActiveTexture(33984);
      GL43.glCullFace(1029);
      GL43.glDisable(2884);
   }

   public void renderToImposterCard(Imposter var1) {
      var1.sinceLastUpdate = 0;
      this.renderingToCard = true;
      boolean var2 = GL11.glGetBoolean(2960);
      int[] var3 = new int[3];
      GL11.glGetIntegerv(2962, var3);
      boolean var4 = GL11.glGetBoolean(2884);
      int var5 = GL11.glGetInteger(2885);
      int var6 = GL11.glGetInteger(36006);
      GL11.glGetIntegerv(2978, vp);
      if (Core.bDebug && DebugOptions.instance.ZombieImposterBlend.getValue()) {
         this.blendImposter(var1);
      } else {
         var1.card.BindDraw();
         GL43.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL43.glClear(16640);
         this.renderCharacter();
      }

      GL43.glBindFramebuffer(36009, var6);
      GL43.glViewport(vp[0], vp[1], vp[2], vp[3]);
      var1.cardRendered = true;
      this.renderingToCard = false;
   }

   public void renderCard(Imposter var1) {
      GL11.glDisable(2884);
      GL11.glEnable(2929);
      GL11.glDepthFunc(515);
      Shader var2 = ShaderManager.instance.getOrCreateShader("imposter", false, false);
      var2.Start();
      ShaderProgram var3 = var2.getShaderProgram();
      ShaderProgram.Uniform var4 = var3.getUniform("transform", 35676);
      Vector3f var5 = new Vector3f();
      Matrix4f var6 = Core.getInstance().modelViewMatrixStack.peek();
      Matrix4f var7 = Core.getInstance().modelViewMatrixStack.alloc();
      var6.get(var7);
      var7.getScale(var5);
      var5.set(1.0F / var5.x, 1.0F / var5.y, 1.0F / var5.z);
      var7.scale(var5);
      var7.setTranslation(0.0F, 0.0F, 0.0F);
      var2.setMatrix(var4.loc, var7);
      ShaderProgram.Uniform var8 = var3.getUniform("tex", 35678);
      ShaderProgram.Uniform var9 = var3.getUniform("depth", 35678);
      if (var8 != null) {
         GL43.glActiveTexture('蓀' + var8.sampler);
         var1.card.BindTexture();
         GL43.glUniform1i(var8.loc, var8.sampler);
      }

      if (var9 != null) {
         GL43.glActiveTexture('蓀' + var9.sampler);
         var1.card.BindDepth();
         GL43.glUniform1i(var9.loc, var9.sampler);
      }

      ShaderProgram.Uniform var10 = var3.getUniform("frameFade", 5126);
      if (var10 != null) {
         if (Core.bDebug && !DebugOptions.instance.ZombieImposterBlend.getValue()) {
            GL43.glUniform1f(var10.loc, 1.0F);
         } else {
            float var11 = 1.0F - (float)var1.sinceLastUpdate / 10.0F;
            GL43.glUniform1f(var10.loc, var11);
         }
      }

      VertexBufferObject var18 = GetCardQuad();
      var18.Draw(var2);
      var2.End();
      GL43.glActiveTexture(33984);
      ++var1.sinceLastUpdate;
      if (Core.bDebug) {
         RenderTexture var12;
         if (DebugOptions.instance.ZombieImposterPreview.getValue()) {
            var12 = var1.card;
         } else if (DebugOptions.instance.ZombieBlendPreview.getValue()) {
            var12 = Imposter.BlendTexture;
         } else {
            var12 = null;
         }

         if (var12 != null) {
            int var13 = GL11.glGetInteger(36010);
            int var14 = var12.GetWidth();
            int var15 = var12.GetHeight();
            int var16 = var14 / 2;
            int var17 = var15 / 2;
            var12.BindRead();
            GL43.glBlitFramebuffer(0, 0, var14, var15, 960 - var16, 540 - var17, 960 + var16, 540 + var17, 16384, 9728);
            GL43.glBindFramebuffer(36008, var13);
         }
      }

      if (Core.bDebug && DebugOptions.instance.RenderTestFSQuad.getValue()) {
         Shader var19 = ShaderManager.instance.getOrCreateShader("imposterTest", true, false);
         var19.Start();
         RenderTarget.DrawFullScreenTri();
         var19.End();
      }

   }

   public boolean checkReady() {
      this.bReady = true;
      if (this.textureCreator != null && !this.textureCreator.isRendered()) {
         this.textureCreator.render();
         if (!this.textureCreator.isRendered()) {
            this.bReady = false;
         }
      }

      for(int var1 = 0; var1 < this.modelData.size(); ++var1) {
         ModelInstanceRenderData var2 = (ModelInstanceRenderData)this.modelData.get(var1);
         ModelInstanceTextureInitializer var3 = var2.modelInstance.getTextureInitializer();
         if (var3 != null && !var3.isRendered()) {
            var3.render();
            if (!var3.isRendered()) {
               this.bReady = false;
            }
         }
      }

      return this.bReady;
   }

   public boolean canRender() {
      return this.bReady || !this.readyModelData.isEmpty();
   }

   public ModelInstanceRenderDataList getModelData() {
      return this.bReady ? this.modelData : this.readyModelData;
   }

   private void renderCharacter() {
      if (Core.bDebug && DebugOptions.instance.ZombieImposterRendering.getValue() && !this.renderingToCard) {
         IsoGameCharacter var2 = this.character;
         if (var2 instanceof IsoZombie) {
            IsoZombie var1 = (IsoZombie)var2;
            if (var1.imposter.card == null) {
               var1.imposter.create();
               this.renderToImposterCard(var1.imposter);
            } else if (var1.imposter.cardRendered && var1.imposter.sinceLastUpdate >= 10) {
               this.renderToImposterCard(var1.imposter);
            }
         }
      }

      this.checkReady();
      if (this.bReady || !this.readyModelData.isEmpty()) {
         if (this.bCharacterOutline) {
            ModelCamera.instance.bDepthMask = false;
            GameProfiler.getInstance().invokeAndMeasure("performRenderCharacterOutline", this, ModelSlotRenderData::performRenderCharacterOutline);
         }

         ModelCamera.instance.bDepthMask = true;
         GameProfiler.getInstance().invokeAndMeasure("renderCharacter", this, ModelSlotRenderData::performRenderCharacter);
         int var3 = SpriteRenderer.instance.getRenderingPlayerIndex();
         IsoPlayer var4 = (IsoPlayer)Type.tryCastTo(this.character, IsoPlayer.class);
         if (var4 != null && !this.bCharacterOutline && var4 == IsoPlayer.players[var3]) {
            ModelOutlines.instance.setPlayerRenderData(this);
         }

         this.bRendered = this.bReady;
      }
   }

   private void renderVehicleDebug() {
      if (Core.bDebug) {
         Vector3 var1 = Model.tempo;
         ModelCamera.instance.Begin();
         Matrix4f var2 = Core.getInstance().modelViewMatrixStack.peek();
         var2.translate(0.0F, this.centerOfMassY, 0.0F);
         if (this.m_debugRenderData != null && !this.modelData.isEmpty()) {
            PZGLUtil.pushAndMultMatrix(5888, ((ModelInstanceRenderData)this.modelData.get(0)).xfrm);
            this.m_debugRenderData.render();
            PZGLUtil.popMatrix(5888);
         }

         BaseVehicle var10;
         if (DebugOptions.instance.Model.Render.Attachments.getValue() && !this.modelData.isEmpty()) {
            var10 = (BaseVehicle)this.object;
            ModelInstanceRenderData var3 = (ModelInstanceRenderData)this.modelData.get(0);
            PZGLUtil.pushAndMultMatrix(5888, this.vehicleTransform);
            float var4 = var10.getScript().getModelScale();
            float var5 = var3.modelInstance.scale;
            Matrix4f var6 = (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
            var6.scaling(1.0F / var4);
            Matrix4f var7 = (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();

            for(int var8 = 0; var8 < var10.getScript().getAttachmentCount(); ++var8) {
               ModelAttachment var9 = var10.getScript().getAttachment(var8);
               var3.modelInstance.getAttachmentMatrix(var9, var7);
               var6.mul(var7, var7);
               PZGLUtil.pushAndMultMatrix(5888, var7);
               Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 2.0F);
               PZGLUtil.popMatrix(5888);
            }

            ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var7);
            ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var6);
            PZGLUtil.popMatrix(5888);
         }

         if (Core.bDebug && DebugOptions.instance.Model.Render.Axis.getValue() && !this.modelData.isEmpty()) {
            var10 = (BaseVehicle)this.object;
            float var11 = var10.getScript().getModelScale();
            Matrix4f var12 = BaseVehicle.allocMatrix4f();
            var12.set(this.vehicleTransform);
            var12.scale(1.0F / var11);
            PZGLUtil.pushAndMultMatrix(5888, var12);
            Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 1.0F, 4.0F);
            PZGLUtil.popMatrix(5888);

            for(int var13 = 1; var13 < this.modelData.size(); ++var13) {
               var12.set(((ModelInstanceRenderData)this.modelData.get(var13)).xfrm);
               var12.scale(1.0F / var11);
               PZGLUtil.pushAndMultMatrix(5888, var12);
               Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 1.0F, 4.0F);
               PZGLUtil.popMatrix(5888);
            }

            BaseVehicle.releaseMatrix4f(var12);
         }

         ModelCamera.instance.End();
      }
   }

   private void performRenderCharacter() {
      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GL11.glEnable(2929);
      GL11.glDisable(3089);
      ModelInstanceRenderDataList var1 = this.modelData;
      if (this.character != null && !this.bReady) {
         var1 = this.readyModelData;
      }

      if (Core.bDebug && DebugOptions.instance.ZombieImposterRendering.getValue()) {
         IsoGameCharacter var3 = this.character;
         if (var3 instanceof IsoZombie) {
            IsoZombie var2 = (IsoZombie)var3;
            if (this.IsRenderingToCard()) {
               ModelCamera.instance.BeginImposter(var2.imposter.card);
            } else {
               float var7 = ModelCamera.instance.m_useAngle;
               ModelCamera.instance.m_useAngle = 0.0F;
               ModelCamera.instance.Begin();
               ModelCamera.instance.m_useAngle = var7;
            }
         } else {
            Model.CharacterModelCameraBegin(this);
         }
      } else {
         Model.CharacterModelCameraBegin(this);
      }

      float var5 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
      this.modelSlot.model.targetDepth = this.squareDepth - (var5 + 1.0F) / 2.0F + 0.5F - 1.0E-4F;
      boolean var6 = false;
      if (Core.bDebug && DebugOptions.instance.ZombieImposterRendering.getValue() && !this.renderingToCard) {
         IsoGameCharacter var4 = this.character;
         if (var4 instanceof IsoZombie) {
            IsoZombie var8 = (IsoZombie)var4;
            if (var8.imposter.cardRendered) {
               this.renderCard(var8.imposter);
               var6 = true;
            }
         }
      }

      int var9;
      ModelInstanceRenderData var10;
      if (!var6) {
         for(var9 = 0; var9 < var1.size(); ++var9) {
            var10 = (ModelInstanceRenderData)var1.get(var9);
            if (var10.modelInstance != null) {
               var10.modelInstance.targetDepth = this.modelSlot.model.targetDepth;
            }

            var10.RenderCharacter(this);
         }
      }

      if (Core.bDebug) {
         this.renderDebug();

         for(var9 = 0; var9 < var1.size(); ++var9) {
            var10 = (ModelInstanceRenderData)var1.get(var9);
            var10.renderDebug();
         }
      }

      if (this.character instanceof IsoZombie) {
         if (this.IsRenderingToCard()) {
            ModelCamera.instance.EndImposter();
         } else {
            Model.CharacterModelCameraEnd();
         }
      } else {
         Model.CharacterModelCameraEnd();
      }

      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      GL11.glEnable(3553);
      SpriteRenderer.ringBuffer.restoreVBOs = true;
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GLStateRenderThread.restore();
   }

   protected void performRenderCharacterOutline() {
      this.performRenderCharacterOutline(false, this.outlineColor, this.bOutlineBehindPlayer);
   }

   protected void performRenderCharacterOutline(boolean var1, ColorInfo var2, boolean var3) {
      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GL11.glEnable(2929);
      GL11.glDisable(3089);
      ModelInstanceRenderDataList var4 = this.modelData;
      if (this.character != null && !this.bReady) {
         var4 = this.readyModelData;
      }

      if (solidColor == null) {
         solidColor = new Shader("aim_outline_solid", false, false);
         solidColorStatic = new Shader("aim_outline_solid", true, false);
      }

      solidColor.Start();
      solidColor.getShaderProgram().setVector4("u_color", var2.r, var2.g, var2.b, 1.0F);
      solidColor.End();
      solidColorStatic.Start();
      solidColorStatic.getShaderProgram().setVector4("u_color", var2.r, var2.g, var2.b, 1.0F);
      solidColorStatic.End();
      boolean var5 = ModelOutlines.instance.beginRenderOutline(var2, var3, var1);
      ModelOutlines.instance.m_fboA.startDrawing(var5, true);
      Model.CharacterModelCameraBegin(this);

      for(int var6 = 0; var6 < var4.size(); ++var6) {
         ModelInstanceRenderData var7 = (ModelInstanceRenderData)var4.get(var6);
         Shader var8 = var7.model.Effect;

         try {
            var7.model.Effect = var7.model.bStatic ? solidColorStatic : solidColor;
            var7.RenderCharacter(this);
         } finally {
            var7.model.Effect = var8;
         }
      }

      Model.CharacterModelCameraEnd();
      ModelOutlines.instance.m_fboA.endDrawing();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      GL11.glEnable(3553);
      SpriteRenderer.ringBuffer.restoreVBOs = true;
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GLStateRenderThread.restore();
   }

   private void renderVehicle() {
      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      if (this.RENDER_TO_TEXTURE) {
         GL11.glClear(256);
      }

      GL11.glEnable(2929);
      GL11.glDisable(3089);
      if (this.RENDER_TO_TEXTURE) {
         ModelManager.instance.bitmap.startDrawing(true, true);
         GL11.glViewport(0, 0, ModelManager.instance.bitmap.getWidth(), ModelManager.instance.bitmap.getHeight());
      }

      for(int var1 = 0; var1 < this.modelData.size(); ++var1) {
         ModelInstanceRenderData var2 = (ModelInstanceRenderData)this.modelData.get(var1);
         var2.RenderVehicle(this);
      }

      this.renderVehicleDebug();
      if (this.RENDER_TO_TEXTURE) {
         ModelManager.instance.bitmap.endDrawing();
      }

      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      GL11.glEnable(3553);
      SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      SpriteRenderer.ringBuffer.restoreVBOs = true;
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GLStateRenderThread.restore();
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
   }

   private void doneWithTextureCreator(ModelInstanceTextureCreator var1) {
      if (var1 != null) {
         if (var1.testNotReady > 0) {
            --var1.testNotReady;
         }

         if (var1.renderRefCount <= 0) {
            if (var1.isRendered()) {
               var1.postRender();
               if (var1 == this.character.getTextureCreator()) {
                  this.character.setTextureCreator((ModelInstanceTextureCreator)null);
               }
            } else if (var1 != this.character.getTextureCreator()) {
               var1.postRender();
            }

         }
      }
   }

   public void postRender() {
      assert this.modelSlot.renderRefCount > 0;

      --this.modelSlot.renderRefCount;
      if (this.textureCreator != null) {
         --this.textureCreator.renderRefCount;
         this.doneWithTextureCreator(this.textureCreator);
         this.textureCreator = null;
      }

      ModelInstanceRenderData.release(this.readyModelData);
      this.readyModelData.clear();
      if (this.bRendered && this.character != null) {
         ModelManager.instance.derefModelInstances(this.character.getReadyModelData());
         this.character.getReadyModelData().clear();

         for(int var1 = 0; var1 < this.modelData.size(); ++var1) {
            ModelInstance var2 = ((ModelInstanceRenderData)this.modelData.get(var1)).modelInstance;
            ++var2.renderRefCount;
            this.character.getReadyModelData().add(var2);
         }
      }

      this.character = null;
      this.object = null;
      this.animPlayer = null;
      this.m_debugRenderData = (ModelSlotDebugRenderData)Pool.tryRelease((IPooledObject)this.m_debugRenderData);
      ModelInstanceRenderData.release(this.modelData);
      pool.release((Object)this);
   }

   public static ModelSlotRenderData alloc() {
      return (ModelSlotRenderData)pool.alloc();
   }
}
