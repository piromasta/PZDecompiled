package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelInstanceRenderData extends AnimatedModel.AnimatedModelInstanceRenderData {
   public float depthBias;
   public float hue;
   public float tintR;
   public float tintG;
   public float tintB;
   public int parentBone;
   public SoftwareModelMeshInstance softwareMesh;
   public boolean m_muzzleFlash = false;
   protected ModelInstanceDebugRenderData m_debugRenderData;
   public static boolean INVERT_ATTACHMENT_SELF_TRANSFORM = false;
   private static final ObjectPool<ModelInstanceRenderData> pool = new ObjectPool(ModelInstanceRenderData::new);

   public ModelInstanceRenderData() {
   }

   public ModelInstanceRenderData init() {
      super.init();

      assert this.modelInstance.character == null || this.modelInstance.AnimPlayer != null;

      if (this.modelInstance.getTextureInitializer() != null) {
         this.modelInstance.getTextureInitializer().renderMain();
      }

      return this;
   }

   public void initModel(ModelInstance var1, AnimatedModel.AnimatedModelInstanceRenderData var2) {
      super.initModel(var1, var2);
      this.model = var1.model;
      this.tex = var1.tex;
      this.depthBias = var1.depthBias;
      this.hue = var1.hue;
      this.parentBone = var1.parentBone;
      this.softwareMesh = var1.softwareMesh;
      this.m_muzzleFlash = false;
      ++var1.renderRefCount;
      VehicleSubModelInstance var3 = (VehicleSubModelInstance)Type.tryCastTo(var1, VehicleSubModelInstance.class);
      if (var1 instanceof VehicleModelInstance || var3 != null) {
         if (var1 instanceof VehicleModelInstance) {
            this.xfrm.set(((BaseVehicle)var1.object).renderTransform);
         } else {
            this.xfrm.set(var3.modelInfo.renderTransform);
         }

         postMultiplyMeshTransform(this.xfrm, var1.model.Mesh);
      }

   }

   public void UpdateCharacter(Shader var1) {
      super.UpdateCharacter(var1);
      if (!PerformanceSettings.FBORenderChunk) {
         this.properties.SetFloat("targetDepth", 0.5F);
      } else if (this.modelInstance.parent != null) {
         this.properties.SetFloat("targetDepth", this.modelInstance.parent.targetDepth);
      }

      this.properties.SetFloat("DepthBias", this.depthBias / 50.0F);
      this.properties.SetFloat("HueShift", this.hue);
      this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
   }

   public void renderDebug() {
      if (this.m_debugRenderData != null) {
         this.m_debugRenderData.render();
      }

   }

   public void RenderCharacter(ModelSlotRenderData var1) {
      this.tintR = this.modelInstance.tintR;
      this.tintG = this.modelInstance.tintG;
      this.tintB = this.modelInstance.tintB;
      this.tex = this.modelInstance.tex;
      if (this.tex != null || this.modelInstance.model.tex != null) {
         this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
         this.model.DrawChar(var1, this);
      }
   }

   public void RenderVehicle(ModelSlotRenderData var1) {
      this.tintR = this.modelInstance.tintR;
      this.tintG = this.modelInstance.tintG;
      this.tintB = this.modelInstance.tintB;
      this.tex = this.modelInstance.tex;
      if (this.tex != null || this.modelInstance.model.tex != null) {
         this.model.DrawVehicle(var1, this);
      }
   }

   public static Matrix4f makeAttachmentTransform(ModelAttachment var0, Matrix4f var1) {
      var1.translation(var0.getOffset());
      Vector3f var2 = var0.getRotate();
      var1.rotateXYZ(var2.x * 0.017453292F, var2.y * 0.017453292F, var2.z * 0.017453292F);
      var1.scale(var0.getScale());
      return var1;
   }

   public static void applyBoneTransform(ModelInstance var0, String var1, Matrix4f var2) {
      if (var0 != null && var0.AnimPlayer != null) {
         Matrix4f var3 = (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
         makeBoneTransform(var0.AnimPlayer, var1, var3);
         var3.mul(var2, var2);
         ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var3);
      }
   }

   public static void makeBoneTransform(AnimationPlayer var0, String var1, Matrix4f var2) {
      var2.identity();
      if (var0 != null) {
         if (!StringUtils.isNullOrWhitespace(var1)) {
            int var3 = var0.getSkinningBoneIndex(var1, -1);
            if (var3 != -1) {
               org.lwjgl.util.vector.Matrix4f var4 = var0.GetPropBoneMatrix(var3);
               PZMath.convertMatrix(var4, var2);
               var2.transpose();
            }
         }
      }
   }

   public static Matrix4f preMultiplyMeshTransform(Matrix4f var0, ModelMesh var1) {
      if (var1 != null && var1.isReady() && var1.m_transform != null) {
         var1.m_transform.transpose();
         var1.m_transform.mul(var0, var0);
         var1.m_transform.transpose();
      }

      return var0;
   }

   public static Matrix4f postMultiplyMeshTransform(Matrix4f var0, ModelMesh var1) {
      if (var1 != null && var1.isReady() && var1.m_transform != null) {
         var1.m_transform.transpose();
         var0.mul(var1.m_transform);
         var1.m_transform.transpose();
      }

      return var0;
   }

   private void testOnBackItem(ModelInstance var1) {
      if (var1.parent != null && var1.parent.m_modelScript != null) {
         AnimationPlayer var2 = var1.parent.AnimPlayer;
         ModelAttachment var3 = null;

         ModelAttachment var5;
         for(int var4 = 0; var4 < var1.parent.m_modelScript.getAttachmentCount(); ++var4) {
            var5 = var1.parent.getAttachment(var4);
            if (var5.getBone() != null && this.parentBone == var2.getSkinningBoneIndex(var5.getBone(), 0)) {
               var3 = var5;
               break;
            }
         }

         if (var3 != null) {
            Matrix4f var6 = (Matrix4f)((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).alloc();
            makeAttachmentTransform(var3, var6);
            this.xfrm.transpose();
            this.xfrm.mul(var6);
            this.xfrm.transpose();
            var5 = var1.getAttachmentById(var3.getId());
            if (var5 != null) {
               makeAttachmentTransform(var5, var6);
               if (INVERT_ATTACHMENT_SELF_TRANSFORM) {
                  var6.invert();
               }

               this.xfrm.transpose();
               this.xfrm.mul(var6);
               this.xfrm.transpose();
            }

            ((BaseVehicle.Matrix4fObjectPool)BaseVehicle.TL_matrix4f_pool.get()).release(var6);
         }
      }
   }

   public static ModelInstanceRenderData alloc() {
      return (ModelInstanceRenderData)pool.alloc();
   }

   public static synchronized void release(ArrayList<ModelInstanceRenderData> var0) {
      for(int var1 = 0; var1 < var0.size(); ++var1) {
         ModelInstanceRenderData var2 = (ModelInstanceRenderData)var0.get(var1);
         if (var2.modelInstance.getTextureInitializer() != null) {
            var2.modelInstance.getTextureInitializer().postRender();
         }

         ModelManager.instance.derefModelInstance(var2.modelInstance);
         var2.modelInstance = null;
         var2.model = null;
         var2.tex = null;
         var2.softwareMesh = null;
         var2.m_debugRenderData = (ModelInstanceDebugRenderData)Pool.tryRelease((IPooledObject)var2.m_debugRenderData);
      }

      pool.release((List)var0);
   }
}
