package zombie.ui;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.SDFShader;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.ServerGUI;

public final class TextManager {
   public AngelCodeFont font;
   public AngelCodeFont font2;
   public AngelCodeFont font3;
   public AngelCodeFont font4;
   public AngelCodeFont main1;
   public AngelCodeFont main2;
   public AngelCodeFont zombiefontcredits1;
   public AngelCodeFont zombiefontcredits2;
   public AngelCodeFont zombienew1;
   public AngelCodeFont zombienew2;
   public AngelCodeFont zomboidDialogue;
   public AngelCodeFont codetext;
   public AngelCodeFont debugConsole;
   public AngelCodeFont intro;
   public AngelCodeFont handwritten;
   public final AngelCodeFont[] normal = new AngelCodeFont[14];
   public AngelCodeFont zombienew3;
   public final AngelCodeFont[] enumToFont = new AngelCodeFont[UIFont.values().length];
   public static SDFShader sdfShader;
   public static final TextManager instance = new TextManager();
   public ArrayList<DeferedTextDraw> todoTextList = new ArrayList();

   public TextManager() {
   }

   public void DrawString(double var1, double var3, String var5) {
      this.font.drawString((float)var1, (float)var3, var5, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void DrawString(double var1, double var3, String var5, double var6, double var8, double var10, double var12) {
      this.font.drawString((float)var1, (float)var3, var5, (float)var6, (float)var8, (float)var10, (float)var12);
   }

   public void DrawString(UIFont var1, double var2, double var4, double var6, String var8, double var9, double var11, double var13, double var15) {
      AngelCodeFont var17 = this.getFontFromEnum(var1);
      var17.drawString((float)var2, (float)var4, (float)var6, var8, (float)var9, (float)var11, (float)var13, (float)var15);
   }

   public void DrawString(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
      AngelCodeFont var15 = this.getFontFromEnum(var1);
      var15.drawString((float)var2, (float)var4, var6, (float)var7, (float)var9, (float)var11, (float)var13);
   }

   public void DrawStringUntrimmed(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
      AngelCodeFont var15 = this.getFontFromEnum(var1);
      var15.drawString((float)var2, (float)var4, var6, (float)var7, (float)var9, (float)var11, (float)var13);
   }

   public void DrawStringCentre(double var1, double var3, String var5, double var6, double var8, double var10, double var12) {
      var1 -= (double)(this.font.getWidth(var5) / 2);
      this.font.drawString((float)var1, (float)var3, var5, (float)var6, (float)var8, (float)var10, (float)var12);
   }

   public void DrawStringCentre(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
      AngelCodeFont var15 = this.getFontFromEnum(var1);
      var2 -= (double)(var15.getWidth(var6) / 2);
      var15.drawString((float)var2, (float)var4, var6, (float)var7, (float)var9, (float)var11, (float)var13);
   }

   public void DrawStringCentreDefered(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
      this.todoTextList.add(new DeferedTextDraw(var1, var2, var4, var6, var7, var9, var11, var13));
   }

   public void DrawTextFromGameWorld() {
      for(int var1 = 0; var1 < this.todoTextList.size(); ++var1) {
         DeferedTextDraw var2 = (DeferedTextDraw)this.todoTextList.get(var1);
         this.DrawStringCentre(var2.font, var2.x, var2.y, var2.str, var2.r, var2.g, var2.b, var2.a);
      }

      this.todoTextList.clear();
   }

   public void DrawStringRight(double var1, double var3, String var5, double var6, double var8, double var10, double var12) {
      var1 -= (double)this.font.getWidth(var5);
      this.font.drawString((float)var1, (float)var3, var5, (float)var6, (float)var8, (float)var10, (float)var12);
   }

   public TextDrawObject GetDrawTextObject(String var1, int var2, boolean var3) {
      TextDrawObject var4 = new TextDrawObject();
      return var4;
   }

   public void DrawTextObject(double var1, double var3, TextDrawObject var5) {
   }

   public void DrawStringBBcode(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
   }

   public AngelCodeFont getNormalFromFontSize(int var1) {
      return this.normal[var1 - 11];
   }

   public AngelCodeFont getFontFromEnum(UIFont var1) {
      if (var1 == null) {
         return this.font;
      } else {
         AngelCodeFont var2 = this.enumToFont[var1.ordinal()];
         return var2 == null ? this.font : var2;
      }
   }

   public int getFontHeight(UIFont var1) {
      AngelCodeFont var2 = this.getFontFromEnum(var1);
      return var2.getLineHeight();
   }

   public void DrawStringRight(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
      AngelCodeFont var15 = this.getFontFromEnum(var1);
      var2 -= (double)var15.getWidth(var6);
      var15.drawString((float)var2, (float)var4, var6, (float)var7, (float)var9, (float)var11, (float)var13);
   }

   private String getFontFilePath(String var1, String var2, String var3) {
      String var4;
      if (var2 != null) {
         var4 = "media/fonts/" + var1 + "/" + var2 + "/" + var3;
         if (ZomboidFileSystem.instance.getString(var4) != var4) {
            return var4;
         }
      }

      var4 = "media/fonts/" + var1 + "/" + var3;
      if (ZomboidFileSystem.instance.getString(var4) != var4) {
         return var4;
      } else {
         if (!"EN".equals(var1)) {
            if (var2 != null) {
               var4 = "media/fonts/EN/" + var2 + "/" + var3;
               if (ZomboidFileSystem.instance.getString(var4) != var4) {
                  return var4;
               }
            }

            var4 = "media/fonts/EN/" + var3;
            if (ZomboidFileSystem.instance.getString(var4) != var4) {
               return var4;
            }
         }

         var4 = "media/fonts/" + var3;
         return ZomboidFileSystem.instance.getString(var4) != var4 ? var4 : "media/" + var3;
      }
   }

   public void Init() throws FileNotFoundException {
      FontsFile var2 = new FontsFile();
      HashMap var3 = new HashMap();
      String var4 = Translator.getLanguage().name();
      String var1;
      if (Core.getInstance().getOptionEnableDyslexicFont()) {
         var1 = ZomboidFileSystem.instance.getString("media/fonts/" + var4 + "/fontsDyslexic.txt");
         var2.read(var1, var3);
      }

      if (var3.isEmpty()) {
         var1 = ZomboidFileSystem.instance.getString("media/fonts/EN/fonts.txt");
         var2.read(var1, var3);
         if (!"EN".equals(var4)) {
            var1 = ZomboidFileSystem.instance.getString("media/fonts/" + var4 + "/fonts.txt");
            var2.read(var1, var3);
         }
      }

      HashMap var5 = new HashMap();
      int var6 = Core.getInstance().getOptionFontSizeReal();
      String var7 = null;
      if (var6 == 2) {
         var7 = "1x";
      } else if (var6 == 3) {
         var7 = "2x";
      } else if (var6 == 4) {
         var7 = "3x";
      } else if (var6 == 5) {
         var7 = "4x";
      }

      AngelCodeFont[] var8 = this.enumToFont;
      int var9 = var8.length;

      int var10;
      AngelCodeFont var11;
      for(var10 = 0; var10 < var9; ++var10) {
         var11 = var8[var10];
         if (var11 != null) {
            var11.destroy();
         }
      }

      Arrays.fill(this.enumToFont, (Object)null);
      var8 = this.normal;
      var9 = var8.length;

      for(var10 = 0; var10 < var9; ++var10) {
         var11 = var8[var10];
         if (var11 != null) {
            var11.destroy();
         }
      }

      Arrays.fill(this.normal, (Object)null);
      UIFont[] var17 = UIFont.values();
      var9 = var17.length;

      for(var10 = 0; var10 < var9; ++var10) {
         UIFont var19 = var17[var10];
         FontsFileFont var12 = (FontsFileFont)var3.get(var19.name());
         if (var12 == null) {
            DebugLog.General.warn("font \"%s\" not found in fonts.txt", var19.name());
         } else {
            String var13 = this.getFontFilePath(var4, var7, var12.fnt);
            String var14 = null;
            if (var12.img != null) {
               var14 = this.getFontFilePath(var4, var7, var12.img);
            }

            String var15 = var13 + "|" + var14;
            if (var5.get(var15) != null) {
               this.enumToFont[var19.ordinal()] = (AngelCodeFont)var5.get(var15);
            } else {
               AngelCodeFont var16 = new AngelCodeFont(var13, var14);
               this.enumToFont[var19.ordinal()] = var16;
               var5.put(var15, var16);
            }
         }
      }

      if (this.enumToFont[UIFont.DebugConsole.ordinal()] == null) {
         this.enumToFont[UIFont.DebugConsole.ordinal()] = this.enumToFont[UIFont.Small.ordinal()];
      }

      for(int var18 = 0; var18 < this.normal.length; ++var18) {
         this.normal[var18] = new AngelCodeFont("media/fonts/zomboidNormal" + (var18 + 11) + ".fnt", "media/fonts/zomboidNormal" + (var18 + 11) + "_0");
      }

      this.font = this.enumToFont[UIFont.Small.ordinal()];
      this.font2 = this.enumToFont[UIFont.Medium.ordinal()];
      this.font3 = this.enumToFont[UIFont.Large.ordinal()];
      this.font4 = this.enumToFont[UIFont.Massive.ordinal()];
      this.main1 = this.enumToFont[UIFont.MainMenu1.ordinal()];
      this.main2 = this.enumToFont[UIFont.MainMenu2.ordinal()];
      this.zombiefontcredits1 = this.enumToFont[UIFont.Cred1.ordinal()];
      this.zombiefontcredits2 = this.enumToFont[UIFont.Cred2.ordinal()];
      this.zombienew1 = this.enumToFont[UIFont.NewSmall.ordinal()];
      this.zombienew2 = this.enumToFont[UIFont.NewMedium.ordinal()];
      this.zombienew3 = this.enumToFont[UIFont.NewLarge.ordinal()];
      this.codetext = this.enumToFont[UIFont.Code.ordinal()];
      this.enumToFont[UIFont.MediumNew.ordinal()] = null;
      this.enumToFont[UIFont.AutoNormSmall.ordinal()] = null;
      this.enumToFont[UIFont.AutoNormMedium.ordinal()] = null;
      this.enumToFont[UIFont.AutoNormLarge.ordinal()] = null;
      this.zomboidDialogue = this.enumToFont[UIFont.Dialogue.ordinal()];
      this.intro = this.enumToFont[UIFont.Intro.ordinal()];
      this.handwritten = this.enumToFont[UIFont.Handwritten.ordinal()];
      this.debugConsole = this.enumToFont[UIFont.DebugConsole.ordinal()];
      RenderThread.invokeOnRenderContext(() -> {
         sdfShader = new SDFShader("sdf");
      });
   }

   public int MeasureStringX(UIFont var1, String var2) {
      if (GameServer.bServer && !ServerGUI.isCreated()) {
         return 0;
      } else if (var2 == null) {
         return 0;
      } else {
         AngelCodeFont var3 = this.getFontFromEnum(var1);
         return var3.getWidth(var2);
      }
   }

   public int CentreStringYOffset(UIFont var1, String var2) {
      return this.MeasureStringYOffset(var1, var2) - (this.MeasureStringY(var1, var2) - this.MeasureStringYReal(var1, var2)) / 2;
   }

   public int MeasureStringY(UIFont var1, String var2) {
      return this.MeasureStringY(var1, var2, false, false);
   }

   public int MeasureStringYReal(UIFont var1, String var2) {
      return this.MeasureStringY(var1, var2, true, false);
   }

   public int MeasureStringYOffset(UIFont var1, String var2) {
      return this.MeasureStringY(var1, var2, false, true);
   }

   public int MeasureStringY(UIFont var1, String var2, boolean var3, boolean var4) {
      if (var1 != null && var2 != null) {
         if (GameServer.bServer && !ServerGUI.isCreated()) {
            return 0;
         } else {
            AngelCodeFont var5 = this.getFontFromEnum(var1);
            return var5.getHeight(var2, var3, var4);
         }
      } else {
         return 0;
      }
   }

   public int MeasureFont(UIFont var1) {
      if (var1 == UIFont.Small) {
         return 10;
      } else if (var1 == UIFont.Dialogue) {
         return 20;
      } else if (var1 == UIFont.Medium) {
         return 20;
      } else if (var1 == UIFont.Large) {
         return 24;
      } else if (var1 == UIFont.Massive) {
         return 30;
      } else if (var1 == UIFont.MainMenu1) {
         return 30;
      } else {
         return var1 == UIFont.MainMenu2 ? 30 : this.getFontFromEnum(var1).getLineHeight();
      }
   }

   public String WrapText(UIFont var1, String var2, int var3) {
      return this.WrapText(var1, var2, var3, -1, "");
   }

   public String WrapText(UIFont var1, String var2, int var3, int var4, String var5) {
      ArrayList var6 = new ArrayList();
      String[] var7 = var2.split("\\r?\\n");
      int var8 = this.MeasureStringX(var1, " ");

      int var9;
      for(var9 = 0; var9 < var7.length; ++var9) {
         int var10 = this.MeasureStringX(var1, var7[var9]);
         if (var10 <= var3) {
            var6.add(var7[var9]);
         } else {
            String[] var11 = var7[var9].split(" ");
            ArrayList var12 = new ArrayList();
            int var13 = 0;

            for(int var14 = 0; var14 < var11.length; ++var14) {
               int var15 = var13 + this.MeasureStringX(var1, var11[var14]);
               String var22;
               if (var15 <= var3) {
                  var12.add(var11[var14]);
                  var13 += this.MeasureStringX(var1, var11[var14]) + var8;
               } else if (var12.size() != 0) {
                  var22 = String.join(" ", var12);
                  var6.add(var22);
                  var12.clear();
                  var12.add(var11[var14]);
                  var13 = this.MeasureStringX(var1, var11[var14]) + var8;
               } else {
                  int var16 = (int)Math.floor((double)var3 / (double)var15 * (double)var11[var14].length()) - 1;
                  int var17 = (int)Math.ceil((double)var11[var14].length() / (double)var16);

                  for(int var18 = 0; var18 < var17 - 1; ++var18) {
                     String var10001 = var11[var14].substring(var18 * var16, (var18 + 1) * var16);
                     var6.add(var10001 + "-");
                  }

                  String var23 = var11[var14].substring((var17 - 1) * var16);
                  var12.add(var23);
                  var13 = this.MeasureStringX(var1, var23) + var8;
               }

               if (var14 == var11.length - 1) {
                  var22 = String.join(" ", var12);
                  var6.add(var22);
                  var13 = 0;
                  var12.clear();
               }
            }
         }
      }

      if (var4 > 0 && var6.size() > var4) {
         var9 = this.MeasureStringX(var1, var5);
         String var10000;
         String var19;
         if (var3 - this.MeasureStringX(var1, (String)var6.get(var4 - 1)) < var9) {
            var19 = (String)var6.get(var4 - 1);
            int var20 = Math.max(var19.length() - var5.length(), 0);
            var10000 = var19.substring(0, var20);
            String var21 = var10000 + var5;
            var6.set(var4 - 1, var21);
         } else {
            var10000 = (String)var6.get(var4 - 1);
            var19 = var10000 + var5;
            var6.set(var4 - 1, var19);
         }

         return String.join("\n", var6.subList(0, var4));
      } else {
         return String.join("\n", var6);
      }
   }

   public static class DeferedTextDraw {
      public double x;
      public double y;
      public UIFont font;
      public String str;
      public double r;
      public double g;
      public double b;
      public double a;

      public DeferedTextDraw(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13) {
         this.font = var1;
         this.x = var2;
         this.y = var4;
         this.str = var6;
         this.r = var7;
         this.g = var9;
         this.b = var11;
         this.a = var13;
      }
   }

   public interface StringDrawer {
      void draw(UIFont var1, double var2, double var4, String var6, double var7, double var9, double var11, double var13);
   }
}
