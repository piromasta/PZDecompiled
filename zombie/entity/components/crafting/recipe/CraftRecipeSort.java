package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import zombie.characters.IsoGameCharacter;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.resources.Resource;
import zombie.inventory.InventoryItem;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class CraftRecipeSort {
   private static final Comparator<CraftRecipe> alphaNumComparator = new Comparator<CraftRecipe>() {
      public int compare(CraftRecipe var1, CraftRecipe var2) {
         return var1.getTranslationName().compareTo(var2.getTranslationName());
      }
   };

   public CraftRecipeSort() {
   }

   public static List<CraftRecipe> alphaNumeric(List<CraftRecipe> var0) {
      var0.sort(alphaNumComparator);
      return var0;
   }

   public static List<CraftRecipe> validRecipes(List<CraftRecipe> var0, IsoGameCharacter var1) {
      var0.sort(new ValidRecipeComparator(var0, var1));
      return var0;
   }

   public static List<CraftRecipe> canPerformAndValidRecipes(List<CraftRecipe> var0, IsoGameCharacter var1, ArrayList<Resource> var2, ArrayList<InventoryItem> var3) {
      var0.sort(new ValidCanPerformRecipeComparator(var0, var1, var2, var3));
      return var0;
   }

   public static class ValidRecipeComparator implements Comparator<CraftRecipe> {
      private final HashSet<CraftRecipe> isValidCache = new HashSet();

      public ValidRecipeComparator(List<CraftRecipe> var1, IsoGameCharacter var2) {
         for(int var3 = 0; var3 < var1.size(); ++var3) {
            CraftRecipe var4 = (CraftRecipe)var1.get(var3);
            if (CraftRecipeManager.isValidRecipeForCharacter(var4, var2, (CraftRecipeMonitor)null)) {
               this.isValidCache.add(var4);
            }
         }

      }

      public int compare(CraftRecipe var1, CraftRecipe var2) {
         boolean var3 = this.isValidCache.contains(var1);
         boolean var4 = this.isValidCache.contains(var2);
         if (var3 && !var4) {
            return -1;
         } else {
            return !var3 && var4 ? 1 : var1.getTranslationName().compareTo(var2.getTranslationName());
         }
      }
   }

   public static class ValidCanPerformRecipeComparator implements Comparator<CraftRecipe> {
      private final HashSet<CraftRecipe> isValidCache = new HashSet();
      private final HashSet<CraftRecipe> canPerformCache = new HashSet();

      public ValidCanPerformRecipeComparator(List<CraftRecipe> var1, IsoGameCharacter var2, ArrayList<Resource> var3, ArrayList<InventoryItem> var4) {
         CraftRecipeData var5 = new CraftRecipeData(CraftMode.Handcraft, true, true, false, true);

         for(int var6 = 0; var6 < var1.size(); ++var6) {
            CraftRecipe var7 = (CraftRecipe)var1.get(var6);
            if (CraftRecipeManager.isValidRecipeForCharacter(var7, var2, (CraftRecipeMonitor)null)) {
               this.isValidCache.add(var7);
            }

            var5.setRecipe(var7);
            if (var5.canPerform(var2, var3, var4)) {
               this.canPerformCache.add(var7);
            }
         }

      }

      public int compare(CraftRecipe var1, CraftRecipe var2) {
         boolean var3 = this.isValidCache.contains(var1);
         boolean var4 = this.isValidCache.contains(var2);
         if (var3 && !var4) {
            return -1;
         } else if (!var3 && var4) {
            return 1;
         } else {
            boolean var5 = this.canPerformCache.contains(var1);
            boolean var6 = this.canPerformCache.contains(var2);
            if (var5 && !var6) {
               return -1;
            } else {
               return !var5 && var6 ? 1 : var1.getTranslationName().compareTo(var2.getTranslationName());
            }
         }
      }
   }
}
