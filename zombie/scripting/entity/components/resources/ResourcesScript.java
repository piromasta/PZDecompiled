package zombie.scripting.entity.components.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

public class ResourcesScript extends ComponentScript {
   private final HashMap<String, ArrayList<String>> groupLines = new HashMap();
   private final ArrayList<String> groupNames = new ArrayList();
   private final ArrayList<ResourceBlueprint> blueprints = new ArrayList();
   private final HashMap<String, ArrayList<ResourceBlueprint>> groups = new HashMap();

   private ResourcesScript() {
      super(ComponentType.Resources);
   }

   public ArrayList<String> getGroupNames() {
      return this.groupNames;
   }

   public ArrayList<ResourceBlueprint> getBlueprintGroup(String var1) {
      return (ArrayList)this.groups.get(var1);
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
      this.groupLines.clear();
      this.groupNames.clear();
      this.blueprints.clear();
      this.groups.clear();
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      Iterator var2 = this.groupLines.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         String var4 = (String)var3.getKey();
         ArrayList var5 = (ArrayList)this.groups.computeIfAbsent(var4, (var0) -> {
            return new ArrayList();
         });
         this.groupNames.add(var4);
         Iterator var6 = ((ArrayList)var3.getValue()).iterator();

         while(var6.hasNext()) {
            String var7 = (String)var6.next();

            try {
               ResourceBlueprint var8 = ResourceBlueprint.DeserializeFromScript(var7);
               var5.add(var8);
               this.blueprints.add(var8);
            } catch (Exception var9) {
               DebugLog.log("Error in resource blueprint line: " + var7 + ", entity: " + this.getName());
               var9.printStackTrace();
               throw new Exception(var9);
            }
         }
      }

   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      this.loadResourceBlock("resources", var1);
      Iterator var2 = var1.children.iterator();

      while(var2.hasNext()) {
         ScriptParser.Block var3 = (ScriptParser.Block)var2.next();
         if (var3.type.equalsIgnoreCase("group")) {
            String var4 = var3.id;
            if (StringUtils.isNullOrWhitespace(var4)) {
               throw new Exception("Group name cannot be null or whitespace.");
            }

            this.loadResourceBlock(var4, var3);
         } else if (var3.type.equalsIgnoreCase("internal")) {
            DebugLog.General.warn("internal block is deprecated");
         } else if (var3.type.equalsIgnoreCase("external")) {
            DebugLog.General.warn("external block is deprecated");
         } else {
            String var10001 = var3.type;
            DebugLog.General.error("Unknown block '" + var10001 + "' in entity script: " + this.getName());
         }
      }

   }

   private void loadResourceBlock(String var1, ScriptParser.Block var2) throws Exception {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new Exception("GroupName cannot be null or whitespace");
      } else {
         ArrayList var3 = (ArrayList)this.groupLines.computeIfAbsent(var1, (var0) -> {
            return new ArrayList();
         });
         Iterator var4 = var2.values.iterator();

         while(true) {
            ScriptParser.Value var5;
            String var6;
            do {
               do {
                  do {
                     if (!var4.hasNext()) {
                        return;
                     }

                     var5 = (ScriptParser.Value)var4.next();
                     var6 = var5.string != null ? var5.string.trim() : null;
                  } while(var6 == null);
               } while(StringUtils.isNullOrWhitespace(var6));
            } while(!var6.contains("@"));

            String var7 = var5.getKey().trim();
            String var8 = var5.getValue().trim();
            if (!var7.isEmpty() && !var8.isEmpty()) {
               var6 = var7 + "@" + var8;
            } else {
               var6 = var1 + "_" + var3.size() + "@" + var6;
            }

            var3.add(var6);
         }
      }
   }
}
