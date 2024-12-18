package zombie.core.skinnedmodel.advancedanimation;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.characters.AttachedItems.AttachedModelName;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.rendering.RenderList;
import zombie.core.rendering.ShaderParameter;
import zombie.core.rendering.ShaderPropertyBlock;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.objects.ShadowParams;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIManager;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

public final class AnimatedModel extends AnimationVariableSource implements IAnimatable, IAnimEventCallback, IActionStateChanged, IAnimalVisual, IHumanVisual {
   private String animSetName = "player-avatar";
   private String outfitName;
   private IsoGameCharacter character;
   private IGrappleable grappleable;
   private BaseVisual baseVisual = null;
   private final HumanVisual humanVisual = new HumanVisual(this);
   private final ItemVisuals itemVisuals = new ItemVisuals();
   private String primaryHandModelName;
   private String secondaryHandModelName;
   private final AttachedModelNames attachedModelNames = new AttachedModelNames();
   private ModelInstance modelInstance;
   private boolean bFemale = false;
   private boolean bZombie = false;
   private boolean bSkeleton = false;
   private String animalType = null;
   private float FinalScale = 1.0F;
   private String state;
   private final Vector2 angle = new Vector2();
   private final Vector3f offset = new Vector3f(0.0F, -0.45F, 0.0F);
   private boolean bIsometric = true;
   private boolean flipY = false;
   private float m_alpha = 1.0F;
   private AnimationPlayer animPlayer = null;
   private final ActionContext actionContext = new ActionContext(this);
   private final AdvancedAnimator advancedAnimator = new AdvancedAnimator();
   private float trackTime = 0.0F;
   private final String m_UID = String.format("%s-%s", this.getClass().getSimpleName(), UUID.randomUUID().toString());
   private float lightsOriginX;
   private float lightsOriginY;
   private float lightsOriginZ;
   private final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
   private final ColorInfo ambient = new ColorInfo();
   private float highResDepthMultiplier = 0.0F;
   private boolean bOutside = true;
   private boolean bRoom = false;
   private boolean bUpdateTextures;
   private boolean bClothingChanged;
   private boolean bAnimate = true;
   private boolean bShowBip01 = false;
   private ModelInstanceTextureCreator textureCreator;
   private int cullFace = 1028;
   private final StateInfo[] stateInfos = new StateInfo[3];
   private boolean bReady;
   private static final ObjectPool<AnimatedModelInstanceRenderData> instDataPool = new ObjectPool(AnimatedModelInstanceRenderData::new);
   private final UIModelCamera uiModelCamera = new UIModelCamera();
   private static final WorldModelCamera worldModelCamera = new WorldModelCamera();

   public IsoGameCharacter getCharacter() {
      return this.character;
   }

   public AnimatedModel() {
      this.advancedAnimator.init(this);
      this.advancedAnimator.animCallbackHandlers.add(this);
      this.actionContext.onStateChanged.add(this);

      int var1;
      for(var1 = 0; var1 < this.lights.length; ++var1) {
         this.lights[var1] = new IsoGridSquare.ResultLight();
      }

      for(var1 = 0; var1 < this.stateInfos.length; ++var1) {
         this.stateInfos[var1] = new StateInfo();
      }

   }

   public void setVisual(BaseVisual var1) {
      this.baseVisual = var1;
   }

   public BaseVisual getVisual() {
      return this.baseVisual;
   }

   public AnimalVisual getAnimalVisual() {
      return (AnimalVisual)Type.tryCastTo(this.baseVisual, AnimalVisual.class);
   }

   public String getAnimalType() {
      return this.animalType;
   }

   public float getAnimalSize() {
      return this.FinalScale;
   }

   public HumanVisual getHumanVisual() {
      return (HumanVisual)Type.tryCastTo(this.baseVisual, HumanVisual.class);
   }

   public void getItemVisuals(ItemVisuals var1) {
      var1.clear();
   }

   public boolean isFemale() {
      return this.bFemale;
   }

   public boolean isZombie() {
      return this.bZombie;
   }

   public boolean isSkeleton() {
      return this.bSkeleton;
   }

