package zombie.core.textures;

import java.util.ArrayList;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;

public class AnimatedTextureID extends Asset {
   public static final AssetType ASSET_TYPE = new AssetType("AnimatedTextureID");
   private int width;
   private int height;
   public final ArrayList<AnimatedTextureIDFrame> frames = new ArrayList();
   public AnimatedTextureIDAssetParams assetParams;

   protected AnimatedTextureID(AssetPath var1, AssetManager var2, AnimatedTextureIDAssetParams var3) {
      super(var1, var2);
      this.assetParams = var3;
   }

   public AssetType getType() {
      return ASSET_TYPE;
   }

   public void setImageData(ImageData var1) {
      this.width = var1.getWidth();
      this.height = var1.getHeight();

      for(int var2 = 0; var2 < var1.frames.size(); ++var2) {
         ImageDataFrame var3 = (ImageDataFrame)var1.frames.get(var2);
         ImageData var4 = new ImageData(var3);
         var3.data = null;
         TextureID var5 = new TextureID(var4);
         AnimatedTextureIDFrame var6 = new AnimatedTextureIDFrame();
         var6.textureID = var5;
         var6.apngFrame = var3.apngFrame;
         this.frames.add(var6);
      }

   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getFrameCount() {
      return this.frames.size();
   }

   public AnimatedTextureIDFrame getFrame(int var1) {
      return var1 >= 0 && var1 < this.frames.size() ? (AnimatedTextureIDFrame)this.frames.get(var1) : null;
   }

   public boolean isDestroyed() {
      return false;
   }

   public void destroy() {
   }

   public static final class AnimatedTextureIDAssetParams extends AssetManager.AssetParams {
      int flags = 0;

      public AnimatedTextureIDAssetParams() {
      }
   }
}
