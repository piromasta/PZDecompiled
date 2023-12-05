package zombie.characters.AttachedItems;

import java.util.ArrayList;
import zombie.characterTextures.BloodBodyPartType;

public final class AttachedWeaponDefinition {
   public String id;
   public int chance;
   public final ArrayList<String> outfit = new ArrayList();
   public final ArrayList<String> weaponLocation = new ArrayList();
   public final ArrayList<BloodBodyPartType> bloodLocations = new ArrayList();
   public boolean addHoles;
   public int daySurvived;
   public String ensureItem;
   public final ArrayList<String> weapons = new ArrayList();

   public AttachedWeaponDefinition() {
   }
}
