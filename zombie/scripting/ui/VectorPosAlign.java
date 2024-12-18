package zombie.scripting.ui;

public enum VectorPosAlign {
   None(0.0F, 0.0F),
   TopLeft(0.0F, 0.0F),
   TopMiddle(0.5F, 0.5F),
   TopRight(1.0F, 1.0F),
   CenterLeft(0.0F, 0.5F),
   CenterMiddle(0.5F, 0.5F),
   CenterRight(1.0F, 0.5F),
   BottomLeft(0.0F, 1.0F),
   BottomMiddle(0.5F, 1.0F),
   BottomRight(1.0F, 1.0F);

   private final float xmod;
   private final float ymod;

   private VectorPosAlign(float var3, float var4) {
      this.xmod = var3;
      this.ymod = var4;
   }

   public float getXmod() {
      return this.xmod;
   }

   public float getYmod() {
      return this.ymod;
   }

   public float getX(XuiScript.XuiVector var1) {
      return var1.isxPercent() ? var1.getX() : var1.getX() - var1.getW() * this.xmod;
   }

   public float getY(XuiScript.XuiVector var1) {
      return var1.isyPercent() ? var1.getY() : var1.getY() - var1.getH() * this.ymod;
   }
}
