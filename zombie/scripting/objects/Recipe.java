package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

public class Recipe extends BaseScriptObject {
   private boolean canBeDoneFromFloor = false;
   public float TimeToMake = 0.0F;
   public String Sound;
   protected String AnimNode;
   protected String Prop1;
   protected String Prop2;
   public final ArrayList<Source> Source = new ArrayList();
   public Result Result = null;
   public final ArrayList<Result> Results = new ArrayList();
   public boolean AllowDestroyedItem = false;
   public boolean AllowFrozenItem = false;
   public boolean AllowRottenItem = false;
   public boolean AllowOnlyOne = false;
   public boolean InSameInventory = false;
   public String name = "recipe";
   private String originalname;
   private String requiredNearObject;
   private String tooltip = null;
   public ArrayList<RequiredSkill> skillRequired = null;
   private boolean needToBeLearn = false;
   protected String category = null;
   protected boolean removeResultItem = false;
   private float heat = 0.0F;
   protected boolean stopOnWalk = true;
   protected boolean stopOnRun = true;
   public boolean hidden = false;
   private String recipeFileText;
   private boolean obsolete = false;
   private boolean requiresWorkstation = false;
   private float stationMultiplier = 0.25F;
   private final HashMap<LuaCall, String> luaCalls = new HashMap();

   public boolean isRequiresWorkstation() {
      return this.requiresWorkstation;
   }

   public float getStationMultiplier() {
      return this.stationMultiplier;
   }

   public Recipe() {
      super(ScriptType.Recipe);
      this.setOriginalname("recipe");
   }

   public void Load(String var1, String var2) throws Exception {
      this.name = Translator.getRecipeName(var1);
      this.originalname = var1;
      this.recipeFileText = var2;
      boolean var3 = false;
      ScriptParser.Block var4 = ScriptParser.parse(var2);
      var4 = (ScriptParser.Block)var4.children.get(0);
      super.LoadCommonBlock(var4);
      Iterator var5 = var4.elements.iterator();

      while(var5.hasNext()) {
         ScriptParser.BlockElement var6 = (ScriptParser.BlockElement)var5.next();
         if (var6.asValue() != null) {
            String var7 = var6.asValue().string;
            if (!var7.trim().isEmpty()) {
               if (var7.contains(":")) {
                  String[] var8 = var7.split(":");
                  String var9 = var8[0].trim();
                  String var10 = var8[1].trim();
                  if (var9.equalsIgnoreCase("Override")) {
                     var3 = var10.trim().equalsIgnoreCase("true");
                  } else {
                     DebugLog.General.error("Could not assign [key]: '" + var9 + "' : '" + var10 + "'.");
                  }
               } else {
                  this.DoSource(var7.trim());
               }
            }
         }
      }

   }

   public void DoSource(String var1) {
      Source var2 = new Source();
      if (var1.contains("=")) {
         var2.count = Float.parseFloat(var1.split("=")[1].trim());
         var1 = var1.split("=")[0].trim();
      }

      if (var1.indexOf("keep") == 0) {
         var1 = var1.replace("keep ", "");
         var2.keep = true;
      }

      if (var1.contains(";")) {
         String[] var3 = var1.split(";");
         var1 = var3[0];
         var2.use = Float.parseFloat(var3[1]);
      }

      if (var1.indexOf("destroy") == 0) {
         var1 = var1.replace("destroy ", "");
         var2.destroy = true;
      }

      if (var1.equals("null")) {
         var2.getItems().clear();
         var2.originalItems.clear();
      } else if (var1.contains("/")) {
         var1 = var1.replaceFirst("keep ", "").trim();
         var2.getItems().addAll(Arrays.asList(var1.split("/")));
         var2.originalItems.addAll(Arrays.asList(var1.split("/")));
      } else {
         var2.getItems().add(var1);
         var2.originalItems.add(var1);
      }

      if (!var1.isEmpty()) {
         this.Source.add(var2);
      }

   }

   public void DoResult(String var1) {
      Result var2 = new Result();
      String[] var3;
      if (var1.contains("=")) {
         var3 = var1.split("=");
         var1 = var3[0].trim();
         var2.count = Integer.parseInt(var3[1].trim());
      }

      if (var1.contains(";")) {
         var3 = var1.split(";");
         var1 = var3[0].trim();
         var2.drainableCount = Integer.parseInt(var3[1].trim());
      }

      if (var1.contains(".")) {
         var2.type = var1.split("\\.")[1];
         var2.module = var1.split("\\.")[0];
      } else {
         var2.type = var1;
      }

      if (this.Result == null) {
         this.Result = var2;
      }

      this.Results.add(var2);
   }

   public int getNumberOfNeededItem() {
      int var1 = 0;

      for(int var2 = 0; var2 < this.getSource().size(); ++var2) {
         Source var3 = (Source)this.getSource().get(var2);
         if (!var3.getItems().isEmpty()) {
            var1 = (int)((float)var1 + var3.getCount());
         }
      }

      return var1;
   }

