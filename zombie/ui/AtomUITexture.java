package zombie.ui;

import java.util.ArrayList;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;

public class AtomUITexture extends AtomUI {
   Texture tex = null;
   double sliceLeft = 0.0;
   double sliceTop = 0.0;
   double sliceRight = 0.0;
   double sliceDown = 0.0;
   double animDelay = 0.0;
   int animFrameNum = 0;
   int animFrameRows = 0;
   int animFrameColumns = 0;
   boolean textureIsReady = false;
   boolean isSlice9 = false;
   final ArrayList<Slice> slices = new ArrayList();
   boolean isAnim = false;
   int frameIndex = 0;
   long frameTimer = 0L;
   long beforeTime;
   boolean isAnimPlay = false;

   public AtomUITexture(KahluaTable var1) {
      super(var1);
   }

   public void render() {
      if (this.visible) {
         if (!this.textureIsReady) {
            this.textureIsReady = this.tex == null || this.tex.isReady();
            if (this.textureIsReady) {
               this.updateSlices();
            }
         } else if (this.isSlice9) {
            this.drawTextureSlice9();
         } else if (this.isAnim) {
            this.drawTextureAnim();
            if (this.isAnimPlay) {
               long var1 = System.currentTimeMillis();
               this.frameTimer += var1 - this.beforeTime;
               if ((double)this.frameTimer > this.animDelay) {
                  this.frameTimer = (long)((double)this.frameTimer - this.animDelay);
                  ++this.frameIndex;
                  if (this.frameIndex >= this.animFrameNum) {
                     this.frameIndex = 0;
                  }
               }

               this.beforeTime = var1;
            }
         } else {
            this.drawTexture();
         }

         super.render();
      }
   }

   public void animPlay() {
      if (this.isAnim) {
         this.beforeTime = System.currentTimeMillis();
         this.isAnimPlay = true;
      }
   }

   public void animStop() {
      if (this.isAnim) {
         this.isAnimPlay = false;
         this.frameIndex = 0;
         this.frameTimer = 0L;
      }
   }

   public void animPause() {
      if (this.isAnim) {
         this.isAnimPlay = false;
      }
   }

   void drawTextureAnim() {
      double[] var1 = this.getAbsolutePosition(this.leftSide, this.topSide);
      double[] var2 = this.getAbsolutePosition(this.rightSide, this.topSide);
      double[] var3 = this.getAbsolutePosition(this.rightSide, this.downSide);
      double[] var4 = this.getAbsolutePosition(this.leftSide, this.downSide);
      Slice var5 = (Slice)this.slices.get(this.frameIndex);
      SpriteRenderer.instance.render(this.tex, var1[0], var1[1], var2[0], var2[1], var3[0], var3[1], var4[0], var4[1], var5.uvLeft, var5.uvTop, var5.uvRight, var5.uvTop, var5.uvRight, var5.uvDown, var5.uvLeft, var5.uvDown, this.colorR, this.colorG, this.colorB, this.colorA);
   }

   void drawTexture() {
      double[] var1 = this.getAbsolutePosition(this.leftSide, this.topSide);
      double[] var2 = this.getAbsolutePosition(this.rightSide, this.topSide);
      double[] var3 = this.getAbsolutePosition(this.rightSide, this.downSide);
      double[] var4 = this.getAbsolutePosition(this.leftSide, this.downSide);
      SpriteRenderer.instance.render(this.tex, var1[0], var1[1], var2[0], var2[1], var3[0], var3[1], var4[0], var4[1], this.colorR, this.colorG, this.colorB, this.colorA, (Consumer)null);
   }

   void drawTextureSlice9() {
      for(int var1 = 0; var1 < this.slices.size(); ++var1) {
         Slice var2 = (Slice)this.slices.get(var1);
         double[] var3 = this.getAbsolutePosition(var2.leftSide, var2.topSide);
         double[] var4 = this.getAbsolutePosition(var2.rightSide, var2.topSide);
         double[] var5 = this.getAbsolutePosition(var2.rightSide, var2.downSide);
         double[] var6 = this.getAbsolutePosition(var2.leftSide, var2.downSide);
         SpriteRenderer.instance.render(this.tex, var3[0], var3[1], var4[0], var4[1], var5[0], var5[1], var6[0], var6[1], var2.uvLeft, var2.uvTop, var2.uvRight, var2.uvTop, var2.uvRight, var2.uvDown, var2.uvLeft, var2.uvDown, this.colorR, this.colorG, this.colorB, this.colorA);
      }

   }

   public void init() {
      super.init();
      this.updateInternalValues();
   }

   void loadFromTable() {
      super.loadFromTable();
      this.tex = this.tryGetTexture("texture");
      this.sliceTop = this.tryGetDouble("sliceTop", 0.0);
      this.sliceDown = this.tryGetDouble("sliceDown", 0.0);
      this.sliceLeft = this.tryGetDouble("sliceLeft", 0.0);
      this.sliceRight = this.tryGetDouble("sliceRight", 0.0);
      this.animDelay = this.tryGetDouble("animDelay", 0.0);
      this.animFrameNum = (int)this.tryGetDouble("animFrameNum", 0.0);
      this.animFrameRows = (int)this.tryGetDouble("animFrameRows", 0.0);
      this.animFrameColumns = (int)this.tryGetDouble("animFrameColumns", 0.0);
   }

