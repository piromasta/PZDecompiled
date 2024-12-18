package zombie.core.physics;

import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;

public final class PhysicsShape extends Asset {
   public PhysicsShapeAssetParams assetParams;
   public ArrayList<OneMesh> meshes = new ArrayList();
   public String postProcess;
   public boolean bAllMeshes = false;
   public int m_modificationCount = 0;
   public String m_fullPath;
   public static final AssetType ASSET_TYPE = new AssetType("PhysicsShape");

   public PhysicsShape(AssetPath var1, AssetManager var2, PhysicsShapeAssetParams var3) {
      super(var1, var2);
      this.assetParams = var3;
      this.postProcess = this.assetParams == null ? null : this.assetParams.postProcess;
      this.bAllMeshes = this.assetParams == null ? false : this.assetParams.bAllMeshes;
   }

   protected void onLoadedX(ProcessedAiScene var1) {
      Iterator var2 = this.meshes.iterator();

      while(var2.hasNext()) {
         OneMesh var3 = (OneMesh)var2.next();
         var3.reset();
      }

      this.meshes.clear();
      var1.applyToPhysicsShape(this);
      ++this.m_modificationCount;
   }

   public void onBeforeReady() {
      super.onBeforeReady();
      if (this.assetParams != null) {
         this.assetParams = null;
      }

   }

   public boolean isReady() {
      return super.isReady();
   }

   public void setAssetParams(AssetManager.AssetParams var1) {
      this.assetParams = (PhysicsShapeAssetParams)var1;
   }

   public AssetType getType() {
      return ASSET_TYPE;
   }

   public static final class PhysicsShapeAssetParams extends AssetManager.AssetParams {
      public String postProcess;
      public boolean bAllMeshes;

      public PhysicsShapeAssetParams() {
      }
   }

   public static final class OneMesh {
      public final Vector3f minXYZ = new Vector3f(3.4028235E38F);
      public final Vector3f maxXYZ = new Vector3f(-3.4028235E38F);
      public Matrix4f m_transform;
      public float[] m_points;

      public OneMesh() {
      }

      public void reset() {
         this.minXYZ.set(3.4028235E38F);
         this.maxXYZ.set(-3.4028235E38F);
         this.m_transform = null;
         this.m_points = null;
      }
   }
}
