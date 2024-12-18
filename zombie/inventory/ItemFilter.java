package zombie.inventory;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemFilterScript;

public class ItemFilter {
   private ItemFilterScript filterScript;

   public ItemFilter() {
   }

   public void setFilterScript(String var1) {
      if (var1 == null) {
         this.filterScript = null;
      } else if (this.filterScript == null || !var1.equalsIgnoreCase(this.filterScript.getScriptObjectFullType())) {
         ItemFilterScript var2 = ScriptManager.instance.getItemFilter(var1);
         if (var2 == null) {
            DebugLog.General.warn("ItemFilter '" + var1 + "' not found.");
         }

         this.filterScript = var2;
      }

   }

   public ItemFilterScript getFilterScript() {
      return this.filterScript;
   }

   public boolean isActive() {
      return this.filterScript != null;
   }

   public boolean allows(String var1) {
      Item var2 = ScriptManager.instance.getItem(var1);
      return this.allows(var2);
   }

   public boolean allows(InventoryItem var1) {
      return !this.isActive() || this.filterScript.allowsItem(var1);
   }

   public boolean allows(Item var1) {
      return !this.isActive() || this.filterScript.allowsItem(var1);
   }

   public void save(ByteBuffer var1) throws IOException {
      var1.put((byte)(this.filterScript != null ? 1 : 0));
      if (this.filterScript != null) {
         GameWindow.WriteString(var1, this.filterScript.getScriptObjectFullType());
      }

   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.filterScript = null;
      if (var1.get() == 1) {
         this.setFilterScript(GameWindow.ReadString(var1));
      }

   }
}
