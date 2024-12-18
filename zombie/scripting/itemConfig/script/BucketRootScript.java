package zombie.scripting.itemConfig.script;

import java.util.Iterator;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.itemConfig.enums.RootType;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.util.StringUtils;

public class BucketRootScript {
   private RootType type;
   private String id;
   private SelectorBucketScript defaultBucket;
   private SelectorBucketScript onCreateBucket;

   public BucketRootScript() {
   }

   public RootType getType() {
      return this.type;
   }

   public String getId() {
      return this.id;
   }

   public void setId(String var1) {
      this.id = var1;
   }

   public boolean idIsVariable() {
      return this.id != null && this.id.startsWith("$");
   }

   public SelectorBucketScript getDefaultBucket() {
      return this.defaultBucket;
   }

   public SelectorBucketScript getOnCreateBucket() {
      return this.onCreateBucket;
   }

   public BucketRootScript copy() {
      BucketRootScript var1 = new BucketRootScript();
      var1.type = this.type;
      var1.id = this.id;
      var1.defaultBucket = this.defaultBucket.copy();
      var1.onCreateBucket = this.onCreateBucket != null ? this.onCreateBucket.copy() : null;
      return var1;
   }

   private void load(ScriptParser.Block var1) throws ItemConfig.ItemConfigException {
      ItemConfig.error_root = var1.id;
      this.type = RootType.valueOf(var1.type.trim());
      this.id = StringUtils.isNullOrWhitespace(var1.id) ? var1.id.trim() : null;
      if (this.type.isRequiresId() && this.id == null) {
         throw new ItemConfig.ItemConfigException("Root node with type '" + this.type + "' requires id.");
      } else {
         Iterator var2 = var1.children.iterator();

         ScriptParser.Block var3;
         while(var2.hasNext()) {
            var3 = (ScriptParser.Block)var2.next();
            if (var3.type.equalsIgnoreCase("default")) {
               this.defaultBucket = this.loadSelectorBucket(var3, (SelectorBucketScript)null);
            } else if (var3.type.equalsIgnoreCase("oncreate")) {
               this.onCreateBucket = this.loadSelectorBucket(var3, (SelectorBucketScript)null);
            }
         }

         if (this.defaultBucket == null) {
            this.defaultBucket = new SelectorBucketScript(SelectorType.Default);
         }

         var2 = var1.children.iterator();

         while(var2.hasNext()) {
            var3 = (ScriptParser.Block)var2.next();
            if (!var3.type.equalsIgnoreCase("default") && !var3.type.equalsIgnoreCase("oncreate")) {
               SelectorBucketScript var4 = this.loadSelectorBucket(var3, this.defaultBucket);
               this.defaultBucket.children.add(var4);
            }
         }

         ItemConfig.error_root = null;
      }
   }

