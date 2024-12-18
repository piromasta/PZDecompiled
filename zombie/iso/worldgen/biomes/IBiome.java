package zombie.iso.worldgen.biomes;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public interface IBiome {
   String name();

   Map<FeatureType, List<Feature>> getFeatures();

   EnumSet<BiomeType.Landscape> landscape();

   EnumSet<BiomeType.Plant> plant();

   EnumSet<BiomeType.Bush> bush();

   EnumSet<BiomeType.Temperature> temperature();

   EnumSet<BiomeType.Hygrometry> hygrometry();

   EnumSet<BiomeType.OreLevel> oreLevel();

   List<String> placement();

   String parent();

   boolean generate();

   float zombies();

   Grass grass();

   IBiome landscape(BiomeType.Landscape var1);

   IBiome plant(BiomeType.Plant var1);

   IBiome bush(BiomeType.Bush var1);

   IBiome temperature(BiomeType.Temperature var1);

   IBiome hygrometry(BiomeType.Hygrometry var1);

   IBiome oreLevel(BiomeType.OreLevel var1);

   IBiome landscape(EnumSet<BiomeType.Landscape> var1);

   IBiome plant(EnumSet<BiomeType.Plant> var1);

   IBiome bush(EnumSet<BiomeType.Bush> var1);

   IBiome temperature(EnumSet<BiomeType.Temperature> var1);

   IBiome hygrometry(EnumSet<BiomeType.Hygrometry> var1);

   IBiome oreLevel(EnumSet<BiomeType.OreLevel> var1);

   IBiome placement(List<String> var1);

   IBiome zombies(float var1);

   IBiome grass(Grass var1);
}
