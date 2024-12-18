package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiBuiltInWrapperProvider;
import jassimp.AiMatrix4f;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiScene;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.PhysicsShape;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ProcessedAiScene {
   private ImportedSkeleton skeleton;
   private ImportedSkinnedMesh skinnedMesh;
   private final ArrayList<ImportedStaticMesh> staticMeshes = new ArrayList();

   private ProcessedAiScene() {
   }

   public static ProcessedAiScene process(ProcessedAiSceneParams var0) {
      ProcessedAiScene var1 = new ProcessedAiScene();
      if (var0.bAllMeshes) {
         var1.processAllMeshes(var0);
         return var1;
      } else {
         var1.processAiScene(var0);
         return var1;
      }
   }

   private void processAiScene(ProcessedAiSceneParams var1) {
      AiScene var2 = var1.scene;
      JAssImpImporter.LoadMode var3 = var1.mode;
      String var4 = var1.meshName;
      AiMesh var5 = this.findMesh(var2, var4);
      if (var5 == null) {
         DebugLog.General.error("No such mesh \"%s\"", var4);
      } else {
         if (var3 != JAssImpImporter.LoadMode.StaticMesh && var5.hasBones()) {
            ImportedSkeletonParams var7 = ImportedSkeletonParams.create(var1, var5);
            this.skeleton = ImportedSkeleton.process(var7);
            if (var3 != JAssImpImporter.LoadMode.AnimationOnly) {
               this.skinnedMesh = new ImportedSkinnedMesh(this.skeleton, var5);
               this.skinnedMesh.transform = this.initMeshTransform(var2, var5);
            }
         } else {
            ImportedStaticMesh var6 = new ImportedStaticMesh(var5);
            var6.transform = this.initMeshTransform(var2, var5);
            this.staticMeshes.add(var6);
         }

      }
   }

   private void processAllMeshes(ProcessedAiSceneParams var1) {
      AiScene var2 = var1.scene;
      JAssImpImporter.LoadMode var3 = var1.mode;
      if (var3 == JAssImpImporter.LoadMode.StaticMesh) {
         Iterator var4 = var2.getMeshes().iterator();

         while(var4.hasNext()) {
            AiMesh var5 = (AiMesh)var4.next();
            ImportedStaticMesh var6 = new ImportedStaticMesh(var5);
            var6.transform = this.initMeshTransform(var2, var5);
            this.staticMeshes.add(var6);
         }

      }
   }

   private Matrix4f initMeshTransform(AiScene var1, AiMesh var2) {
      AiBuiltInWrapperProvider var3 = new AiBuiltInWrapperProvider();
      AiNode var4 = (AiNode)var1.getSceneRoot(var3);
      AiNode var5 = this.findParentNodeForMesh(var1.getMeshes().indexOf(var2), var4);
      if (var5 == null) {
         return null;
      } else {
         Matrix4f var6 = JAssImpImporter.getMatrixFromAiMatrix((AiMatrix4f)var5.getTransform(var3));
         Matrix4f var7 = new Matrix4f();

         for(AiNode var8 = var5.getParent(); var8 != null; var8 = var8.getParent()) {
            JAssImpImporter.getMatrixFromAiMatrix((AiMatrix4f)var8.getTransform(var3), var7);
            Matrix4f.mul(var7, var6, var6);
         }

         return var6;
      }
   }

   private AiMesh findMesh(AiScene var1, String var2) {
      if (var1.getNumMeshes() == 0) {
         return null;
      } else {
         Iterator var3;
         AiMesh var4;
         if (StringUtils.isNullOrWhitespace(var2)) {
            var3 = var1.getMeshes().iterator();

            do {
               if (!var3.hasNext()) {
                  return (AiMesh)var1.getMeshes().get(0);
               }

               var4 = (AiMesh)var3.next();
            } while(!var4.hasBones());

            return var4;
         } else {
            var3 = var1.getMeshes().iterator();

            do {
               if (!var3.hasNext()) {
                  AiBuiltInWrapperProvider var7 = new AiBuiltInWrapperProvider();
                  AiNode var8 = (AiNode)var1.getSceneRoot(var7);
                  AiNode var5 = JAssImpImporter.FindNode(var2, var8);
                  if (var5 != null && var5.getNumMeshes() == 1) {
                     int var6 = var5.getMeshes()[0];
                     return (AiMesh)var1.getMeshes().get(var6);
                  }

                  return null;
               }

               var4 = (AiMesh)var3.next();
            } while(!var4.getName().equalsIgnoreCase(var2));

            return var4;
         }
      }
   }

   private AiNode findParentNodeForMesh(int var1, AiNode var2) {
      for(int var3 = 0; var3 < var2.getNumMeshes(); ++var3) {
         if (var2.getMeshes()[var3] == var1) {
            return var2;
         }
      }

      Iterator var6 = var2.getChildren().iterator();

      AiNode var5;
      do {
         if (!var6.hasNext()) {
            return null;
         }

         AiNode var4 = (AiNode)var6.next();
         var5 = this.findParentNodeForMesh(var1, var4);
      } while(var5 == null);

      return var5;
   }

   public void applyToMesh(ModelMesh var1, JAssImpImporter.LoadMode var2, boolean var3, SkinningData var4) {
      var1.m_transform = null;
      ImportedStaticMesh var5 = this.staticMeshes.isEmpty() ? null : (ImportedStaticMesh)this.staticMeshes.get(0);
      VertexBufferObject.VertexArray var6;
      int[] var7;
      if (var5 != null) {
         var1.minXYZ.set(var5.minXYZ);
         var1.maxXYZ.set(var5.maxXYZ);
         if (var5.transform != null) {
            var1.m_transform = PZMath.convertMatrix(var5.transform, new org.joml.Matrix4f());
         }

         if (!ModelManager.NoOpenGL) {
            var1.m_bHasVBO = true;
            var6 = var5.verticesUnskinned;
            var7 = var5.elements;
            RenderThread.queueInvokeOnRenderContext(() -> {
               var1.SetVertexBuffer(new VertexBufferObject(var6, var7));
               if (ModelManager.instance.bCreateSoftwareMeshes) {
                  var1.softwareMesh.vb = var1.vb;
               }

            });
         }
      }

      if (var1.skinningData != null) {
         if (var4 == null || var1.skinningData.AnimationClips != var4.AnimationClips) {
            var1.skinningData.AnimationClips.clear();
         }

         var1.skinningData.InverseBindPose.clear();
         var1.skinningData.BindPose.clear();
         var1.skinningData.BoneOffset.clear();
         var1.skinningData.BoneIndices.clear();
         var1.skinningData.SkeletonHierarchy.clear();
         var1.skinningData = null;
      }

      if (this.skeleton != null) {
         ImportedSkeleton var8 = this.skeleton;
         HashMap var9 = var8.clips;
         var1.meshAnimationClips.clear();
         var1.meshAnimationClips.putAll(var9);
         if (var4 != null) {
            var8.clips.clear();
            var9 = var4.AnimationClips;
         }

         JAssImpImporter.replaceHashMapKeys(var8.boneIndices, "SkinningData.boneIndices");
         var1.skinningData = new SkinningData(var9, var8.bindPose, var8.invBindPose, var8.skinOffsetMatrices, var8.SkeletonHierarchy, var8.boneIndices);
      }

      if (this.skinnedMesh != null) {
         if (this.skinnedMesh.transform != null) {
            var1.m_transform = PZMath.convertMatrix(this.skinnedMesh.transform, new org.joml.Matrix4f());
         }

         if (!ModelManager.NoOpenGL) {
            var1.m_bHasVBO = true;
            var6 = this.skinnedMesh.vertices;
            var7 = this.skinnedMesh.elements;
            RenderThread.queueInvokeOnRenderContext(() -> {
               var1.SetVertexBuffer(new VertexBufferObject(var6, var7, var3));
               if (ModelManager.instance.bCreateSoftwareMeshes) {
                  var1.softwareMesh.vb = var1.vb;
               }

            });
         }
      }

      this.skeleton = null;
      this.skinnedMesh = null;
      this.staticMeshes.clear();
   }

   public void applyToPhysicsShape(PhysicsShape var1) {
      for(int var2 = 0; var2 < this.staticMeshes.size(); ++var2) {
         ImportedStaticMesh var3 = (ImportedStaticMesh)this.staticMeshes.get(var2);
         PhysicsShape.OneMesh var4 = new PhysicsShape.OneMesh();
         var4.m_transform = null;
         if (var3.transform != null) {
            var4.m_transform = PZMath.convertMatrix(var3.transform, new org.joml.Matrix4f());
         }

         var4.minXYZ.set(var3.minXYZ);
         var4.maxXYZ.set(var3.maxXYZ);
         VertexBufferObject.VertexArray var5 = var3.verticesUnskinned;
         int var6 = var5.m_format.indexOf(VertexBufferObject.VertexType.VertexArray);
         if (var6 != -1) {
            var4.m_points = new float[var5.m_numVertices * 3];

            for(int var7 = 0; var7 < var5.m_numVertices; ++var7) {
               float var8 = var5.getElementFloat(var7, var6, 0);
               float var9 = var5.getElementFloat(var7, var6, 1);
               float var10 = var5.getElementFloat(var7, var6, 2);
               var4.m_points[var7 * 3] = var8;
               var4.m_points[var7 * 3 + 1] = var9;
               var4.m_points[var7 * 3 + 2] = var10;
            }

            var1.meshes.add(var4);
         }
      }

      this.skeleton = null;
      this.skinnedMesh = null;
      this.staticMeshes.clear();
   }

   public void applyToAnimation(AnimationAsset var1) {
      Iterator var2 = this.skeleton.clips.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         Keyframe[] var4 = ((AnimationClip)var3.getValue()).getKeyframes();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Keyframe var7 = var4[var6];
            var7.BoneName = JAssImpImporter.getSharedString(var7.BoneName, "Keyframe.BoneName");
         }
      }

      var1.AnimationClips = this.skeleton.clips;
      this.skeleton = null;
   }
}
