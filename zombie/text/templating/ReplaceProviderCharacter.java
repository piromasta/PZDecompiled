package zombie.text.templating;

import zombie.characters.IsoGameCharacter;

public class ReplaceProviderCharacter extends ReplaceProvider {
   public ReplaceProviderCharacter(final IsoGameCharacter var1) {
      this.addReplacer("firstname", new IReplace() {
         public String getString() {
            return var1 != null && var1.getDescriptor() != null && var1.getDescriptor().getForename() != null ? var1.getDescriptor().getForename() : "Bob";
         }
      });
      this.addReplacer("lastname", new IReplace() {
         public String getString() {
            return var1 != null && var1.getDescriptor() != null && var1.getDescriptor().getSurname() != null ? var1.getDescriptor().getSurname() : "Smith";
         }
      });
   }
}
