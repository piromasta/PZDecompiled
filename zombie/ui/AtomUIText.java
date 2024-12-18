package zombie.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.debug.DebugOptions;

public class AtomUIText extends AtomUI {
   AngelCodeFont fontToUse;
   String text;
   double textTracking = 0.0;
   double textLeading = 0.0;
   int autoWidth = -1;
   float outlineThick = 0.0F;
   float outlineColorR = 0.0F;
   float outlineColorG = 0.0F;
   float outlineColorB = 0.0F;
   float outlineColorA = 0.0F;
   boolean shadow = false;
   float shadowValue = 0.0F;
   private int charNum = 0;
   private int textWidth;
   private int textHeight;
   private int realTextHeight;
   private static char[] data = new char[256];
   ArrayList<CharData> textData = new ArrayList();

   public AtomUIText(KahluaTable var1) {
      super(var1);
   }

   public void render() {
      if (this.visible) {
         this.drawText();
         super.render();
      }
   }

   public void init() {
      super.init();
      this.updateInternalValues();
   }

   void drawText() {
      DebugOptions.instance.IsoSprite.ForceNearestMagFilter.setValue(false);
      TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
      TextManager.sdfShader.updateShadow(this.shadowValue);
      TextManager.sdfShader.updateOutline(this.outlineThick, this.outlineColorR, this.outlineColorG, this.outlineColorB, this.outlineColorA);
      IndieGL.StartShader(TextManager.sdfShader);
      double var1 = this.pivotX * (double)this.textWidth;
      double var3 = this.pivotY * (double)this.textHeight;
      Iterator var5 = this.textData.iterator();

      while(var5.hasNext()) {
         CharData var6 = (CharData)var5.next();
         double var7 = var6.x + (double)var6.def.xoffset - var1;
         double var9 = var6.y + (double)var6.def.yoffset - var3;
         double var11 = var7 + (double)var6.def.width;
         double var13 = var9 + (double)var6.def.height;
         double[] var15 = this.getAbsolutePosition(var7, var9);
         double[] var16 = this.getAbsolutePosition(var11, var9);
         double[] var17 = this.getAbsolutePosition(var11, var13);
         double[] var18 = this.getAbsolutePosition(var7, var13);
         SpriteRenderer.instance.render(var6.def.image, var15[0], var15[1], var16[0], var16[1], var17[0], var17[1], var18[0], var18[1], this.colorR, this.colorG, this.colorB, this.colorA, (Consumer)null);
      }

      IndieGL.EndShader();
   }

   float getSdfThreshold() {
      double[] var1 = this.getAbsolutePosition(-5.0, 0.0);
      double[] var2 = this.getAbsolutePosition(5.0, 0.0);
      double var3 = Math.hypot(var1[0] - var2[0], var1[1] - var2[1]);
      return (float)(0.125 / (var3 / 10.0));
   }

   void loadFromTable() {
      super.loadFromTable();
      this.fontToUse = TextManager.instance.getFontFromEnum(this.tryGetFont("font", UIFont.SdfRegular));
      this.text = this.tryGetString("text", "");
      this.textTracking = this.tryGetDouble("textTracking", 0.0);
      this.textLeading = this.tryGetDouble("textLeading", 0.0);
      this.autoWidth = (int)this.tryGetDouble("autoWidth", -1.0);
      this.outlineThick = (float)this.tryGetDouble("outlineThick", 0.0);
      this.outlineColorR = (float)this.tryGetDouble("outlineColorR", 0.0);
      this.outlineColorG = (float)this.tryGetDouble("outlineColorG", 0.0);
      this.outlineColorB = (float)this.tryGetDouble("outlineColorB", 0.0);
      this.outlineColorA = (float)this.tryGetDouble("outlineColorA", 0.0);
      this.shadow = this.tryGetBoolean("shadow", false);
      this.shadowValue = this.shadow ? 1.0F : 0.0F;
   }

   void updateCharData(AngelCodeFont.CharDef var1, double var2, double var4) {
      if (this.charNum >= this.textData.size()) {
         this.textData.add(new CharData());
      }

      CharData var6 = (CharData)this.textData.get(this.charNum);
      var6.def = var1;
      var6.x = var2;
      var6.y = var4;
      ++this.charNum;
   }

