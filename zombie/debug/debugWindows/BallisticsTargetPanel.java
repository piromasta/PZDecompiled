package zombie.debug.debugWindows;

import gnu.trove.iterator.hash.TObjectHashIterator;
import gnu.trove.set.hash.THashSet;
import imgui.ImGui;
import java.util.Iterator;
import java.util.List;
import zombie.core.physics.BallisticsTarget;
import zombie.util.IPooledObject;
import zombie.util.Pool;

public class BallisticsTargetPanel extends PZDebugWindow {
   public BallisticsTargetPanel() {
   }

   public String getTitle() {
      return "Ballistics Targets";
   }

   protected void doWindowContents() {
      ImGui.begin(this.getTitle(), 64);
      if (PZImGui.collapsingHeader("Ballistics Target Pool")) {
         Pool var1 = BallisticsTarget.getBallisticsTargetPool();
         Pool.PoolStacks var2 = (Pool.PoolStacks)var1.getPoolStacks().get();
         THashSet var3 = var2.getInUse();
         List var4 = var2.getReleased();
         TObjectHashIterator var5 = var3.iterator();

         IPooledObject var6;
         BallisticsTarget var7;
         int var8;
         while(var5.hasNext()) {
            var6 = (IPooledObject)var5.next();
            var7 = (BallisticsTarget)var6;
            if (var7 != null) {
               var8 = var7.getID();
               if (PZImGui.collapsingHeader("InUse: " + Integer.toString(var8))) {
               }
            }
         }

         Iterator var9 = var4.iterator();

         while(var9.hasNext()) {
            var6 = (IPooledObject)var9.next();
            var7 = (BallisticsTarget)var6;
            if (var7 != null) {
               var8 = var7.getID();
               if (PZImGui.collapsingHeader("Released: " + Integer.toString(var8))) {
               }
            }
         }
      }

      ImGui.end();
   }
}
