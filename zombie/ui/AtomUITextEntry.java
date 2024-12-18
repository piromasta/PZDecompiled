package zombie.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import org.lwjglx.input.Keyboard;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.core.Clipboard;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;

public class AtomUITextEntry extends AtomUI implements UITextEntryInterface {
   boolean DoingTextEntry = false;
   int TextEntryCursorPos = 0;
   boolean IsEditable = true;
   boolean IsSelectable = true;
   int ToSelectionIndex = 0;
   boolean bMask = false;
   boolean BlinkState = true;
   int BlinkFramesOn = 6;
   int BlinkFramesOff = 4;
   float BlinkFrame;
   boolean SelectingRange;
   int TextEntryMaxLength;
   boolean onlyNumbers;
   boolean onlyText;
   int maxTextLength;
   boolean forceUpperCase;
   boolean bMultiline;
   AngelCodeFont.CharDef cursorDef;
   AngelCodeFont.CharDef maskDef;
   AngelCodeFont fontToUse;
   String text;
   double textTracking;
   double textLeading;
   private int charNum;
   private int textWidth;
   private int textHeight;
   private static char[] data = new char[256];
   ArrayList<CharData> textData;
   Object luaOnTextChange;

   public AtomUITextEntry(KahluaTable var1) {
      super(var1);
      this.BlinkFrame = (float)this.BlinkFramesOn;
      this.SelectingRange = false;
      this.TextEntryMaxLength = 2000;
      this.onlyNumbers = false;
      this.onlyText = false;
      this.maxTextLength = -1;
      this.forceUpperCase = false;
      this.bMultiline = false;
      this.textTracking = 0.0;
      this.textLeading = 0.0;
      this.charNum = 0;
      this.textData = new ArrayList();
   }

   public void render() {
      if (this.visible) {
         this.drawText();
         this.drawSelection();
         if (this.DoingTextEntry && this.BlinkState) {
            this.drawCursor();
         }

         super.render();
      }
   }

   public void init() {
      super.init();
      this.updateInternalValues();
      byte var1 = 124;
      this.cursorDef = this.fontToUse.chars[var1];
      var1 = 42;
      this.maskDef = this.fontToUse.chars[var1];
   }

   public void update() {
      if (this.maxTextLength > -1 && this.text.length() > this.maxTextLength) {
         this.text = this.text.substring(0, this.maxTextLength);
      }

      if (this.forceUpperCase) {
         this.text = this.text.toUpperCase();
      }

      super.update();
      if (this.BlinkFrame > 0.0F) {
         this.BlinkFrame -= GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 30.0F;
      } else {
         this.BlinkState = !this.BlinkState;
         if (this.BlinkState) {
            this.BlinkFrame = (float)this.BlinkFramesOn;
         } else {
            this.BlinkFrame = (float)this.BlinkFramesOff;
         }
      }

   }

   void drawSelection() {
      if (this.ToSelectionIndex != this.TextEntryCursorPos) {
         int var1 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
         int var2 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
         double var3 = this.pivotX * (double)this.textWidth;
         double var5 = this.pivotY * (double)this.textHeight;
         double var7 = 9999999.0;
         double var9 = 9999999.0;

         for(int var11 = var1; var11 <= var2; ++var11) {
            CharData var12;
            if (var11 < this.textData.size()) {
               var12 = (CharData)this.textData.get(var11);
               if (var7 == 9999999.0 || var11 > 0 && ((CharData)this.textData.get(var11 - 1)).id == 10) {
                  var7 = var12.x - var3;
                  var9 = var12.y - var5;
               }
            }

            double var13;
            double var15;
            double var17;
            double[] var19;
            double[] var20;
            double[] var21;
            double[] var22;
            if (var11 < this.textData.size() - 2 && ((CharData)this.textData.get(var11 + 1)).id == 10) {
               var12 = (CharData)this.textData.get(var11);
               var13 = var12.def == null ? 0.0 : (double)var12.def.xadvance;
               var15 = var12.x - var3 + var13;
               var17 = var9 + (double)this.fontToUse.getLineHeight();
               var19 = this.getAbsolutePosition(var7, var9);
               var20 = this.getAbsolutePosition(var15, var9);
               var21 = this.getAbsolutePosition(var15, var17);
               var22 = this.getAbsolutePosition(var7, var17);
               SpriteRenderer.instance.render((Texture)null, var19[0], var19[1], var20[0], var20[1], var21[0], var21[1], var22[0], var22[1], 0.39215687F, 0.39215687F, 0.8627451F, 0.627451F, (Consumer)null);
            } else if (var11 == var2) {
               var12 = (CharData)this.textData.get(var11 - 1);
               var13 = var12.def == null ? 0.0 : (double)var12.def.xadvance;
               var15 = var12.x - var3 + var13;
               var17 = var9 + (double)this.fontToUse.getLineHeight();
               var19 = this.getAbsolutePosition(var7, var9);
               var20 = this.getAbsolutePosition(var15, var9);
               var21 = this.getAbsolutePosition(var15, var17);
               var22 = this.getAbsolutePosition(var7, var17);
               SpriteRenderer.instance.render((Texture)null, var19[0], var19[1], var20[0], var20[1], var21[0], var21[1], var22[0], var22[1], 0.39215687F, 0.39215687F, 0.8627451F, 0.627451F, (Consumer)null);
            }
         }
      }

   }

