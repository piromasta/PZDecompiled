package zombie.entity.debug;

import java.util.ArrayList;
import java.util.Arrays;

public enum EntityDebugTestType {
   BaseTest;

   private static final ArrayList<EntityDebugTestType> typeList = new ArrayList();

   private EntityDebugTestType() {
   }

   public static ArrayList<EntityDebugTestType> getValueList() {
      return typeList;
   }

   static {
      typeList.addAll(Arrays.asList(values()));
   }
}