   void updateInternalValues() {
      super.updateInternalValues();
      this.textureIsReady = this.tex == null || this.tex.isReady();
      this.isSlice9 = this.sliceLeft != 0.0 || this.sliceRight != 0.0 || this.sliceTop != 0.0 || this.sliceDown != 0.0;
      this.isAnim = this.animDelay != 0.0 && this.animFrameNum != 0 && this.animFrameRows != 0 && this.animFrameColumns != 0;
      this.updateSlices();
   }

   private void updateSlices() {
      this.slices.clear();
      if (this.tex != null && this.tex.isReady()) {
         if (this.isSlice9) {
            this.updateSlices9();
         } else if (this.isAnim) {
            this.updateSlicesAnim();
         }

      }
   }

   private void updateSlicesAnim() {
      double var1 = (double)this.tex.getXStart();
      double var3 = (double)this.tex.getYStart();
      double var5 = (double)this.tex.getXEnd() - var1;
      double var7 = (double)this.tex.getYEnd() - var3;
      double var9 = var5 / (double)this.animFrameColumns;
      double var11 = var7 / (double)this.animFrameRows;

      for(int var13 = 0; var13 < this.animFrameRows; ++var13) {
         for(int var14 = 0; var14 < this.animFrameColumns; ++var14) {
            int var15 = var13 * this.animFrameColumns + var14;
            if (var15 >= this.animFrameNum) {
               return;
            }

            this.slices.add(new Slice(0.0, 0.0, 0.0, 0.0, var1 + var9 * (double)var14, var1 + var9 * (double)(var14 + 1), var3 + var11 * (double)var13, var3 + var11 * (double)(var13 + 1)));
         }
      }

   }

   private void updateSlices9() {
      double var1 = (double)this.tex.getWidth();
      double var3 = (double)this.tex.getHeight();
      double var5 = (double)this.tex.getXStart();
      double var7 = (double)this.tex.getYStart();
      double var9 = (double)this.tex.getXEnd() - var5;
      double var11 = (double)this.tex.getYEnd() - var7;
      double var13 = this.leftSide;
      double var15 = this.leftSide + this.sliceLeft;
      double var17 = this.rightSide - this.sliceRight;
      double var19 = this.rightSide;
      double var21 = this.topSide;
      double var23 = this.topSide + this.sliceTop;
      double var25 = this.downSide - this.sliceDown;
      double var27 = this.downSide;
      double var31 = var5 + var9 * (this.sliceLeft / var1);
      double var33 = var5 + var9 * ((var1 - this.sliceRight) / var1);
      double var35 = var5 + var9;
      double var39 = var7 + var11 * (this.sliceTop / var3);
      double var41 = var7 + var11 * ((var3 - this.sliceDown) / var3);
      double var43 = var7 + var11;
      if (this.sliceLeft != 0.0 && this.sliceTop != 0.0) {
         this.slices.add(new Slice(var13, var15, var21, var23, var5, var31, var7, var39));
      }

      if (this.sliceTop != 0.0) {
         this.slices.add(new Slice(var15, var17, var21, var23, var31, var33, var7, var39));
      }

      if (this.sliceRight != 0.0 && this.sliceTop != 0.0) {
         this.slices.add(new Slice(var17, var19, var21, var23, var33, var35, var7, var39));
      }

      if (this.sliceLeft != 0.0) {
         this.slices.add(new Slice(var13, var15, var23, var25, var5, var31, var39, var41));
      }

      this.slices.add(new Slice(var15, var17, var23, var25, var31, var33, var39, var41));
      if (this.sliceRight != 0.0) {
         this.slices.add(new Slice(var17, var19, var23, var25, var33, var35, var39, var41));
      }

      if (this.sliceLeft != 0.0 && this.sliceDown != 0.0) {
         this.slices.add(new Slice(var13, var15, var25, var27, var5, var31, var41, var43));
      }

      if (this.sliceDown != 0.0) {
         this.slices.add(new Slice(var15, var17, var25, var27, var31, var33, var41, var43));
      }

      if (this.sliceRight != 0.0 && this.sliceDown != 0.0) {
         this.slices.add(new Slice(var17, var19, var25, var27, var33, var35, var41, var43));
      }

   }

   public void setTexture(Texture var1) {
      this.tex = var1;
      this.updateInternalValues();
   }

   public void setSlice9(double var1, double var3, double var5, double var7) {
      this.sliceLeft = var1;
      this.sliceRight = var3;
      this.sliceTop = var5;
      this.sliceDown = var7;
      this.updateInternalValues();
   }

   public void setAnimValues(double var1, double var3, double var5, double var7) {
      this.animDelay = var1;
      this.animFrameNum = (int)var3;
      this.animFrameRows = (int)var5;
      this.animFrameColumns = (int)var7;
      this.updateInternalValues();
   }

   Texture tryGetTexture(String var1) {
      Object var2 = UIManager.tableget(this.table, var1);
      return var2 instanceof Texture ? (Texture)var2 : null;
   }

   static class Slice {
      public double leftSide = 0.0;
      public double rightSide = 256.0;
      public double topSide = 0.0;
      public double downSide = 256.0;
      public double uvLeft = 0.0;
      public double uvRight = 1.0;
      public double uvTop = 0.0;
      public double uvDown = 1.0;

      public Slice(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15) {
         this.leftSide = var1;
         this.rightSide = var3;
         this.topSide = var5;
         this.downSide = var7;
         this.uvLeft = var9;
         this.uvRight = var11;
         this.uvTop = var13;
         this.uvDown = var15;
      }
   }
}