   void drawCursor() {
      DebugOptions.instance.IsoSprite.ForceNearestMagFilter.setValue(false);
      TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
      IndieGL.StartShader(TextManager.sdfShader);
      double var1 = this.pivotX * (double)this.textWidth;
      double var3 = this.pivotY * (double)this.textHeight;
      double[] var14;
      double[] var15;
      double[] var16;
      if (this.textData.size() == 0) {
         double var5 = (double)(-this.cursorDef.width) / 2.0 - var1;
         double var7 = (double)this.cursorDef.yoffset - var3;
         double var9 = var5 + (double)this.cursorDef.width;
         double var11 = var7 + (double)this.cursorDef.height;
         double[] var13 = this.getAbsolutePosition(var5, var7);
         var14 = this.getAbsolutePosition(var9, var7);
         var15 = this.getAbsolutePosition(var9, var11);
         var16 = this.getAbsolutePosition(var5, var11);
         SpriteRenderer.instance.render(this.cursorDef.image, var13[0], var13[1], var14[0], var14[1], var15[0], var15[1], var16[0], var16[1], this.colorR, this.colorG, this.colorB, this.colorA, (Consumer)null);
      } else {
         double var6;
         double var8;
         double var10;
         CharData var18;
         if (this.TextEntryCursorPos == 0) {
            var18 = (CharData)this.textData.get(0);
            var6 = var18.x - (double)this.cursorDef.xoffset - (double)this.cursorDef.width / 4.0 - var1;
            var8 = var18.y + (double)this.cursorDef.yoffset - var3;
         } else if (this.TextEntryCursorPos >= this.textData.size()) {
            var18 = (CharData)this.textData.get(this.textData.size() - 1);
            var10 = var18.def == null ? 0.0 : (double)var18.def.xadvance;
            var6 = var18.x - (double)this.cursorDef.xoffset + var10 - (double)this.cursorDef.width / 4.0 - var1;
            var8 = var18.y + (double)this.cursorDef.yoffset - var3;
         } else {
            var18 = (CharData)this.textData.get(this.TextEntryCursorPos);
            var6 = var18.x - (double)this.cursorDef.xoffset - (double)this.cursorDef.width / 4.0 - var1;
            var8 = var18.y + (double)this.cursorDef.yoffset - var3;
         }

         if (this.TextEntryCursorPos >= this.textData.size() && this.text.substring(this.TextEntryCursorPos - 1).equals("\n")) {
            var6 = (double)this.cursorDef.xoffset - var1;
            var8 += (double)this.fontToUse.getLineHeight();
         }

         var10 = var6 + (double)this.cursorDef.width;
         double var12 = var8 + (double)this.cursorDef.height;
         var14 = this.getAbsolutePosition(var6, var8);
         var15 = this.getAbsolutePosition(var10, var8);
         var16 = this.getAbsolutePosition(var10, var12);
         double[] var17 = this.getAbsolutePosition(var6, var12);
         SpriteRenderer.instance.render(this.cursorDef.image, var14[0], var14[1], var15[0], var15[1], var16[0], var16[1], var17[0], var17[1], this.colorR, this.colorG, this.colorB, this.colorA, (Consumer)null);
      }

      IndieGL.EndShader();
   }

