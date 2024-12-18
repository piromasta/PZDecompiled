package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.ui.UIElement;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;

public class UIPanel extends BaseDebugWindow {
   private float selectedUIX;
   private float selectedUIY;
   private float selectedUIWidth;
   private float selectedUIHeight;
   private String selectedNode = "";

   public UIPanel() {
   }

   public String getTitle() {
      return "UI";
   }

   private void doUITree(UIElement var1) {
      String var2 = var1.getUIName();
      int var3 = 192;
      if (var1.Controls.isEmpty()) {
         var3 |= 256;
      }

      if (String.valueOf(var1.hashCode()).equalsIgnoreCase(this.selectedNode)) {
         var3 |= 1;
         if (var1.isVisible()) {
            this.selectedUIX = var1.getAbsoluteX().floatValue();
            this.selectedUIY = var1.getAbsoluteY().floatValue();
            this.selectedUIWidth = var1.getWidth().floatValue();
            this.selectedUIHeight = var1.getHeight().floatValue();
         } else {
            this.selectedUIX = 0.0F;
            this.selectedUIY = 0.0F;
            this.selectedUIWidth = 0.0F;
            this.selectedUIHeight = 0.0F;
         }

         DebugContext.instance.Viewport.highlight(this.selectedUIX, this.selectedUIY, this.selectedUIWidth, this.selectedUIHeight, ImGui.colorConvertFloat4ToU32(1.0F, 0.0F, 0.0F, 1.0F));
      }

      if (!var1.isVisible()) {
         ImGui.pushStyleColor(0, ImGui.colorConvertFloat4ToU32(0.4F, 0.4F, 0.4F, 1.0F));
      }

      if (ImGui.treeNodeEx(String.valueOf(var1.hashCode()), var3, var2)) {
         if (ImGui.isItemClicked()) {
            this.selectedNode = String.valueOf(var1.hashCode());
         }

         ArrayList var4 = var1.getControls();

         for(int var5 = 0; var5 < var4.size(); ++var5) {
            UIElement var6 = (UIElement)var4.get(var5);
            this.doUITree(var6);
         }

         ImGui.treePop();
      } else if (ImGui.isItemClicked()) {
         this.selectedNode = String.valueOf(var1.hashCode());
      }

      if (!var1.isVisible()) {
         ImGui.popStyleColor();
      }

   }

   protected void doWindowContents() {
      ArrayList var1 = UIManager.getUI();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         UIElementInterface var3 = (UIElementInterface)var1.get(var2);
         if (var3 instanceof UIElement) {
            this.doUITree((UIElement)var3);
         }
      }

   }
}
