package zombie.core.textures;

import zombie.core.utils.ImageUtils;

public final class ImageDataFrame {
   public ImageData owner;
   public int widthHW;
   public int heightHW;
   public MipMapLevel data;
   public final APNGFrame apngFrame = new APNGFrame();

   public ImageDataFrame() {
   }

   public ImageDataFrame set(ImageData var1, APNGFrame var2) {
      this.owner = var1;
      this.widthHW = ImageUtils.getNextPowerOfTwoHW(var2.width);
      this.heightHW = ImageUtils.getNextPowerOfTwoHW(var2.height);
      this.data = new MipMapLevel(this.widthHW, this.heightHW);
      this.apngFrame.set(var2);
      return this;
   }
}