   void drawText() {
      DebugOptions.instance.IsoSprite.ForceNearestMagFilter.setValue(false);
      TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
      IndieGL.StartShader(TextManager.sdfShader);
      double var1 = this.pivotX * (double)this.textWidth;
      double var3 = this.pivotY * (double)this.textHeight;
      Iterator var5 = this.textData.iterator();

      while(var5.hasNext()) {
         CharData var6 = (CharData)var5.next();
         if (var6.id != 10) {
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
      this.onlyNumbers = this.tryGetBoolean("onlyNumbers", false);
      this.onlyText = this.tryGetBoolean("onlyText", false);
      this.forceUpperCase = this.tryGetBoolean("forceUpperCase", false);
      this.maxTextLength = (int)this.tryGetDouble("maxTextLength", -1.0);
      this.bMask = this.tryGetBoolean("isMask", false);
      this.bMultiline = this.tryGetBoolean("isMultiline", false);
      this.textTracking = this.tryGetDouble("textTracking", 0.0);
      this.textLeading = this.tryGetDouble("textLeading", 0.0);
      this.luaOnTextChange = this.tryGetClosure("onTextChange");
   }

   void updateCharData(AngelCodeFont.CharDef var1, double var2, double var4, int var6) {
      if (this.charNum >= this.textData.size()) {
         this.textData.add(new CharData());
      }

      CharData var7 = (CharData)this.textData.get(this.charNum);
      var7.def = var1;
      var7.x = var2;
      var7.y = var4;
      var7.id = var6;
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
      float var2 = 0.0F;
      float var3 = 0.0F;
      AngelCodeFont.CharDef var4 = null;

      for(int var5 = 0; var5 < var1; ++var5) {
         char var6 = data[var5];
         AngelCodeFont.CharDef var7;
         if (var6 == '\n') {
            var7 = this.fontToUse.chars[var6];
            this.updateCharData(var7, (double)var2, (double)var3, var6);
            this.textWidth = (int)Math.max(var2, (float)this.textWidth);
            var2 = 0.0F;
            var3 = (float)((double)var3 + (double)this.fontToUse.getLineHeight() + this.textLeading);
         } else {
            if (this.bMask) {
               var6 = '*';
            }

            if (var6 >= this.fontToUse.chars.length) {
               var6 = '?';
            }

            var7 = this.fontToUse.chars[var6];
            if (var7 != null) {
               if (var4 != null) {
                  var2 += (float)var4.getKerning(var6);
               }

               var4 = var7;
               this.updateCharData(var7, (double)var2, (double)var3, var6);
               var2 = (float)((double)var2 + (double)var7.xadvance + this.textTracking);
            }
         }
      }

      this.textWidth = (int)Math.max(var2, (float)this.textWidth);
   }

   public void setFont(UIFont var1) {
      this.fontToUse = TextManager.instance.getFontFromEnum(var1);
      this.updateInternalValues();
   }

   public void setText(String var1) {
      this.text = var1;
      this.updateInternalValues();
      if (this.luaOnTextChange != null) {
         LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
      }

   }

   UIFont tryGetFont(String var1, UIFont var2) {
      Object var3 = UIManager.tableget(this.table, var1);
      return var3 instanceof UIFont ? (UIFont)var3 : var2;
   }

   public void setMask(boolean var1) {
      this.bMask = var1;
      this.updateInternalValues();
   }

   public boolean isMask() {
      return this.bMask;
   }

   public void focus() {
      this.DoingTextEntry = true;
      Core.CurrentTextEntryBox = this;
      this.TextEntryCursorPos = this.text.length();
      this.ToSelectionIndex = this.text.length();
   }

   public void unfocus() {
      this.DoingTextEntry = false;
      if (Core.CurrentTextEntryBox == this) {
         Core.CurrentTextEntryBox = null;
      }

   }

   public void onOtherKey(int var1) {
   }

   public void putCharacter(char var1) {
      int var2;
      if (this.TextEntryCursorPos == this.ToSelectionIndex) {
         var2 = this.TextEntryCursorPos;
         if (var2 < this.text.length()) {
            this.text = this.text.substring(0, var2) + var1 + this.text.substring(var2);
         } else {
            this.text = this.text + var1;
         }

         ++this.TextEntryCursorPos;
         ++this.ToSelectionIndex;
         if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
         }
      } else {
         var2 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
         int var3 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
         if (this.text.length() > 0) {
            this.text = this.text.substring(0, var2) + var1 + this.text.substring(var3);
         } else {
            this.text = "" + var1;
         }

         this.ToSelectionIndex = var2 + 1;
         this.TextEntryCursorPos = var2 + 1;
         if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
         }
      }

      this.updateInternalValues();
   }

