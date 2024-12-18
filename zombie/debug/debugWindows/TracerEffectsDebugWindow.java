package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.Iterator;
import zombie.iso.objects.IsoBulletTracerEffects;

public class TracerEffectsDebugWindow extends PZDebugWindow {
   public TracerEffectsDebugWindow() {
   }

   public String getTitle() {
      return "Tracer Effects Editor";
   }

   protected void doWindowContents() {
      ImGui.begin(this.getTitle());
      Iterator var1 = IsoBulletTracerEffects.getInstance().getIsoBulletTracerEffectsConfigOptionsHashMap().keySet().iterator();

      while(var1.hasNext()) {
         String var2 = (String)var1.next();
         boolean var3 = false;
         IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions var4 = (IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions)IsoBulletTracerEffects.getInstance().getIsoBulletTracerEffectsConfigOptionsHashMap().get(var2);
         int var5 = var4.getOptionCount();
         if (PZImGui.collapsingHeader(var2)) {
            ImGui.beginChild(var2);

            for(int var6 = 0; var6 < var5; ++var6) {
               IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption var7 = (IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption)var4.getOptionByIndex(var6);
               float var8 = PZImGui.sliderFloat(var7.getName(), (float)var7.getValue(), (float)var7.getMin(), (float)var7.getMax());
               if ((double)var8 != var7.getValue()) {
                  var7.setValue((double)var8);
                  var3 = true;
               }
            }

            if (PZImGui.button("Reset To Default")) {
               IsoBulletTracerEffects.getInstance().reset(var2);
               var3 = true;
            }

            ImGui.endChild();
         }

         if (var3) {
            IsoBulletTracerEffects.getInstance().save(var2);
         }
      }

      ImGui.end();
   }
}
