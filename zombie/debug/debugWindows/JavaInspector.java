package zombie.debug.debugWindows;

import com.google.common.collect.Lists;
import imgui.ImGui;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;

public class JavaInspector extends BaseDebugWindow {
   Object obj;
   String selectedNode = "";
   int defaultflags = 192;
   int selectedflags;

   public JavaInspector(Object var1) {
      this.selectedflags = this.defaultflags | 1;
      this.obj = var1;
   }

   public String getTitle() {
      return this.obj == null ? "Java Inspector" : this.obj.toString();
   }

   boolean doTreeNode(String var1, String var2, boolean var3) {
      boolean var4 = var1.equals(this.selectedNode);
      int var5 = var4 ? this.selectedflags : this.defaultflags;
      if (var3) {
         var5 |= 256;
      }

      boolean var6 = ImGui.treeNodeEx(var1, var5, var2);
      if (ImGui.isItemClicked()) {
         this.selectedNode = var1;
      }

      return var6;
   }

   public static Iterable<Field> getFieldsUpTo(Class<?> var0, Class<?> var1) {
      ArrayList var2 = Lists.newArrayList(var0.getDeclaredFields());
      Class var3 = var0.getSuperclass();
      if (var3 != null && (var1 == null || !var3.equals(var1))) {
         List var4 = (List)getFieldsUpTo(var3, var1);
         var2.addAll(var4);
      }

      return var2;
   }

   protected void doWindowContents() {
      Class var1 = this.obj.getClass();
      Iterable var2 = getFieldsUpTo(var1, Object.class);
      ArrayList var3 = new ArrayList();

      Field var5;
      for(Iterator var4 = var2.iterator(); var4.hasNext(); var3.add(var5)) {
         var5 = (Field)var4.next();
         if (var5.getName().contains("BodyDamage")) {
            boolean var6 = false;
         }
      }

      var3.sort((var0, var1x) -> {
         return var0.getName().toLowerCase().compareTo(var1x.getName().toLowerCase());
      });
      short var13 = 3905;
      if (ImGui.beginTable("fields", 3, var13)) {
         ImGui.tableSetupColumn("Name", 128);
         ImGui.tableSetupColumn("Type");
         ImGui.tableSetupColumn("Value", 16, 180.0F);
         ImGui.tableHeadersRow();

         for(int var14 = 0; var14 < var3.size(); ++var14) {
            ImGui.tableNextRow();
            ImGui.tableNextColumn();
            Field var15 = (Field)var3.get(var14);
            String var7 = var15.getName();
            Object var8 = null;
            String var9 = "";
            boolean var10 = var15.getType().isPrimitive();
            var15.setAccessible(true);

            try {
               var8 = var15.get(this.obj);
               if (var8 == null) {
                  var9 = "null";
               } else {
                  var9 = var8.toString();
               }
            } catch (Exception var12) {
               var9 = "<inaccessible>";
            }

            ImGui.textUnformatted(var7);
            ImGui.tableNextColumn();
            ImGui.textUnformatted(var15.getType().getSimpleName());
            ImGui.tableNextColumn();
            if (var8 != null) {
               ImGui.selectable(var9);
               this.doPopupMenu(var8, var10);
            } else {
               ImGui.textUnformatted(var9);
            }
         }

         ImGui.endTable();
      }

   }

   private void doPopupMenu(Object var1, boolean var2) {
      if (var1 != null) {
         if (ImGui.beginPopupContextItem()) {
            if (!var2 && ImGui.selectable("inspect class")) {
               DebugContext.instance.inspectJava(var1);
            }

            ImGui.endPopup();
         }

      }
   }
}
