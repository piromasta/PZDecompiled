package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImString;
import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;

public class LuaPanel extends BaseDebugWindow {
   ImString searchString = new ImString(128);
   int selectedIndex = 0;

   public LuaPanel() {
   }

   public String getTitle() {
      return "Lua";
   }

   protected void doWindowContents() {
      ArrayList var1 = new ArrayList();
      if (this.searchString.toString().length() > 0) {
         KahluaTableImpl var2 = (KahluaTableImpl)LuaManager.env;
         KahluaTableIterator var3 = var2.iterator();

         while(var3.advance()) {
            if (var3.getKey().toString().toLowerCase().contains(this.searchString.toString().toLowerCase())) {
               var1.add(var3.getKey().toString());
            }
         }
      }

      if (ImGui.inputText("search string", this.searchString, 0)) {
         boolean var4 = false;
      }

      if (ImGui.beginListBox("##empty", -DebugContext.FLT_MIN, 0.0F)) {
         for(int var5 = 0; var5 < var1.size(); ++var5) {
            if (ImGui.selectable((String)var1.get(var5), this.selectedIndex == var5)) {
               this.selectedIndex = var5;
            }
         }

         ImGui.endListBox();
      }

   }
}
