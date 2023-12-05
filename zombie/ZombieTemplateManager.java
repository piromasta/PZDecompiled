package zombie;

import java.util.ArrayList;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.textures.Texture;

public class ZombieTemplateManager {
   public ZombieTemplateManager() {
   }

   public Texture addOverlayToTexture(ArrayList<BodyOverlay> var1, Texture var2) {
      return null;
   }

   public class ZombieTemplate {
      public Texture tex;

      public ZombieTemplate() {
      }
   }

   public class BodyOverlay {
      public BodyPartType location;
      public OverlayType type;

      public BodyOverlay() {
      }
   }

   public static enum OverlayType {
      BloodLight,
      BloodMedium,
      BloodHeavy;

      private OverlayType() {
      }
   }
}