   public void setAnimSetName(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalArgumentException("invalid AnimSet \"" + var1 + "\"");
      } else {
         this.animSetName = var1;
      }
   }

   public void setOutfitName(String var1, boolean var2, boolean var3) {
      this.outfitName = var1;
      this.bFemale = var2;
      this.bZombie = var3;
   }

   public void setCharacter(IsoGameCharacter var1) {
      this.outfitName = null;
      if (this.baseVisual != null) {
         this.baseVisual.clear();
      }

      this.itemVisuals.clear();
      if (var1 instanceof IHumanVisual) {
         var1.getItemVisuals(this.itemVisuals);
         this.character = var1;
         this.setGrappleable(var1);
         if (var1.getAttachedItems() != null) {
            this.attachedModelNames.initFrom(var1.getAttachedItems());
         }

         if (var1 instanceof IsoAnimal) {
            this.setModelData(((IsoAnimal)var1).getAnimalVisual(), this.itemVisuals, (IsoAnimal)var1);
         } else {
            this.setModelData(((IHumanVisual)var1).getHumanVisual(), this.itemVisuals);
         }

      }
   }

   public void setGrappleable(IGrappleable var1) {
      this.grappleable = var1;
   }

   public void setSurvivorDesc(SurvivorDesc var1) {
      this.outfitName = null;
      if (this.baseVisual != null) {
         this.baseVisual.clear();
      }

      this.itemVisuals.clear();
      var1.getWornItems().getItemVisuals(this.itemVisuals);
      this.attachedModelNames.clear();
      this.setModelData(var1.getHumanVisual(), this.itemVisuals);
   }

   public void setPrimaryHandModelName(String var1) {
      this.primaryHandModelName = var1;
   }

   public void setSecondaryHandModelName(String var1) {
      this.secondaryHandModelName = var1;
   }

   public void setAttachedModelNames(AttachedModelNames var1) {
      this.attachedModelNames.copyFrom(var1);
   }

   public void setModelData(BaseVisual var1, ItemVisuals var2) {
      this.setModelData(var1, var2, (IsoAnimal)null);
   }

   public void setModelData(BaseVisual var1, ItemVisuals var2, IsoAnimal var3) {
      AnimationPlayer var4 = this.animPlayer;
      Model var5 = this.animPlayer == null ? null : var4.getModel();
      if (this.baseVisual != var1) {
         if (this.baseVisual == null || this.baseVisual.getClass() != var1.getClass()) {
            if (var1 instanceof AnimalVisual) {
               this.baseVisual = new AnimalVisual(this);
            }

            if (var1 instanceof HumanVisual) {
               this.baseVisual = new HumanVisual(this);
            }
         }

         this.baseVisual.copyFrom(var1);
      }

      if (this.itemVisuals != var2) {
         this.itemVisuals.clear();
         this.itemVisuals.addAll(var2);
      }

      if (this.baseVisual != var1) {
         this.bFemale = false;
         this.bZombie = false;
         this.bSkeleton = false;
         this.animalType = null;
         this.FinalScale = 1.0F;
         AnimalVisual var6 = (AnimalVisual)Type.tryCastTo(var1, AnimalVisual.class);
         if (var6 != null) {
            this.animalType = var6.getAnimalType();
            this.FinalScale = var6.getAnimalSize();
            this.bSkeleton = var6.isSkeleton();
         }

         HumanVisual var7 = (HumanVisual)Type.tryCastTo(var1, HumanVisual.class);
         if (var7 != null) {
            this.bFemale = var7.isFemale();
            this.bZombie = var7.isZombie();
            this.bSkeleton = var7.isSkeleton();
         }
      }

      if (this.modelInstance != null) {
         ModelManager.instance.resetModelInstanceRecurse(this.modelInstance, this);
      }

      Model var13 = var1.getModel();
      if (var1 instanceof AnimalVisual) {
         this.character = var3;
      }

      this.getAnimationPlayer().setModel(var13);
      this.modelInstance = ModelManager.instance.newInstance(var13, (IsoGameCharacter)null, this.getAnimationPlayer());
      this.modelInstance.m_modelScript = var1.getModelScript();
      this.modelInstance.setOwner(this);
      this.populateCharacterModelSlot();
      this.DoCharacterModelEquipped();
      boolean var14 = false;
      if (this.bAnimate) {
         AnimationSet var8 = AnimationSet.GetAnimationSet(this.GetAnimSetName(), false);
         if (var8 != this.advancedAnimator.animSet || var4 != this.getAnimationPlayer() || var5 != var13) {
            var14 = true;
         }
      } else {
         var14 = true;
      }

      if (var14) {
         this.advancedAnimator.OnAnimDataChanged(false);
      }

      if (this.bAnimate) {
         ActionGroup var15 = ActionGroup.getActionGroup(this.GetAnimSetName());
         if (var15 != this.actionContext.getGroup()) {
            this.actionContext.setGroup(var15);
         }

         String var9 = StringUtils.isNullOrWhitespace(this.state) ? this.actionContext.getCurrentStateName() : this.state;
         this.advancedAnimator.SetState(var9, PZArrayUtil.listConvert(this.actionContext.getChildStates(), (var0) -> {
            return var0.getName();
         }));
      } else if (!StringUtils.isNullOrWhitespace(this.state)) {
         this.advancedAnimator.SetState(this.state);
      }

      if (var14) {
         float var16 = GameTime.getInstance().FPSMultiplier;
         GameTime.getInstance().FPSMultiplier = 100.0F;

         try {
            this.advancedAnimator.update(this.getAnimationTimeDelta());
         } finally {
            GameTime.getInstance().FPSMultiplier = var16;
         }
      }

      if (Core.bDebug && !this.bAnimate && this.stateInfoMain().readyData.isEmpty()) {
         this.getAnimationPlayer().resetBoneModelTransforms();
      }

      this.trackTime = 0.0F;
      this.stateInfoMain().bModelsReady = this.isReadyToRender();
   }

   private float getAnimationTimeDelta() {
      return this.character != null ? this.character.getAnimationTimeDelta() : GameTime.instance.getTimeDelta();
   }

   public void setAmbient(ColorInfo var1, boolean var2, boolean var3) {
      this.ambient.set(var1.r, var1.g, var1.b, 1.0F);
      this.bOutside = var2;
      this.bRoom = var3;
   }

   public void setLights(IsoGridSquare.ResultLight[] var1, float var2, float var3, float var4) {
      this.lightsOriginX = var2;
      this.lightsOriginY = var3;
      this.lightsOriginZ = var4;

      for(int var5 = 0; var5 < var1.length; ++var5) {
         this.lights[var5].copyFrom(var1[var5]);
      }

   }

   public void setState(String var1) {
      this.state = var1;
   }

   public String getState() {
      return this.state;
   }

   public void setAngle(Vector2 var1) {
      this.angle.set(var1);
   }

   public void setOffset(float var1, float var2, float var3) {
      this.offset.set(var1, var2, var3);
   }

   public void setIsometric(boolean var1) {
      this.bIsometric = var1;
   }

   public boolean isIsometric() {
      return this.bIsometric;
   }

   public void setFlipY(boolean var1) {
      this.flipY = var1;
   }

   public void setAlpha(float var1) {
      this.m_alpha = var1;
   }

   public void setTint(float var1, float var2, float var3) {
      StateInfo var4 = this.stateInfoMain();
      var4.tintR = var1;
      var4.tintG = var2;
      var4.tintB = var3;
   }

   public void setTint(ColorInfo var1) {
      this.setTint(var1.r, var1.g, var1.b);
   }

   public void setTrackTime(float var1) {
      this.trackTime = var1;
   }

   public void setScale(float var1) {
      this.FinalScale = PZMath.max(0.0F, var1);
   }

   public float getScale() {
      return this.FinalScale;
   }

   public void setCullFace(int var1) {
      this.cullFace = var1;
   }

   public void setHighResDepthMultiplier(float var1) {
      this.highResDepthMultiplier = var1;
   }

   public void clothingItemChanged(String var1) {
      this.bClothingChanged = true;
   }

   public boolean isAnimate() {
      return this.bAnimate;
   }

   public void setAnimate(boolean var1) {
      this.bAnimate = var1;
   }

   public void setShowBip01(boolean var1) {
      this.bShowBip01 = var1;
   }

   private void initOutfit() {
      String var1 = this.outfitName;
      this.outfitName = null;
      if (!StringUtils.isNullOrWhitespace(var1)) {
         ModelManager.instance.create();
         this.baseVisual.dressInNamedOutfit(var1, this.itemVisuals);
         this.setModelData(this.baseVisual, this.itemVisuals);
      }
   }

   private void populateCharacterModelSlot() {
      HumanVisual var1 = this.getHumanVisual();
      if (var1 == null) {
         this.bUpdateTextures = true;
      } else {
         CharacterMask var2 = HumanVisual.GetMask(this.itemVisuals);
         if (var2.isPartVisible(CharacterMask.Part.Head)) {
            this.addHeadHair(this.itemVisuals.findHat());
         }

         int var3;
         ItemVisual var4;
         ClothingItem var5;
         for(var3 = this.itemVisuals.size() - 1; var3 >= 0; --var3) {
            var4 = (ItemVisual)this.itemVisuals.get(var3);
            var5 = var4.getClothingItem();
            if (var5 != null && var5.isReady() && !this.isItemModelHidden(this.itemVisuals, var4)) {
               this.addClothingItem(var4, var5);
            }
         }

         for(var3 = var1.getBodyVisuals().size() - 1; var3 >= 0; --var3) {
            var4 = (ItemVisual)var1.getBodyVisuals().get(var3);
            var5 = var4.getClothingItem();
            if (var5 != null && var5.isReady()) {
               this.addClothingItem(var4, var5);
            }
         }

         this.bUpdateTextures = true;
         Lambda.forEachFrom(PZArrayUtil::forEach, (List)this.modelInstance.sub, this.modelInstance, (var0, var1x) -> {
            var0.AnimPlayer = var1x.AnimPlayer;
         });
      }
   }

   private void addHeadHair(ItemVisual var1) {
      HumanVisual var2 = this.getHumanVisual();
      ImmutableColor var3 = var2.getHairColor();
      ImmutableColor var4 = var2.getBeardColor();
      HairStyle var5;
      if (this.isFemale()) {
         var5 = HairStyles.instance.FindFemaleStyle(var2.getHairModel());
         if (var5 != null && var1 != null && var1.getClothingItem() != null) {
            var5 = HairStyles.instance.getAlternateForHat(var5, var1.getClothingItem().m_HatCategory);
         }

         if (var5 != null && var5.isValid()) {
            if (DebugLog.isEnabled(DebugType.Clothing)) {
               DebugLog.Clothing.debugln("  Adding female hair: " + var5.name);
            }

            this.addHeadHairItem(var5.model, var5.texture, var3);
         }
      } else {
         var5 = HairStyles.instance.FindMaleStyle(var2.getHairModel());
         if (var5 != null && var1 != null && var1.getClothingItem() != null) {
            var5 = HairStyles.instance.getAlternateForHat(var5, var1.getClothingItem().m_HatCategory);
         }

         if (var5 != null && var5.isValid()) {
            if (DebugLog.isEnabled(DebugType.Clothing)) {
               DebugLog.Clothing.debugln("  Adding male hair: " + var5.name);
            }

            this.addHeadHairItem(var5.model, var5.texture, var3);
         }

         BeardStyle var6 = BeardStyles.instance.FindStyle(var2.getBeardModel());
         if (var6 != null && var6.isValid()) {
            if (var1 != null && var1.getClothingItem() != null && !StringUtils.isNullOrEmpty(var1.getClothingItem().m_HatCategory) && var1.getClothingItem().m_HatCategory.contains("nobeard")) {
               return;
            }

            if (DebugLog.isEnabled(DebugType.Clothing)) {
               DebugLog.Clothing.debugln("  Adding beard: " + var6.name);
            }

            this.addHeadHairItem(var6.model, var6.texture, var4);
         }
      }

   }

   private void addHeadHairItem(String var1, String var2, ImmutableColor var3) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         if (DebugLog.isEnabled(DebugType.Clothing)) {
            DebugLog.Clothing.warn("No model specified.");
         }

      } else {
         var1 = this.processModelFileName(var1);
         ModelInstance var4 = ModelManager.instance.newAdditionalModelInstance(var1, var2, (IsoGameCharacter)null, this.modelInstance.AnimPlayer, (String)null);
         if (var4 != null) {
            this.postProcessNewItemInstance(this.modelInstance, var4, var3);
         }
      }
   }

   private void addClothingItem(ItemVisual var1, ClothingItem var2) {
      String var3 = var2.getModel(this.bFemale);
      if (StringUtils.isNullOrWhitespace(var3)) {
         if (DebugLog.isEnabled(DebugType.Clothing)) {
            DebugLog.Clothing.debugln("No model specified by item: " + var2.m_Name);
         }

      } else {
         var3 = this.processModelFileName(var3);
         String var4 = var1.getTextureChoice(var2);
         ImmutableColor var5 = var1.getTint(var2);
         String var6 = var2.m_AttachBone;
         String var7 = var2.m_Shader;
         ModelInstance var8;
         if (var6 != null && var6.length() > 0) {
            var8 = this.addStatic(var3, var4, var6, var7);
         } else {
            var8 = ModelManager.instance.newAdditionalModelInstance(var3, var4, (IsoGameCharacter)null, this.modelInstance.AnimPlayer, var7);
         }

         if (var8 != null) {
            this.postProcessNewItemInstance(this.modelInstance, var8, var5);
            var8.setItemVisual(var1);
         }
      }
   }

   private boolean isItemModelHidden(ItemVisuals var1, ItemVisual var2) {
      BodyLocationGroup var3 = BodyLocations.getGroup("Human");
      return PopTemplateManager.instance.isItemModelHidden(var3, var1, var2);
   }

   private String processModelFileName(String var1) {
      var1 = var1.replaceAll("\\\\", "/");
      var1 = var1.toLowerCase(Locale.ENGLISH);
      return var1;
   }

   private void postProcessNewItemInstance(ModelInstance var1, ModelInstance var2, ImmutableColor var3) {
      var2.depthBias = 0.0F;
      var2.matrixModel = this.modelInstance;
      var2.tintR = var3.r;
      var2.tintG = var3.g;
      var2.tintB = var3.b;
      var2.AnimPlayer = this.modelInstance.AnimPlayer;
      var1.sub.add(var2);
      var2.setOwner(this);
   }

   private void DoCharacterModelEquipped() {
      ModelInstance var1;
      if (!StringUtils.isNullOrWhitespace(this.primaryHandModelName)) {
         var1 = this.addStatic(this.primaryHandModelName, "Bip01_Prop1");
         this.postProcessNewItemInstance(this.modelInstance, var1, ImmutableColor.white);
      }

      if (!StringUtils.isNullOrWhitespace(this.secondaryHandModelName)) {
         var1 = this.addStatic(this.secondaryHandModelName, "Bip01_Prop2");
         this.postProcessNewItemInstance(this.modelInstance, var1, ImmutableColor.white);
      }

      for(int var7 = 0; var7 < this.attachedModelNames.size(); ++var7) {
         AttachedModelName var2 = this.attachedModelNames.get(var7);
         if (!ModelManager.instance.shouldHideModel(this.itemVisuals, var2.attachmentNameSelf) && !ModelManager.instance.shouldHideModel(this.itemVisuals, var2.attachmentNameParent)) {
            ModelInstance var3 = ModelManager.instance.addStatic((ModelInstance)null, var2.modelName, var2.attachmentNameSelf, var2.attachmentNameParent);
            this.postProcessNewItemInstance(this.modelInstance, var3, ImmutableColor.white);
            if (var2.bloodLevel > 0.0F && !Core.getInstance().getOptionSimpleWeaponTextures()) {
               ModelInstanceTextureInitializer var4 = ModelInstanceTextureInitializer.alloc();
               var4.init(var3, var2.bloodLevel);
               var3.setTextureInitializer(var4);
            }

            for(int var8 = 0; var8 < var2.getChildCount(); ++var8) {
               AttachedModelName var5 = var2.getChildByIndex(var8);
               ModelInstance var6 = ModelManager.instance.addStatic(var3, var5.modelName, var5.attachmentNameSelf, var5.attachmentNameParent);
               var3.sub.remove(var6);
               this.postProcessNewItemInstance(var3, var6, ImmutableColor.white);
            }
         }
      }

   }

   private ModelInstance addStatic(String var1, String var2) {
      String var3 = var1;
      String var4 = var1;
      String var5 = null;
      ModelScript var6 = ScriptManager.instance.getModelScript(var1);
      if (var6 != null) {
         var3 = var6.getMeshName();
         var4 = var6.getTextureName();
         var5 = var6.getShaderName();
      }

      return this.addStatic(var3, var4, var2, var5);
   }

   private ModelInstance addStatic(String var1, String var2, String var3, String var4) {
      DebugType.ModelManager.debugln("Adding static Model: %s", var1);
      Model var5 = ModelManager.instance.tryGetLoadedModel(var1, var2, true, var4, false);
      if (var5 == null) {
         ModelManager.instance.loadStaticModel(var1.toLowerCase(), var2, var4);
         var5 = ModelManager.instance.getLoadedModel(var1, var2, true, var4);
         if (var5 == null) {
            DebugType.ModelManager.error("ModelManager.addStatic> Model not found. model:" + var1 + " tex:" + var2);
            return null;
         }
      }

      ModelInstance var6 = ModelManager.instance.newInstance(var5, (IsoGameCharacter)null, this.modelInstance.AnimPlayer);
      var6.parent = this.modelInstance;
      if (this.modelInstance.AnimPlayer != null) {
         var6.parentBone = this.modelInstance.AnimPlayer.getSkinningBoneIndex(var3, var6.parentBone);
         var6.parentBoneName = var3;
      }

      return var6;
   }

   private StateInfo stateInfoMain() {
      int var1 = SpriteRenderer.instance.getMainStateIndex();
      return this.stateInfos[var1];
   }

   private StateInfo stateInfoRender() {
      int var1 = SpriteRenderer.instance.getRenderStateIndex();
      return this.stateInfos[var1];
   }

   public void update() {
      GameProfiler.getInstance().invokeAndMeasure("AnimatedModel.Update", this, AnimatedModel::updateInternal);
   }

   private void updateInternal() {
      this.initOutfit();
      if (this.bClothingChanged) {
         this.bClothingChanged = false;
         this.setModelData(this.baseVisual, this.itemVisuals);
      }

      this.modelInstance.SetForceDir(this.angle);
      GameTime var1 = GameTime.getInstance();
      float var2 = var1.FPSMultiplier;
      float var3 = var1.getTrueMultiplier();
      if (this.bAnimate) {
         var1.setMultiplier(1.0F);
         if (UIManager.useUIFBO) {
            var1.FPSMultiplier *= GameWindow.averageFPS / (float)Core.getInstance().getOptionUIRenderFPS();
         }

         this.actionContext.update();
         this.advancedAnimator.update(this.getAnimationTimeDelta());
         this.animPlayer.Update();
         int var4 = SpriteRenderer.instance.getMainStateIndex();
         StateInfo var5 = this.stateInfos[var4];
         if (!var5.readyData.isEmpty()) {
            ModelInstance var6 = ((AnimatedModelInstanceRenderData)var5.readyData.get(0)).modelInstance;
            if (var6 != this.modelInstance && var6.AnimPlayer != this.modelInstance.AnimPlayer) {
               var6.Update(this.getAnimationTimeDelta());
            }
         }

         var1.FPSMultiplier = var2;
         var1.setMultiplier(var3);
      } else {
         var1.FPSMultiplier = 100.0F;

         try {
            this.advancedAnimator.update(this.getAnimationTimeDelta());
         } finally {
            var1.FPSMultiplier = var2;
         }

         if (this.trackTime > 0.0F && this.animPlayer.getMultiTrack().getTrackCount() > 0) {
            ((AnimationTrack)this.animPlayer.getMultiTrack().getTracks().get(0)).setCurrentTimeValue(this.trackTime);
         }

         this.animPlayer.Update(0.0F);
      }

   }

   private boolean isModelInstanceReady(ModelInstance var1) {
      if (var1.model != null && var1.model.isReady()) {
         if (var1.model.Mesh.isReady() && var1.model.Mesh.vb != null) {
            for(int var2 = 0; var2 < var1.sub.size(); ++var2) {
               ModelInstance var3 = (ModelInstance)var1.sub.get(var2);
               if (!this.isModelInstanceReady(var3)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void incrementRefCount(ModelInstance var1) {
      ++var1.renderRefCount;

      for(int var2 = 0; var2 < var1.sub.size(); ++var2) {
         ModelInstance var3 = (ModelInstance)var1.sub.get(var2);
         this.incrementRefCount(var3);
      }

   }

   private void initRenderData(StateInfo var1, AnimatedModelInstanceRenderData var2, ModelInstance var3) {
      AnimatedModelInstanceRenderData var4 = (AnimatedModelInstanceRenderData)instDataPool.alloc();
      var4.initModel(var3, var2);
      var4.init();
      var4.modelInstance.targetDepth = 0.5F;
      var1.instData.add(var4);
      var4.transformToParent(var2);

      for(int var5 = 0; var5 < var3.sub.size(); ++var5) {
         ModelInstance var6 = (ModelInstance)var3.sub.get(var5);
         this.initRenderData(var1, var4, var6);
      }

   }

   public boolean isReadyToRender() {
      if (!this.animPlayer.isReady()) {
         return false;
      } else {
         return this.isModelInstanceReady(this.modelInstance);
      }
   }

   public int renderMain() {
      StateInfo var1 = this.stateInfoMain();
      if (this.modelInstance != null) {
         if (this.bUpdateTextures) {
            this.bUpdateTextures = false;
            this.textureCreator = ModelInstanceTextureCreator.alloc();
            this.textureCreator.init(this.getVisual(), this.itemVisuals, this.modelInstance);
         }

         this.incrementRefCount(this.modelInstance);
         instDataPool.release((List)var1.instData);
         var1.instData.clear();
         if (!var1.bModelsReady && this.isReadyToRender()) {
            float var2 = GameTime.getInstance().FPSMultiplier;
            GameTime.getInstance().FPSMultiplier = 100.0F;

            try {
               this.advancedAnimator.update(this.getAnimationTimeDelta());
            } finally {
               GameTime.getInstance().FPSMultiplier = var2;
            }

            this.animPlayer.Update(0.0F);
            var1.bModelsReady = true;
         }

         this.initRenderData(var1, (AnimatedModelInstanceRenderData)null, this.modelInstance);
      }

      var1.modelInstance = this.modelInstance;
      var1.textureCreator = this.textureCreator != null && !this.textureCreator.isRendered() ? this.textureCreator : null;

      for(int var6 = 0; var6 < var1.readyData.size(); ++var6) {
         AnimatedModelInstanceRenderData var3 = (AnimatedModelInstanceRenderData)var1.readyData.get(var6);
         if (var3.modelInstance.AnimPlayer == null || var3.modelInstance.AnimPlayer.getModel() == var3.modelInstance.model) {
            var3.init();
            var3.transformToParent(var1.getParentData(var3.modelInstance));
         }
      }

      var1.bRendered = false;
      return SpriteRenderer.instance.getMainStateIndex();
   }

   public boolean isRendered() {
      return this.stateInfoRender().bRendered;
   }

   private void doneWithTextureCreator(ModelInstanceTextureCreator var1) {
      if (var1 != null) {
         for(int var2 = 0; var2 < this.stateInfos.length; ++var2) {
            if (this.stateInfos[var2].textureCreator == var1) {
               return;
            }
         }

         if (var1.isRendered()) {
            var1.postRender();
            if (var1 == this.textureCreator) {
               this.textureCreator = null;
            }
         } else if (var1 != this.textureCreator) {
            var1.postRender();
         }

      }
   }

   private void release(ArrayList<AnimatedModelInstanceRenderData> var1) {
      for(int var2 = 0; var2 < var1.size(); ++var2) {
         AnimatedModelInstanceRenderData var3 = (AnimatedModelInstanceRenderData)var1.get(var2);
         if (var3.modelInstance.getTextureInitializer() != null) {
            var3.modelInstance.getTextureInitializer().postRender();
         }

         ModelManager.instance.derefModelInstance(var3.modelInstance);
      }

      instDataPool.release((List)var1);
   }

   public void postRender(boolean var1) {
      int var2 = SpriteRenderer.instance.getMainStateIndex();
      StateInfo var3 = this.stateInfos[var2];
      ModelInstanceTextureCreator var4 = var3.textureCreator;
      var3.textureCreator = null;
      this.doneWithTextureCreator(var4);
      var3.modelInstance = null;
      if (this.bAnimate && var3.bRendered) {
         this.release(var3.readyData);
         var3.readyData.clear();
         var3.readyData.addAll(var3.instData);
         var3.instData.clear();
      } else if (!this.bAnimate) {
      }

      this.release(var3.instData);
      var3.instData.clear();
   }

   public void setTargetDepth(float var1) {
      int var2 = SpriteRenderer.instance.getRenderStateIndex();
      StateInfo var3 = this.stateInfos[var2];

      for(int var4 = 0; var4 < var3.instData.size(); ++var4) {
         AnimatedModelInstanceRenderData var5 = (AnimatedModelInstanceRenderData)var3.instData.get(var4);
         var5.modelInstance.targetDepth = var1;
      }

   }

   public void DoRender(IModelCamera var1) {
      int var2 = SpriteRenderer.instance.getRenderStateIndex();
      StateInfo var3 = this.stateInfos[var2];
      this.bReady = true;
      ModelInstanceTextureCreator var4 = var3.textureCreator;
      if (var4 != null && !var4.isRendered()) {
         var4.render();
         if (!var4.isRendered()) {
            this.bReady = false;
         }
      }

      if (!this.isModelInstanceReady(this.modelInstance)) {
         this.bReady = false;
      }

      for(int var5 = 0; var5 < var3.instData.size(); ++var5) {
         AnimatedModelInstanceRenderData var6 = (AnimatedModelInstanceRenderData)var3.instData.get(var5);
         ModelInstanceTextureInitializer var7 = var6.modelInstance.getTextureInitializer();
         if (var7 != null && !var7.isRendered()) {
            var7.render();
            if (!var7.isRendered()) {
               this.bReady = false;
            }
         }
      }

      if (this.bReady && !var3.bModelsReady) {
         this.bReady = false;
      }

      if (this.bReady || !var3.readyData.isEmpty()) {
         GL11.glPushClientAttrib(-1);
         GL11.glPushAttrib(1048575);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glEnable(3008);
         GL11.glAlphaFunc(516, 0.0F);
         var1.Begin();
         this.StartCharacter();
         this.Render();
         boolean var11 = false;
         if (var11) {
            GL11.glDisable(2884);
            VBORenderer var12 = VBORenderer.getInstance();
            float var13 = 1.0F;
            float var8 = 1.0F;
            float var9 = 1.0F;
            float var10 = 1.0F;
            var12.setDepthTestForAllRuns(Boolean.TRUE);
            var12.addBox(1.0F, 0.1F, 1.0F, var13, var8, var9, var10, (ShaderProgram)null);
            var12.flush();
            var12.setDepthTestForAllRuns((Boolean)null);
            GL11.glEnable(2884);
         }

         this.EndCharacter();
         var1.End();
         GL11.glDepthFunc(519);
         GL11.glPopAttrib();
         GL11.glPopClientAttrib();
         Texture.lastTextureID = -1;
         GLStateRenderThread.restore();
         SpriteRenderer.ringBuffer.restoreVBOs = true;
         var3.bRendered = this.bReady;
      }
   }

   public void DoRender(int var1, int var2, int var3, int var4, float var5, float var6) {
      GL11.glClear(256);
      this.uiModelCamera.x = var1;
      this.uiModelCamera.y = var2;
      this.uiModelCamera.w = var3;
      this.uiModelCamera.h = var4;
      this.uiModelCamera.sizeV = var5;
      this.uiModelCamera.m_animPlayerAngle = var6;
      this.DoRender(this.uiModelCamera);
   }

   public void DoRenderToWorld(float var1, float var2, float var3, float var4) {
      worldModelCamera.x = var1;
      worldModelCamera.y = var2;
      worldModelCamera.z = var3;
      worldModelCamera.angle = var4;
      worldModelCamera.animatedModel = this;
      this.DoRender(worldModelCamera);
   }

   private void debugDrawAxes() {
      if (Core.bDebug && DebugOptions.instance.Model.Render.Axis.getValue()) {
         Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 1.0F, 4.0F);
      }

   }

   private void StartCharacter() {
      GL11.glEnable(2929);
      GL11.glEnable(3042);
      if (UIManager.useUIFBO) {
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
      } else {
         GL11.glBlendFunc(770, 771);
      }

      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.0F);
      GL11.glDisable(3089);
      GL11.glDepthMask(true);
   }

   private void EndCharacter() {
      GL11.glDepthMask(false);
      GL11.glViewport(0, 0, Core.width, Core.height);
   }

   private void Render() {
      int var1 = SpriteRenderer.instance.getRenderStateIndex();
      StateInfo var2 = this.stateInfos[var1];
      ModelInstance var3 = var2.modelInstance;
      if (var3 == null) {
         boolean var4 = true;
      } else {
         ArrayList var7 = this.bReady ? var2.instData : var2.readyData;

         for(int var5 = 0; var5 < var7.size(); ++var5) {
            AnimatedModelInstanceRenderData var6 = (AnimatedModelInstanceRenderData)var7.get(var5);
            this.DrawChar(var6);
         }
      }

      this.debugDrawAxes();
   }

   private void DrawChar(AnimatedModelInstanceRenderData var1) {
      StateInfo var2 = this.stateInfoRender();
      ModelInstance var3 = var1.modelInstance;
      FloatBuffer var4 = var1.matrixPalette;
      if (var3 != null) {
         if (var3.AnimPlayer != null) {
            if (var3.AnimPlayer.hasSkinningData()) {
               if (var3.model != null) {
                  if (var3.model.isReady()) {
                     if (var3.tex != null || var3.model.tex != null) {
                        if (var3.model.Effect == null) {
                           var3.model.CreateShader("basicEffect");
                        }

                        GL11.glEnable(2884);
                        GL11.glCullFace(this.cullFace);
                        GL11.glEnable(2929);
                        GL11.glEnable(3008);
                        GL11.glDepthFunc(513);
                        GL11.glDepthRange(0.0, 1.0);
                        GL11.glAlphaFunc(516, 0.01F);
                        if (!var3.model.Effect.isInstanced()) {
                           this.DrawCharSingular(var1);
                        } else {
                           var1.properties.SetFloat("Alpha", this.m_alpha);
                           var1.properties.SetVector3("AmbientColour", this.ambient.r * 0.45F, this.ambient.g * 0.45F, this.ambient.b * 0.45F);
                           var1.properties.SetVector3("TintColour", this.modelInstance.tintR * var2.tintR, this.modelInstance.tintG * var2.tintG, this.modelInstance.tintB * var2.tintB);
                           if (this.highResDepthMultiplier != 0.0F) {
                              var1.properties.SetFloat("HighResDepthMultiplier", this.highResDepthMultiplier);
                           }

                           Matrix4f var5 = Core.getInstance().modelViewMatrixStack.alloc();
                           VertexBufferObject.getModelViewProjection(var5);
                           var1.properties.SetMatrix4("mvp", var5).transpose();
                           Core.getInstance().modelViewMatrixStack.release(var5);
                           RenderList.DrawImmediate((ModelSlotRenderData)null, var1);
                           ShaderHelper.forgetCurrentlyBound();
                           ShaderHelper.glUseProgramObjectARB(0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void DrawCharSingular(AnimatedModelInstanceRenderData var1) {
      StateInfo var2 = this.stateInfoRender();
      ModelInstance var3 = var1.modelInstance;
      FloatBuffer var4 = var1.matrixPalette;
      Shader var5 = var3.model.Effect;
      int var7;
      int var15;
      if (var5 != null) {
         var5.Start();
         if (var3.model.bStatic) {
            var5.setTransformMatrix(var1.xfrm, true);
         } else {
            var5.setMatrixPalette(var4, true);
         }

         var5.setLight(0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
         var5.setLight(1, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
         var5.setLight(2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
         var5.setLight(3, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
         var5.setLight(4, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
         float var6 = 0.7F;

         for(var7 = 0; var7 < this.lights.length; ++var7) {
            IsoGridSquare.ResultLight var8 = this.lights[var7];
            if (var8.radius > 0) {
               var5.setLight(var7, (float)var8.x + 0.5F, (float)var8.y + 0.5F, (float)var8.z + 0.5F, var8.r * var6, var8.g * var6, var8.b * var6, (float)var8.radius, var1.m_animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, (IsoMovingObject)null);
            }
         }

         if (var3.tex != null) {
            var5.setTexture(var3.tex, "Texture", 0);
         } else if (var3.model.tex != null) {
            var5.setTexture(var3.model.tex, "Texture", 0);
         }

         float var14;
         if (this.bOutside) {
            var14 = ModelInstance.MODEL_LIGHT_MULT_OUTSIDE;
            var5.setLight(3, this.lightsOriginX - 2.0F, this.lightsOriginY - 2.0F, this.lightsOriginZ + 1.0F, this.ambient.r * var14 / 4.0F, this.ambient.g * var14 / 4.0F, this.ambient.b * var14 / 4.0F, 5000.0F, var1.m_animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, (IsoMovingObject)null);
            var5.setLight(4, this.lightsOriginX + 2.0F, this.lightsOriginY + 2.0F, this.lightsOriginZ + 1.0F, this.ambient.r * var14 / 4.0F, this.ambient.g * var14 / 4.0F, this.ambient.b * var14 / 4.0F, 5000.0F, var1.m_animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, (IsoMovingObject)null);
         } else if (this.bRoom) {
            var14 = ModelInstance.MODEL_LIGHT_MULT_ROOM;
            var5.setLight(4, this.lightsOriginX + 2.0F, this.lightsOriginY + 2.0F, this.lightsOriginZ + 1.0F, this.ambient.r * var14 / 4.0F, this.ambient.g * var14 / 4.0F, this.ambient.b * var14 / 4.0F, 5000.0F, var1.m_animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, (IsoMovingObject)null);
         }

         var14 = var3.targetDepth;
         var5.setTargetDepth(var14);
         var5.setDepthBias(var3.depthBias / 50.0F);
         var5.setAmbient(this.ambient.r * 0.45F, this.ambient.g * 0.45F, this.ambient.b * 0.45F);
         var5.setLightingAmount(1.0F);
         var5.setHueShift(var3.hue);
         var5.setTint(var3.tintR * var2.tintR, var3.tintG * var2.tintG, var3.tintB * var2.tintB);
         var5.setAlpha(this.m_alpha);
         var5.setScale(this.FinalScale);
         if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
            for(var15 = 0; var15 < 5; ++var15) {
               var5.setLight(var15, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F / 0.0F, var3);
            }

            var5.setAmbient(1.0F, 1.0F, 1.0F);
         }

         if (this.highResDepthMultiplier != 0.0F) {
            var5.setHighResDepthMultiplier(this.highResDepthMultiplier);
         }
      }

      var3.model.Mesh.Draw(var5);
      if (var5 != null) {
         if (this.highResDepthMultiplier != 0.0F) {
            var5.setHighResDepthMultiplier(0.0F);
         }

         var5.End();
      }

      DefaultShader var10000 = SceneShaderStore.DefaultShader;
      DefaultShader.isActive = false;
      ShaderHelper.forgetCurrentlyBound();
      GL20.glUseProgram(0);
      if (Core.bDebug && DebugOptions.instance.Model.Render.Lights.getValue() && var3.parent == null) {
         Model var17;
         if (this.lights[0].radius > 0) {
            var17 = var3.model;
            Model.debugDrawLightSource((float)this.lights[0].x, (float)this.lights[0].y, (float)this.lights[0].z, 0.0F, 0.0F, 0.0F, -var1.m_animPlayerAngle);
         }

         if (this.lights[1].radius > 0) {
            var17 = var3.model;
            Model.debugDrawLightSource((float)this.lights[1].x, (float)this.lights[1].y, (float)this.lights[1].z, 0.0F, 0.0F, 0.0F, -var1.m_animPlayerAngle);
         }

         if (this.lights[2].radius > 0) {
            var17 = var3.model;
            Model.debugDrawLightSource((float)this.lights[2].x, (float)this.lights[2].y, (float)this.lights[2].z, 0.0F, 0.0F, 0.0F, -var1.m_animPlayerAngle);
         }
      }

      if (Core.bDebug && DebugOptions.instance.Model.Render.Bones.getValue()) {
         VBORenderer var12 = VBORenderer.getInstance();
         var12.startRun(var12.FORMAT_PositionColor);
         var12.setMode(1);
         var12.setLineWidth(1.0F);
         var12.setDepthTest(false);

         for(var7 = 0; var7 < var3.AnimPlayer.getModelTransformsCount(); ++var7) {
            var15 = (Integer)var3.AnimPlayer.getSkinningData().SkeletonHierarchy.get(var7);
            if (var15 >= 0) {
               org.lwjgl.util.vector.Matrix4f var9 = var3.AnimPlayer.getModelTransformAt(var7);
               org.lwjgl.util.vector.Matrix4f var10 = var3.AnimPlayer.getModelTransformAt(var15);
               Color var11 = Model.debugDrawColours[var7 % Model.debugDrawColours.length];
               var12.addLine(var9.m03, var9.m13, var9.m23, var10.m03, var10.m13, var10.m23, var11.r, var11.g, var11.b, 1.0F);
            }
         }

         var12.endRun();
         var12.flush();
         GL11.glColor3f(1.0F, 1.0F, 1.0F);
         GL11.glEnable(2929);
      }

      if (this.bShowBip01 && var3.AnimPlayer.getModelTransformsCount() > 0) {
         int var13 = var3.AnimPlayer.getSkinningBoneIndex("Bip01", -1);
         if (var13 != -1) {
            org.lwjgl.util.vector.Matrix4f var16 = var3.AnimPlayer.getModelTransformAt(var13);
            Model.debugDrawAxis(var16.m03, 0.0F * var16.m13, var16.m23, 0.1F, 4.0F);
         }
      }

      ShaderHelper.glUseProgramObjectARB(0);
   }

   public ShadowParams calculateShadowParams(ShadowParams var1, boolean var2) {
      return IsoGameCharacter.calculateShadowParams(this.getAnimationPlayer(), this.getAnimalSize(), var2, var1);
   }

   public void releaseAnimationPlayer() {
      if (this.animPlayer != null) {
         this.animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.animPlayer);
      }

   }

   public void OnAnimEvent(AnimLayer var1, AnimEvent var2) {
      if (!StringUtils.isNullOrWhitespace(var2.m_EventName)) {
         int var3 = var1.getDepth();
         this.actionContext.reportEvent(var3, var2.m_EventName);
      }
   }

   public boolean hasAnimationPlayer() {
      return true;
   }

   public IGrappleable getGrappleable() {
      return this.grappleable;
   }

   public AnimationPlayer getAnimationPlayer() {
      Model var1 = this.getVisual().getModel();
      if (this.animPlayer != null && this.animPlayer.getModel() != var1) {
         this.animPlayer = (AnimationPlayer)Pool.tryRelease((IPooledObject)this.animPlayer);
      }

      if (this.animPlayer == null) {
         this.animPlayer = AnimationPlayer.alloc(var1);
      }

      return this.animPlayer;
   }

   public void actionStateChanged(ActionContext var1) {
      this.advancedAnimator.SetState(var1.getCurrentStateName(), PZArrayUtil.listConvert(var1.getChildStates(), (var0) -> {
         return var0.getName();
      }));
   }

   public AnimationPlayerRecorder getAnimationPlayerRecorder() {
      return null;
   }

   public boolean isAnimationRecorderActive() {
      return false;
   }

   public ActionContext getActionContext() {
      return this.actionContext;
   }

   public AdvancedAnimator getAdvancedAnimator() {
      return this.advancedAnimator;
   }

   public ModelInstance getModelInstance() {
      return this.modelInstance;
   }

   public String GetAnimSetName() {
      return this.animSetName;
   }

   public String getUID() {
      return this.m_UID;
   }

   public static final class StateInfo {
      ModelInstance modelInstance;
      ModelInstanceTextureCreator textureCreator;
      final ArrayList<AnimatedModelInstanceRenderData> instData = new ArrayList();
      final ArrayList<AnimatedModelInstanceRenderData> readyData = new ArrayList();
      boolean bModelsReady;
      boolean bRendered;
      float tintR = 1.0F;
      float tintG = 1.0F;
      float tintB = 1.0F;

      public StateInfo() {
      }

      AnimatedModelInstanceRenderData getParentData(ModelInstance var1) {
         for(int var2 = 0; var2 < this.readyData.size(); ++var2) {
            AnimatedModelInstanceRenderData var3 = (AnimatedModelInstanceRenderData)this.readyData.get(var2);
            if (var3.modelInstance == var1.parent) {
               return var3;
            }
         }

         return null;
      }
   }

   private final class UIModelCamera extends ModelCamera {
      int x;
      int y;
      int w;
      int h;
      float sizeV;
      float m_animPlayerAngle;

      private UIModelCamera() {
      }

      public void Begin() {
         GL11.glViewport(this.x, this.y, this.w, this.h);
         Matrix4f var1 = Core.getInstance().projectionMatrixStack.alloc();
         float var2 = (float)this.w / (float)this.h;
         if (AnimatedModel.this.flipY) {
            var1.setOrtho(-this.sizeV * var2, this.sizeV * var2, this.sizeV, -this.sizeV, -100.0F, 100.0F);
         } else {
            var1.setOrtho(-this.sizeV * var2, this.sizeV * var2, -this.sizeV, this.sizeV, -100.0F, 100.0F);
         }

         float var3 = Math.sqrt(2048.0F);
         var1.scale(-var3, var3, var3);
         Core.getInstance().projectionMatrixStack.push(var1);
         Matrix4f var4 = Core.getInstance().modelViewMatrixStack.alloc();
         var4.identity();
         if (AnimatedModel.this.bIsometric) {
            var4.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
            var4.rotate(this.m_animPlayerAngle + 0.7853982F, 0.0F, 1.0F, 0.0F);
         } else {
            var4.rotate(this.m_animPlayerAngle, 0.0F, 1.0F, 0.0F);
         }

         var4.translate(AnimatedModel.this.offset.x(), AnimatedModel.this.offset.y(), AnimatedModel.this.offset.z());
         Core.getInstance().modelViewMatrixStack.push(var4);
      }

      public void End() {
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
      }
   }

   public static class AnimatedModelInstanceRenderData {
      public Model model;
      public Texture tex;
      public ModelInstance modelInstance;
      public FloatBuffer matrixPalette;
      private boolean bMatrixPaletteValid = false;
      public final Matrix4f xfrm = new Matrix4f();
      float m_animPlayerAngle;
      public final ShaderPropertyBlock properties = new ShaderPropertyBlock();
      public AnimatedModelInstanceRenderData parent;

      public AnimatedModelInstanceRenderData() {
      }

      public void initMatrixPalette() {
         this.m_animPlayerAngle = 0.0F / 0.0F;
         this.bMatrixPaletteValid = false;
         if (this.modelInstance.AnimPlayer != null && this.modelInstance.AnimPlayer.isReady()) {
            this.m_animPlayerAngle = this.modelInstance.AnimPlayer.getRenderedAngle();
            if (!this.modelInstance.model.bStatic) {
               SkinningData var1 = (SkinningData)this.modelInstance.model.Tag;
               if (Core.bDebug && var1 == null) {
                  DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
               }

               org.lwjgl.util.vector.Matrix4f[] var2 = this.modelInstance.AnimPlayer.getSkinTransforms(var1);
               if (this.matrixPalette == null || this.matrixPalette.capacity() < var2.length * 16) {
                  this.matrixPalette = BufferUtils.createFloatBuffer(var2.length * 16);
               }

               this.matrixPalette.clear();

               for(int var3 = 0; var3 < var2.length; ++var3) {
                  var2[var3].store(this.matrixPalette);
               }

               this.matrixPalette.flip();
               this.bMatrixPaletteValid = true;
            }
         }

      }

      public AnimatedModelInstanceRenderData init() {
         if (this.bMatrixPaletteValid) {
            ShaderParameter var2 = this.properties.GetParameter("MatrixPalette");
            org.lwjgl.util.vector.Matrix4f[] var1;
            if (var2 == null) {
               var1 = new org.lwjgl.util.vector.Matrix4f[60];
               this.properties.SetMatrix4Array("MatrixPalette", var1);
            } else {
               var1 = var2.GetMatrix4Array();
            }

            int var3 = this.matrixPalette.limit() / 64;

            for(int var4 = 0; var4 < var3; ++var4) {
               var1[var4].load(this.matrixPalette);
            }

            this.matrixPalette.position(0);
         }

         if (this.modelInstance.getTextureInitializer() != null) {
            this.modelInstance.getTextureInitializer().renderMain();
         }

         this.UpdateCharacter(this.modelInstance.model.Effect);
         return this;
      }

      public void initModel(ModelInstance var1, AnimatedModelInstanceRenderData var2) {
         this.xfrm.identity();
         this.modelInstance = var1;
         this.parent = var2;
         if (!var1.model.bStatic && this.matrixPalette == null) {
            this.matrixPalette = BufferUtils.createFloatBuffer(960);
         }

         this.initMatrixPalette();
      }

      public void UpdateCharacter(Shader var1) {
         this.properties.SetFloat("Alpha", 1.0F);
         this.properties.SetVector2("UVScale", 1.0F, 1.0F);
         this.properties.SetFloat("targetDepth", this.modelInstance.targetDepth);
         this.properties.SetFloat("DepthBias", this.modelInstance.depthBias / 50.0F);
         this.properties.SetFloat("LightingAmount", 1.0F);
         this.properties.SetFloat("HueChange", this.modelInstance.hue);
         this.properties.SetVector3("TintColour", 1.0F, 1.0F, 1.0F);
         if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
            for(int var2 = 0; var2 < 4; ++var2) {
               this.properties.SetVector3ArrayElement("LightDirection", var2, 0.0F, 1.0F, 0.0F);
            }

            this.properties.SetVector3("AmbientColour", 1.0F, 1.0F, 1.0F);
         }

         this.properties.SetMatrix4("transform", this.xfrm);
      }

      public AnimatedModelInstanceRenderData transformToParent(AnimatedModelInstanceRenderData var1) {
         if (!(this.modelInstance instanceof VehicleModelInstance) && !(this.modelInstance instanceof VehicleSubModelInstance)) {
            if (var1 == null) {
               return this;
            } else {
               this.xfrm.set(var1.xfrm);
               this.xfrm.transpose();
               Matrix4f var2 = (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
               ModelAttachment var3 = var1.modelInstance.getAttachmentById(this.modelInstance.attachmentNameParent);
               if (var3 == null && this.modelInstance.parentBoneName != null) {
                  var3 = var1.modelInstance.getAttachmentById(this.modelInstance.parentBoneName);
               }

               if (var3 == null) {
                  if (this.modelInstance.parentBoneName != null && var1.modelInstance.AnimPlayer != null) {
                     ModelInstanceRenderData.applyBoneTransform(var1.modelInstance, this.modelInstance.parentBoneName, this.xfrm);
                  }
               } else {
                  ModelInstanceRenderData.applyBoneTransform(var1.modelInstance, var3.getBone(), this.xfrm);
                  ModelInstanceRenderData.makeAttachmentTransform(var3, var2);
                  this.xfrm.mul(var2);
               }

               ModelAttachment var4 = this.modelInstance.getAttachmentById(this.modelInstance.attachmentNameSelf);
               if (var4 == null && this.modelInstance.parentBoneName != null) {
                  var4 = this.modelInstance.getAttachmentById(this.modelInstance.parentBoneName);
               }

               if (var4 != null) {
                  ModelInstanceRenderData.makeAttachmentTransform(var4, var2);
                  if (ModelInstanceRenderData.INVERT_ATTACHMENT_SELF_TRANSFORM) {
                     var2.invert();
                  }

                  this.xfrm.mul(var2);
               }

               ModelInstanceRenderData.postMultiplyMeshTransform(this.xfrm, this.modelInstance.model.Mesh);
               if (this.modelInstance.scale != 1.0F) {
                  this.xfrm.scale(this.modelInstance.scale);
               }

               this.xfrm.transpose();
               ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var2);
               return this;
            }
         } else {
            return this;
         }
      }
   }

   private static final class WorldModelCamera extends ModelCamera {
      float x;
      float y;
      float z;
      float angle;
      AnimatedModel animatedModel;

      private WorldModelCamera() {
      }

      public void Begin() {
         Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, this.angle, false);
         GL11.glDepthMask(true);
         if (PerformanceSettings.FBORenderChunk) {
            float var1 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), this.x, this.y, this.z).depthStart;
            float var2 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
            var1 = var1 - (var2 + 1.0F) / 2.0F + 0.5F;
            this.animatedModel.setTargetDepth(var1);
         } else {
            this.animatedModel.setTargetDepth(0.5F);
         }

      }

      public void End() {
         Core.getInstance().DoPopIsoStuff();
      }
   }
}