   private SelectorBucketScript loadSelectorBucket(ScriptParser.Block var1, SelectorBucketScript var2) throws ItemConfig.ItemConfigException {
      String var10000 = var1.type != null ? var1.type : "null";
      ItemConfig.error_bucket = var10000 + " " + (var1.id != null ? var1.id : "");
      SelectorType var3 = SelectorType.None;
      SituatedType var4 = SituatedType.None;
      int var5 = 0;
      String var6 = null;
      if (var1.type.equalsIgnoreCase("default")) {
         var3 = SelectorType.Default;
         if (var2 != null) {
            throw new ItemConfig.ItemConfigException("Default block may not be nested or defined twice.");
         }
      } else if (var1.type.equalsIgnoreCase("oncreate")) {
         var3 = SelectorType.OnCreate;
         if (var2 != null) {
            throw new ItemConfig.ItemConfigException("OnCreate block may not be nested or defined twice.");
         }
      } else if (var1.type.equalsIgnoreCase("situated")) {
         var3 = SelectorType.Situated;
         if (StringUtils.isNullOrWhitespace(var1.id)) {
            throw new ItemConfig.ItemConfigException("Block 'Situated' requires a parameter.");
         }

         if (var1.id.equalsIgnoreCase("interior")) {
            var4 = SituatedType.Interior;
         } else if (var1.id.equalsIgnoreCase("exterior")) {
            var4 = SituatedType.Exterior;
         } else if (var1.id.equalsIgnoreCase("shop")) {
            var4 = SituatedType.Shop;
         } else {
            if (!var1.id.equalsIgnoreCase("junk")) {
               throw new ItemConfig.ItemConfigException("Block 'Situated' requires a valid parameter.");
            }

            var4 = SituatedType.Junk;
         }
      } else if (var1.type.equalsIgnoreCase("zone")) {
         var3 = SelectorType.Zone;
         var6 = var1.id;
         if (StringUtils.isNullOrWhitespace(var6)) {
            throw new ItemConfig.ItemConfigException("Block 'Zone' requires a parameter.");
         }
      } else if (var1.type.equalsIgnoreCase("vehicle")) {
         var3 = SelectorType.Vehicle;
         var6 = var1.id;
         if (StringUtils.isNullOrWhitespace(var6)) {
            throw new ItemConfig.ItemConfigException("Block 'Vehicle' requires a parameter.");
         }
      } else if (var1.type.equalsIgnoreCase("room")) {
         var3 = SelectorType.Room;
         var6 = var1.id;
         if (StringUtils.isNullOrWhitespace(var6)) {
            throw new ItemConfig.ItemConfigException("Block 'Room' requires a parameter.");
         }
      } else if (var1.type.equalsIgnoreCase("container")) {
         var3 = SelectorType.Container;
         var6 = var1.id;
         if (StringUtils.isNullOrWhitespace(var6)) {
            throw new ItemConfig.ItemConfigException("Block 'Container' requires a parameter.");
         }
      } else if (var1.type.equalsIgnoreCase("tile")) {
         var3 = SelectorType.Tile;
         var6 = var1.id;
         if (StringUtils.isNullOrWhitespace(var6)) {
            throw new ItemConfig.ItemConfigException("Block 'Tile' requires a parameter.");
         }
      } else if (var1.type.equalsIgnoreCase("worldagedays")) {
         var3 = SelectorType.WorldAge;
         if (StringUtils.isNullOrWhitespace(var1.id)) {
            throw new ItemConfig.ItemConfigException("Block 'WorldAgeDays' requires a parameter.");
         }

         var5 = Integer.parseInt(var1.id);
         if (var5 < 0) {
            throw new ItemConfig.ItemConfigException("Block 'WorldAgeDays' requires a value greater than zero.");
         }
      } else {
         var3 = SelectorType.None;
         if (!StringUtils.isNullOrWhitespace(var1.id)) {
            DebugLog.General.warn("A custom block should not have a parameter, typo in block identifier? Block: " + var1.type + " " + var1.id);
         }
      }

      if (var3 == SelectorType.Default && var1.children.size() > 0) {
         throw new ItemConfig.ItemConfigException("Default block may not have any nested children.");
      } else if (var3 == SelectorType.OnCreate && var1.children.size() > 0) {
         throw new ItemConfig.ItemConfigException("OnCreate block may not have any nested children.");
      } else {
         SelectorBucketScript var7 = new SelectorBucketScript(var3);
         var7.selectorSituated = var4;
         var7.selectorWorldAge = var5;
         var7.selectorString = var6;
         Iterator var8 = var1.values.iterator();

         while(var8.hasNext()) {
            ScriptParser.Value var9 = (ScriptParser.Value)var8.next();
            if (!StringUtils.isNullOrWhitespace(var9.string)) {
               ItemConfig.error_line = var9.string;
               var7.randomizers.add(var9.string.trim());
               ItemConfig.error_line = null;
            }
         }

         if (var3 == SelectorType.OnCreate && var7.randomizers.size() == 0) {
            throw new ItemConfig.ItemConfigException("OnCreate block needs at least one randomizer parameter defined.");
         } else if (var3 == SelectorType.None && var7.randomizers.size() > 0) {
            throw new ItemConfig.ItemConfigException("A custom container bucket may not have any randomizer parameters.");
         } else {
            var8 = var1.children.iterator();

            while(var8.hasNext()) {
               ScriptParser.Block var11 = (ScriptParser.Block)var8.next();
               SelectorBucketScript var10 = this.loadSelectorBucket(var11, var7);
               var7.children.add(var10);
            }

            return var7;
         }
      }
   }

   public static BucketRootScript TryLoad(ScriptParser.Block var0) throws ItemConfig.ItemConfigException {
      BucketRootScript var1 = new BucketRootScript();
      var1.load(var0);
      return var1;
   }
}
