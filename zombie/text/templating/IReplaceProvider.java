package zombie.text.templating;

public interface IReplaceProvider {
   boolean hasReplacer(String var1);

   IReplace getReplacer(String var1);
}
