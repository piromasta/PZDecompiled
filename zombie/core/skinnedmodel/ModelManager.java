package zombie.core.skinnedmodel;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.Util;
import zombie.DebugFileWatcher;
import zombie.GameWindow;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetPath;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorFactory;
import zombie.characters.AttachedItems.AttachedModels;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderPrograms;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.SoftwareSkinnedModelAnim;
import zombie.core.skinnedmodel.animation.StaticAnimation;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.AnimationAssetManager;
import zombie.core.skinnedmodel.model.MeshAssetManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.core.textures.TextureID;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.FireShader;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.ParticlesFire;
import zombie.iso.PlayerCamera;
import zombie.iso.PuddlesShader;
import zombie.iso.SmokeShader;
import zombie.iso.Vector2;
import zombie.iso.WaterShader;
import zombie.iso.sprite.SkyBox;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ItemReplacement;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelManager {
   public static boolean NoOpenGL = false;
   public static final ModelManager instance = new ModelManager();
   private final HashMap<String, Model> m_modelMap = new HashMap();
   public Model m_maleModel;
   public Model m_femaleModel;
   public Model m_skeletonMaleModel;
   public Model m_skeletonFemaleModel;
   public TextureFBO bitmap;
   private boolean m_bCreated = false;
   public boolean bDebugEnableModels = true;
   public boolean bCreateSoftwareMeshes = false;
   public final HashMap<String, SoftwareSkinnedModelAnim> SoftwareMeshAnims = new HashMap();
   private final ArrayList<ModelSlot> m_modelSlots = new ArrayList();
   private final ObjectPool<ModelInstance> m_modelInstancePool = new ObjectPool(ModelInstance::new);
   private final ArrayList<WeaponPart> m_tempWeaponPartList = new ArrayList();
   private ModelMesh m_animModel;
   private final HashMap<String, AnimationAsset> m_animationAssets = new HashMap();
   private final ModAnimations m_gameAnimations = new ModAnimations("game");
   private final HashMap<String, ModAnimations> m_modAnimations = new HashMap();
   private final ArrayList<StaticAnimation> m_cachedAnims = new ArrayList();
   private final HashSet<IsoGameCharacter> m_contains = new HashSet();
   private final ArrayList<IsoGameCharacter> ToRemove = new ArrayList();
   private final ArrayList<IsoGameCharacter> ToResetNextFrame = new ArrayList();
   private final ArrayList<IsoGameCharacter> ToResetEquippedNextFrame = new ArrayList();
   private final ArrayList<ModelSlot> m_resetAfterRender = new ArrayList();
   private final Vector2 m_tempVec2 = new Vector2();
   private final Vector2 m_tempVec2_2 = new Vector2();
   private static final TreeMap<String, ModelMetaData> modelMetaData;
   static String basicEffect;
   static String isStaticTrue;
   static String shaderEquals;
   static String texA;
   static String amp;
   static HashMap<String, String> toLower;
   static HashMap<String, String> toLowerTex;
   static HashMap<String, String> toLowerKeyRoot;
   static StringBuilder builder;

   public ModelManager() {
   }

   public boolean isCreated() {
      return this.m_bCreated;
   }

   public void create() {
      if (!this.m_bCreated) {
         if (!GameServer.bServer || ServerGUI.isCreated()) {
            Texture var1 = new Texture(1024, 1024, 16);
            Texture var2 = new Texture(1024, 1024, 512);
            PerformanceSettings.UseFBOs = false;

            try {
               this.bitmap = new TextureFBO(var1, var2, false);
            } catch (Exception var7) {
               var7.printStackTrace();
               PerformanceSettings.UseFBOs = false;
               DebugLog.Animation.error("FBO not compatible with gfx card at this time.");
               return;
            }
         }

         DebugLog.Animation.println("Loading 3D models");
         this.initAnimationMeshes(false);
         this.m_modAnimations.put(this.m_gameAnimations.m_modID, this.m_gameAnimations);
         AnimationsMesh var8 = ScriptManager.instance.getAnimationsMesh("Human");
         ModelMesh var9 = var8.modelMesh;
         if (!NoOpenGL && this.bCreateSoftwareMeshes) {
            SoftwareSkinnedModelAnim var3 = new SoftwareSkinnedModelAnim((StaticAnimation[])this.m_cachedAnims.toArray(new StaticAnimation[0]), var9.softwareMesh, var9.skinningData);
            this.SoftwareMeshAnims.put(var9.getPath().getPath(), var3);
         }

         Model var10 = this.loadModel("skinned/malebody", (String)null, var9);
         Model var4 = this.loadModel("skinned/femalebody", (String)null, var9);
         Model var5 = this.loadModel("skinned/Male_Skeleton", (String)null, var9);
         Model var6 = this.loadModel("skinned/Female_Skeleton", (String)null, var9);
         this.m_animModel = var9;
         this.loadModAnimations();
         var10.addDependency(this.getAnimationAssetRequired("bob/bob_idle"));
         var10.addDependency(this.getAnimationAssetRequired("bob/bob_walk"));
         var10.addDependency(this.getAnimationAssetRequired("bob/bob_run"));
         var4.addDependency(this.getAnimationAssetRequired("bob/bob_idle"));
         var4.addDependency(this.getAnimationAssetRequired("bob/bob_walk"));
         var4.addDependency(this.getAnimationAssetRequired("bob/bob_run"));
         this.m_maleModel = var10;
         this.m_femaleModel = var4;
         this.m_skeletonMaleModel = var5;
         this.m_skeletonFemaleModel = var6;
         this.m_bCreated = true;
         AdvancedAnimator.systemInit();
         PopTemplateManager.instance.init();
      }
   }

   public void loadAdditionalModel(String var1, String var2, boolean var3, String var4) {
      boolean var5 = this.bCreateSoftwareMeshes;
      if (DebugLog.isEnabled(DebugType.Animation)) {
         DebugType.ModelManager.debugln("createSoftwareMesh: %B, model: %s", var5, var1);
      }

      Model var6 = this.loadModelInternal(var1, var2, var4, this.m_animModel, var3, (String)null);
      if (var5) {
         SoftwareSkinnedModelAnim var7 = new SoftwareSkinnedModelAnim((StaticAnimation[])this.m_cachedAnims.toArray(new StaticAnimation[0]), var6.softwareMesh, (SkinningData)var6.Tag);
         this.SoftwareMeshAnims.put(var1.toLowerCase(), var7);
      }

   }

   public ModelInstance newAdditionalModelInstance(String var1, String var2, IsoGameCharacter var3, AnimationPlayer var4, String var5) {
      Model var6 = this.tryGetLoadedModel(var1, var2, false, var5, false);
      if (var6 == null) {
         boolean var7 = false;
         instance.loadAdditionalModel(var1, var2, var7, var5);
      }

      var6 = this.getLoadedModel(var1, var2, false, var5);
      return this.newInstance(var6, var3, var4);
   }

   private void loadAnimsFromDir(String var1, ModelMesh var2, ArrayList<String> var3) {
      File var4 = new File(ZomboidFileSystem.instance.base.canonicalFile, var1);
      this.loadAnimsFromDir(ZomboidFileSystem.instance.base.lowercaseURI, ZomboidFileSystem.instance.getMediaLowercaseURI(), var4, var2, this.m_gameAnimations, var3);
   }

   private void loadAnimsFromDir(URI var1, URI var2, File var3, ModelMesh var4, ModAnimations var5, ArrayList<String> var6) {
      if (!var3.exists()) {
         DebugLog.General.error("ERROR: %s", var3.getPath());

         for(File var7 = var3.getParentFile(); var7 != null; var7 = var7.getParentFile()) {
            DebugLog.General.error(" - Parent exists: %B, %s", var7.exists(), var7.getPath());
         }
      }

      if (var3.isDirectory()) {
         File[] var14 = var3.listFiles();
         if (var14 != null) {
            boolean var8 = false;
            File[] var9 = var14;
            int var10 = var14.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               File var12 = var9[var11];
               if (var12.isDirectory()) {
                  this.loadAnimsFromDir(var1, var2, var12, var4, var5, var6);
               } else if (this.checkAnimationMeshPrefixes(var12.getName(), var6)) {
                  String var13 = ZomboidFileSystem.instance.getAnimName(var2, var12);
                  this.loadAnim(var13, var4, var5);
                  var8 = true;
                  if (!NoOpenGL && RenderThread.RenderThread == null) {
                     Display.processMessages();
                  }
               }
            }

            if (var8) {
               DebugFileWatcher.instance.add((new AnimDirReloader(var1, var2, var3.getPath(), var4, var5)).GetFileWatcher());
            }

         }
      }
   }

   private boolean checkAnimationMeshPrefixes(String var1, ArrayList<String> var2) {
      if (var2 == null) {
         return true;
      } else if (var2.isEmpty()) {
         return true;
      } else {
         boolean var3 = false;
         Iterator var4 = var2.iterator();

         while(var4.hasNext()) {
            String var5 = (String)var4.next();
            if (var5.startsWith("!") && StringUtils.startsWithIgnoreCase(var1, var5.substring(1))) {
               return false;
            }

            if (StringUtils.startsWithIgnoreCase(var1, var5)) {
               var3 = true;
            }
         }

         return var3;
      }
   }

   public void RenderSkyBox(TextureDraw var1, int var2, int var3, int var4, int var5) {
      int var6 = TextureFBO.getCurrentID();
      switch (var4) {
         case 1:
            GL30.glBindFramebuffer(36160, var5);
            break;
         case 2:
            ARBFramebufferObject.glBindFramebuffer(36160, var5);
            break;
         case 3:
            EXTFramebufferObject.glBindFramebufferEXT(36160, var5);
      }

      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      Matrix4f var7 = Core.getInstance().projectionMatrixStack.alloc();
      var7.setOrtho(0.0F, 1.0F, 1.0F, 0.0F, -1.0F, 1.0F);
      Core.getInstance().projectionMatrixStack.push(var7);
      GL11.glViewport(0, 0, 512, 512);
      GL11.glClear(16384);
      Matrix4f var8 = Core.getInstance().modelViewMatrixStack.alloc();
      var8.identity();
      Core.getInstance().modelViewMatrixStack.push(var8);
      ShaderHelper.glUseProgramObjectARB(var2);
      if (Shader.ShaderMap.containsKey(var2)) {
         ((Shader)Shader.ShaderMap.get(var2)).startRenderThread(var1);
      }

      VBORenderer var9 = VBORenderer.getInstance();
      var9.startRun(var9.FORMAT_PositionColorUV);
      var9.setMode(7);
      var9.setShaderProgram(ShaderPrograms.getInstance().getProgramByID(var2));
      var9.addQuad(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.13F, 0.96F, 0.13F, 1.0F);
      var9.endRun();
      var9.flush();
      ShaderHelper.forgetCurrentlyBound();
      ShaderHelper.glUseProgramObjectARB(0);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      switch (var4) {
         case 1:
            GL30.glBindFramebuffer(36160, var6);
            break;
         case 2:
            ARBFramebufferObject.glBindFramebuffer(36160, var6);
            break;
         case 3:
            EXTFramebufferObject.glBindFramebufferEXT(36160, var6);
      }

      SkyBox.getInstance().swapTextureFBO();
   }

   public void RenderWater(TextureDraw var1, int var2, int var3, boolean var4) {
      try {
         Util.checkGLError();
      } catch (Throwable var9) {
      }

      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      Matrix4f var5 = Core.getInstance().projectionMatrixStack.alloc();
      IsoWater.getInstance().waterProjection(var5);
      Core.getInstance().projectionMatrixStack.push(var5);
      PlayerCamera var6 = SpriteRenderer.instance.getRenderingPlayerCamera(var3);
      Matrix4f var7 = Core.getInstance().modelViewMatrixStack.alloc();
      var7.identity();
      Core.getInstance().modelViewMatrixStack.push(var7);
      ShaderHelper.glUseProgramObjectARB(var2);
      Shader var8 = (Shader)Shader.ShaderMap.get(var2);
      if (var8 instanceof WaterShader) {
         ((WaterShader)var8).updateWaterParams(var1, var3);
      }

      VertexBufferObject.setModelViewProjection(var8.getProgram());
      IsoWater.getInstance().waterGeometry(var4);
      ShaderHelper.glUseProgramObjectARB(0);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      if (!PZGLUtil.checkGLError(true)) {
         DebugLog.General.println("DEBUG: EXCEPTION RenderWater");
         PZGLUtil.printGLState(DebugLog.General);
      }

      GLStateRenderThread.restore();
   }

   public void RenderPuddles(int var1, int var2, int var3, int var4) {
      PZGLUtil.checkGLError(true);
      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      Matrix4f var5 = Core.getInstance().projectionMatrixStack.alloc();
      IsoPuddles.getInstance().puddlesProjection(var5);
      Core.getInstance().projectionMatrixStack.push(var5);
      Matrix4f var6 = Core.getInstance().modelViewMatrixStack.alloc();
      var6.identity();
      Core.getInstance().modelViewMatrixStack.push(var6);
      int var7 = IsoPuddles.getInstance().Effect.getID();
      ShaderHelper.glUseProgramObjectARB(var7);
      Shader var8 = IsoPuddles.getInstance().Effect;
      if (var8 instanceof PuddlesShader) {
         ((PuddlesShader)var8).updatePuddlesParams(var1, var2);
      }

      VertexBufferObject.setModelViewProjection(var8.getProgram());
      IsoPuddles.getInstance().puddlesGeometry(var3, var4);
      ShaderHelper.glUseProgramObjectARB(0);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      GLStateRenderThread.restore();
      if (!PZGLUtil.checkGLError(true)) {
         DebugLog.General.println("DEBUG: EXCEPTION RenderPuddles");
         PZGLUtil.printGLState(DebugLog.General);
      }

   }

   public void RenderParticles(TextureDraw var1, int var2, int var3) {
      int var4 = ParticlesFire.getInstance().getFireShaderID();
      int var5 = ParticlesFire.getInstance().getSmokeShaderID();
      int var6 = ParticlesFire.getInstance().getVapeShaderID();

      try {
         Util.checkGLError();
      } catch (Throwable var9) {
      }

      GL11.glPushClientAttrib(-1);
      GL11.glPushAttrib(1048575);
      GL11.glMatrixMode(5889);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      GL11.glViewport(0, 0, SpriteRenderer.instance.getRenderingPlayerCamera(var2).OffscreenWidth, SpriteRenderer.instance.getRenderingPlayerCamera(var2).OffscreenHeight);
      GL11.glMatrixMode(5888);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
      float var7 = ParticlesFire.getInstance().getShaderTime();
      GL11.glBlendFunc(770, 1);
      ShaderHelper.glUseProgramObjectARB(var4);
      Shader var8 = (Shader)Shader.ShaderMap.get(var4);
      if (var8 instanceof FireShader) {
         ((FireShader)var8).updateFireParams(var1, var2, var7);
      }

      ParticlesFire.getInstance().getGeometryFire(var3);
      GL11.glBlendFunc(770, 771);
      ShaderHelper.glUseProgramObjectARB(var5);
      var8 = (Shader)Shader.ShaderMap.get(var5);
      if (var8 instanceof SmokeShader) {
         ((SmokeShader)var8).updateSmokeParams(var1, var2, var7);
      }

      ParticlesFire.getInstance().getGeometry(var3);
      GL20.glUseProgram(0);
      GL11.glMatrixMode(5888);
      GL11.glPopMatrix();
      GL11.glMatrixMode(5889);
      GL11.glPopMatrix();
      GL11.glPopAttrib();
      GL11.glPopClientAttrib();
      Texture.lastTextureID = -1;
      GL11.glViewport(0, 0, SpriteRenderer.instance.getRenderingPlayerCamera(var2).OffscreenWidth, SpriteRenderer.instance.getRenderingPlayerCamera(var2).OffscreenHeight);
      if (!PZGLUtil.checkGLError(true)) {
         DebugLog.General.println("DEBUG: EXCEPTION RenderParticles");
         PZGLUtil.printGLState(DebugLog.General);
      }

   }

   public void Reset(IsoGameCharacter var1) {
      if (var1.legsSprite != null && var1.legsSprite.modelSlot != null) {
         ModelSlot var2 = var1.legsSprite.modelSlot;
         this.resetModelInstance(var2.model, var2);

         for(int var3 = 0; var3 < var2.sub.size(); ++var3) {
            ModelInstance var4 = (ModelInstance)var2.sub.get(var3);
            if (var4 != var1.primaryHandModel && var4 != var1.secondaryHandModel && !var2.attachedModels.contains(var4)) {
               this.resetModelInstanceRecurse(var4, var2);
            }
         }

         this.derefModelInstances(var1.getReadyModelData());
         var1.getReadyModelData().clear();
         this.dressInRandomOutfit(var1);
         Model var5 = this.getBodyModel(var1);
         var2.model = this.newInstance(var5, var1, var1.getAnimationPlayer());
         var2.model.setOwner(var2);
         var2.model.m_modelScript = var1.getVisual().getModelScript();
         this.DoCharacterModelParts(var1, var2);
      }
   }

   public void reloadAllOutfits() {
      Iterator var1 = this.m_contains.iterator();

      while(var1.hasNext()) {
         IsoGameCharacter var2 = (IsoGameCharacter)var1.next();
         var2.reloadOutfit();
      }

   }

   public void Add(IsoGameCharacter var1) {
      if (this.m_bCreated) {
         if (var1.isSceneCulled()) {
            if (this.ToRemove.contains(var1)) {
               this.ToRemove.remove(var1);
               var1.legsSprite.modelSlot.bRemove = false;
            } else {
               ModelSlot var2 = this.getSlot(var1);
               var2.framesSinceStart = 0;
               if (var2.model != null) {
                  ModelInstance var10000 = var2.model;
                  Objects.requireNonNull(var10000);
                  RenderThread.invokeOnRenderContext(var10000::destroySmartTextures);
               }

               this.dressInRandomOutfit(var1);
               Model var3 = this.getBodyModel(var1);
               var2.model = this.newInstance(var3, var1, var1.getAnimationPlayer());
               var2.model.setOwner(var2);
               var2.model.m_modelScript = var1.getVisual().getModelScript();
               this.DoCharacterModelParts(var1, var2);
               var2.active = true;
               var2.character = var1;
               var2.model.character = var1;
               var2.model.object = var1;
               var2.model.SetForceDir(var2.model.character.getForwardDirection());

               for(int var4 = 0; var4 < var2.sub.size(); ++var4) {
                  ModelInstance var5 = (ModelInstance)var2.sub.get(var4);
                  var5.character = var1;
                  var5.object = var1;
               }

               var1.legsSprite.modelSlot = var2;
               this.m_contains.add(var1);
               var1.onCullStateChanged(this, false);
               if (var2.model.AnimPlayer != null && var2.model.AnimPlayer.isBoneTransformsNeedFirstFrame()) {
                  try {
                     var2.Update(var1.getAnimationTimeDelta());
                  } catch (Throwable var6) {
                     ExceptionLogger.logException(var6);
                  }
               }

            }
         }
      }
   }

   public void dressInRandomOutfit(IsoGameCharacter var1) {
      IsoZombie var2 = (IsoZombie)Type.tryCastTo(var1, IsoZombie.class);
      if (var2 != null && !var2.isReanimatedPlayer() && !var2.wasFakeDead()) {
         if (DebugOptions.instance.ZombieOutfitRandom.getValue() && !var1.isPersistentOutfitInit()) {
            var2.bDressInRandomOutfit = true;
         }

         if (var2.bDressInRandomOutfit) {
            var2.getDescriptor().setForename(SurvivorFactory.getRandomForename(var2.isFemale()));
            var2.bDressInRandomOutfit = false;
            var2.dressInRandomOutfit();
         }

         if (!var1.isPersistentOutfitInit()) {
            var2.dressInPersistentOutfitID(var1.getPersistentOutfitID());
         }

      } else {
         if (GameClient.bClient && var2 != null && !var1.isPersistentOutfitInit() && var1.getPersistentOutfitID() != 0) {
            var2.dressInPersistentOutfitID(var1.getPersistentOutfitID());
         }

      }
   }

   public Model getBodyModel(IsoGameCharacter var1) {
      if (var1.isAnimal()) {
         return var1.getVisual().getModel();
      } else if (var1.isZombie() && ((IsoZombie)var1).isSkeleton()) {
         return var1.isFemale() ? this.m_skeletonFemaleModel : this.m_skeletonMaleModel;
      } else {
         return var1.isFemale() ? this.m_femaleModel : this.m_maleModel;
      }
   }

   public boolean ContainsChar(IsoGameCharacter var1) {
      return this.m_contains.contains(var1) && !this.ToRemove.contains(var1);
   }

   public void ResetCharacterEquippedHands(IsoGameCharacter var1) {
      if (var1 != null && var1.legsSprite != null && var1.legsSprite.modelSlot != null) {
         this.DoCharacterModelEquipped(var1, var1.legsSprite.modelSlot);
      }
   }

   public boolean shouldHideModel(ItemVisuals var1, String var2) {
      return BodyLocations.getGroup("Human").getLocation(var2) != null && PopTemplateManager.instance.isItemModelHidden(var1, var2);
   }

   private void DoCharacterModelEquipped(IsoGameCharacter var1, ModelSlot var2) {
      // $FF: Couldn't be decompiled
   }

   private ModelInstance addEquippedModelInstance(IsoGameCharacter var1, ModelSlot var2, InventoryItem var3, String var4, ItemReplacement var5, boolean var6) {
      HandWeapon var8 = (HandWeapon)Type.tryCastTo(var3, HandWeapon.class);
      ModelInstance var7;
      if (var3.getClothingItem() == null && var8 != null && var3.getClothingItem() == null) {
         String var9 = var8.getStaticModel();
         var7 = this.addStatic(var2, var9, var4);
         this.addWeaponPartModels(var2, var8, var7);
         if (Core.getInstance().getOptionSimpleWeaponTextures()) {
            return var7;
         } else {
            ModelInstanceTextureInitializer var10 = ModelInstanceTextureInitializer.alloc();
            var10.init(var7, var8);
            var7.setTextureInitializer(var10);
            return var7;
         }
      } else {
         if (var3 != null) {
            if (var5 != null && !StringUtils.isNullOrEmpty(var5.maskVariableValue) && (var5.clothingItem != null || !StringUtils.isNullOrWhitespace(var3.getStaticModel()))) {
               var7 = this.addMaskingModel(var2, var1, var3, var5, var5.maskVariableValue, var5.attachment, var4);
               return var7;
            }

            if (var6 && !StringUtils.isNullOrWhitespace(var3.getStaticModel())) {
               var7 = this.addStatic(var2, var3.getStaticModel(), var4);
               return var7;
            }
         }

         return null;
      }
   }

   private ModelInstance addMaskingModel(ModelSlot var1, IsoGameCharacter var2, InventoryItem var3, ItemReplacement var4, String var5, String var6, String var7) {
      ModelInstance var8 = null;
      ItemVisual var9 = var3.getVisual();
      if (var4.clothingItem != null && var9 != null) {
         var8 = PopTemplateManager.instance.addClothingItem(var2, var1, var9, var4.clothingItem);
      } else {
         if (StringUtils.isNullOrWhitespace(var3.getStaticModel())) {
            return null;
         }

         String var10 = null;
         if (var9 != null && var3.getClothingItem() != null) {
            var10 = (String)var3.getClothingItem().getTextureChoices().get(var9.getTextureChoice());
         }

         if (!StringUtils.isNullOrEmpty(var6)) {
            var8 = this.addStaticForcedTex(var1.model, var3.getStaticModel(), var6, var6, var10);
         } else {
            var8 = this.addStaticForcedTex(var1, var3.getStaticModel(), var7, var10);
         }

         var8.maskVariableValue = var5;
         if (var9 != null) {
            var8.tintR = var9.m_Tint.r;
            var8.tintG = var9.m_Tint.g;
            var8.tintB = var9.m_Tint.b;
         }
      }

      if (!StringUtils.isNullOrEmpty(var5)) {
         var2.setVariable(var4.maskVariableName, var5);
         var2.bUpdateEquippedTextures = true;
      }

      return var8;
   }

   private void addWeaponPartModels(ModelSlot var1, HandWeapon var2, ModelInstance var3) {
      ArrayList var4 = var2.getModelWeaponPart();
      if (var4 != null) {
         ArrayList var5 = var2.getAllWeaponParts(this.m_tempWeaponPartList);

         for(int var6 = 0; var6 < var5.size(); ++var6) {
            WeaponPart var7 = (WeaponPart)var5.get(var6);

            for(int var8 = 0; var8 < var4.size(); ++var8) {
               ModelWeaponPart var9 = (ModelWeaponPart)var4.get(var8);
               if (var7.getFullType().equals(var9.partType)) {
                  ModelInstance var10 = this.addStatic(var3, var9.modelName, var9.attachmentNameSelf, var9.attachmentParent);
                  var10.setOwner(var1);
               }
            }
         }
      }

   }

   public void resetModelInstance(ModelInstance var1, Object var2) {
      if (var1 != null) {
         var1.clearOwner(var2);
         if (var1.isRendering()) {
            var1.bResetAfterRender = true;
         } else {
            if (var1 instanceof VehicleModelInstance) {
               return;
            }

            if (var1 instanceof VehicleSubModelInstance) {
               return;
            }

            var1.reset();
            this.m_modelInstancePool.release((Object)var1);
         }

      }
   }

   public void resetModelInstanceRecurse(ModelInstance var1, Object var2) {
      if (var1 != null) {
         this.resetModelInstancesRecurse(var1.sub, var2);
         this.resetModelInstance(var1, var2);
      }
   }

   public void resetModelInstancesRecurse(ArrayList<ModelInstance> var1, Object var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         ModelInstance var4 = (ModelInstance)var1.get(var3);
         this.resetModelInstance(var4, var2);
      }

   }

   public void derefModelInstance(ModelInstance var1) {
      if (var1 != null) {
         assert var1.renderRefCount > 0;

         --var1.renderRefCount;
         if (var1.bResetAfterRender && !var1.isRendering()) {
            assert var1.getOwner() == null;

            if (var1 instanceof VehicleModelInstance) {
               return;
            }

            if (var1 instanceof VehicleSubModelInstance) {
               return;
            }

            var1.reset();
            this.m_modelInstancePool.release((Object)var1);
         }

      }
   }

   public void derefModelInstances(ArrayList<ModelInstance> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         ModelInstance var3 = (ModelInstance)var1.get(var2);
         this.derefModelInstance(var3);
      }

   }

   private void DoCharacterModelParts(IsoGameCharacter var1, ModelSlot var2) {
      if (var2.isRendering()) {
         boolean var3 = false;
      }

      if (var1 instanceof IsoAnimal) {
         var1.postUpdateModelTextures();
      } else {
         if (DebugLog.isEnabled(DebugType.Clothing)) {
            DebugLog.Clothing.debugln("Char: " + var1 + " Slot: " + var2);
         }

         var2.sub.clear();
         PopTemplateManager.instance.populateCharacterModelSlot(var1, var2);
         this.DoCharacterModelEquipped(var1, var2);
         var1.OnClothingUpdated();
         var1.OnEquipmentUpdated();
      }
   }

   public void update() {
      int var1;
      IsoGameCharacter var2;
      for(var1 = 0; var1 < this.ToResetNextFrame.size(); ++var1) {
         var2 = (IsoGameCharacter)this.ToResetNextFrame.get(var1);
         this.Reset(var2);
      }

      this.ToResetNextFrame.clear();

      for(var1 = 0; var1 < this.ToResetEquippedNextFrame.size(); ++var1) {
         var2 = (IsoGameCharacter)this.ToResetEquippedNextFrame.get(var1);
         this.ResetCharacterEquippedHands(var2);
      }

      this.ToResetEquippedNextFrame.clear();

      for(var1 = 0; var1 < this.ToRemove.size(); ++var1) {
         var2 = (IsoGameCharacter)this.ToRemove.get(var1);
         this.DoRemove(var2);
      }

      this.ToRemove.clear();

      for(var1 = 0; var1 < this.m_resetAfterRender.size(); ++var1) {
         ModelSlot var10 = (ModelSlot)this.m_resetAfterRender.get(var1);
         if (!var10.isRendering()) {
            var10.reset();
            this.m_resetAfterRender.remove(var1--);
         }
      }

      if (IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null) {
         ArrayList var12 = IsoWorld.instance.CurrentCell.getVehicles();

         for(int var11 = 0; var11 < var12.size(); ++var11) {
            BaseVehicle var3 = (BaseVehicle)var12.get(var11);
            IsoGridSquare var4 = var3.getCurrentSquare();
            if (var4 != null && var3.sprite != null && var3.sprite.hasActiveModel()) {
               VehicleModelInstance var5 = (VehicleModelInstance)var3.sprite.modelSlot.model;
               int var6 = -1;

               for(int var7 = 0; var7 < IsoPlayer.numPlayers; ++var7) {
                  IsoPlayer var8 = IsoPlayer.players[var7];
                  if (var8 != null) {
                     boolean var9 = var8.getVehicle() == var3;
                     if ((var9 || var4.lighting[var7].bSeen()) && (var9 || var4.lighting[var7].bCouldSee())) {
                        var6 = var7;
                        break;
                     }
                  }
               }

               if (var6 == -1) {
                  ModelInstance.EffectLight[] var13 = var5.getLights();

                  for(int var14 = 0; var14 < var13.length; ++var14) {
                     var13[var14].clear();
                  }
               } else {
                  var5.UpdateLights(var6);
               }
            }
         }
      }

   }

   private ModelSlot addNewSlot(IsoGameCharacter var1) {
      ModelSlot var2 = new ModelSlot(this.m_modelSlots.size(), (ModelInstance)null, var1);
      this.m_modelSlots.add(var2);
      return var2;
   }

   public ModelSlot getSlot(IsoGameCharacter var1) {
      for(int var2 = 0; var2 < this.m_modelSlots.size(); ++var2) {
         ModelSlot var3 = (ModelSlot)this.m_modelSlots.get(var2);
         if (!var3.bRemove && !var3.isRendering() && !var3.active) {
            return var3;
         }
      }

      return this.addNewSlot(var1);
   }

   private boolean DoRemove(IsoGameCharacter var1) {
      if (!this.m_contains.contains(var1)) {
         return false;
      } else {
         boolean var2 = false;

         for(int var3 = 0; var3 < this.m_modelSlots.size(); ++var3) {
            ModelSlot var4 = (ModelSlot)this.m_modelSlots.get(var3);
            if (var4.character == var1) {
               var1.legsSprite.modelSlot = null;
               this.m_contains.remove(var1);
               if (!var1.isSceneCulled()) {
                  var1.onCullStateChanged(this, true);
               }

               if (!this.m_resetAfterRender.contains(var4)) {
                  this.m_resetAfterRender.add(var4);
               }

               var2 = true;
            }
         }

         return var2;
      }
   }

   public void Remove(IsoGameCharacter var1) {
      if (!var1.isSceneCulled()) {
         if (!this.ToRemove.contains(var1)) {
            var1.legsSprite.modelSlot.bRemove = true;
            this.ToRemove.add(var1);
            var1.onCullStateChanged(this, true);
         } else if (this.ContainsChar(var1)) {
            throw new IllegalStateException("IsoGameCharacter.isSceneCulled() = true inconsistent with ModelManager.ContainsChar() = true");
         }

      }
   }

   public void Remove(BaseVehicle var1) {
      if (var1.sprite != null && var1.sprite.modelSlot != null) {
         ModelSlot var2 = var1.sprite.modelSlot;
         if (!this.m_resetAfterRender.contains(var2)) {
            this.m_resetAfterRender.add(var2);
         }

         var1.sprite.modelSlot = null;
      }

   }

   public void ResetNextFrame(IsoGameCharacter var1) {
      if (!this.ToResetNextFrame.contains(var1)) {
         this.ToResetNextFrame.add(var1);
      }
   }

   public void ResetEquippedNextFrame(IsoGameCharacter var1) {
      if (!this.ToResetEquippedNextFrame.contains(var1)) {
         this.ToResetEquippedNextFrame.add(var1);
      }
   }

   public void Reset() {
      RenderThread.invokeOnRenderContext(() -> {
         Iterator var1 = this.ToRemove.iterator();

         while(var1.hasNext()) {
            IsoGameCharacter var2 = (IsoGameCharacter)var1.next();
            this.DoRemove(var2);
         }

         this.ToRemove.clear();

         try {
            if (!this.m_contains.isEmpty()) {
               IsoGameCharacter[] var7 = (IsoGameCharacter[])this.m_contains.toArray(new IsoGameCharacter[0]);
               IsoGameCharacter[] var8 = var7;
               int var3 = var7.length;

               for(int var4 = 0; var4 < var3; ++var4) {
                  IsoGameCharacter var5 = var8[var4];
                  this.DoRemove(var5);
               }
            }

            this.m_modelSlots.clear();
         } catch (Exception var6) {
            DebugType.ModelManager.error("Exception thrown removing Models.");
            ExceptionLogger.logException(var6);
         }

      });
   }

   public void getSquareLighting(int var1, IsoMovingObject var2, ModelInstance.EffectLight[] var3) {
      for(int var4 = 0; var4 < var3.length; ++var4) {
         var3[var4].clear();
      }

      IsoGridSquare var9 = var2.getCurrentSquare();
      if (var9 != null) {
         IsoGridSquare.ILighting var5 = var9.lighting[var1];
         int var6 = var5.resultLightCount();

         for(int var7 = 0; var7 < var6; ++var7) {
            IsoGridSquare.ResultLight var8 = var5.getResultLight(var7);
            var3[var7].x = (float)var8.x;
            var3[var7].y = (float)var8.y;
            var3[var7].z = (float)var8.z;
            var3[var7].r = var8.r;
            var3[var7].g = var8.g;
            var3[var7].b = var8.b;
            var3[var7].radius = var8.radius;
            if (var7 == var3.length - 1) {
               break;
            }
         }

      }
   }

   public void addVehicle(BaseVehicle var1) {
      if (this.m_bCreated) {
         if (!GameServer.bServer || ServerGUI.isCreated()) {
            if (var1 != null && var1.getScript() != null) {
               VehicleScript var2 = var1.getScript();
               String var3 = var1.getScript().getModel().file;
               Model var4 = this.getLoadedModel(var3);
               if (var4 == null) {
                  DebugType.ModelManager.error("Failed to find vehicle model: %s", var3);
               } else {
                  if (DebugLog.isEnabled(DebugType.Animation)) {
                     DebugType.ModelManager.debugln("%s", var3);
                  }

                  VehicleModelInstance var5 = new VehicleModelInstance();
                  var5.init(var4, (IsoGameCharacter)null, var1.getAnimationPlayer());
                  var5.applyModelScriptScale(var3);
                  var1.getSkin();
                  VehicleScript.Skin var6 = var2.getTextures();
                  if (var1.getSkinIndex() >= 0 && var1.getSkinIndex() < var2.getSkinCount()) {
                     var6 = var2.getSkin(var1.getSkinIndex());
                  }

                  var5.LoadTexture(var6.texture);
                  var5.tex = var6.textureData;
                  var5.textureMask = var6.textureDataMask;
                  var5.textureDamage1Overlay = var6.textureDataDamage1Overlay;
                  var5.textureDamage1Shell = var6.textureDataDamage1Shell;
                  var5.textureDamage2Overlay = var6.textureDataDamage2Overlay;
                  var5.textureDamage2Shell = var6.textureDataDamage2Shell;
                  var5.textureLights = var6.textureDataLights;
                  var5.textureRust = var6.textureDataRust;
                  if (var5.tex != null) {
                     var5.tex.bindAlways = true;
                  } else {
                     DebugType.ModelManager.error("texture not found:", var1.getSkin());
                  }

                  ModelSlot var7 = this.getSlot((IsoGameCharacter)null);
                  var7.model = var5;
                  var5.setOwner(var7);
                  var5.object = var1;
                  var7.sub.clear();

                  for(int var8 = 0; var8 < var1.models.size(); ++var8) {
                     BaseVehicle.ModelInfo var9 = (BaseVehicle.ModelInfo)var1.models.get(var8);
                     Model var10 = this.getLoadedModel(var9.scriptModel.file == null ? var9.modelScript.name : var9.scriptModel.file);
                     if (var10 == null) {
                        DebugType.ModelManager.error("vehicle.models[%d] not found: %s", var8, var9.scriptModel.file);
                     } else {
                        VehicleSubModelInstance var11 = new VehicleSubModelInstance();
                        var11.init(var10, (IsoGameCharacter)null, var9.getAnimationPlayer());
                        var11.setOwner(var7);
                        var11.applyModelScriptScale(var9.scriptModel.file == null ? var9.modelScript.name : var9.scriptModel.file);
                        var11.object = var1;
                        var11.parent = var5;
                        var5.sub.add(var11);
                        var11.modelInfo = var9;
                        if (var11.tex == null) {
                           var11.tex = var5.tex;
                        }

                        var7.sub.add(var11);
                        var9.modelInstance = var11;
                     }
                  }

                  var7.active = true;
                  var1.sprite.modelSlot = var7;
               }
            }
         }
      }
   }

   public ModelInstance addStatic(ModelSlot var1, String var2, String var3, String var4, String var5) {
      ModelInstance var6 = this.newStaticInstance(var1, var2, var3, var4, var5);
      if (var6 == null) {
         return null;
      } else {
         var1.sub.add(var6);
         var6.setOwner(var1);
         var1.model.sub.add(var6);
         return var6;
      }
   }

   public ModelInstance newStaticInstance(ModelSlot var1, String var2, String var3, String var4, String var5) {
      if (DebugLog.isEnabled(DebugType.Animation)) {
         DebugType.ModelManager.debugln("Adding Static Model:" + var2);
      }

      Model var6 = this.tryGetLoadedModel(var2, var3, true, var5, false);
      if (var6 == null && var2 != null) {
         this.loadStaticModel(var2, var3, var5);
         var6 = this.getLoadedModel(var2, var3, true, var5);
         if (var6 == null) {
            if (DebugLog.isEnabled(DebugType.Animation)) {
               DebugType.ModelManager.error("Model not found. model:" + var2 + " tex:" + var3);
            }

            return null;
         }
      }

      if (var2 == null) {
         var6 = this.tryGetLoadedModel("vehicles_wheel02", "vehicles/vehicle_wheel02", true, "vehiclewheel", false);
      }

      ModelInstance var7 = this.newInstance(var6, var1.character, var1.model.AnimPlayer);
      var7.parent = var1.model;
      if (var1.model.AnimPlayer != null) {
         var7.parentBone = var1.model.AnimPlayer.getSkinningBoneIndex(var4, var7.parentBone);
         var7.parentBoneName = var4;
      }

      var7.AnimPlayer = var1.model.AnimPlayer;
      return var7;
   }

   private ModelInstance addStatic(ModelSlot var1, String var2, String var3) {
      return this.addStaticForcedTex(var1, var2, var3, (String)null);
   }

   private ModelInstance addStaticForcedTex(ModelSlot var1, String var2, String var3, String var4) {
      String var5 = ScriptManager.getItemName(var2);
      String var6 = ScriptManager.getItemName(var2);
      String var7 = null;
      ModelMetaData var8 = (ModelMetaData)modelMetaData.get(var2);
      if (var8 != null) {
         if (!StringUtils.isNullOrWhitespace(var8.meshName)) {
            var5 = var8.meshName;
         }

         if (!StringUtils.isNullOrWhitespace(var8.textureName)) {
            var6 = var8.textureName;
         }

         if (!StringUtils.isNullOrWhitespace(var8.shaderName)) {
            var7 = var8.shaderName;
         }
      }

      if (!StringUtils.isNullOrEmpty(var4)) {
         var6 = var4;
      }

      ModelScript var9 = ScriptManager.instance.getModelScript(var2);
      if (var9 != null) {
         var5 = var9.getMeshName();
         var6 = var9.getTextureName();
         var7 = var9.getShaderName();
         ModelInstance var10 = this.addStatic(var1, var5, var6, var3, var7);
         if (var10 != null) {
            var10.applyModelScriptScale(var2);
         }

         return var10;
      } else {
         return this.addStatic(var1, var5, var6, var3, var7);
      }
   }

   public ModelInstance addStatic(ModelInstance var1, String var2, String var3, String var4) {
      return this.addStaticForcedTex(var1, var2, var3, var4, (String)null);
   }

   public ModelInstance addStaticForcedTex(ModelInstance var1, String var2, String var3, String var4, String var5) {
      String var6 = ScriptManager.getItemName(var2);
      String var7 = ScriptManager.getItemName(var2);
      String var8 = null;
      ModelScript var9 = ScriptManager.instance.getModelScript(var2);
      if (var9 != null) {
         var6 = var9.getMeshName();
         var7 = var9.getTextureName();
         var8 = var9.getShaderName();
      }

      if (!StringUtils.isNullOrEmpty(var5)) {
         var7 = var5;
      }

      Model var10 = this.tryGetLoadedModel(var6, var7, true, var8, false);
      if (var10 == null && var6 != null) {
         this.loadStaticModel(var6, var7, var8);
         var10 = this.getLoadedModel(var6, var7, true, var8);
         if (var10 == null) {
            if (DebugLog.isEnabled(DebugType.Animation)) {
               DebugType.ModelManager.error("Model not found. model:" + var6 + " tex:" + var7);
            }

            return null;
         }
      }

      if (var6 == null) {
         var10 = this.tryGetLoadedModel("vehicles_wheel02", "vehicles/vehicle_wheel02", true, "vehiclewheel", false);
      }

      if (var10 == null) {
         return null;
      } else {
         ModelInstance var11 = (ModelInstance)this.m_modelInstancePool.alloc();
         if (var1 != null) {
            var11.init(var10, var1.character, var1.AnimPlayer);
            var11.parent = var1;
            var1.sub.add(var11);
         } else {
            var11.init(var10, (IsoGameCharacter)null, (AnimationPlayer)null);
         }

         if (var9 != null) {
            var11.applyModelScriptScale(var2);
         }

         var11.attachmentNameSelf = var3;
         var11.attachmentNameParent = var4;
         return var11;
      }
   }

   private String modifyShaderName(String var1) {
      if ((StringUtils.equals(var1, "vehicle") || StringUtils.equals(var1, "vehicle_multiuv") || StringUtils.equals(var1, "vehicle_norandom_multiuv")) && !Core.getInstance().getPerfReflectionsOnLoad()) {
         var1 = var1 + "_noreflect";
      }

      return var1;
   }

   private Model loadModelInternal(String var1, String var2, String var3, ModelMesh var4, boolean var5, String var6) {
      var3 = this.modifyShaderName(var3);
      Model.ModelAssetParams var7 = new Model.ModelAssetParams();
      var7.animationsModel = var4;
      var7.bStatic = var5;
      var7.meshName = var1;
      var7.shaderName = var3;
      var7.textureName = var2;
      var7.postProcess = var6;
      if (var3 != null && StringUtils.startsWithIgnoreCase(var3, "vehicle")) {
         var7.textureFlags = TextureID.bUseCompression ? 4 : 0;
         var7.textureFlags |= 256;
      } else {
         var7.textureFlags = this.getTextureFlags();
      }

      if (!this.shouldLimitTextureSize(var2)) {
         var7.textureFlags &= -385;
      }

      String var8 = this.createModelKey(var1, var2, var5, var3);
      Model var9 = (Model)ModelAssetManager.instance.load(new AssetPath(var8), var7);
      if (var9 != null) {
         this.putLoadedModel(var1, var2, var5, var3, var9);
      }

      return var9;
   }

   public int getTextureFlags() {
      int var1 = TextureID.bUseCompression ? 4 : 0;
      if (Core.getInstance().getOptionModelTextureMipmaps()) {
      }

      var1 |= 128;
      return var1;
   }

   public boolean shouldLimitTextureSize(String var1) {
      if (var1 == null) {
         return true;
      } else {
         return !var1.equals("IsoObject/MODELS_fixtures_doors_fences_01");
      }
   }

   public void setModelMetaData(String var1, String var2, String var3, boolean var4) {
      this.setModelMetaData(var1, var1, var2, var3, var4);
   }

   public void setModelMetaData(String var1, String var2, String var3, String var4, boolean var5) {
      ModelMetaData var6 = new ModelMetaData();
      var6.meshName = var2;
      var6.textureName = var3;
      var6.shaderName = var4;
      var6.bStatic = var5;
      modelMetaData.put(var1, var6);
   }

   public Model loadStaticModel(String var1, String var2, String var3) {
      String var4 = this.modifyShaderName(var3);
      return this.loadModelInternal(var1, var2, var4, (ModelMesh)null, true, (String)null);
   }

   public Model loadModel(String var1, String var2, ModelMesh var3, String var4) {
      return this.loadModelInternal(var1, var2, var4, var3, false, (String)null);
   }

   private Model loadModel(String var1, String var2, ModelMesh var3) {
      return this.loadModelInternal(var1, var2, "basicEffect", var3, false, (String)null);
   }

   public Model getLoadedModel(String var1) {
      ModelScript var2 = ScriptManager.instance.getModelScript(var1);
      if (var2 != null) {
         if (var2.loadedModel != null) {
            return var2.loadedModel;
         } else {
            var2.shaderName = this.modifyShaderName(var2.shaderName);
            Model var10 = this.tryGetLoadedModel(var2.getMeshName(), var2.getTextureName(), var2.bStatic, var2.getShaderName(), false);
            if (var10 != null) {
               var2.loadedModel = var10;
               return var10;
            } else {
               AnimationsMesh var11 = var2.animationsMesh == null ? null : ScriptManager.instance.getAnimationsMesh(var2.animationsMesh);
               ModelMesh var12 = var11 == null ? null : var11.modelMesh;
               var10 = var2.bStatic ? this.loadModelInternal(var2.getMeshName(), var2.getTextureName(), var2.getShaderName(), (ModelMesh)null, true, var2.postProcess) : this.loadModelInternal(var2.getMeshName(), var2.getTextureName(), var2.getShaderName(), var12, false, var2.postProcess);
               var2.loadedModel = var10;
               return var10;
            }
         }
      } else {
         ModelMetaData var3 = (ModelMetaData)modelMetaData.get(var1);
         Model var4;
         if (var3 != null) {
            var3.shaderName = this.modifyShaderName(var3.shaderName);
            var4 = this.tryGetLoadedModel(var3.meshName, var3.textureName, var3.bStatic, var3.shaderName, false);
            if (var4 != null) {
               return var4;
            } else {
               return var3.bStatic ? this.loadStaticModel(var3.meshName, var3.textureName, var3.shaderName) : this.loadModel(var3.meshName, var3.textureName, this.m_animModel);
            }
         } else {
            var4 = this.tryGetLoadedModel(var1, (String)null, false, (String)null, false);
            if (var4 != null) {
               return var4;
            } else {
               String var5 = var1.toLowerCase().trim();
               Iterator var6 = this.m_modelMap.entrySet().iterator();

               while(var6.hasNext()) {
                  Map.Entry var7 = (Map.Entry)var6.next();
                  String var8 = (String)var7.getKey();
                  if (var8.startsWith(var5)) {
                     Model var9 = (Model)var7.getValue();
                     if (var9 != null && (var8.length() == var5.length() || var8.charAt(var5.length()) == '&')) {
                        var4 = var9;
                        break;
                     }
                  }
               }

               if (var4 == null && DebugLog.isEnabled(DebugType.Animation)) {
                  DebugType.ModelManager.error("ModelManager.getLoadedModel> Model missing for key=\"" + var5 + "\"");
               }

               return var4;
            }
         }
      }
   }

   public Model getLoadedModel(String var1, String var2, boolean var3, String var4) {
      return this.tryGetLoadedModel(var1, var2, var3, var4, true);
   }

   public Model tryGetLoadedModel(String var1, String var2, boolean var3, String var4, boolean var5) {
      String var6 = this.createModelKey(var1, var2, var3, var4);
      if (var6 == null) {
         return null;
      } else {
         Model var7 = (Model)this.m_modelMap.get(var6);
         if (var7 == null && var5 && DebugLog.isEnabled(DebugType.Animation)) {
            DebugType.ModelManager.error("ModelManager.getLoadedModel> Model missing for key=\"" + var6 + "\"");
         }

         return var7;
      }
   }

   public void putLoadedModel(String var1, String var2, boolean var3, String var4, Model var5) {
      String var6 = this.createModelKey(var1, var2, var3, var4);
      if (var6 != null) {
         Model var7 = (Model)this.m_modelMap.get(var6);
         if (var7 != var5) {
            if (var7 != null) {
               DebugType.ModelManager.debugln("Override key=\"%s\" old=%s new=%s", var6, var7, var5);
            } else {
               DebugType.ModelManager.debugln("key=\"%s\" model=%s", var6, var5);
            }

            this.m_modelMap.put(var6, var5);
            var5.Name = var6;
         }
      }
   }

   private String createModelKey(String var1, String var2, boolean var3, String var4) {
      builder.delete(0, builder.length());
      if (var1 == null) {
         return null;
      } else {
         if (!toLowerKeyRoot.containsKey(var1)) {
            toLowerKeyRoot.put(var1, var1.toLowerCase(Locale.ENGLISH).trim());
         }

         builder.append((String)toLowerKeyRoot.get(var1));
         builder.append(amp);
         if (StringUtils.isNullOrWhitespace(var4)) {
            var4 = basicEffect;
         }

         builder.append(shaderEquals);
         if (!toLower.containsKey(var4)) {
            toLower.put(var4, var4.toLowerCase().trim());
         }

         builder.append((String)toLower.get(var4));
         if (!StringUtils.isNullOrWhitespace(var2)) {
            builder.append(texA);
            if (!toLowerTex.containsKey(var2)) {
               toLowerTex.put(var2, var2.toLowerCase().trim());
            }

            builder.append((String)toLowerTex.get(var2));
         }

         if (var3) {
            builder.append(isStaticTrue);
         }

         return builder.toString();
      }
   }

   private String createModelKey2(String var1, String var2, boolean var3, String var4) {
      if (var1 == null) {
         return null;
      } else {
         if (StringUtils.isNullOrWhitespace(var4)) {
            var4 = "basicEffect";
         }

         String var5 = "shader=" + var4.toLowerCase().trim();
         if (!StringUtils.isNullOrWhitespace(var2)) {
            var5 = var5 + ";tex=" + var2.toLowerCase().trim();
         }

         if (var3) {
            var5 = var5 + ";isStatic=true";
         }

         String var6 = var1.toLowerCase(Locale.ENGLISH).trim();
         return var6 + "&" + var5;
      }
   }

   private AnimationAsset loadAnim(String var1, ModelMesh var2, ModAnimations var3) {
      DebugType.ModelManager.debugln("Adding asset to queue: %s", var1);
      AnimationAsset.AnimationAssetParams var4 = new AnimationAsset.AnimationAssetParams();
      var4.animationsMesh = var2;
      AnimationAsset var5 = (AnimationAsset)AnimationAssetManager.instance.load(new AssetPath(var1), var4);
      var5.skinningData = var2.skinningData;
      this.putAnimationAsset(var1, var5, var3);
      return var5;
   }

   private void putAnimationAsset(String var1, AnimationAsset var2, ModAnimations var3) {
      String var4 = var1.toLowerCase();
      AnimationAsset var5 = (AnimationAsset)var3.m_animationAssetMap.getOrDefault(var4, (Object)null);
      if (var5 != null) {
         DebugType.ModelManager.debugln("Overwriting asset: %s", this.animAssetToString(var5));
         DebugType.ModelManager.debugln("New asset        : %s", this.animAssetToString(var2));
         var3.m_animationAssetList.remove(var5);
      }

      var2.modelManagerKey = var4;
      var2.modAnimations = var3;
      var3.m_animationAssetMap.put(var4, var2);
      var3.m_animationAssetList.add(var2);
   }

   private String animAssetToString(AnimationAsset var1) {
      if (var1 == null) {
         return "null";
      } else {
         AssetPath var2 = var1.getPath();
         return var2 == null ? "null-path" : String.valueOf(var2.getPath());
      }
   }

   public AnimationAsset getAnimationAsset(String var1) {
      String var2 = var1.toLowerCase(Locale.ENGLISH);
      return (AnimationAsset)this.m_animationAssets.get(var2);
   }

   private AnimationAsset getAnimationAssetRequired(String var1) {
      AnimationAsset var2 = this.getAnimationAsset(var1);
      if (var2 == null) {
         throw new NullPointerException("Required Animation Asset not found: " + var1);
      } else {
         return var2;
      }
   }

   public void addAnimationClip(String var1, AnimationClip var2) {
      this.m_animModel.skinningData.AnimationClips.put(var1, var2);
   }

   public AnimationClip getAnimationClip(String var1) {
      return (AnimationClip)this.m_animModel.skinningData.AnimationClips.get(var1);
   }

   public Collection<AnimationClip> getAllAnimationClips() {
      return this.m_animModel.skinningData.AnimationClips.values();
   }

   public ModelInstance newInstance(Model var1, IsoGameCharacter var2, AnimationPlayer var3) {
      if (var1 == null) {
         System.err.println("ModelManager.newInstance> Model is null.");
         return null;
      } else {
         ModelInstance var4 = (ModelInstance)this.m_modelInstancePool.alloc();
         var4.init(var1, var2, var3);
         return var4;
      }
   }

   public boolean isLoadingAnimations() {
      Iterator var1 = this.m_animationAssets.values().iterator();

      AnimationAsset var2;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         var2 = (AnimationAsset)var1.next();
      } while(!var2.isEmpty());

      return true;
   }

   public void reloadModelsMatching(String var1) {
      var1 = var1.toLowerCase(Locale.ENGLISH);
      Set var2 = this.m_modelMap.keySet();
      Iterator var3 = var2.iterator();

      while(var3.hasNext()) {
         String var4 = (String)var3.next();
         if (var4.contains(var1)) {
            Model var5 = (Model)this.m_modelMap.get(var4);
            if (!var5.isEmpty()) {
               DebugLog.General.printf("reloading model %s\n", var4);
               ModelMesh.MeshAssetParams var6 = new ModelMesh.MeshAssetParams();
               var6.animationsMesh = var5.Mesh.m_animationsMesh;
               var6.postProcess = var5.Mesh.postProcess;
               if (var5.Mesh.vb == null) {
                  var6.bStatic = var4.contains(";isStatic=true");
               } else {
                  var6.bStatic = var5.Mesh.vb.bStatic;
               }

               MeshAssetManager.instance.reload(var5.Mesh, var6);
            }
         }
      }

   }

   public void loadModAnimations() {
      Iterator var1 = this.m_modAnimations.values().iterator();

      while(var1.hasNext()) {
         ModAnimations var2 = (ModAnimations)var1.next();
         var2.setPriority(var2 == this.m_gameAnimations ? 0 : -1);
      }

      ArrayList var12 = ScriptManager.instance.getAllAnimationsMeshes();
      ArrayList var13 = ZomboidFileSystem.instance.getModIDs();

      for(int var3 = 0; var3 < var13.size(); ++var3) {
         String var4 = (String)var13.get(var3);
         ChooseGameInfo.Mod var5 = ChooseGameInfo.getAvailableModDetails(var4);
         if (var5 != null && (var5.animsXFile.common.absoluteFile.isDirectory() || var5.animsXFile.version.absoluteFile.isDirectory())) {
            ModAnimations var6 = (ModAnimations)this.m_modAnimations.get(var4);
            if (var6 != null) {
               var6.setPriority(var3 + 1);
            } else {
               var6 = new ModAnimations(var4);
               var6.setPriority(var3 + 1);
               this.m_modAnimations.put(var4, var6);
               Iterator var7 = var12.iterator();

               while(var7.hasNext()) {
                  AnimationsMesh var8 = (AnimationsMesh)var7.next();
                  Iterator var9 = var8.animationDirectories.iterator();

                  while(var9.hasNext()) {
                     String var10 = (String)var9.next();
                     if (var8.modelMesh.isReady()) {
                        File var11 = new File(var5.animsXFile.common.canonicalFile, var10);
                        if (var11.exists()) {
                           this.loadAnimsFromDir(var5.baseFile.common.lowercaseURI, var5.mediaFile.common.lowercaseURI, var11, var8.modelMesh, var6, var8.animationPrefixes);
                        }

                        var11 = new File(var5.animsXFile.version.canonicalFile, var10);
                        if (var11.exists()) {
                           this.loadAnimsFromDir(var5.baseFile.version.lowercaseURI, var5.mediaFile.version.lowercaseURI, var11, var8.modelMesh, var6, var8.animationPrefixes);
                        }
                     }
                  }
               }

               this.loadHumanAnimations(var5, var6);
            }
         }
      }

      this.setActiveAnimations();
   }

   void setActiveAnimations() {
      this.m_animationAssets.clear();
      ArrayList var1 = ScriptManager.instance.getAllAnimationsMeshes();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         AnimationsMesh var3 = (AnimationsMesh)var2.next();
         if (var3.modelMesh.isReady()) {
            var3.modelMesh.skinningData.AnimationClips.clear();
            if (var3.bKeepMeshAnimations) {
               var3.modelMesh.skinningData.AnimationClips.putAll(var3.modelMesh.meshAnimationClips);
            }
         }
      }

      var2 = this.m_modAnimations.values().iterator();

      label47:
      while(true) {
         ModAnimations var7;
         do {
            if (!var2.hasNext()) {
               return;
            }

            var7 = (ModAnimations)var2.next();
         } while(!var7.isActive());

         Iterator var4 = var7.m_animationAssetList.iterator();

         while(true) {
            AnimationAsset var5;
            AnimationAsset var6;
            do {
               if (!var4.hasNext()) {
                  continue label47;
               }

               var5 = (AnimationAsset)var4.next();
               var6 = (AnimationAsset)this.m_animationAssets.get(var5.modelManagerKey);
            } while(var6 != null && var6 != var5 && var6.modAnimations.m_priority > var7.m_priority);

            this.m_animationAssets.put(var5.modelManagerKey, var5);
            if (var5.isReady()) {
               var5.skinningData.AnimationClips.putAll(var5.AnimationClips);
            }
         }
      }
   }

   public void animationAssetLoaded(AnimationAsset var1) {
      if (var1.modAnimations.isActive()) {
         AnimationAsset var2 = (AnimationAsset)this.m_animationAssets.get(var1.modelManagerKey);
         if (var2 == null || var2 == var1 || var2.modAnimations.m_priority <= var1.modAnimations.m_priority) {
            this.m_animationAssets.put(var1.modelManagerKey, var1);
            var1.skinningData.AnimationClips.putAll(var1.AnimationClips);
         }
      }
   }

   public void initAnimationMeshes(boolean var1) {
      ArrayList var2 = ScriptManager.instance.getAllAnimationsMeshes();

      Iterator var3;
      AnimationsMesh var4;
      for(var3 = var2.iterator(); var3.hasNext(); var4.modelMesh.m_animationsMesh = var4.modelMesh) {
         var4 = (AnimationsMesh)var3.next();
         ModelMesh.MeshAssetParams var5 = new ModelMesh.MeshAssetParams();
         var5.bStatic = false;
         var5.animationsMesh = null;
         var5.postProcess = var4.postProcess;
         var4.modelMesh = (ModelMesh)MeshAssetManager.instance.getAssetTable().get(var4.meshFile);
         if (var4.modelMesh == null) {
            var4.modelMesh = (ModelMesh)MeshAssetManager.instance.load(new AssetPath(var4.meshFile), var5);
         }
      }

      if (!var1) {
         while(this.isLoadingAnimationMeshes()) {
            GameWindow.fileSystem.updateAsyncTransactions();

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var8) {
            }

            if (!GameServer.bServer) {
               Core.getInstance().StartFrame();
               Core.getInstance().EndFrame();
               Core.getInstance().StartFrameUI();
               Core.getInstance().EndFrameUI();
            }
         }

         var3 = var2.iterator();

         while(var3.hasNext()) {
            var4 = (AnimationsMesh)var3.next();
            Iterator var9 = var4.animationDirectories.iterator();

            while(var9.hasNext()) {
               String var6 = (String)var9.next();
               if (var4.modelMesh.isReady()) {
                  File var7 = new File(ZomboidFileSystem.instance.getAnimsXFile(), var6);
                  if (var7.exists()) {
                     this.loadAnimsFromDir("media/anims_X/" + var6, var4.modelMesh, var4.animationPrefixes);
                  }
               }
            }
         }

      }
   }

   private boolean isLoadingAnimationMeshes() {
      ArrayList var1 = ScriptManager.instance.getAllAnimationsMeshes();
      Iterator var2 = var1.iterator();

      AnimationsMesh var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (AnimationsMesh)var2.next();
      } while(var3.modelMesh.isFailure() || var3.modelMesh.isReady());

      return true;
   }

   private void loadHumanAnimations(ChooseGameInfo.Mod var1, ModAnimations var2) {
      AnimationsMesh var3 = ScriptManager.instance.getAnimationsMesh("Human");
      if (var3 != null && var3.modelMesh != null && var3.modelMesh.isReady()) {
         File[] var4 = var1.animsXFile.common.canonicalFile.listFiles();
         if (var4 != null) {
            URI var5 = var1.animsXFile.common.lowercaseURI;
            File[] var6 = var4;
            int var7 = var4.length;

            int var8;
            File var9;
            String var10;
            for(var8 = 0; var8 < var7; ++var8) {
               var9 = var6[var8];
               if (var9.isDirectory()) {
                  if (!this.isAnimationsMeshDirectory(var9.getName())) {
                     this.loadAnimsFromDir(var1.baseFile.common.lowercaseURI, var1.mediaFile.common.lowercaseURI, var9, var3.modelMesh, var2, (ArrayList)null);
                  }
               } else {
                  var10 = ZomboidFileSystem.instance.getAnimName(var5, var9);
                  this.loadAnim(var10, var3.modelMesh, var2);
               }
            }

            var4 = var1.animsXFile.version.canonicalFile.listFiles();
            if (var4 != null) {
               var5 = var1.animsXFile.version.lowercaseURI;
               var6 = var4;
               var7 = var4.length;

               for(var8 = 0; var8 < var7; ++var8) {
                  var9 = var6[var8];
                  if (var9.isDirectory()) {
                     if (!this.isAnimationsMeshDirectory(var9.getName())) {
                        this.loadAnimsFromDir(var1.baseFile.version.lowercaseURI, var1.mediaFile.version.lowercaseURI, var9, var3.modelMesh, var2, (ArrayList)null);
                     }
                  } else {
                     var10 = ZomboidFileSystem.instance.getAnimName(var5, var9);
                     this.loadAnim(var10, var3.modelMesh, var2);
                  }
               }

            }
         }
      }
   }

   private boolean isAnimationsMeshDirectory(String var1) {
      ArrayList var2 = ScriptManager.instance.getAllAnimationsMeshes();
      Iterator var3 = var2.iterator();

      AnimationsMesh var4;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         var4 = (AnimationsMesh)var3.next();
      } while(!var4.animationDirectories.contains(var1));

      return true;
   }

   static {
      modelMetaData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
      basicEffect = "basicEffect";
      isStaticTrue = ";isStatic=true";
      shaderEquals = "shader=";
      texA = ";tex=";
      amp = "&";
      toLower = new HashMap();
      toLowerTex = new HashMap();
      toLowerKeyRoot = new HashMap();
      builder = new StringBuilder();
   }

   public static final class ModAnimations {
      public final String m_modID;
      public final ArrayList<AnimationAsset> m_animationAssetList = new ArrayList();
      public final HashMap<String, AnimationAsset> m_animationAssetMap = new HashMap();
      public int m_priority;

      public ModAnimations(String var1) {
         this.m_modID = var1;
      }

      public void setPriority(int var1) {
         assert var1 >= -1;

         this.m_priority = var1;
      }

      public boolean isActive() {
         return this.m_priority != -1;
      }
   }

   class AnimDirReloader implements PredicatedFileWatcher.IPredicatedFileWatcherCallback {
      URI m_baseURI;
      URI m_mediaURI;
      String m_dir;
      String m_dirSecondary;
      String m_dirAbsolute;
      String m_dirSecondaryAbsolute;
      ModelMesh m_animationsModel;
      ModAnimations m_modAnimations;

      public AnimDirReloader(URI var2, URI var3, String var4, ModelMesh var5, ModAnimations var6) {
         var4 = ZomboidFileSystem.instance.getRelativeFile(var2, var4);
         this.m_baseURI = var2;
         this.m_mediaURI = var3;
         this.m_dir = ZomboidFileSystem.instance.normalizeFolderPath(var4);
         this.m_dirAbsolute = ZomboidFileSystem.instance.normalizeFolderPath((new File(new File(this.m_baseURI), this.m_dir)).toString());
         if (this.m_dir.contains("/anims/")) {
            this.m_dirSecondary = this.m_dir.replace("/anims/", "/anims_X/");
            this.m_dirSecondaryAbsolute = ZomboidFileSystem.instance.normalizeFolderPath((new File(new File(this.m_baseURI), this.m_dirSecondary)).toString());
         }

         this.m_animationsModel = var5;
         this.m_modAnimations = var6;
      }

      private boolean IsInDir(String var1) {
         var1 = ZomboidFileSystem.instance.normalizeFolderPath(var1);

         try {
            if (this.m_dirSecondary == null) {
               return var1.startsWith(this.m_dirAbsolute);
            } else {
               return var1.startsWith(this.m_dirAbsolute) || var1.startsWith(this.m_dirSecondaryAbsolute);
            }
         } catch (Exception var3) {
            var3.printStackTrace();
            return false;
         }
      }

      public void call(String var1) {
         String var2 = var1.toLowerCase();
         if (var2.endsWith(".fbx") || var2.endsWith(".x") || var2.endsWith(".txt")) {
            String var3 = ZomboidFileSystem.instance.getAnimName(this.m_mediaURI, new File(var1));
            AnimationAsset var4 = ModelManager.this.getAnimationAsset(var3);
            if (var4 != null) {
               if (!var4.isEmpty()) {
                  DebugLog.General.debugln("Reloading animation: %s", ModelManager.this.animAssetToString(var4));

                  assert var4.getRefCount() == 1;

                  AnimationAsset.AnimationAssetParams var5 = new AnimationAsset.AnimationAssetParams();
                  var5.animationsMesh = this.m_animationsModel;
                  AnimationAssetManager.instance.reload(var4, var5);
               }

            } else {
               ModelManager.this.loadAnim(var3, this.m_animationsModel, this.m_modAnimations);
            }
         }
      }

      public PredicatedFileWatcher GetFileWatcher() {
         return new PredicatedFileWatcher(this.m_dir, this::IsInDir, this);
      }
   }

   public static class ModelSlot {
      public int ID;
      public ModelInstance model;
      public IsoGameCharacter character;
      public final ArrayList<ModelInstance> sub = new ArrayList();
      protected final AttachedModels attachedModels = new AttachedModels();
      public boolean active;
      public boolean bRemove;
      public int renderRefCount = 0;
      public int framesSinceStart;

      public ModelSlot(int var1, ModelInstance var2, IsoGameCharacter var3) {
         this.ID = var1;
         this.model = var2;
         this.character = var3;
      }

      public void Update(float var1) {
         if (this.character != null && !this.bRemove) {
            ++this.framesSinceStart;
            if (this != this.character.legsSprite.modelSlot) {
               boolean var2 = false;
            }

            if (this.model.AnimPlayer != this.character.getAnimationPlayer()) {
               this.model.AnimPlayer = this.character.getAnimationPlayer();
            }

            synchronized(this.model.m_lock) {
               this.model.UpdateDir();
               this.model.Update(var1);

               for(int var3 = 0; var3 < this.sub.size(); ++var3) {
                  ((ModelInstance)this.sub.get(var3)).AnimPlayer = this.model.AnimPlayer;
               }

            }
         }
      }

      public boolean isRendering() {
         return this.renderRefCount > 0;
      }

      public void reset() {
         ModelManager.instance.resetModelInstanceRecurse(this.model, this);
         if (this.character != null) {
            this.character.primaryHandModel = null;
            this.character.secondaryHandModel = null;
            ModelManager.instance.derefModelInstances(this.character.getReadyModelData());
            this.character.getReadyModelData().clear();
         }

         this.active = false;
         this.character = null;
         this.bRemove = false;
         this.renderRefCount = 0;
         this.model = null;
         this.sub.clear();
         this.attachedModels.clear();
      }
   }

   private static final class ModelMetaData {
      String meshName;
      String textureName;
      String shaderName;
      boolean bStatic;

      private ModelMetaData() {
      }
   }
}
