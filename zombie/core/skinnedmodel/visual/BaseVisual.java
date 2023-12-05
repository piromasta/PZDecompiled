package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.skinnedmodel.model.Model;
import zombie.scripting.objects.ModelScript;

public abstract class BaseVisual {
   public BaseVisual() {
   }

   public abstract void save(ByteBuffer var1) throws IOException;

   public abstract void load(ByteBuffer var1, int var2) throws IOException;

   public abstract Model getModel();

   public abstract ModelScript getModelScript();

   public abstract void clear();

   public abstract void copyFrom(BaseVisual var1);

   public abstract void dressInNamedOutfit(String var1, ItemVisuals var2);
}
