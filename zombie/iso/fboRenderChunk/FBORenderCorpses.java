package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import org.lwjgl.opengl.GL11;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class FBORenderCorpses {
   private static FBORenderCorpses instance = null;
   private final Vector2 tempVector2 = new Vector2();
   private final Stack<RenderJob> JobPool = new Stack();
   private final ArrayList<RenderJob> RenderJobs = new ArrayList();
   private static final ChunkCamera chunkCamera = new ChunkCamera();

   public FBORenderCorpses() {
   }

   public static FBORenderCorpses getInstance() {
      if (instance == null) {
         instance = new FBORenderCorpses();
      }

      return instance;
   }

   public void render(int var1, IsoDeadBody var2) {
      RenderJob var3 = FBORenderCorpses.RenderJob.getNew();
      var3.renderChunkIndex = var1;
      BodyParams var4 = new BodyParams();
      var4.init(var2);
      var3.init(var4);
      if (!var2.isAnimal()) {
         boolean var5 = var2.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])var2.getDiedBoneTransforms());
         var3.animatedModel.calculateShadowParams(var2.getShadowParams(), var5);
      }

      this.RenderJobs.add(var3);
   }

   public void render(int var1, IsoMannequin var2) {
      RenderJob var3 = FBORenderCorpses.RenderJob.getNew();
      var3.renderChunkIndex = var1;
      BodyParams var4 = new BodyParams();
      var4.init(var2);
      var3.init(var4);
      this.RenderJobs.add(var3);
   }

   public void update() {
      for(int var1 = 0; var1 < this.RenderJobs.size(); ++var1) {
         RenderJob var2 = (RenderJob)this.RenderJobs.get(var1);
         if (var2.done != 1 || var2.renderRefCount <= 0) {
            if (var2.done == 1 && var2.renderRefCount == 0) {
               this.RenderJobs.remove(var1--);

               assert !this.JobPool.contains(var2);

               this.JobPool.push(var2);
            } else {
               FBORenderChunk var3 = (FBORenderChunk)FBORenderChunkManager.instance.chunks.get(var2.renderChunkIndex);
               if (var3 == null) {
                  var2.done = 1;
               } else if (var2.renderMain()) {
                  ++var2.renderRefCount;
                  if (!FBORenderChunkManager.instance.toRenderThisFrame.contains(var3)) {
                     FBORenderChunkManager.instance.toRenderThisFrame.add(var3);
                  }

                  int var4 = IsoCamera.frameState.playerIndex;
                  SpriteRenderer.instance.glDoEndFrame();
                  SpriteRenderer.instance.glDoStartFrameFlipY(var3.w, var3.h, 1.0F, var4);
                  var3.beginMainThread(false);
                  SpriteRenderer.instance.drawGeneric(var2);
                  var3.endMainThread();
                  SpriteRenderer.instance.glDoEndFrame();
                  SpriteRenderer.instance.glDoStartFrame(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), Core.getInstance().getCurrentPlayerZoom(), var4);
               }
            }
         }
      }

   }

   public void Reset() {
      this.JobPool.forEach(RenderJob::Reset);
      this.JobPool.clear();
      this.RenderJobs.clear();
   }

   private static final class RenderJob extends TextureDraw.GenericDrawer {
      public int renderChunkIndex;
      public final BodyParams body = new BodyParams();
      public AnimatedModel animatedModel;
      public float m_animPlayerAngle;
      public int done = 0;
      public int renderRefCount;

      private RenderJob() {
      }

      public static RenderJob getNew() {
         return FBORenderCorpses.instance.JobPool.isEmpty() ? new RenderJob() : (RenderJob)FBORenderCorpses.instance.JobPool.pop();
      }

      public RenderJob init(BodyParams var1) {
         this.body.init(var1);
         if (this.animatedModel == null) {
            this.animatedModel = new AnimatedModel();
            this.animatedModel.setAnimate(false);
         }

         this.animatedModel.setAnimSetName(var1.animSetName);
         this.animatedModel.setState(var1.stateName);
         this.animatedModel.setPrimaryHandModelName(var1.primaryHandItem);
         this.animatedModel.setSecondaryHandModelName(var1.secondaryHandItem);
         this.animatedModel.setAttachedModelNames(var1.attachedModelNames);
         this.animatedModel.setAmbient(var1.ambient, var1.bOutside, var1.bRoom);
         this.animatedModel.setLights(var1.lights, var1.x, var1.y, var1.z);
         this.animatedModel.setCullFace(1029);
         this.animatedModel.setVariable("FallOnFront", var1.fallOnFront);
         this.animatedModel.setVariable("KilledByFall", var1.bKilledByFall);
         var1.variables.forEach((var1x, var2) -> {
            this.animatedModel.setVariable(var1x, var2);
         });
         this.animatedModel.setModelData(var1.baseVisual, var1.itemVisuals);
         this.animatedModel.setAngle(FBORenderCorpses.instance.tempVector2.setLengthAndDirection(this.body.angle, 1.0F));
         if (var1.ragdollFall && !PZArrayUtil.isNullOrEmpty((Object[])var1.diedBoneTransforms)) {
            this.setRagdollDeathPose();
         }

         this.animatedModel.setTrackTime(var1.trackTime);
         this.animatedModel.setGrappleable(var1.grappleable);
         this.animatedModel.update();
         this.done = 0;
         this.renderRefCount = 0;
         return this;
      }

      private void setRagdollDeathPose() {
         AnimationPlayer var1 = this.animatedModel.getAnimationPlayer();
         var1.stopAll();
         AnimationTrack var2 = var1.play("DeathPose", true, true, -1.0F);
         if (var2 != null) {
            var2.BlendDelta = 1.0F;
            var2.initRagdollTransforms(this.body.diedBoneTransforms);
         }

      }

      public boolean renderMain() {
         if (this.animatedModel.isReadyToRender()) {
            this.animatedModel.setTint(this.body.tintR, this.body.tintG, this.body.tintB);
            this.animatedModel.renderMain();
            this.m_animPlayerAngle = this.animatedModel.getAnimationPlayer().getRenderedAngle();
            return true;
         } else {
            return false;
         }
      }

      public void render() {
         if (this.done != 1) {
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            this.animatedModel.setOffset(0.0F, 0.0F, 0.0F);
            FBORenderChunk var1 = (FBORenderChunk)SpriteRenderer.instance.getRenderingState().cachedRenderChunkIndexMap.get(this.renderChunkIndex);
            FBORenderCorpses.chunkCamera.set(var1, this.body.x, this.body.y, this.body.z, this.m_animPlayerAngle);
            FBORenderCorpses.chunkCamera.m_animatedModel = this.animatedModel;
            if (var1.bHighRes) {
               this.animatedModel.setHighResDepthMultiplier(0.5F);
            }

            this.animatedModel.DoRender(FBORenderCorpses.chunkCamera);
            if (var1.bHighRes) {
               this.animatedModel.setHighResDepthMultiplier(0.0F);
            }

            if (this.animatedModel.isRendered()) {
               this.done = 1;
            }

         }
      }

      public void postRender() {
         if (this.animatedModel != null) {
            this.animatedModel.postRender(this.done == 1);

            assert this.renderRefCount > 0;

            --this.renderRefCount;
         }
      }

      public void Reset() {
         this.body.Reset();
         if (this.animatedModel != null) {
            this.animatedModel.releaseAnimationPlayer();
            this.animatedModel = null;
         }

      }
   }

   private static final class BodyParams {
      BaseVisual baseVisual;
      final ItemVisuals itemVisuals = new ItemVisuals();
      IsoDirections dir;
      float angle;
      boolean bFemale;
      boolean bZombie;
      boolean bSkeleton;
      String animSetName;
      String stateName;
      final HashMap<String, String> variables = new HashMap();
      TwistableBoneTransform[] diedBoneTransforms = null;
      boolean bStanding;
      String primaryHandItem;
      String secondaryHandItem;
      final AttachedModelNames attachedModelNames = new AttachedModelNames();
      float x;
      float y;
      float z;
      float trackTime;
      boolean bOutside;
      boolean bRoom;
      final ColorInfo ambient = new ColorInfo();
      boolean fallOnFront = false;
      boolean bKilledByFall = false;
      boolean ragdollFall = false;
      final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
      float tintR = 1.0F;
      float tintG = 1.0F;
      float tintB = 1.0F;
      IGrappleable grappleable;

      BodyParams() {
         for(int var1 = 0; var1 < this.lights.length; ++var1) {
            this.lights[var1] = new IsoGridSquare.ResultLight();
         }

      }

      void init(BodyParams var1) {
         this.baseVisual = var1.baseVisual;
         this.itemVisuals.clear();
         this.itemVisuals.addAll(var1.itemVisuals);
         this.dir = var1.dir;
         this.angle = var1.angle;
         this.bFemale = var1.bFemale;
         this.bZombie = var1.bZombie;
         this.bSkeleton = var1.bSkeleton;
         this.animSetName = var1.animSetName;
         this.stateName = var1.stateName;
         this.variables.clear();
         this.variables.putAll(var1.variables);
         this.bStanding = var1.bStanding;
         this.primaryHandItem = var1.primaryHandItem;
         this.secondaryHandItem = var1.secondaryHandItem;
         this.attachedModelNames.copyFrom(var1.attachedModelNames);
         this.x = var1.x;
         this.y = var1.y;
         this.z = var1.z;
         this.trackTime = var1.trackTime;
         this.fallOnFront = var1.fallOnFront;
         this.bKilledByFall = var1.bKilledByFall;
         this.ragdollFall = var1.ragdollFall;
         this.bOutside = var1.bOutside;
         this.bRoom = var1.bRoom;
         this.tintR = var1.tintR;
         this.tintG = var1.tintG;
         this.tintB = var1.tintB;
         this.ambient.set(var1.ambient.r, var1.ambient.g, var1.ambient.b, 1.0F);
         this.grappleable = var1.grappleable;

         for(int var2 = 0; var2 < this.lights.length; ++var2) {
            this.lights[var2].copyFrom(var1.lights[var2]);
         }

         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         this.diedBoneTransforms = (TwistableBoneTransform[])PZArrayUtil.clone(var1.diedBoneTransforms, TwistableBoneTransform::alloc, TwistableBoneTransform::set);
      }

      void init(IsoDeadBody var1) {
         this.baseVisual = var1.getVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var1.dir;
         this.angle = var1.getAngle();
         this.bFemale = var1.isFemale();
         this.bZombie = var1.isZombie();
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.initFrom(var1.getAttachedItems());
         this.animSetName = "zombie";
         this.stateName = var1.isBeingGrappled() ? "grappled" : "onground";
         this.variables.clear();
         this.grappleable = var1;
         this.bStanding = false;
         this.trackTime = 0.0F;
         if (var1.getPrimaryHandItem() == null && var1.getSecondaryHandItem() == null) {
            if (!var1.isAnimal() && !var1.isZombie() && var1.isKilledByFall()) {
               this.animSetName = "player";
               this.stateName = "deadbody";
               this.trackTime = 1.0F;
            }
         } else {
            if (var1.getPrimaryHandItem() != null && !StringUtils.isNullOrEmpty(var1.getPrimaryHandItem().getStaticModel())) {
               this.primaryHandItem = var1.getPrimaryHandItem().getStaticModel();
            }

            if (var1.getSecondaryHandItem() != null && !StringUtils.isNullOrEmpty(var1.getSecondaryHandItem().getStaticModel())) {
               this.secondaryHandItem = var1.getSecondaryHandItem().getStaticModel();
            }

            this.animSetName = "player";
            this.stateName = "deadbody";
            if (var1.isKilledByFall()) {
               this.trackTime = 1.0F;
            }
         }

         if (var1.isAnimal()) {
            this.animSetName = var1.animalAnimSet;
            this.stateName = "deadbody";
         }

         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         this.fallOnFront = var1.isFallOnFront();
         this.bKilledByFall = var1.isKilledByFall();
         this.ragdollFall = var1.ragdollFall;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         this.diedBoneTransforms = (TwistableBoneTransform[])PZArrayUtil.clone(var1.getDiedBoneTransforms(), TwistableBoneTransform::alloc, TwistableBoneTransform::set);
         this.bOutside = var1.square != null && var1.square.isOutside();
         this.bRoom = var1.square != null && var1.square.getRoom() != null;
         if (var1.isHighlighted()) {
            this.tintR = var1.getHighlightColor().r;
            this.tintG = var1.getHighlightColor().g;
            this.tintB = var1.getHighlightColor().b;
         } else {
            this.tintR = this.tintG = this.tintB = 1.0F;
         }

         this.initAmbient(var1.square);
         this.initLights(var1.square);
      }

      void init(IsoMannequin var1) {
         this.baseVisual = var1.getHumanVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.dir = var1.dir;
         this.angle = this.dir.ToVector().getDirection();
         this.bFemale = var1.isFemale();
         this.bZombie = var1.isZombie();
         this.bSkeleton = var1.isSkeleton();
         this.primaryHandItem = null;
         this.secondaryHandItem = null;
         this.attachedModelNames.clear();
         this.animSetName = var1.getAnimSetName();
         this.stateName = var1.getAnimStateName();
         this.variables.clear();
         var1.getVariables(this.variables);
         this.bStanding = true;
         this.x = var1.getX() + 0.5F;
         this.y = var1.getY() + 0.5F;
         this.z = var1.getZ();
         if (var1.getObjectIndex() != -1) {
            IsoObject[] var2 = (IsoObject[])var1.square.getObjects().getElements();

            for(int var3 = 0; var3 < var1.square.getObjects().size(); ++var3) {
               IsoObject var4 = var2[var3];
               if (var4 == var1) {
                  break;
               }

               if (var4.isTableSurface()) {
                  this.z += (var4.getSurfaceOffset() + 1.0F) / 96.0F;
               }
            }
         }

         this.trackTime = 0.0F;
         this.fallOnFront = false;
         this.ragdollFall = false;
         this.bKilledByFall = false;
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
         this.bOutside = var1.square != null && var1.square.isOutside();
         this.bRoom = var1.square != null && var1.square.getRoom() != null;
         if (var1.isHighlighted()) {
            this.tintR = var1.getHighlightColor().r;
            this.tintG = var1.getHighlightColor().g;
            this.tintB = var1.getHighlightColor().b;
         } else {
            this.tintR = this.tintG = this.tintB = 1.0F;
         }

         this.initAmbient(var1.square);
         this.initLights((IsoGridSquare)null);
      }

      void initAmbient(IsoGridSquare var1) {
         this.ambient.set(1.0F, 1.0F, 1.0F, 1.0F);
         if (var1 != null) {
            var1.interpolateLight(this.ambient, this.x % 1.0F, this.y % 1.0F);
         }

      }

      void initLights(IsoGridSquare var1) {
         for(int var2 = 0; var2 < this.lights.length; ++var2) {
            this.lights[var2].radius = 0;
         }

         if (var1 != null) {
            IsoGridSquare.ILighting var5 = var1.lighting[0];
            int var3 = var5.resultLightCount();

            for(int var4 = 0; var4 < var3; ++var4) {
               this.lights[var4].copyFrom(var5.getResultLight(var4));
            }
         }

      }

      void Reset() {
         this.baseVisual = null;
         this.itemVisuals.clear();
         Arrays.fill(this.lights, (Object)null);
         this.diedBoneTransforms = (TwistableBoneTransform[])Pool.tryRelease((IPooledObject[])this.diedBoneTransforms);
      }
   }

   private static final class ChunkCamera extends FBORenderChunkCamera {
      AnimatedModel m_animatedModel;

      private ChunkCamera() {
      }

      public void Begin() {
         super.Begin();
         float var1 = IsoDepthHelper.calculateDepth(this.m_x, this.m_y, this.m_z);
         float var2;
         if (!this.renderChunk.chunk.containsPoint(this.m_x, this.m_y)) {
            var2 = (Float)Core.getInstance().FloatParamMap.get(0);
            float var3 = (Float)Core.getInstance().FloatParamMap.get(1);
            byte var4 = 8;
            var1 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var2), PZMath.fastfloor(var3), this.m_x, this.m_y, this.m_z).depthStart;
            float var5 = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(var2 / (float)var4), PZMath.fastfloor(var3 / (float)var4), this.renderChunk.chunk.wx, this.renderChunk.chunk.wy, PZMath.fastfloor(this.m_z)).depthStart;
            var1 -= var5;
         }

         var2 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
         var1 = var1 - (var2 + 1.0F) / 2.0F + 0.5F;
         this.m_animatedModel.setTargetDepth(var1);
      }
   }
}