   public ArrayList<String> getRequiredSkills() {
      ArrayList var1 = null;
      if (this.skillRequired != null) {
         var1 = new ArrayList();

         for(int var2 = 0; var2 < this.skillRequired.size(); ++var2) {
            RequiredSkill var3 = (RequiredSkill)this.skillRequired.get(var2);
            PerkFactory.Perk var4 = PerkFactory.getPerk(var3.perk);
            if (var4 == null) {
               var1.add(var3.perk.name + " " + var3.level);
            } else {
               String var5 = var4.name + " " + var3.level;
               var1.add(var5);
            }
         }
      }

      return var1;
   }

   public int getRequiredSkillCount() {
      return this.skillRequired == null ? 0 : this.skillRequired.size();
   }

   public RequiredSkill getRequiredSkill(int var1) {
      return this.skillRequired != null && var1 >= 0 && var1 < this.skillRequired.size() ? (RequiredSkill)this.skillRequired.get(var1) : null;
   }

   public void clearRequiredSkills() {
      if (this.skillRequired != null) {
         this.skillRequired.clear();
      }
   }

   public void addRequiredSkill(PerkFactory.Perk var1, int var2) {
      if (this.skillRequired == null) {
         this.skillRequired = new ArrayList();
      }

      this.skillRequired.add(new RequiredSkill(var1, var2));
   }

   public Source findSource(String var1) {
      for(int var2 = 0; var2 < this.Source.size(); ++var2) {
         Source var3 = (Source)this.Source.get(var2);

         for(int var4 = 0; var4 < var3.getItems().size(); ++var4) {
            if (((String)var3.getItems().get(var4)).equals(var1)) {
               return var3;
            }
         }
      }

      return null;
   }

   public ArrayList<Source> getSource() {
      return this.Source;
   }

   public String getOriginalname() {
      return this.originalname;
   }

   public void setOriginalname(String var1) {
      this.originalname = var1;
   }

   public String getFullType() {
      return this.getModule().name + "." + this.originalname;
   }

   public String getName() {
      return this.name;
   }

   public float getHeat() {
      return this.heat;
   }

   public Result getResult() {
      return this.Result;
   }

   /** @deprecated */
   @Deprecated
   public String getNearItem() {
      return this.requiredNearObject;
   }

   /** @deprecated */
   @Deprecated
   public void setNearItem(String var1) {
      this.requiredNearObject = var1;
   }

   public ArrayList<Result> getResults() {
      return this.Results;
   }

   public static final class Result {
      public String module = null;
      public String type;
      public int count = 1;
      public int drainableCount = 0;

      public Result() {
      }

      public String getType() {
         return this.type;
      }

      public void setType(String var1) {
         this.type = var1;
      }

      public int getCount() {
         return this.count;
      }

      public void setCount(int var1) {
         this.count = var1;
      }

      public String getModule() {
         return this.module;
      }

      public void setModule(String var1) {
         this.module = var1;
      }

      public String getFullType() {
         return this.module + "." + this.type;
      }

      public int getDrainableCount() {
         return this.drainableCount;
      }

      public void setDrainableCount(int var1) {
         this.drainableCount = var1;
      }
   }

   public static final class Source {
      public boolean keep = false;
      private final ArrayList<String> items = new ArrayList();
      private final ArrayList<String> originalItems = new ArrayList();
      public boolean destroy = false;
      public float count = 1.0F;
      public float use = 0.0F;

      public Source() {
      }

      public boolean isDestroy() {
         return this.destroy;
      }

      public void setDestroy(boolean var1) {
         this.destroy = var1;
      }

      public boolean isKeep() {
         return this.keep;
      }

      public void setKeep(boolean var1) {
         this.keep = var1;
      }

      public float getCount() {
         return this.count;
      }

      public void setCount(float var1) {
         this.count = var1;
      }

      public float getUse() {
         return this.use;
      }

      public void setUse(float var1) {
         this.use = var1;
      }

      public ArrayList<String> getItems() {
         return this.items;
      }

      public ArrayList<String> getOriginalItems() {
         return this.originalItems;
      }

      public String getOnlyItem() {
         if (this.items.size() != 1) {
            throw new RuntimeException("items.size() == " + this.items.size());
         } else {
            return (String)this.items.get(0);
         }
      }
   }

   public static final class RequiredSkill {
      private final PerkFactory.Perk perk;
      private final int level;

      public RequiredSkill(PerkFactory.Perk var1, int var2) {
         this.perk = var1;
         this.level = var2;
      }

      public PerkFactory.Perk getPerk() {
         return this.perk;
      }

      public int getLevel() {
         return this.level;
      }
   }

   public static enum LuaCall {
      LuaAttributes,
      LuaTest,
      LuaCreate,
      LuaGrab,
      LuaCanPerform,
      LuaGiveXP;

      private LuaCall() {
      }
   }
}
