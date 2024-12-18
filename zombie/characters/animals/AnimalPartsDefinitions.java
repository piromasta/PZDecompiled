package zombie.characters.animals;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;

public class AnimalPartsDefinitions {
   public AnimalPartsDefinitions() {
   }

   public static String getLeather(String var0) {
      KahluaTableImpl var1 = getAnimalDef(var0);
      return var1 == null ? null : var1.rawgetStr("leather");
   }

   public static ArrayList<AnimalPart> getAllPartsDef(String var0) {
      KahluaTableImpl var1 = getAnimalDef(var0);
      return var1 == null ? new ArrayList() : getDef(var1, "parts");
   }

   public static ArrayList<AnimalPart> getAllBonesDef(String var0) {
      KahluaTableImpl var1 = getAnimalDef(var0);
      return var1 == null ? new ArrayList() : getDef(var1, "bones");
   }

   public static ArrayList<AnimalPart> getDef(KahluaTableImpl var0, String var1) {
      ArrayList var2 = new ArrayList();
      KahluaTableImpl var3 = (KahluaTableImpl)var0.rawget(var1);
      if (var3 == null) {
         return var2;
      } else {
         KahluaTableIterator var4 = var3.iterator();

         while(var4.advance()) {
            KahluaTableImpl var5 = (KahluaTableImpl)var4.getValue();
            AnimalPart var6 = new AnimalPart(var5);
            var2.add(var6);
         }

         return var2;
      }
   }

   public static KahluaTableImpl getAnimalDef(String var0) {
      KahluaTableImpl var1 = (KahluaTableImpl)LuaManager.env.rawget("AnimalPartsDefinitions");
      if (var1 == null) {
         return null;
      } else {
         KahluaTableImpl var2 = (KahluaTableImpl)var1.rawget("animals");
         if (var2 == null) {
            return null;
         } else {
            KahluaTableIterator var3 = var2.iterator();

            String var4;
            do {
               if (!var3.advance()) {
                  return null;
               }

               var4 = var3.getKey().toString();
            } while(!var0.equals(var4));

            return (KahluaTableImpl)var3.getValue();
         }
      }
   }
}
