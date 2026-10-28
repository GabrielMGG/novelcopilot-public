import {
  Church,
  Circle,
  Coins,
  KeyRound,
  Landmark,
  MapPin,
  PawPrint,
  Scale,
  ScrollText,
  Shield,
  Sparkles,
  UsersRound,
} from "lucide-react";
import type { WorldNoteCategory } from "../../types/worldNote";

type WorldNoteCategoryIconProps = {
  category: WorldNoteCategory;
};

export function WorldNoteCategoryIcon({ category }: WorldNoteCategoryIconProps) {
  const iconProps = {
    size: 16,
    strokeWidth: 2,
  };

  switch (category) {
    case "LOCATION":
      return <MapPin {...iconProps} />;

    case "FACTION":
      return <UsersRound {...iconProps} />;

    case "CULTURE":
      return <Landmark {...iconProps} />;

    case "HISTORY":
      return <ScrollText {...iconProps} />;

    case "MAGIC_SYSTEM":
      return <Sparkles {...iconProps} />;

    case "RULE":
      return <Scale {...iconProps} />;

    case "CREATURE":
      return <PawPrint {...iconProps} />;

    case "ITEM":
      return <KeyRound {...iconProps} />;

    case "RELIGION":
      return <Church {...iconProps} />;

    case "POLITICS":
      return <Shield {...iconProps} />;

    case "ECONOMY":
      return <Coins {...iconProps} />;

    case "OTHER":
    default:
      return <Circle {...iconProps} />;
  }
}
