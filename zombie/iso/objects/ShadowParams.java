package zombie.iso.objects;

public final class ShadowParams {
   public float w;
   public float fm;
   public float bm;

   public ShadowParams(float var1, float var2, float var3) {
      this.w = var1;
      this.fm = var2;
      this.bm = var3;
   }

   public ShadowParams set(float var1, float var2, float var3) {
      this.w = var1;
      this.fm = var2;
      this.bm = var3;
      return this;
   }
}
