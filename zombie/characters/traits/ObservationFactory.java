package zombie.characters.traits;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.interfaces.IListBoxItem;

public final class ObservationFactory {
   public static HashMap<String, Observation> ObservationMap = new HashMap();

   public ObservationFactory() {
   }

   public static void init() {
   }

   public static void setMutualExclusive(String var0, String var1) {
      ((Observation)ObservationMap.get(var0)).MutuallyExclusive.add(var1);
      ((Observation)ObservationMap.get(var1)).MutuallyExclusive.add(var0);
   }

   public static void addObservation(String var0, String var1, String var2) {
      ObservationMap.put(var0, new Observation(var0, var1, var2));
   }

   public static Observation getObservation(String var0) {
      return ObservationMap.containsKey(var0) ? (Observation)ObservationMap.get(var0) : null;
   }

   public static class Observation implements IListBoxItem {
      private String traitID;
      private String name;
      private String description;
      public ArrayList<String> MutuallyExclusive = new ArrayList(0);

      public Observation(String var1, String var2, String var3) {
         this.setTraitID(var1);
         this.setName(var2);
         this.setDescription(var3);
      }

      public String getLabel() {
         return this.getName();
      }

      public String getLeftLabel() {
         return this.getName();
      }

      public String getRightLabel() {
         return null;
      }

      public String getDescription() {
         return this.description;
      }

      public void setDescription(String var1) {
         this.description = var1;
      }

      public String getTraitID() {
         return this.traitID;
      }

      public void setTraitID(String var1) {
         this.traitID = var1;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String var1) {
         this.name = var1;
      }
   }
}
