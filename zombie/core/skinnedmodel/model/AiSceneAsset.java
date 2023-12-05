package zombie.core.skinnedmodel.model;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import java.util.EnumSet;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;

/** @deprecated */
@Deprecated
public final class AiSceneAsset extends Asset {
   AiScene m_scene;
   EnumSet<AiPostProcessSteps> m_post_process_step_set;
   AiSceneAssetParams assetParams;
   public static final AssetType ASSET_TYPE = new AssetType("AiScene");

   protected AiSceneAsset(AssetPath var1, AssetManager var2, AiSceneAssetParams var3) {
      super(var1, var2);
      this.assetParams = var3;
      this.m_scene = null;
      this.m_post_process_step_set = var3.post_process_step_set;
   }

   public AssetType getType() {
      return ASSET_TYPE;
   }

   public static final class AiSceneAssetParams extends AssetManager.AssetParams {
      EnumSet<AiPostProcessSteps> post_process_step_set;

      public AiSceneAssetParams() {
      }
   }
}
