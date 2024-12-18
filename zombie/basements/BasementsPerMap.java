package zombie.basements;

import java.util.ArrayList;
import java.util.HashMap;

public final class BasementsPerMap {
   final String mapID;
   final ArrayList<BasementDefinition> basementDefinitions = new ArrayList();
   final HashMap<String, BasementDefinition> basementDefinitionByName = new HashMap();
   final ArrayList<BasementDefinition> basementAccessDefinitions = new ArrayList();
   final HashMap<String, BasementDefinition> basementAccessDefinitionByName = new HashMap();
   final ArrayList<BasementSpawnLocation> basementSpawnLocations = new ArrayList();

   public BasementsPerMap(String var1) {
      this.mapID = var1;
   }
}
