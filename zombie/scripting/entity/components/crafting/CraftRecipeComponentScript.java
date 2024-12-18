package zombie.scripting.entity.components.crafting;

import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

public class CraftRecipeComponentScript extends ComponentScript {
   private CraftRecipe craftRecipe;

   private CraftRecipeComponentScript() {
      super(ComponentType.CraftRecipe);
   }

   protected void copyFrom(ComponentScript var1) {
   }

   public boolean isoMasterOnly() {
      return true;
   }

   public void PreReload() {
      this.craftRecipe = null;
   }

   public void reset() {
   }

   public void InitLoadPP(String var1) {
      super.InitLoadPP(var1);
   }

   public void OnScriptsLoaded(ScriptLoadMode var1) throws Exception {
      super.OnScriptsLoaded(var1);
      this.craftRecipe.OnScriptsLoaded(var1);
   }

   public void OnLoadedAfterLua() throws Exception {
      this.craftRecipe.OnLoadedAfterLua();
      this.craftRecipe.overrideTranslationName(this.getTranslationName());
      this.craftRecipe.overrideIconTexture(this.getIconTexture());
   }

   public void OnPostWorldDictionaryInit() throws Exception {
      this.craftRecipe.OnPostWorldDictionaryInit();
   }

   protected void load(ScriptParser.Block var1) throws Exception {
      this.craftRecipe = new CraftRecipe();
      this.craftRecipe.InitLoadPP(this.getScriptObjectName());
      this.craftRecipe.setModule(this.getModule());
      var1.addValue("tags", "EntityRecipe");
      this.craftRecipe.Load(this.getScriptObjectName(), var1);
   }

   public CraftRecipe getCraftRecipe() {
      return this.craftRecipe;
   }

   private Texture getIconTexture() {
      Texture var1 = this.craftRecipe.getIconTexture();
      GameEntityScript var2 = (GameEntityScript)this.getParent();
      UiConfigScript var3 = (UiConfigScript)var2.getComponentScriptFor(ComponentType.UiConfig);
      SpriteConfigScript var4 = (SpriteConfigScript)var2.getComponentScriptFor(ComponentType.SpriteConfig);
      String var5;
      if (var3 != null) {
         var5 = var3.getXuiSkinName();
         String var6 = var3.getEntityStyle();
         XuiSkin var7 = XuiManager.GetSkin(var5);
         if (var7 == null) {
            var7 = XuiManager.GetDefaultSkin();
         }

         if (var7 != null) {
            XuiSkin.EntityUiStyle var8 = var7.getEntityUiStyle(var6);
            if (var8 != null && var8.getIcon() != null) {
               var1 = var8.getIcon();
            }
         }
      } else if (var4 != null) {
         var5 = var4.getAllTileNames().size() > 0 ? (String)var4.getAllTileNames().get(0) : "default";
         var1 = Texture.getSharedTexture(var5);
      }

      return var1;
   }

   private String getTranslationName() {
      String var1 = Translator.getText("EC_Entity_DisplayName_Default");
      GameEntityScript var2 = (GameEntityScript)this.getParent();
      UiConfigScript var3 = (UiConfigScript)var2.getComponentScriptFor(ComponentType.UiConfig);
      if (var3 != null) {
         String var4 = var3.getXuiSkinName();
         String var5 = var3.getEntityStyle();
         XuiSkin var6 = XuiManager.GetSkin(var4);
         if (var6 == null) {
            var6 = XuiManager.GetDefaultSkin();
         }

         if (var6 != null) {
            XuiSkin.EntityUiStyle var7 = var6.getEntityUiStyle(var5);
            if (var7 != null && var7.getDisplayName() != null) {
               var1 = var7.getDisplayName();
            }
         }
      } else {
         var1 = this.craftRecipe.getTranslationName();
      }

      return var1;
   }

   public String getBuildCategory() {
      return this.craftRecipe.getTags().size() > 0 ? (String)this.craftRecipe.getTags().get(0) : null;
   }
}
