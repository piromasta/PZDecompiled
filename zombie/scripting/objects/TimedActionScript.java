package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@DebugClassFields
public class TimedActionScript extends BaseScriptObject {
   private Metabolics metabolics;
   private int time;
   private boolean faceObject;
   private String prop1;
   private String prop2;
   private String actionAnim;
   private String animVarKey;
   private String animVarVal;
   private String sound;
   private String completionSound;
   private float muscleStrainFactor;
   private ArrayList<BodyPartType> muscleStrainParts;
   private PerkFactory.Perk muscleStrainSkill;
   private boolean cantSit;

   public TimedActionScript() {
      super(ScriptType.TimedAction);
      this.metabolics = Metabolics.Default;
      this.time = -1;
      this.faceObject = false;
      this.prop1 = null;
      this.prop2 = null;
      this.actionAnim = null;
      this.animVarKey = null;
      this.animVarVal = null;
      this.sound = null;
      this.completionSound = null;
      this.muscleStrainFactor = 0.0F;
      this.muscleStrainParts = new ArrayList();
      this.muscleStrainSkill = null;
      this.cantSit = false;
   }

   public String getName() {
      return this.getScriptObjectName();
   }

   public String getFullType() {
      return this.getScriptObjectFullType();
   }

   public Metabolics getMetabolics() {
      return this.metabolics;
   }

   public int getTime() {
      return this.time;
   }

   public boolean isFaceObject() {
      return this.faceObject;
   }

   public boolean isCantSit() {
      return this.cantSit;
   }

   public String getProp1() {
      return this.prop1;
   }

   public String getProp2() {
      return this.prop2;
   }

   public String getActionAnim() {
      return this.actionAnim;
   }

   public String getAnimVarKey() {
      return this.animVarKey;
   }

   public String getAnimVarVal() {
      return this.animVarVal;
   }

   public String getSound() {
      return this.sound;
   }

   public String getCompletionSound() {
      return this.completionSound;
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void Load(String var1, String var2) throws Exception {
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      super.LoadCommonBlock(var3);
      Iterator var4 = var3.values.iterator();

      while(true) {
         while(true) {
            String var6;
            String var7;
            do {
               do {
                  if (!var4.hasNext()) {
                     return;
                  }

                  ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
                  var6 = var5.getKey().trim();
                  var7 = var5.getValue().trim();
               } while(var6.isEmpty());
            } while(var7.isEmpty());

            if (var6.equalsIgnoreCase("metabolics")) {
               this.metabolics = Metabolics.valueOf(var7);
            } else if (var6.equalsIgnoreCase("time")) {
               this.time = Integer.parseInt(var7);
            } else if (var6.equalsIgnoreCase("faceObject")) {
               this.faceObject = Boolean.parseBoolean(var7);
            } else if (var6.equalsIgnoreCase("cantSit")) {
               this.cantSit = Boolean.parseBoolean(var7);
            } else if (var6.equalsIgnoreCase("prop1")) {
               this.prop1 = var7;
            } else if (var6.equalsIgnoreCase("prop2")) {
               this.prop2 = var7;
            } else if (var6.equalsIgnoreCase("actionAnim")) {
               this.actionAnim = var7;
            } else if (var6.equalsIgnoreCase("animVarKey")) {
               this.animVarKey = var7;
            } else if (var6.equalsIgnoreCase("animVarVal")) {
               this.animVarVal = var7;
            } else if (var6.equalsIgnoreCase("sound")) {
               this.sound = var7;
            } else if (var6.equalsIgnoreCase("completionSound")) {
               this.completionSound = var7;
            } else if (var6.equalsIgnoreCase("muscleStrainFactor")) {
               this.muscleStrainFactor = Float.parseFloat(var7);
            } else if (var6.equalsIgnoreCase("muscleStrainSkill")) {
               PerkFactory.Perk var10 = PerkFactory.Perks.FromString(var7);
               if (var10 == PerkFactory.Perks.MAX) {
                  DebugLog.Recipe.warn("Unknown skill \"%s\" in timedaction script \"%s\"", var7, this);
               } else {
                  this.muscleStrainSkill = var10;
               }
            } else if (var6.equalsIgnoreCase("muscleStrainParts")) {
               this.muscleStrainParts = new ArrayList();
               String[] var8 = var7.split(";");

               for(int var9 = 0; var9 < var8.length; ++var9) {
                  this.muscleStrainParts.add(BodyPartType.FromString(var8[var9].trim()));
               }
            }
         }
      }
   }

   public void PreReload() {
      this.metabolics = Metabolics.Default;
      this.time = -1;
      this.faceObject = false;
      this.cantSit = false;
      this.prop1 = null;
      this.prop2 = null;
      this.actionAnim = null;
      this.animVarKey = null;
      this.animVarVal = null;
      this.sound = null;
      this.completionSound = null;
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
   }

   public void OnLoadedAfterLua() throws Exception {
   }

   public void OnPostWorldDictionaryInit() throws Exception {
   }

   public boolean hasMuscleStrain() {
      return this.muscleStrainParts.size() > 0 && this.muscleStrainFactor > 0.0F;
   }

   public void applyMuscleStrain(IsoGameCharacter var1) {
      if (this.hasMuscleStrain()) {
         float var2 = GameTime.instance.getMultiplier() * this.muscleStrainFactor;
         int var3;
         if (this.muscleStrainSkill != null) {
            var3 = var1.getPerkLevel(this.muscleStrainSkill);
            var2 *= (float)(1.0 - (double)var3 * 0.5);
         }

         for(var3 = 0; var3 < this.muscleStrainParts.size(); ++var3) {
            var1.addStiffness((BodyPartType)this.muscleStrainParts.get(var3), var2);
         }

      }
   }
}
