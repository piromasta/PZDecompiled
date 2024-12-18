package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.SoundManager;

public class PZImGui extends Wrappers {
   public PZImGui() {
   }

   public static float sliderFloat(String var0, float var1, float var2, float var3) {
      float var4 = Wrappers.sliderFloat(var0, var1, var2, var3);
      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UISelectListItem");
      }

      return var4;
   }

   public static boolean button(String var0) {
      boolean var1 = ImGui.button(var0);
      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UIToggleTickBox");
      }

      return var1;
   }

   public static boolean combo(String var0, ImInt var1, String[] var2) {
      boolean var3 = ImGui.combo(var0, var1, var2);
      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UIToggleComboBox");
      }

      return var3;
   }

   public static boolean collapsingHeader(String var0) {
      boolean var1 = ImGui.collapsingHeader(var0);
      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UIToggleTickBox");
      }

      return var1;
   }

   public static boolean begin(String var0, ImBoolean var1, int var2) {
      boolean var3 = ImGui.begin(var0, var1, var2);
      if (ImGui.isItemClicked() || ImGui.isItemActivated()) {
         SoundManager.instance.playUISound("UIActivateTab");
      }

      return var3;
   }

   public static boolean checkboxWithDefaultValueHighlight(String var0, Supplier<Boolean> var1, Consumer<Boolean> var2, boolean var3, int var4) {
      valueBoolean.set((Boolean)var1.get());
      if (valueBoolean.get() != var3) {
         ImGui.pushStyleColor(0, var4);
         ImGui.checkbox(var0, valueBoolean);
         ImGui.popStyleColor();
      } else {
         ImGui.checkbox(var0, valueBoolean);
      }

      if (valueBoolean.get() != (Boolean)var1.get()) {
         var2.accept(valueBoolean.get());
      }

      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UIToggleTickBox");
      }

      return valueBoolean.get();
   }

   public static boolean checkbox(String var0, Supplier<Boolean> var1, Consumer<Boolean> var2) {
      valueBoolean.set((Boolean)var1.get());
      ImGui.checkbox(var0, valueBoolean);
      if (valueBoolean.get() != (Boolean)var1.get()) {
         var2.accept(valueBoolean.get());
      }

      if (ImGui.isItemClicked()) {
         SoundManager.instance.playUISound("UIToggleTickBox");
      }

      return valueBoolean.get();
   }
}