   void updateInternalValues() {
      super.updateInternalValues();
      this.textWidth = 0;
      this.textHeight = this.fontToUse.getHeight(this.text);
      this.textData.clear();
      this.charNum = 0;
      int var1 = this.text.length();
      if (data.length < var1) {
         data = new char[(var1 + 128 - 1) / 128 * 128];
      }

      this.text.getChars(0, var1, data, 0);
      ArrayList var2 = new ArrayList();
      int var3 = -1;
      int var4 = 0;
      double var5 = 0.0;
      float var7 = 0.0F;
      float var8 = 0.0F;
      float var9 = 0.0F;
      AngelCodeFont.CharDef var10 = null;
      int var11 = 0;
      int var12;
      char var13;
      AngelCodeFont.CharDef var16;
      if (this.autoWidth != -1) {
         for(var12 = 0; var12 < var1; ++var12) {
            var13 = data[var12];
            if (var13 == '\n') {
               var7 = 0.0F;

               for(int var17 = var4; var17 <= var12; ++var17) {
                  var2.add(0.0);
               }

               var4 = var12 + 1;
               var3 = -1;
               var11 = 0;
               ++var8;
            } else {
               if (var13 == ' ') {
                  var5 = (double)((float)this.autoWidth - var7);
                  if (var8 == 0.0F && var10 != null) {
                     var5 -= (double)var10.xadvance / 2.0;
                  }

                  var3 = var12;
                  var9 = var7;
                  ++var11;
               }

               if (var7 >= (float)this.autoWidth && var3 != -1) {
                  data[var3] = '\n';
                  boolean var14 = true;

                  for(int var15 = var4; var15 <= var3; ++var15) {
                     if (data[var15] == ' ') {
                        if (!var14) {
                           var2.add(var5 / (double)(var11 - 1));
                        } else {
                           --var11;
                           var2.add(0.0);
                        }
                     } else {
                        var14 = false;
                        var2.add(0.0);
                     }
                  }

                  var4 = var3 + 1;
                  var3 = -1;
                  var11 = 0;
                  ++var8;
                  var7 -= var9;
               }

               if (var13 >= this.fontToUse.chars.length) {
                  var13 = '?';
               }

               var16 = this.fontToUse.chars[var13];
               if (var16 != null) {
                  if (var10 != null) {
                     var7 += (float)var10.getKerning(var13);
                  }

                  var10 = var16;
                  var7 = (float)((double)var7 + (double)var16.xadvance + this.textTracking);
               }
            }
         }
      }

      var7 = 0.0F;
      var8 = 0.0F;
      var10 = null;

      for(var12 = 0; var12 < var1; ++var12) {
         var13 = data[var12];
         if (var13 == '\n') {
            this.textWidth = (int)Math.max(var7, (float)this.textWidth);
            var7 = 0.0F;
            var8 = (float)((double)var8 + (double)this.fontToUse.getLineHeight() + this.textLeading);
         } else {
            if (var13 >= this.fontToUse.chars.length) {
               var13 = '?';
            }

            var16 = this.fontToUse.chars[var13];
            if (var16 != null) {
               if (var10 != null) {
                  var7 += (float)var10.getKerning(var13);
               }

               var10 = var16;
               this.updateCharData(var16, (double)var7, (double)var8);
               var7 = (float)((double)var7 + (double)var16.xadvance + this.textTracking);
               if (var12 < var2.size()) {
                  var7 = (float)((double)var7 + (Double)var2.get(var12));
               }
            }
         }
      }

      this.textWidth = (int)Math.max(var7, (float)this.textWidth);
      this.realTextHeight = (int)Math.max(var8, (float)this.textHeight);
   }

   public void setFont(UIFont var1) {
      this.fontToUse = TextManager.instance.getFontFromEnum(var1);
      this.updateInternalValues();
   }

   public void setText(String var1) {
      this.text = var1;
      this.updateInternalValues();
   }

   public void setAutoWidth(Double var1) {
      this.autoWidth = var1.intValue();
      this.updateInternalValues();
   }

   public Double getTextHeight() {
      return (double)this.realTextHeight;
   }

   public Double getTextWidth() {
      return (double)this.textWidth;
   }

   UIFont tryGetFont(String var1, UIFont var2) {
      Object var3 = UIManager.tableget(this.table, var1);
      return var3 instanceof UIFont ? (UIFont)var3 : var2;
   }

   static class CharData {
      public AngelCodeFont.CharDef def;
      public double x;
      public double y;

      CharData() {
      }
   }
}
