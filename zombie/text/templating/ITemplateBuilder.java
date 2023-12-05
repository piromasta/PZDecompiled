package zombie.text.templating;

import se.krka.kahlua.j2se.KahluaTableImpl;

public interface ITemplateBuilder {
   String Build(String var1);

   String Build(String var1, IReplaceProvider var2);

   String Build(String var1, KahluaTableImpl var2);

   void RegisterKey(String var1, KahluaTableImpl var2);

   void RegisterKey(String var1, IReplace var2);

   void Reset();
}
