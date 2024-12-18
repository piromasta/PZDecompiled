package zombie.core.skinnedmodel.model;

import java.util.HashMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.core.skinnedmodel.shader.Shader;

public final class ModelMesh extends Asset {
   public VertexBufferObject vb;
   public final Vector3f minXYZ = new Vector3f(3.4028235E38F);
   public final Vector3f maxXYZ = new Vector3f(-3.4028235E38F);
   public final HashMap<String, AnimationClip> meshAnimationClips = new HashMap();
   public SkinningData skinningData;
   public SoftwareModelMesh softwareMesh;
   public MeshAssetParams assetParams;
   public Matrix4f m_transform;
   public boolean m_bHasVBO = false;
   protected boolean bStatic;
   public ModelMesh m_animationsMesh;
   public String postProcess;
   public int m_modificationCount = 0;
   public String m_fullPath;
   public static final AssetType ASSET_TYPE = new AssetType("Mesh");

   public ModelMesh(AssetPath var1, AssetManager var2, MeshAssetParams var3) {
      super(var1, var2);
      this.assetParams = var3;
      this.bStatic = this.assetParams != null && this.assetParams.bStatic;
      if (!this.bStatic && this.assetParams.animationsMesh == null) {
         this.assetParams.animationsMesh = this;
      }

      this.m_animationsMesh = this.assetParams == null ? null : this.assetParams.animationsMesh;
      this.postProcess = this.assetParams == null ? null : this.assetParams.postProcess;
   }

   protected void onLoadedX(ProcessedAiScene var1) {
      JAssImpImporter.LoadMode var2 = this.assetParams.bStatic ? JAssImpImporter.LoadMode.StaticMesh : JAssImpImporter.LoadMode.Normal;
      SkinningData var3 = this.assetParams.animationsMesh == null ? null : this.assetParams.animationsMesh.skinningData;
      var1.applyToMesh(this, var2, false, var3);
      if (this == this.assetParams.animationsMesh) {
         if (this.skinningData == null) {
            boolean var4 = true;
         } else {
            this.skinningData.AnimationClips.putAll(this.meshAnimationClips);
         }
      }

      ++this.m_modificationCount;
   }

   protected void onLoadedTxt(ModelTxt var1) {
      SkinningData var2 = this.assetParams.animationsMesh == null ? null : this.assetParams.animationsMesh.skinningData;
      ModelLoader.instance.applyToMesh(var1, this, var2);
   }

   public void SetVertexBuffer(VertexBufferObject var1) {
      this.clear();
      this.vb = var1;
      this.bStatic = var1 == null || var1.bStatic;
   }

   public void Draw(Shader var1) {
      if (this.vb != null) {
         this.vb.Draw(var1);
      }

   }

   public void DrawInstanced(Shader var1, int var2) {
      this.vb.DrawInstanced(var1, var2);
   }

   public void onBeforeReady() {
      super.onBeforeReady();
      if (this.assetParams != null) {
         this.assetParams.animationsMesh = null;
         this.assetParams = null;
      }

   }

   public boolean isReady() {
      return super.isReady() && (!this.m_bHasVBO || this.vb != null);
   }

   public void setAssetParams(AssetManager.AssetParams var1) {
      this.assetParams = (MeshAssetParams)var1;
   }

   public AssetType getType() {
      return ASSET_TYPE;
   }

   public void clear() {
      if (this.vb != null) {
         this.vb.clear();
         this.vb = null;
      }
   }

   public static final class MeshAssetParams extends AssetManager.AssetParams {
      public boolean bStatic;
      public ModelMesh animationsMesh;
      public String postProcess;

      public MeshAssetParams() {
      }
   }
}
