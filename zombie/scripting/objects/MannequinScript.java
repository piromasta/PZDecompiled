package zombie.scripting.objects;

import java.util.Iterator;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class MannequinScript extends BaseScriptObject {
   private String name;
   private boolean bFemale = true;
   private String modelScriptName;
   private String texture;
   private String animSet;
   private String animState;
   private String pose;
   private String outfit;

   public MannequinScript() {
   }

   public String getName() {
      return this.name;
   }

   public boolean isFemale() {
      return this.bFemale;
   }

   public void setFemale(boolean var1) {
      this.bFemale = var1;
   }

   public String getModelScriptName() {
      return this.modelScriptName;
   }

   public void setModelScriptName(String var1) {
      this.modelScriptName = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getTexture() {
      return this.texture;
   }

   public void setTexture(String var1) {
      this.texture = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getAnimSet() {
      return this.animSet;
   }

   public void setAnimSet(String var1) {
      this.animSet = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getAnimState() {
      return this.animState;
   }

   public void setAnimState(String var1) {
      this.animState = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getPose() {
      return this.pose;
   }

   public void setPose(String var1) {
      this.pose = StringUtils.discardNullOrWhitespace(var1);
   }

   public String getOutfit() {
      return this.outfit;
   }

   public void setOutfit(String var1) {
      this.outfit = StringUtils.discardNullOrWhitespace(var1);
   }

   public void Load(String var1, String var2) {
      this.name = var1;
      ScriptParser.Block var3 = ScriptParser.parse(var2);
      var3 = (ScriptParser.Block)var3.children.get(0);
      Iterator var4 = var3.values.iterator();

      while(var4.hasNext()) {
         ScriptParser.Value var5 = (ScriptParser.Value)var4.next();
         String var6 = var5.getKey().trim();
         String var7 = var5.getValue().trim();
         if ("female".equalsIgnoreCase(var6)) {
            this.bFemale = StringUtils.tryParseBoolean(var7);
         } else if ("model".equalsIgnoreCase(var6)) {
            this.modelScriptName = StringUtils.discardNullOrWhitespace(var7);
         } else if ("texture".equalsIgnoreCase(var6)) {
            this.texture = StringUtils.discardNullOrWhitespace(var7);
         } else if ("animSet".equalsIgnoreCase(var6)) {
            this.animSet = StringUtils.discardNullOrWhitespace(var7);
         } else if ("animState".equalsIgnoreCase(var6)) {
            this.animState = StringUtils.discardNullOrWhitespace(var7);
         } else if ("pose".equalsIgnoreCase(var6)) {
            this.pose = StringUtils.discardNullOrWhitespace(var7);
         } else if ("outfit".equalsIgnoreCase(var6)) {
            this.outfit = StringUtils.discardNullOrWhitespace(var7);
         }
      }

   }

   public void reset() {
      this.modelScriptName = null;
      this.texture = null;
      this.animSet = null;
      this.animState = null;
      this.pose = null;
      this.outfit = null;
   }
}
