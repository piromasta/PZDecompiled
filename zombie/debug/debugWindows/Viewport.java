package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.ImVec2;
import zombie.core.Core;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.input.Mouse;

public class Viewport extends BaseDebugWindow {
   private float viewWidth;
   private float viewHeight;
   private float highlightX1;
   private float highlightY1;
   private float highlightX2;
   private float highlightY2;
   private int highlightCol;

   public Viewport() {
   }

   public boolean doFrameStartTick() {
      return true;
   }

   public String getTitle() {
      return "Viewport";
   }

   public float transformXToGame(float var1) {
      var1 -= this.contentMin.x;
      var1 -= this.x;
      var1 /= this.viewWidth;
      var1 *= (float)Core.width;
      return var1;
   }

   public float transformXToWindow(float var1) {
      var1 /= (float)Core.width;
      var1 *= this.viewWidth;
      var1 += this.x;
      var1 += this.contentMin.x;
      return var1;
   }

   public float transformYToGame(float var1) {
      var1 -= this.contentMin.y;
      var1 -= this.y;
      var1 /= this.viewHeight;
      var1 *= (float)Core.height;
      return var1;
   }

   public float transformYToWindow(float var1) {
      var1 /= (float)Core.height;
      var1 *= this.viewHeight;
      var1 += this.y;
      var1 += this.contentMin.y;
      return var1;
   }

   protected void doWindowContents() {
      float var1 = (float)DebugContext.instance.DebugViewportTexture.getWidth() / (float)DebugContext.instance.DebugViewportTexture.getTexture().getWidthHW();
      float var2 = (float)DebugContext.instance.DebugViewportTexture.getHeight() / (float)DebugContext.instance.DebugViewportTexture.getTexture().getHeightHW();
      float var3 = (float)DebugContext.instance.DebugViewportTexture.getHeight() / (float)DebugContext.instance.DebugViewportTexture.getWidth();
      float var4 = (ImGui.getWindowHeight() - this.contentMin.y) / var3;
      float var5 = ImGui.getWindowHeight() - this.contentMin.y;
      var4 -= 10.0F;
      var5 -= 10.0F;
      if (var4 > ImGui.getWindowWidth()) {
         var4 = ImGui.getWindowWidth() - this.contentMin.x;
         var4 -= 10.0F;
         var5 = var4 * var3;
      }

      float var6 = ImGui.getWindowPosX();
      float var7 = ImGui.getWindowPosY();
      ImGui.getCurrentDrawList().addRectFilled(var6, var7, var6 + ImGui.getWindowWidth(), var7 + ImGui.getWindowWidth(), ImGui.colorConvertFloat4ToU32(0.0F, 0.0F, 0.0F, 1.0F));
      ImGui.image(DebugContext.instance.DebugViewportTexture.getTexture().getID(), var4, var5, 0.0F, var2, var1, 0.0F);
      DebugContext.instance.bFocusedGameViewport = ImGui.isWindowFocused();
      this.viewWidth = var4;
      this.viewHeight = var5;
      this.contentMin = this.contentMin;
      float var8 = (float)DebugContext.instance.getViewportMouseX();
      float var9 = (float)DebugContext.instance.getViewportMouseY();
      float var10 = 0.0F;
      float var11 = 0.0F;
      if (DebugContext.instance.bFocusedGameViewport) {
         var8 = ImGui.getMousePosX();
         var9 = ImGui.getMousePosY();
         ImVec2 var12 = ImGui.getWindowContentRegionMax();
         var8 -= this.contentMin.x;
         var9 -= this.contentMin.y;
         var8 -= ImGui.getWindowPosX();
         var9 -= ImGui.getWindowPosY();
         var8 /= var4;
         var9 /= var5;
         var8 *= (float)Core.width;
         var9 *= (float)Core.height;
         DebugContext.instance.setViewportX(var8);
         DebugContext.instance.setViewportY(var9);
      } else {
         DebugContext.instance.setViewportX(-1.0F);
         DebugContext.instance.setViewportY(-1.0F);
      }

      if (DebugContext.instance.bFocusedGameViewport) {
         if (Mouse.isButtonDown(1)) {
            ImGui.setMouseCursor(-1);
         } else {
            ImGui.setMouseCursor(0);
         }
      }

      ImGui.getCurrentDrawList().addRect(this.highlightX1, this.highlightY1, this.highlightX2, this.highlightY2, this.highlightCol, 0.0F, 0, 3.0F);
   }

   public void highlight(float var1, float var2, float var3, float var4, int var5) {
      float var6 = this.transformXToWindow(var1);
      float var7 = this.transformXToWindow(var1 + var3);
      float var8 = this.transformYToWindow(var2);
      float var9 = this.transformYToWindow(var2 + var4);
      this.highlightX1 = var6;
      this.highlightX2 = var7;
      this.highlightY1 = var8;
      this.highlightY2 = var9;
      this.highlightCol = var5;
   }
}
