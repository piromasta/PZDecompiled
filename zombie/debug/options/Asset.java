package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Asset extends OptionGroup {
   public final BooleanDebugOption SlowLoad = this.newOption("SlowLoad", false);
   public final BooleanDebugOption CheckItemTexAndNames = this.newOption("CheckItemTexAndNames", false);

   public Asset() {
   }
}
