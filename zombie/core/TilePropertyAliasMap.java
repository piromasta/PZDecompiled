package zombie.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;

public final class TilePropertyAliasMap {
   public static final TilePropertyAliasMap instance = new TilePropertyAliasMap();
   public final HashMap<String, Integer> PropertyToID = new HashMap();
   public final ArrayList<TileProperty> Properties = new ArrayList();

   public TilePropertyAliasMap() {
   }

   public void Generate(HashMap<String, ArrayList<String>> var1) {
      this.Properties.clear();
      this.PropertyToID.clear();
      Iterator var2 = var1.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         this.register((String)var3.getKey(), (ArrayList)var3.getValue());
      }

      this.generateEntityProperties();
   }

   private void generateEntityProperties() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = ScriptManager.instance.getAllGameEntities().iterator();

      while(var2.hasNext()) {
         GameEntityScript var3 = (GameEntityScript)var2.next();
         var1.add(var3.getName());
      }

      this.register("EntityScriptName", var1);
   }

   private void register(String var1, ArrayList<String> var2) {
      this.PropertyToID.put(var1, this.Properties.size());
      TileProperty var5 = new TileProperty();
      this.Properties.add(var5);
      var5.propertyName = var1;
      var5.possibleValues.addAll(var2);
      ArrayList var6 = var5.possibleValues;

      for(int var7 = 0; var7 < var6.size(); ++var7) {
         String var8 = (String)var6.get(var7);
         var5.idMap.put(var8, var7);
      }

   }

   public int getIDFromPropertyName(String var1) {
      return (Integer)this.PropertyToID.getOrDefault(var1, -1);
   }

   public int getIDFromPropertyValue(int var1, String var2) {
      TileProperty var3 = (TileProperty)this.Properties.get(var1);
      if (var3.possibleValues.isEmpty()) {
         return 0;
      } else {
         return !var3.idMap.containsKey(var2) ? 0 : (Integer)var3.idMap.get(var2);
      }
   }

   public String getPropertyValueString(int var1, int var2) {
      TileProperty var3 = (TileProperty)this.Properties.get(var1);
      return var3.possibleValues.isEmpty() ? "" : (String)var3.possibleValues.get(var2);
   }

   public static final class TileProperty {
      public String propertyName;
      public final ArrayList<String> possibleValues = new ArrayList();
      public final HashMap<String, Integer> idMap = new HashMap();

      public TileProperty() {
      }
   }
}
