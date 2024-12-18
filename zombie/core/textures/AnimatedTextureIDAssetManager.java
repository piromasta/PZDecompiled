package zombie.core.textures;

import java.util.Objects;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask_RunFileTask;
import zombie.asset.FileTask_LoadImageData;
import zombie.core.opengl.RenderThread;
import zombie.fileSystem.FileSystem;

public final class AnimatedTextureIDAssetManager extends AssetManager {
   public static final AnimatedTextureIDAssetManager instance = new AnimatedTextureIDAssetManager();

   public AnimatedTextureIDAssetManager() {
   }

   protected void startLoading(Asset var1) {
      AnimatedTextureID var2 = (AnimatedTextureID)var1;
      FileSystem var3 = this.getOwner().getFileSystem();
      FileTask_LoadImageData var4 = new FileTask_LoadImageData(var1.getPath().getPath(), var3, (var2x) -> {
         this.onFileTaskFinished(var1, var2x);
      });
      var4.setPriority(7);
      AssetTask_RunFileTask var5 = new AssetTask_RunFileTask(var4, var1);
      this.setTask(var1, var5);
      var5.execute();
   }

   protected void unloadData(Asset var1) {
      AnimatedTextureID var2 = (AnimatedTextureID)var1;
      if (!var2.isDestroyed()) {
         Objects.requireNonNull(var2);
         RenderThread.invokeOnRenderContext(var2::destroy);
      }
   }

   protected Asset createAsset(AssetPath var1, AssetManager.AssetParams var2) {
      return new AnimatedTextureID(var1, this, (AnimatedTextureID.AnimatedTextureIDAssetParams)var2);
   }

   protected void destroyAsset(Asset var1) {
   }

   private void onFileTaskFinished(Asset var1, Object var2) {
      AnimatedTextureID var3 = (AnimatedTextureID)var1;
      if (var2 instanceof ImageData) {
         var3.setImageData((ImageData)var2);
         this.onLoadingSucceeded(var1);
      } else {
         this.onLoadingFailed(var1);
      }

   }
}
