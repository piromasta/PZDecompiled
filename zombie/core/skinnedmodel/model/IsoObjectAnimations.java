package zombie.core.skinnedmodel.model;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoObject;
import zombie.iso.SpriteModel;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.objects.IsoDoor;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;
import zombie.util.Type;

public final class IsoObjectAnimations {
   private static IsoObjectAnimations instance = null;
   final ObjectPool<AnimatedObject> m_animatedObjectPool = new ObjectPool(AnimatedObject::new);
   final ArrayList<AnimatedObject> m_animatedObjects = new ArrayList();
   final ArrayList<MatrixPaletteForFrame> m_matrixPalettes = new ArrayList();
   final ArrayList<IsoObject> m_dancingDoors = new ArrayList();
   float m_dancingDoorsTimer = 0.0F;
   boolean m_bDancingDoorsOpen = false;

   public IsoObjectAnimations() {
   }

   public static IsoObjectAnimations getInstance() {
      if (instance == null) {
         instance = new IsoObjectAnimations();
      }

      return instance;
   }

   public void addObject(IsoObject var1, SpriteModel var2, String var3) {
      if (!GameServer.bServer) {
         AnimatedObject var4 = this.getAnimatedObject(var1);
         if (var4 == null) {
            var4 = (AnimatedObject)this.m_animatedObjectPool.alloc();
            this.m_animatedObjects.add(var4);
         }

         var4.object = var1;
         var4.spriteModel = var2;
         var4.animationName = var3;
         var4.modelScript = ScriptManager.instance.getModelScript(var2.getModelScriptName());
         var4.initModel();
         var4.initAnimationPlayer();
      }
   }

   public void update() {
      if (!GameServer.bServer) {
         this.updateDancingDoors();

         for(int var1 = 0; var1 < this.m_animatedObjects.size(); ++var1) {
            AnimatedObject var2 = (AnimatedObject)this.m_animatedObjects.get(var1);
            if (var2.object.getObjectIndex() == -1) {
               this.m_animatedObjects.remove(var1--);
               this.m_animatedObjectPool.release((Object)var2);
            } else {
               boolean var3 = var2.update();
               if (var3) {
                  var2.object.onAnimationFinished();
                  var2.object.invalidateRenderChunkLevel(FBORenderChunk.DIRTY_OBJECT_MODIFY);
                  this.m_animatedObjects.remove(var1--);
                  this.m_animatedObjectPool.release((Object)var2);
               }
            }
         }

      }
   }

   private AnimatedObject getAnimatedObject(IsoObject var1) {
      for(int var2 = 0; var2 < this.m_animatedObjects.size(); ++var2) {
         AnimatedObject var3 = (AnimatedObject)this.m_animatedObjects.get(var2);
         if (var3.object == var1) {
            return var3;
         }
      }

      return null;
   }

   public AnimationPlayer getAnimationPlayer(IsoObject var1) {
      AnimatedObject var2 = this.getAnimatedObject(var1);
      return var2 == null ? null : var2.animationPlayer;
   }

   private MatrixPaletteForFrame getOrCreateMatrixPaletteForFrame(Model var1, String var2, float var3) {
      MatrixPaletteForFrame var4 = null;

      for(int var5 = 0; var5 < this.m_matrixPalettes.size(); ++var5) {
         MatrixPaletteForFrame var6 = (MatrixPaletteForFrame)this.m_matrixPalettes.get(var5);
         if (var6.model == var1 && var6.animation.equalsIgnoreCase(var2) && var6.time == var3) {
            var4 = var6;
            break;
         }
      }

      if (var4 == null) {
         var4 = new MatrixPaletteForFrame();
         var4.model = var1;
         var4.animation = var2;
         var4.time = var3;
         var4.init();
         this.m_matrixPalettes.add(var4);
      }

      if (var4.matrixPalette == null || var4.modificationCount != var1.Mesh.m_modificationCount) {
         var4.init();
      }

      return var4;
   }

   public FloatBuffer getMatrixPaletteForFrame(Model var1, String var2, float var3) {
      MatrixPaletteForFrame var4 = this.getOrCreateMatrixPaletteForFrame(var1, var2, var3);
      return var4.matrixPalette;
   }

   public TFloatArrayList getBonesForFrame(Model var1, String var2, float var3) {
      MatrixPaletteForFrame var4 = this.getOrCreateMatrixPaletteForFrame(var1, var2, var3);
      return var4.m_boneCoords;
   }

   public void addDancingDoor(IsoObject var1) {
      if (!GameServer.bServer) {
         if (DebugOptions.instance.Animation.DancingDoors.getValue()) {
            this.m_dancingDoors.add(var1);
         }
      }
   }

   public void removeDancingDoor(IsoObject var1) {
      if (!GameServer.bServer) {
         if (DebugOptions.instance.Animation.DancingDoors.getValue()) {
            this.m_dancingDoors.remove(var1);
         }
      }
   }

   private void updateDancingDoors() {
      if (!GameServer.bServer) {
         if (DebugOptions.instance.Animation.DancingDoors.getValue()) {
            this.m_dancingDoorsTimer += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
            if (!(this.m_dancingDoorsTimer < 2.0F)) {
               while(this.m_dancingDoorsTimer >= 2.0F) {
                  this.m_dancingDoorsTimer -= 2.0F;
               }

               this.m_bDancingDoorsOpen = !this.m_bDancingDoorsOpen;

               for(int var1 = 0; var1 < this.m_dancingDoors.size(); ++var1) {
                  IsoObject var2 = (IsoObject)this.m_dancingDoors.get(var1);
                  if (var2.getObjectIndex() == -1) {
                     this.m_dancingDoors.remove(var1--);
                  } else {
                     IsoDoor var3 = (IsoDoor)Type.tryCastTo(var2, IsoDoor.class);
                     if (var3 != null && var3.IsOpen() != this.m_bDancingDoorsOpen) {
                        var3.ToggleDoorActual(IsoPlayer.getInstance());
                     }
                  }
               }

            }
         }
      }
   }