   int getCursorPos(double var1, double var3) {
      double var5 = 99999.0;
      int var7 = 0;
      double[] var8 = this.getAbsolutePosition(var1, var3);
      double var9 = this.pivotX * (double)this.textWidth;
      double var11 = this.pivotY * (double)this.textHeight;

      for(int var13 = 0; var13 < this.textData.size(); ++var13) {
         CharData var14 = (CharData)this.textData.get(var13);
         if (var14.id != 10) {
            double var15 = var14.x + (double)var14.def.xoffset - var9;
            double var17 = var14.y + (double)this.fontToUse.getLineHeight() / 2.0 - var11;
            double[] var19 = this.getAbsolutePosition(var15, var17);
            double var20 = Math.hypot(var8[0] - var19[0], var8[1] - var19[1]);
            if (var20 < var5) {
               var7 = var13;
               var5 = var20;
            }

            if (var13 == this.textData.size() - 1 || ((CharData)this.textData.get(var13 + 1)).id == 10) {
               var15 += (double)var14.def.width;
               var19 = this.getAbsolutePosition(var15, var17);
               var20 = Math.hypot(var8[0] - var19[0], var8[1] - var19[1]);
               if (var20 < var5) {
                  var7 = var13 + 1;
                  if (var13 + 1 < this.textData.size() && ((CharData)this.textData.get(var13 + 1)).id == 10) {
                     var7 = var13 + 1;
                  }

                  var5 = var20;
               }
            }
         }
      }

      return var7;
   }

   public boolean onConsumeMouseButtonDown(int var1, double var2, double var4) {
      boolean var6 = super.onConsumeMouseButtonDown(var1, var2, var4);
      double[] var7 = this.toLocalCoordinates(var2, var4);
      if (var1 != 0) {
         return var6;
      } else if (!this.IsEditable && !this.IsSelectable) {
         return var6;
      } else {
         if (Core.CurrentTextEntryBox != this) {
            if (Core.CurrentTextEntryBox != null) {
               Core.CurrentTextEntryBox.setDoingTextEntry(false);
            }

            Core.CurrentTextEntryBox = this;
            Core.CurrentTextEntryBox.setSelectingRange(true);
         }

         if (!this.DoingTextEntry) {
            this.focus();
         }

         this.TextEntryCursorPos = this.getCursorPos(var7[0], var7[1]);
         this.ToSelectionIndex = this.TextEntryCursorPos;
         return true;
      }
   }

   public Boolean onConsumeMouseMove(double var1, double var3, double var5, double var7) {
      Boolean var9 = super.onConsumeMouseMove(var1, var3, var5, var7);
      double[] var10 = this.toLocalCoordinates(var5, var7);
      if ((this.IsEditable || this.IsSelectable) && this.SelectingRange) {
         this.TextEntryCursorPos = this.getCursorPos(var10[0], var10[1]);
         return true;
      } else {
         return var9;
      }
   }

   public void onExtendMouseMoveOutside(double var1, double var3, double var5, double var7) {
      super.onExtendMouseMoveOutside(var1, var3, var5, var7);
      double[] var9 = this.toLocalCoordinates(var5, var7);
      if ((this.IsEditable || this.IsSelectable) && this.SelectingRange) {
         this.TextEntryCursorPos = this.getCursorPos(var9[0], var9[1]);
      }

   }

   public boolean onConsumeMouseButtonUp(int var1, double var2, double var4) {
      boolean var6 = super.onConsumeMouseButtonUp(var1, var2, var4);
      this.SelectingRange = false;
      return var6;
   }

   public void onMouseButtonUpOutside(int var1, double var2, double var4) {
      super.onMouseButtonUpOutside(var1, var2, var4);
      this.SelectingRange = false;
   }

   public boolean isDoingTextEntry() {
      return this.DoingTextEntry;
   }

   public void setDoingTextEntry(boolean var1) {
      this.DoingTextEntry = var1;
   }

   public boolean isEditable() {
      return this.IsEditable;
   }

   public UINineGrid getFrame() {
      return null;
   }

   public boolean isIgnoreFirst() {
      return false;
   }

   public void setIgnoreFirst(boolean var1) {
   }

   public void setSelectingRange(boolean var1) {
      this.SelectingRange = var1;
   }

   public Color getStandardFrameColour() {
      return new Color(50, 50, 50, 212);
   }

   public void onKeyEnter() {
      if (!this.bMultiline) {
         this.unfocus();
      } else {
         int var1;
         String var10001;
         if (this.TextEntryCursorPos != this.ToSelectionIndex) {
            var1 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
            int var2 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
            if (this.text.length() > 0) {
               var10001 = this.text.substring(0, var1);
               this.text = var10001 + "\n" + this.text.substring(var2);
            } else {
               this.text = "\n";
            }

            this.TextEntryCursorPos = var1 + 1;
         } else {
            var1 = this.TextEntryCursorPos;
            var10001 = this.text.substring(0, var1);
            this.text = var10001 + "\n" + this.text.substring(var1);
            this.TextEntryCursorPos = var1 + 1;
         }

         this.ToSelectionIndex = this.TextEntryCursorPos;
         this.updateInternalValues();
      }
   }

   public void onKeyHome() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      this.TextEntryCursorPos = 0;
      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

      this.resetBlink();
   }

   public void onKeyEnd() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      this.TextEntryCursorPos = this.text.length();
      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

      this.resetBlink();
   }

   public void resetBlink() {
      this.BlinkState = true;
      this.BlinkFrame = (float)this.BlinkFramesOn;
   }

   public void onKeyUp() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      int var2 = 0;
      boolean var3 = true;

      label53:
      for(int var4 = this.TextEntryCursorPos; var4 > 0; --var4) {
         CharData var5 = (CharData)this.textData.get(var4 - 1);
         if (var5.id == 10 || var4 == 1) {
            if (!var3) {
               this.TextEntryCursorPos = var4 - 1;
               int var6 = 1;

               while(true) {
                  if (var6 > var2 || var4 + var6 - 1 == this.textData.size() || var4 + var6 - 1 < this.textData.size() && ((CharData)this.textData.get(var4 + var6 - 1)).id == 10) {
                     break label53;
                  }

                  ++this.TextEntryCursorPos;
                  ++var6;
               }
            }

            var3 = false;
         }

         if (var3) {
            ++var2;
         }
      }

      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

   }

   public void onKeyDown() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      int var2 = 0;

      int var3;
      CharData var4;
      for(var3 = this.TextEntryCursorPos; var3 > 0; --var3) {
         var4 = (CharData)this.textData.get(var3 - 1);
         if (var4.id == 10 || var3 == 1) {
            break;
         }

         ++var2;
      }

      DebugLog.General.warn(var2);

      label47:
      for(var3 = this.TextEntryCursorPos; var3 < this.textData.size(); ++var3) {
         var4 = (CharData)this.textData.get(var3);
         if (var4.id == 10) {
            this.TextEntryCursorPos = var3 - 1;
            int var5 = 1;

            while(true) {
               if (var5 > var2 || var3 + var5 - 1 == this.textData.size() || var3 + var5 + 1 < this.textData.size() && ((CharData)this.textData.get(var3 + var5 + 1)).id == 10) {
                  break label47;
               }

               ++this.TextEntryCursorPos;
               ++var5;
            }
         }
      }

      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

   }

   public void onKeyLeft() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      --this.TextEntryCursorPos;
      if (this.TextEntryCursorPos < 0) {
         this.TextEntryCursorPos = 0;
      }

      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

   }

   public void onKeyRight() {
      boolean var1 = Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
      ++this.TextEntryCursorPos;
      if (this.TextEntryCursorPos > this.text.length()) {
         this.TextEntryCursorPos = this.text.length();
      }

      if (!var1) {
         this.ToSelectionIndex = this.TextEntryCursorPos;
      }

   }

   public void onKeyDelete() {
      if (this.TextEntryCursorPos != this.ToSelectionIndex) {
         this.onTextDelete();
      }

      if (this.text.length() != 0 && this.TextEntryCursorPos < this.text.length()) {
         if (this.TextEntryCursorPos > 0) {
            String var10001 = this.text.substring(0, this.TextEntryCursorPos);
            this.text = var10001 + this.text.substring(this.TextEntryCursorPos + 1);
         } else {
            this.text = this.text.substring(1);
         }

         if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
         }

         this.updateInternalValues();
      }
   }

   void onTextDelete() {
      int var1 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
      int var2 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
      String var10001 = this.text.substring(0, var1);
      this.text = var10001 + this.text.substring(var2);
      this.ToSelectionIndex = var1;
      this.TextEntryCursorPos = var1;
      if (this.luaOnTextChange != null) {
         LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
      }

      this.updateInternalValues();
   }

   public void onKeyBack() {
      if (this.TextEntryCursorPos != this.ToSelectionIndex) {
         this.onTextDelete();
      }

      if (this.text.length() != 0 && this.TextEntryCursorPos > 0) {
         if (this.TextEntryCursorPos > this.text.length()) {
            this.text = this.text.substring(0, this.text.length() - 1);
         } else {
            int var1 = this.TextEntryCursorPos;
            String var10001 = this.text.substring(0, var1 - 1);
            this.text = var10001 + this.text.substring(var1);
         }

         --this.TextEntryCursorPos;
         this.ToSelectionIndex = this.TextEntryCursorPos;
         if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
         }

         this.updateInternalValues();
      }
   }

   public void pasteFromClipboard() {
      String var1 = Clipboard.getClipboard();
      if (var1 != null) {
         if (this.TextEntryCursorPos != this.ToSelectionIndex) {
            int var2 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
            int var3 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
            this.text = this.text.substring(0, var2) + var1 + this.text.substring(var3);
            this.ToSelectionIndex = var2 + var1.length();
            this.TextEntryCursorPos = var2 + var1.length();
         } else {
            if (this.TextEntryCursorPos < this.text.length()) {
               this.text = this.text.substring(0, this.TextEntryCursorPos) + var1 + this.text.substring(this.TextEntryCursorPos);
            } else {
               this.text = this.text + var1;
            }

            this.TextEntryCursorPos += var1.length();
            this.ToSelectionIndex += var1.length();
         }

         this.updateInternalValues();
         if (this.luaOnTextChange != null) {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaOnTextChange, this.table);
         }

      }
   }

   public void copyToClipboard() {
      if (this.TextEntryCursorPos != this.ToSelectionIndex) {
         int var1 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
         int var2 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
         String var3 = this.text.substring(var1, var2);
         if (var3.length() > 0) {
            Clipboard.setClipboard(var3);
         }

         this.updateInternalValues();
      }
   }

   public void cutToClipboard() {
      if (this.TextEntryCursorPos != this.ToSelectionIndex) {
         int var1 = Math.min(this.TextEntryCursorPos, this.ToSelectionIndex);
         int var2 = Math.max(this.TextEntryCursorPos, this.ToSelectionIndex);
         String var3 = this.text.substring(var1, var2);
         if (var3.length() > 0) {
            Clipboard.setClipboard(var3);
         }

         String var10001 = this.text.substring(0, var1);
         this.text = var10001 + this.text.substring(var2);
         this.ToSelectionIndex = var1;
         this.TextEntryCursorPos = var1;
         this.updateInternalValues();
      }
   }

   public void selectAll() {
      this.TextEntryCursorPos = this.text.length();
      this.ToSelectionIndex = 0;
   }

   public boolean isTextLimit() {
      return this.text.length() >= this.TextEntryMaxLength;
   }

   public boolean isOnlyNumbers() {
      return this.onlyNumbers;
   }

   public boolean isOnlyText() {
      return this.onlyText;
   }

   public void setOnlyNumbers(boolean var1) {
      this.onlyNumbers = var1;
   }

   public void setOnlyText(boolean var1) {
      this.onlyText = var1;
   }

   public int getMaxTextLength() {
      return this.maxTextLength;
   }

   public void setMaxTextLength(int var1) {
      this.maxTextLength = var1;
   }

   public boolean getForceUpperCase() {
      return this.forceUpperCase;
   }

   public void setForceUpperCase(boolean var1) {
      this.forceUpperCase = var1;
   }

   public boolean isMultiline() {
      return this.bMultiline;
   }

   public void setMultiline(boolean var1) {
      this.bMultiline = var1;
   }

   public String getText() {
      return this.text;
   }

   static class CharData {
      public AngelCodeFont.CharDef def;
      public double x;
      public double y;
      public int id;

      CharData() {
      }
   }
}