   static final class AnimatedObject {
      IsoObject object;
      SpriteModel spriteModel;
      String animationName;
      ModelScript modelScript;
      Model model;
      AnimationPlayer animationPlayer;
      AnimationTrack track;

      AnimatedObject() {
      }

      void initModel() {
         String var1 = this.modelScript.getMeshName();
         String var2 = this.modelScript.getTextureName();
         String var3 = this.modelScript.getShaderName();
         boolean var4 = this.modelScript.bStatic;
         this.model = ModelManager.instance.tryGetLoadedModel(var1, var2, var4, var3, true);
         if (this.model == null && !var4 && this.modelScript.animationsMesh != null) {
            AnimationsMesh var5 = ScriptManager.instance.getAnimationsMesh(this.modelScript.animationsMesh);
            if (var5 != null && var5.modelMesh != null) {
               this.model = ModelManager.instance.loadModel(var1, var2, var5.modelMesh, var3);
            }
         }

         if (this.model == null) {
            ModelManager.instance.loadAdditionalModel(var1, var2, var4, var3);
            this.model = ModelManager.instance.getLoadedModel(var1, var2, var4, var3);
         }

      }

      void initAnimationPlayer() {
         if (this.track != null) {
            this.animationPlayer.getMultiTrack().removeTrack(this.track);
            this.track = null;
         }

         if (this.animationPlayer != null && this.animationPlayer.getModel() != this.model) {
            this.animationPlayer.release();
            this.animationPlayer = null;
         }

         if (this.animationPlayer == null) {
            this.animationPlayer = AnimationPlayer.alloc(this.model);
         }

         this.track = this.animationPlayer.play(this.animationName, false);
         if (this.track != null) {
            this.track.setLayerIdx(0);
            this.track.BlendDelta = 1.0F;
            this.track.SpeedDelta = 1.5F;
            this.track.IsPlaying = true;
            this.track.reverse = false;
         }

      }

      boolean update() {
         if (this.animationPlayer == null) {
            return true;
         } else {
            this.animationPlayer.Update();
            return this.track == null ? true : this.track.isFinished();
         }
      }
   }

   static final class MatrixPaletteForFrame {
      static AnimationPlayer animationPlayer = null;
      Model model;
      int modificationCount = 0;
      String animation;
      float time;
      FloatBuffer matrixPalette;
      final TFloatArrayList m_boneCoords = new TFloatArrayList();

      MatrixPaletteForFrame() {
      }

      void init() {
         if (animationPlayer != null) {
            while(animationPlayer.getMultiTrack().getTrackCount() > 0) {
               animationPlayer.getMultiTrack().removeTrackAt(0);
            }
         }

         if (animationPlayer != null && animationPlayer.getModel() != this.model) {
            animationPlayer.release();
            animationPlayer = null;
         }

         if (animationPlayer == null) {
            animationPlayer = AnimationPlayer.alloc(this.model);
         }

         if (animationPlayer.isReady()) {
            this.modificationCount = this.model.Mesh.m_modificationCount;
            AnimationTrack var1 = animationPlayer.play(this.animation, false);
            if (var1 != null) {
               var1.setLayerIdx(0);
               float var2 = var1.getDuration();
               var1.setCurrentTimeValue(this.time * var2);
               var1.BlendDelta = 1.0F;
               var1.SpeedDelta = 1.0F;
               var1.IsPlaying = false;
               var1.reverse = false;
               animationPlayer.Update(100.0F);
               this.initMatrixPalette();
               this.initSkeleton(animationPlayer);
            }
         }
      }

      private void initMatrixPalette() {
         SkinningData var1 = (SkinningData)this.model.Tag;
         if (var1 == null) {
            DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
         } else {
            Matrix4f[] var2 = animationPlayer.getSkinTransforms(var1);
            if (this.matrixPalette == null || this.matrixPalette.capacity() < var2.length * 16) {
               this.matrixPalette = BufferUtils.createFloatBuffer(var2.length * 16);
            }

            this.matrixPalette.clear();

            for(int var3 = 0; var3 < var2.length; ++var3) {
               var2[var3].store(this.matrixPalette);
            }

            this.matrixPalette.flip();
         }
      }

      void initSkeleton(AnimationPlayer var1) {
         this.m_boneCoords.clear();
         if (var1 != null && var1.hasSkinningData() && !var1.isBoneTransformsNeedFirstFrame()) {
            Integer var2 = (Integer)var1.getSkinningData().BoneIndices.get("Translation_Data");

            for(int var3 = 0; var3 < var1.getModelTransformsCount(); ++var3) {
               if (var2 == null || var3 != var2) {
                  int var4 = (Integer)var1.getSkinningData().SkeletonHierarchy.get(var3);
                  if (var4 >= 0) {
                     this.initSkeleton(var1, var3);
                     this.initSkeleton(var1, var4);
                  }
               }
            }

         }
      }

      void initSkeleton(AnimationPlayer var1, int var2) {
         Matrix4f var3 = var1.getModelTransformAt(var2);
         float var4 = var3.m03;
         float var5 = var3.m13;
         float var6 = var3.m23;
         this.m_boneCoords.add(var4);
         this.m_boneCoords.add(var5);
         this.m_boneCoords.add(var6);
      }
   }
}
